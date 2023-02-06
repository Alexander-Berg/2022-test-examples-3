package ru.yandex.chemodan.app.djfs.core.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;


/**
 * @author eoshch
 */
@Configuration
@Profile(ActivateInMemoryClient.PROFILE)
public class DjfsInMemoryClientContextConfiguration {
    @Bean
    @Primary
    public MockBlackbox2 mockBlackbox2() {
        return new MockBlackbox2();
    }

    @Bean
    public MockMpfsClient mockMpfsClient() {
        return new MockMpfsClient();
    }

    @Bean
    public MockDataApiHttpClient mockDataApiHttpClient() {
        return new MockDataApiHttpClient();
    }

    @Bean
    public MockDiskSearchHttpClient mockDiskSearchHttpClient() {
        return new MockDiskSearchHttpClient();
    }

    @Bean
    public MockOperationCallbackHttpClient operationCallbackHttpClient()
    {
        return new MockOperationCallbackHttpClient();
    }

    @Bean
    public MockLogReaderHttpClient mockLogReaderHttpClient() {
        return new MockLogReaderHttpClient();
    }
}
