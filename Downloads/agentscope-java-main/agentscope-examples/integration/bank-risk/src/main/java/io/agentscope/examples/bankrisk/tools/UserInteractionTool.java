package io.agentscope.examples.bankrisk.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolSuspendException;
import java.util.List;
import java.util.Map;

public class UserInteractionTool {

    public static final String TOOL_NAME = "ask_user";

    @Tool(
            name = TOOL_NAME,
            description =
                    "Ask the user for clarification or additional information. Use 'text' for"
                        + " free-form input, 'select' for choosing one from a list (provide"
                        + " options), 'multi_select' for choosing multiple from a list, 'confirm'"
                        + " for yes/no questions, 'form' for collecting multiple fields, 'date' for"
                        + " date selection, 'number' for numeric input.")
    public String askUser(
            @ToolParam(name = "question", description = "The question to ask the user")
                    String question,
            @ToolParam(
                            name = "ui_type",
                            description =
                                    "UI type: text, select, multi_select, confirm, form, date,"
                                            + " number",
                            required = false)
                    String uiType,
            @ToolParam(
                            name = "options",
                            description = "Options for select/multi_select",
                            required = false)
                    List<String> options,
            @ToolParam(
                            name = "fields",
                            description = "Field definitions for form ui_type",
                            required = false)
                    List<Map<String, Object>> fields,
            @ToolParam(name = "default_value", description = "Default value", required = false)
                    Object defaultValue,
            @ToolParam(
                            name = "allow_other",
                            description = "Allow custom input for select/multi_select",
                            required = false)
                    Boolean allowOther) {
        String reason = question != null ? question : "Waiting for user input";
        throw new ToolSuspendException(reason);
    }
}
