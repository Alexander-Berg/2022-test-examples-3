package ru.yandex.direct.jobs.configuration;

import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.bannerstorage.client.BannerStorageClient;

@Configuration
@Import(JobsTestingConfiguration.class)
public class JobsTestingSpyConfiguration {

    /* Если это поле сделать в JobsTestingConfiguration то получается ошибка
        Error creating bean with name 'bannerStorageClient':
        Requested bean is currently in creation: Is there an unresolvable circular reference?
        https://paste.yandex-team.ru/9751196
    */
    @SpyBean
    public BannerStorageClient bannerStorageClient;
}
