package ru.yandex.market.arbiter.test.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import ru.yandex.businesschat.api.client.BusinesschatClientApi;
import ru.yandex.market.arbiter.api.consumer.client.ArbiterConsumerApi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * @author moskovkin@yandex-team.ru
 * @since 09.06.2020
 */
@Configuration
public class MockConfig {
    @Bean
    public ArbiterConsumerApi mockSuperappArbiterConsumerApi() {
        ArbiterConsumerApi result = Mockito.mock(ArbiterConsumerApi.class);
        doAnswer(invocation ->
            ResponseEntity.ok().build()
        )
        .when(result).conversationVerdictPostWithHttpInfo(anyLong(), any());
        return result;
    }

    @Bean
    public BusinesschatClientApi mockBusinesschatClientApi() {
        BusinesschatClientApi result = Mockito.mock(BusinesschatClientApi.class);
        doAnswer(invocation ->
                ResponseEntity.ok().build()
        )
        .when(result).pushProviderNameChatIdPostWithHttpInfo(anyString(), anyString(), any());
        return result;
    }
}
