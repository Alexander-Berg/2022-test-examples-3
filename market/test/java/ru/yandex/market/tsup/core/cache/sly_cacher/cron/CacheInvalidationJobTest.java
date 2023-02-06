package ru.yandex.market.tsup.core.cache.sly_cacher.cron;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.tpl.common.data_provider.provider.DataProvider;
import ru.yandex.market.tsup.core.cache.sly_cacher.service.CacheInvalidationService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CacheInvalidationJobTest {
    public static final String BEAN_NAME = "bean_name";

    private final ApplicationContext applicationContext = mock(ApplicationContext.class);
    private final CacheInvalidationService cacheInvalidationService = mock(CacheInvalidationService.class);
    private final JobExecutionContext jobExecutionContext = mock(JobExecutionContext.class);
    private final DataProvider<?, ?> dataProvider = mock(DataProvider.class);

    private final Method method = anyMethod();

    private final CacheInvalidationJob cacheInvalidationJob = new CacheInvalidationJob(
        applicationContext,
        cacheInvalidationService
    );

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            applicationContext,
            cacheInvalidationService,
            jobExecutionContext,
            dataProvider
        );
    }

    @Test
    void execute() throws JobExecutionException {
        when(jobExecutionContext.getMergedJobDataMap())
            .thenReturn(new JobDataMap(Map.of(
                CacheInvalidationJob.DATA_PROVIDER_BEAN_NAME_FIELD, BEAN_NAME
            )));
        when(applicationContext.getBean(eq(BEAN_NAME))).thenReturn(dataProvider);
        when(dataProvider.getProvideMethod()).thenReturn(Optional.of(method));

        cacheInvalidationJob.execute(jobExecutionContext);

        verify(jobExecutionContext).getMergedJobDataMap();
        verify(applicationContext).getBean(eq(BEAN_NAME));
        verify(dataProvider).getProvideMethod();
        verify(cacheInvalidationService).callInvalidationStrategy(eq(dataProvider), eq(method));
    }

    /**
     * Создать мок на метод нельзя. Тут не важно, какой именно будет метод, потому возьмём hashCode
     */
    @SneakyThrows
    @NotNull
    private Method anyMethod() {
        return Object.class.getMethod("hashCode");
    }
}
