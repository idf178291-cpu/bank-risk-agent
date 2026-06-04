package io.agentscope.examples.bankriskdiagnosis.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PipelineContextTest {

    @Test
    void shouldCreateWithSessionId() {
        PipelineContext ctx = PipelineContext.create("XX银行");
        assertNotNull(ctx.getSessionId());
        assertEquals("XX银行", ctx.getBankName());
        assertEquals(PipelineContext.PipelineStatus.PENDING, ctx.getStatus());
        assertEquals(0, ctx.getCurrentStep());
        assertEquals(8, ctx.getTotalSteps());
        assertNotNull(ctx.getStartedAt());
    }

    @Test
    void shouldTrackProgress() {
        PipelineContext ctx = PipelineContext.create("BB银行");
        ctx.setCurrentStep(4);
        assertEquals(50.0, ctx.getProgressPercentage());
        ctx.setCurrentStep(8);
        assertEquals(100.0, ctx.getProgressPercentage());
    }

    @Test
    void shouldManageParsedContents() {
        PipelineContext ctx = PipelineContext.create("CC银行");
        ctx.addParsedContent("审计报告.docx", "全文内容...");
        Map<String, String> contents = ctx.getParsedContents();
        assertEquals(1, contents.size());
        assertTrue(contents.containsKey("审计报告.docx"));
    }

    @Test
    void shouldTransitionStatus() {
        PipelineContext ctx = PipelineContext.create("DD银行");
        ctx.setStatus(PipelineContext.PipelineStatus.RUNNING);
        assertEquals(PipelineContext.PipelineStatus.RUNNING, ctx.getStatus());
        ctx.setStatus(PipelineContext.PipelineStatus.COMPLETED);
        assertEquals(PipelineContext.PipelineStatus.COMPLETED, ctx.getStatus());
    }
}
