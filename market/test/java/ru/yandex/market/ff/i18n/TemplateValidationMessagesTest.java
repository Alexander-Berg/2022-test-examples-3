package ru.yandex.market.ff.i18n;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.repository.TankerEntryRepository;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.TankerEntriesService;
import ru.yandex.market.ff.service.implementation.tanker.TankerEntriesServiceImpl;
import ru.yandex.market.tanker.client.TankerClient;
import ru.yandex.market.tanker.client.TranslationClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link TemplateValidationMessages}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TemplateValidationMessagesTest extends IntegrationTest {

    @Autowired
    private TankerClient tankerClient;

    @Autowired
    private TankerEntryRepository tankerEntryRepository;

    private TemplateValidationMessages templateValidationMessagesFromTanker;

    @BeforeEach
    public void initExecutor() {
        TankerClient tankerClientSpy = spy(tankerClient);
        TranslationClient translationClientMock = mock(TranslationClient.class);
        when(tankerClientSpy.translations()).thenReturn(translationClientMock);

        DateTimeService dateTimeService = mock(DateTimeService.class);
        TankerEntriesService tankerEntriesService = new TankerEntriesServiceImpl(tankerClientSpy,
                tankerEntryRepository, dateTimeService, tankerCacheManager);
        templateValidationMessagesFromTanker = new TemplateValidationMessages(tankerEntriesService);
        ReflectionTestUtils.setField(tankerEntriesService, "keySets",
                Set.of("shared.fulfillment.file-errors"));
        ReflectionTestUtils.setField(tankerEntriesService, "projectId", "market-partner");
    }

    @Test
    @Order(1)
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    public void getMessage() {
        String onWrongRowsCount = templateValidationMessagesFromTanker.getOnWrongRowsCount(1, 10000);
        assertEquals("Некорректное количество строк. Допустимое количество строк: 1 - 10000",
                onWrongRowsCount);
    }

    @Test
    @Order(2)
    @DatabaseSetup("classpath:service/tanker/after-loading-keys.xml")
    public void getMessageWithExceptionForNonExistingTankerKey() {
        assertThrows(RuntimeException.class,
                () -> templateValidationMessagesFromTanker.getOnCanNotReadCellValue(1, 4));
    }

    @Test
    @Order(3)
    @DatabaseSetup("classpath:service/tanker/after-loading-keys.xml")
    public void getMessageWithExceptionForInvalidNumberOfArgs() {
        assertThrows(RuntimeException.class, () -> templateValidationMessagesFromTanker
                .getOnSupplyItemsQuotaAlreadyExceeded("01.10.2020", 4, 1));
    }
}
