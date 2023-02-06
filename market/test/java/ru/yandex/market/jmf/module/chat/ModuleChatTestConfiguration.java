package ru.yandex.market.jmf.module.chat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

import static org.mockito.Mockito.mock;

@Configuration
public class ModuleChatTestConfiguration extends AbstractModuleConfiguration {
    protected ModuleChatTestConfiguration() {
        super("module/chat/test");
    }

    @Bean
    @Primary
    public FeedbackClient mockFeedbackClient() {
        return mock(FeedbackClient.class);
    }

    @Bean
    @Primary
    public MessengerClient mockMessengerClient() {
        return mock(MessengerClient.class);
    }

    @Bean
    @Primary
    public ChatClientService mockChatClientService() {
        return mock(ChatClientService.class);
    }
}
