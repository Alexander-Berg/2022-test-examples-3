package ru.yandex.market.b2b.clients.mock;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;

@Configuration
public class MdsS3ClientMockConfiguration {

    @Bean
    @Primary
    public MdsS3Client mdsS3Client() {
        return Mockito.mock(MdsS3Client.class);
    }
}
