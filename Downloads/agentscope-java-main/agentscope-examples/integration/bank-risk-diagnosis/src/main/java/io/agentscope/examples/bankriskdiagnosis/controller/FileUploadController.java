package io.agentscope.examples.bankriskdiagnosis.controller;

import io.agentscope.examples.bankriskdiagnosis.service.DocumentParserService;
import io.agentscope.examples.bankriskdiagnosis.service.DocumentParserService.ParsedDocument;
import io.agentscope.examples.bankriskdiagnosis.service.SessionStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
            @RequestPart(value = "bankName", required = false) String bankName,
            @RequestPart(value = "sessionId", required = false) String existingSessionId) {

        final String bank = (bankName == null || bankName.isBlank()) ? "未命名银行" : bankName;
        // Reuse existing session for supplement uploads, or create new
        final String sessionId;
        if (existingSessionId != null && !existingSessionId.isBlank()) {
            sessionId = existingSessionId;
            log.info("Supplement upload to existing session {}", sessionId);
        } else {
            sessionId = sessions.create(bank);
        }

        return files.flatMap(
                        file ->
                                file.content()
                                        .map(
                                                buf -> {
                                                    byte[] bytes =
                                                            new byte[buf.readableByteCount()];
                                                    buf.read(bytes);
                                                    return bytes;
                                                })
                                        .reduce(
                                                new byte[0],
                                                (a, b) -> {
                                                    byte[] r = new byte[a.length + b.length];
                                                    System.arraycopy(a, 0, r, 0, a.length);
                                                    System.arraycopy(b, 0, r, a.length, b.length);
                                                    return r;
                                                })
                                        .map(allBytes -> new FileBytes(file.filename(), allBytes)))
                .collectList()
                .map(
                        fileBytesList -> {
                            List<Map<String, String>> results = new ArrayList<>();
                            for (FileBytes fb : fileBytesList) {
                                try {
                                    ParsedDocument doc = parser.parse(fb.name(), fb.bytes());
                                    sessions.get(sessionId)
                                            .addParsedContent(fb.name(), doc.content());
                                    results.add(
                                            Map.of(
                                                    "fileName",
                                                    fb.name(),
                                                    "status",
                                                    "OK",
                                                    "fileType",
                                                    doc.fileType().name(),
                                                    "charCount",
                                                    String.valueOf(doc.content().length())));
                                } catch (Exception e) {
                                    log.error("Parse failed: {}", fb.name(), e);
                                    results.add(
                                            Map.of(
                                                    "fileName",
                                                    fb.name(),
                                                    "status",
                                                    "FAIL",
                                                    "error",
                                                    e.getMessage()));
                                }
                            }
                            Map<String, Object> resp = new LinkedHashMap<>();
                            resp.put("sessionId", sessionId);
                            resp.put("bankName", bank);
                            resp.put("filesProcessed", results.size());
                            resp.put("results", results);
                            return ResponseEntity.ok(resp);
                        });
    }

    private record FileBytes(String name, byte[] bytes) {}
}
