package io.agentscope.examples.bankriskdiagnosis.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolSuspendException;
import java.util.List;
import java.util.Map;

public class UserInteractionTool {

    @Tool(
            name = "ask_user",
            description =
                    "Ask the user for clarification or additional information. Use 'text' for"
                            + " free-form input, 'select' for choosing one from a list,"
                            + " 'multi_select' for choosing multiple from a list, 'confirm' for"
                            + " yes/no questions, 'form' for collecting multiple fields.")
    public String askUser(
            @ToolParam(name = "question", description = "The question to ask the user")
                    String question,
            @ToolParam(
                            name = "ui_type",
                            description = "UI type: text, select, multi_select, confirm, form",
                            required = false)
                    String uiType,
            @ToolParam(
                            name = "options",
                            description =
                                    "Options for select/multi_select. Each option is"
                                            + " {label: string, value: string}",
                            required = false)
                    List<Map<String, String>> options,
            @ToolParam(
                            name = "fields",
                            description = "Field definitions for form ui_type",
                            required = false)
                    List<Map<String, Object>> fields) {
        String reason = question != null ? question : "Waiting for user input";
        throw new ToolSuspendException(reason);
    }
}
