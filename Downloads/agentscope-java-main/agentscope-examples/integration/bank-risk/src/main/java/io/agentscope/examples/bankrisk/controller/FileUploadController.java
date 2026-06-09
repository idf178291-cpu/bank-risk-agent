package io.agentscope.examples.bankrisk.controller;

import io.agentscope.examples.bankrisk.model.InstitutionSession;
import io.agentscope.examples.bankrisk.service.DocumentParserService;
import io.agentscope.examples.bankrisk.service.SessionStore;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final Logger log = LoggerFactory.getLogger(FileUploadController.class);

    private final DocumentParserService parser;
    private final SessionStore sessions;

    public FileUploadController(DocumentParserService parser, SessionStore sessions) {
        this.parser = parser;
        this.sessions = sessions;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> uploadFiles(
            @RequestPart("files") Flux<FilePart> files,
            @RequestPart(value = "sessionId", required = false) String sessionId) {

        InstitutionSession session;
        if (sessionId != null && !sessionId.isBlank() && sessions.exists(sessionId)) {
            session = sessions.get(sessionId);
            log.info("Appending to existing session: {}", sessionId);
        } else {
            session = sessions.create();
            log.info("Created new session: {}", session.getSessionId());
        }

        final InstitutionSession finalSession = session;

        return files.flatMap(
                        filePart ->
                                DataBufferUtils.join(filePart.content())
                                        .publishOn(Schedulers.boundedElastic())
                                        .map(
                                                buf -> {
                                                    byte[] bytes =
                                                            new byte[buf.readableByteCount()];
                                                    buf.read(bytes);
                                                    DataBufferUtils.release(buf);
                                                    return bytes;
                                                })
                                        .map(
                                                bytes -> {
                                                    var result =
                                                            parser.parse(
                                                                    filePart.filename(), bytes);
                                                    finalSession.addDocument(
                                                            filePart.filename(), result.content());
                                                    Map<String, Object> m = new LinkedHashMap<>();
                                                    m.put("fileName", filePart.filename());
                                                    m.put("fileType", result.fileType());
                                                    m.put("charCount", result.content().length());
                                                    m.put(
                                                            "status",
                                                            result.content().isEmpty()
                                                                    ? "FAIL"
                                                                    : "OK");
                                                    return m;
                                                })
                                        .onErrorResume(
                                                e -> {
                                                    log.error(
                                                            "Failed to parse {}: {}",
                                                            filePart.filename(),
                                                            e.getMessage());
                                                    Map<String, Object> err = new LinkedHashMap<>();
                                                    err.put("fileName", filePart.filename());
                                                    err.put("status", "FAIL");
                                                    err.put("error", e.getMessage());
                                                    return Mono.just(err);
                                                }))
                .collectList()
                .map(
                        results -> {
                            Map<String, Object> response = new LinkedHashMap<>();
                            response.put("sessionId", finalSession.getSessionId());
                            response.put("totalFiles", finalSession.getTotalFiles());
                            response.put("totalCharCount", finalSession.getTotalCharCount());
                            response.put("files", results);
                            return ResponseEntity.ok(response);
                        });
    }
}
