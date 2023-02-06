package ru.yandex.market.ff.service.implementation.tanker;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.i18n.TemplateValidationMessages;
import ru.yandex.market.ff.model.entity.TankerEntry;
import ru.yandex.market.ff.repository.TankerEntryRepository;
import ru.yandex.market.ff.service.DateTimeService;
import ru.yandex.market.ff.service.TankerEntriesService;
import ru.yandex.market.ff.tms.SyncTankerKeysExecutor;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;
import ru.yandex.market.tanker.client.TankerClient;
import ru.yandex.market.tanker.client.TranslationClient;
import ru.yandex.market.tanker.client.model.KeySet;
import ru.yandex.market.tanker.client.model.KeySetTranslation;
import ru.yandex.market.tanker.client.model.Language;
import ru.yandex.market.tanker.client.request.TranslationRequestBuilder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TankerEntriesServiceTest extends IntegrationTest {

    private static final Map<String, Object> UPDATES_FOR_KEYS = Map.of(
            "internal.BOOLEAN_FAIL", "Значение не соответствует булеву значению",
            "internal.CALENDARING_IS_NOT_APPLICABLE_FOR_REQUEST_BUT_REQUIRED", "Календаризация неприменима",
            "internal.XDOC_SUPPLY_FOR_NOT_EXISTING_XDOC_FULFILLMENT_RELATION",
            "Создание xDoc поставки для некорректной связки xDoc склада и конечного склада");

    @Autowired
    private TankerClient tankerClient;

    @Autowired
    private TankerCacheManager tankerCacheManager;

    @Autowired
    private TankerEntryRepository tankerEntryRepository;

    private SyncTankerKeysExecutor syncTankerKeysExecutor;
    private TranslationClient translationClientMock;
    private DateTimeService dateTimeService;

    @BeforeEach
    public void initExecutor() {
        TankerClient tankerClientSpy = spy(tankerClient);
        translationClientMock = mock(TranslationClient.class);
        when(tankerClientSpy.translations()).thenReturn(translationClientMock);

        dateTimeService = mock(DateTimeService.class);
        TankerEntriesService tankerEntriesService = new TankerEntriesServiceImpl(tankerClientSpy,
                tankerEntryRepository, dateTimeService, tankerCacheManager);
        ReflectionTestUtils.setField(tankerEntriesService, "keySets",
                Set.of("shared.fulfillment.file-errors"));
        ReflectionTestUtils.setField(tankerEntriesService, "projectId", "market-partner");

        syncTankerKeysExecutor = new SyncTankerKeysExecutor(tankerEntriesService);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/before-loading-keys.xml")
    @ExpectedDatabase(value = "classpath:service/tanker/after-loading-keys.xml", assertionMode = NON_STRICT_UNORDERED)
    public void loadEntries() {
        when(translationClientMock.keySet(any(TranslationRequestBuilder.class)))
                .thenReturn(new KeySetTranslation(Map.of(Language.RU, new KeySet(UPDATES_FOR_KEYS))));
        when(dateTimeService.localDateTimeNow()).thenReturn(ZonedDateTime.of(
                LocalDateTime.of(2021, 1, 1, 9, 9, 9), ZoneId.systemDefault())
                .toLocalDateTime());
        syncTankerKeysExecutor.doJob(null);
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/before-loading-keys.xml")
    public void loadEntriesWithNoKeysetException() {
        when(translationClientMock.keySet(any(TranslationRequestBuilder.class))).thenReturn(null);
        assertThrows(RuntimeException.class, () -> syncTankerKeysExecutor.doJob(null));
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/before-loading-keys.xml")
    public void loadEntriesWithNoRuTranslationException() {
        when(translationClientMock.keySet(any(TranslationRequestBuilder.class)))
                .thenReturn(new KeySetTranslation(Map.of(Language.EN, new KeySet(UPDATES_FOR_KEYS))));
        assertThrows(RuntimeException.class, () -> syncTankerKeysExecutor.doJob(null));
    }

    @Test
    @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml")
    @Transactional
    @JpaQueriesCount(1)
    public void cachedKeysTest() {
        TankerEntryRepository tankerEntryRepositorySpy = spy(tankerEntryRepository);
        TankerClient tankerClientSpy = spy(tankerClient);
        TankerEntriesService tankerEntriesServiceMocked = new TankerEntriesServiceImpl(
                tankerClientSpy, tankerEntryRepositorySpy, dateTimeService, tankerCacheManager);
        TemplateValidationMessages templateValidationMessagesFromTanker =
                new TemplateValidationMessages(tankerEntriesServiceMocked);

        ReflectionTestUtils.setField(tankerEntriesServiceMocked, "keySets",
                Set.of("shared.fulfillment.file-errors"));
        ReflectionTestUtils.setField(tankerEntriesServiceMocked, "projectId", "market-partner");

        when(tankerClientSpy.translations()).thenReturn(translationClientMock);
        when(translationClientMock.keySet(any(TranslationRequestBuilder.class)))
                .thenReturn(new KeySetTranslation(Map.of(Language.RU, new KeySet(UPDATES_FOR_KEYS))));
        when(dateTimeService.localDateTimeNow())
                .thenReturn(LocalDateTime.of(2021, 1, 1, 9, 9, 9));

        for (int i = 0; i < 10; i++) {
            templateValidationMessagesFromTanker.getOnMandatoryFail();
        }
        tankerEntriesServiceMocked.loadEntries();
        for (int i = 0; i < 10; i++) {
            templateValidationMessagesFromTanker.getOnMandatoryFail();
        }
        verify(tankerEntryRepositorySpy, times(1)).findAll();
        verify(tankerEntryRepositorySpy, times(1)).save((Iterable<TankerEntry>) any());
    }
}
