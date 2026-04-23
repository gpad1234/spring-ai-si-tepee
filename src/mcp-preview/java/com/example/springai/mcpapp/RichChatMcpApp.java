package com.example.springai.mcpapp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.mcp.annotation.context.MetaProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.example.springai.chat.ChatService;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Minimal Pattern 6 MCP App skeleton.
 *
 * <p>This lives in a profile-scoped source set so the default build stays on the current
 * Spring AI line while the MCP preview profile carries the real annotation-based server
 * implementation.
 */
@Service
public class RichChatMcpApp {

    private final ChatService chatService;

    @Autowired
    public RichChatMcpApp(ChatService chatService) {
        this.chatService = chatService;
    }

    @Value(RichChatApp.RESOURCE_PATH)
    private Resource richChatResource;

    @McpResource(
            name = "Rich Chat App Resource",
            title = "Rich Chat App Resource",
            uri = RichChatApp.RESOURCE_URI,
            description = "Embeds a lightweight rich chat MCP application.",
            mimeType = RichChatApp.RESOURCE_MIME_TYPE,
            metaProvider = CspMetaProvider.class)
    public String richChatResource() throws IOException {
        return richChatResource.getContentAsString(Charset.defaultCharset());
    }

    @McpTool(
            title = "Open Rich Chat",
            name = RichChatApp.TOOL_NAME,
            description = "Opens the rich chat MCP app so the user can interact with a focused UI inside the host.",
            metaProvider = RichChatMetaProvider.class)
    public String openRichChat() {
        return "Opening rich chat MCP app.";
    }

    @McpTool(
            title = "Rich Chat Respond",
            name = RichChatApp.RESPONSE_TOOL_NAME,
            description = "Generates a backend chat response using the same ChatService as the REST chat pattern.")
    public String richChatRespond(
            @McpToolParam(description = "User message to send to the assistant", required = true) String message,
            @McpToolParam(description = "Optional system prompt override", required = false) String systemPrompt) {
        return chatService.chat(message, systemPrompt);
    }

    public static final class CspMetaProvider implements MetaProvider {
        @Override
        public Map<String, Object> getMeta() {
            return Map.of("ui",
                    Map.of("csp",
                            Map.of("resourceDomains",
                                    List.of("https://unpkg.com"))));
        }
    }

    public static final class RichChatMetaProvider implements MetaProvider {
        @Override
        public Map<String, Object> getMeta() {
            return Map.of("ui", Map.of("resourceUri", RichChatApp.RESOURCE_URI));
        }
    }
}