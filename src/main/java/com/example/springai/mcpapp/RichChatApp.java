package com.example.springai.mcpapp;

/**
 * Planned MCP App identifiers for Pattern 6.
 *
 * <p>This class intentionally contains only constants until the repository is upgraded
 * to the Spring AI MCP-capable line and the actual {@code @McpTool}/{@code @McpResource}
 * implementation is introduced.
 */
public final class RichChatApp {

    public static final String TOOL_NAME = "open-rich-chat";
    public static final String RESPONSE_TOOL_NAME = "rich-chat-respond";
    public static final String RESOURCE_URI = "ui://chat/rich-chat.html";
    public static final String RESOURCE_PATH = "classpath:/app/rich-chat.html";
    public static final String RESOURCE_MIME_TYPE = "text/html;profile=mcp-app";

    private RichChatApp() {
    }
}