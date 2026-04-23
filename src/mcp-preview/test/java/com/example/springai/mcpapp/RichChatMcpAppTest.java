package com.example.springai.mcpapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.ai.mcp.annotation.McpTool;
import com.example.springai.chat.ChatService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

class RichChatMcpAppTest {

    private static final class StubChatService extends ChatService {
        private String answer = "stub-answer";
        private String lastMessage;
        private String lastSystemPrompt;

        StubChatService() {
            super(null, null);
        }

        void answerWith(String answer) {
            this.answer = answer;
        }

        String lastMessage() {
            return lastMessage;
        }

        String lastSystemPrompt() {
            return lastSystemPrompt;
        }

        @Override
        public String chat(String message, String systemPrompt) {
            this.lastMessage = message;
            this.lastSystemPrompt = systemPrompt;
            return answer;
        }
    }

    @Test
    void constantsRemainContractAligned() {
        assertThat(RichChatApp.RESOURCE_URI).endsWith("/rich-chat.html");
        assertThat(RichChatApp.RESOURCE_PATH).endsWith("/app/rich-chat.html");
        assertThat(RichChatApp.TOOL_NAME).isEqualTo("open-rich-chat");
        assertThat(RichChatApp.RESPONSE_TOOL_NAME).isEqualTo("rich-chat-respond");
    }

    @Test
    void resourceMethodExposesExpectedMetadataAndHtml() throws Exception {
        RichChatMcpApp app = new RichChatMcpApp(new StubChatService());
        ReflectionTestUtils.setField(app, "richChatResource", new ClassPathResource("app/rich-chat.html"));

        Method resourceMethod = RichChatMcpApp.class.getMethod("richChatResource");
        McpResource resource = resourceMethod.getAnnotation(McpResource.class);

        assertThat(resource).isNotNull();
        assertThat(resource.uri()).isEqualTo(RichChatApp.RESOURCE_URI);
        assertThat(resource.mimeType()).isEqualTo(RichChatApp.RESOURCE_MIME_TYPE);
        assertThat(app.richChatResource())
                .contains("Rich Chat Workspace")
                .contains("@modelcontextprotocol/ext-apps");

        Map<String, Object> meta = new RichChatMcpApp.CspMetaProvider().getMeta();
        assertThat(meta)
                .containsEntry("ui", Map.of("csp", Map.of("resourceDomains", List.of("https://unpkg.com"))));
    }

    @Test
    void toolMethodReferencesTheExpectedUiResource() throws Exception {
        Method toolMethod = RichChatMcpApp.class.getMethod("openRichChat");
        McpTool tool = toolMethod.getAnnotation(McpTool.class);

        assertThat(tool).isNotNull();
        assertThat(tool.name()).isEqualTo(RichChatApp.TOOL_NAME);
        assertThat(tool.description()).contains("rich chat MCP app");
        assertThat(new RichChatMcpApp(new StubChatService()).openRichChat()).isEqualTo("Opening rich chat MCP app.");
        assertThat(new RichChatMcpApp.RichChatMetaProvider().getMeta())
                .containsEntry("ui", Map.of("resourceUri", RichChatApp.RESOURCE_URI));
    }

    @Test
    void responseToolDelegatesToChatService() throws Exception {
        StubChatService chatService = new StubChatService();
        chatService.answerWith("hello there");

        RichChatMcpApp app = new RichChatMcpApp(chatService);

        Method toolMethod = RichChatMcpApp.class.getMethod("richChatRespond", String.class, String.class);
        McpTool tool = toolMethod.getAnnotation(McpTool.class);

        assertThat(tool).isNotNull();
        assertThat(tool.name()).isEqualTo(RichChatApp.RESPONSE_TOOL_NAME);
        assertThat(app.richChatRespond("hi", "be brief")).isEqualTo("hello there");
        assertThat(chatService.lastMessage()).isEqualTo("hi");
        assertThat(chatService.lastSystemPrompt()).isEqualTo("be brief");
    }
}