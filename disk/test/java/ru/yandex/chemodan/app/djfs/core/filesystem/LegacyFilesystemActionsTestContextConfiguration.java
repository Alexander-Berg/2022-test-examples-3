package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LegacyFilesystemActionsTestContextConfiguration {
    @Bean
    @Primary
    SupportDao supportDaoProxy() {
        return Mockito.mock(SupportDao.class);
    }

}
