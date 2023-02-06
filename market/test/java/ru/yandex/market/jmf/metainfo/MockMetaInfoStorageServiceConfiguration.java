package ru.yandex.market.jmf.metainfo;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MockMetaInfoStorageServiceConfiguration {
    @Bean
    @Primary
    public MetaInfoStorageService mockMetaInfoStorageService() {
        MetaInfoStorageService mock = Mockito.mock(MetaInfoStorageService.class);
        Mockito.when(mock.getAll(Mockito.anyString(), Mockito.any()))
                .thenReturn(List.of());

        return mock;
    }
}
