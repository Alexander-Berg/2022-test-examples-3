package ru.yandex.direct.useractionlog.dict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.matcher.AssertionMatcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.direct.binlog.reader.EnrichedRow;
import ru.yandex.direct.test.utils.TestUtils;
import ru.yandex.direct.useractionlog.AdGroupId;
import ru.yandex.direct.useractionlog.CampaignId;
import ru.yandex.direct.useractionlog.ClientId;
import ru.yandex.direct.useractionlog.schema.AdId;
import ru.yandex.direct.useractionlog.schema.ObjectPath;

@ParametersAreNonnullByDefault
public class CacheDictRepositoryTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private MemoryDictRepository baseRepository;

    private static EnrichedRow rowMock() {
        EnrichedRow row = Mockito.mock(EnrichedRow.class);
        Mockito.when(row.getGtid()).thenReturn("12345678-9abc-def0-1234-56789abcdef0:123");
        return row;
    }

    @Before
    public void setUp() {
        baseRepository = new MemoryDictRepository()
                .addData(DictDataCategory.CAMPAIGN_PATH, 123L,
                        new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(123L)))
                .addData(DictDataCategory.CAMPAIGN_PATH, 124L,
                        new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(124L)))
                .addData(DictDataCategory.CAMPAIGN_NAME, 123L, "Hello campaign 123")
                .addData(DictDataCategory.CAMPAIGN_NAME, 124L, "Hello campaign 124")
                .addData(DictDataCategory.ADGROUP_NAME, 456L, "Hello adgroup 456")
                .addData(DictDataCategory.ADGROUP_NAME, 457L, "Hello adgroup 457");
    }

    /**
     * Проверяет, что {@link CacheDictRepository} работает - передаёт всё, что знает другой {@link
     * DictRepository}, которого он проксирует.
     */
    @Test
    public void getDataWorks() throws Exception {
        CacheDictRepository cache = new CacheDictRepository(baseRepository);
        Collection<DictRequest> dictRequests = new ArrayList<>();
        DictRequestsFiller dictRequestsFiller = new DictRequestsFiller(dictRequests);
        dictRequestsFiller.requireCampaignPath(123L);
        dictRequestsFiller.requireCampaignPath(124L);
        dictRequestsFiller.requireCampaignNames(123L);
        dictRequestsFiller.requireCampaignNames(124L);
        dictRequestsFiller.requireAdGroupNames(456L);
        dictRequestsFiller.requireAdGroupNames(457L);

        Map<DictRequest, Object> dictResponses = cache.getData(dictRequests);

        softly.assertThat(dictResponses).isEqualTo(ImmutableMap.builder()
                .put(new DictRequest(DictDataCategory.CAMPAIGN_PATH, 123L),
                        new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(123L)))
                .put(new DictRequest(DictDataCategory.CAMPAIGN_PATH, 124L),
                        new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(124L)))
                .put(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L), "Hello campaign 123")
                .put(new DictRequest(DictDataCategory.CAMPAIGN_NAME, 124L), "Hello campaign 124")
                .put(new DictRequest(DictDataCategory.ADGROUP_NAME, 456L), "Hello adgroup 456")
                .put(new DictRequest(DictDataCategory.ADGROUP_NAME, 457L), "Hello adgroup 457")
                .build());
    }

    /**
     * Проверяет, что {@link CacheDictRepository} кеширует, т.е. не запрашивает повторно у проксируемого словаря
     * одни и те же данные.
     */
    @Test
    public void getDataFetchesOnce() throws Exception {
        CacheDictRepository cache = new CacheDictRepository(baseRepository);

        List<DictRequest> requests1 = new ArrayList<>();
        EnrichedRow row1 = rowMock();
        DictRequestsFiller filler1 = new DictRequestsFiller(requests1);
        filler1.requireCampaignPath(123L);
        filler1.requireCampaignNames(123L);
        filler1.requireAdGroupNames(456L);
        cache.getData(requests1);

        Assertions.assertThat(baseRepository.requestLog).containsExactlyInAnyOrder(
                new DictRequest(DictDataCategory.CAMPAIGN_PATH, 123L),
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 123L),
                new DictRequest(DictDataCategory.ADGROUP_NAME, 456L));
        baseRepository.requestLog.clear();

        // repeat of one id of each type
        List<DictRequest> requests2 = new ArrayList<>();
        EnrichedRow row2 = rowMock();
        DictRequestsFiller filler2 = new DictRequestsFiller(requests2);
        filler2.requireCampaignPath(123L);
        filler2.requireCampaignPath(124L);
        filler2.requireCampaignNames(123L);
        filler2.requireCampaignNames(124L);
        filler2.requireAdGroupNames(456L);
        filler2.requireAdGroupNames(457L);
        cache.getData(requests2);

        Assertions.assertThat(baseRepository.requestLog).containsExactlyInAnyOrder(
                new DictRequest(DictDataCategory.CAMPAIGN_PATH, 124L),
                new DictRequest(DictDataCategory.CAMPAIGN_NAME, 124L),
                new DictRequest(DictDataCategory.ADGROUP_NAME, 457L));
        baseRepository.requestLog.clear();

        // repeat of all ids
        List<DictRequest> requests3 = new ArrayList<>();
        EnrichedRow row3 = rowMock();
        DictRequestsFiller filler3 = new DictRequestsFiller(requests3);
        filler3.requireCampaignPath(123L);
        filler3.requireCampaignPath(124L);
        filler3.requireCampaignNames(123L);
        filler3.requireCampaignNames(124L);
        filler3.requireAdGroupNames(456L);
        filler3.requireAdGroupNames(457L);
        cache.getData(requests3);

        Assertions.assertThat(baseRepository.requestLog)
                .isEmpty();
    }

    /**
     * Проверяет, что {@link CacheDictRepository} не ломается, если какие-то словарные данные не были найдены
     * проксируемым словарём.
     */
    @Test
    public void getDataHoles() throws Exception {
        CacheDictRepository cache = new CacheDictRepository(baseRepository);

        List<DictRequest> requests = new ArrayList<>();
        EnrichedRow row = rowMock();
        DictRequestsFiller filler = new DictRequestsFiller(requests);

        // existent dictionary data
        filler.requireCampaignPath(123L);
        filler.requireCampaignNames(123L);
        filler.requireAdGroupNames(456L);

        // nonexistent dictionary data
        filler.requireCampaignPath(999L);
        filler.requireCampaignNames(999L);
        filler.requireAdGroupNames(999L);

        Map<DictRequest, Object> responses = cache.getData(requests);

        Assertions.assertThat(responses).isEqualTo(ImmutableMap.builder()
                .put(requests.get(0), new ObjectPath.CampaignPath(new ClientId(100500L), new CampaignId(123L)))
                .put(requests.get(1), "Hello campaign 123")
                .put(requests.get(2), "Hello adgroup 456")
                .build());
    }

    /**
     * Значения с одинаковыми идентификаторами и разными категориями не должны перезаписывать друг друга.
     * DIRECT-73712
     */
    @Test
    public void noCollisionsByCategory() {
        final Map<DictDataCategory, Object> expectedOld = new EnumMap<>(DictDataCategory.class);
        expectedOld.put(DictDataCategory.AD_PATH,
                new ObjectPath.AdPath(new ClientId(123L), new CampaignId(456L), new AdGroupId(789L), new AdId(135L)));
        expectedOld.put(DictDataCategory.AD_TITLE, "Old AD_TITLE");
        expectedOld.put(DictDataCategory.ADGROUP_GEO, "Old ADGROUP_GEO");
        expectedOld.put(DictDataCategory.ADGROUP_NAME, "Old ADGROUP_NAME");
        expectedOld.put(DictDataCategory.ADGROUP_PATH,
                new ObjectPath.AdGroupPath(new ClientId(123L), new CampaignId(456L), new AdGroupId(789L)));
        expectedOld.put(DictDataCategory.CAMPAIGN_DISABLED_IPS, "Old DISABLED_IPS");
        expectedOld.put(DictDataCategory.CAMPAIGN_DISABLED_SSP, "Old DISABLED_SSP");
        expectedOld.put(DictDataCategory.CAMPAIGN_DONT_SHOW, "Old DONT_SHOW");
        expectedOld.put(DictDataCategory.CAMPAIGN_GEO, "Old GEO");
        expectedOld.put(DictDataCategory.CAMPAIGN_MINUS_WORDS, "Old MINUS_WORDS");
        expectedOld.put(DictDataCategory.CAMPAIGN_NAME, "Old CAMPAIGN_NAME");
        expectedOld.put(DictDataCategory.CAMPAIGN_PATH,
                new ObjectPath.CampaignPath(new ClientId(123L), new CampaignId(456L)));
        expectedOld.put(DictDataCategory.CAMPAIGN_TIME_TARGET, "Old TIME_TARGET");
        expectedOld.put(DictDataCategory.HIERARCHICAL_MULTIPLIERS_RECORD, "Old HIERARCHICAL_MULTIPLIERS_RECORD");
        expectedOld.put(DictDataCategory.RETARGETING_CONDITION_NAME, "Old RETARGETING_CONDITION_NAME");
        expectedOld.put(DictDataCategory.CAMPAIGN_STRATEGY_DATA, "Old CAMPAIGN_STRATEGY_DATA");
        TestUtils.assumeThat("В тестах должны быть все возможные категории",
                expectedOld.keySet(),
                Matchers.equalTo(EnumSet.allOf(DictDataCategory.class)));

        final Map<DictDataCategory, Object> expectedNew = new EnumMap<>(DictDataCategory.class);
        expectedNew.put(DictDataCategory.AD_PATH,
                new ObjectPath.AdPath(new ClientId(321L), new CampaignId(654L), new AdGroupId(987L), new AdId(531L)));
        expectedNew.put(DictDataCategory.AD_TITLE, "New AD_TITLE");
        expectedNew.put(DictDataCategory.ADGROUP_GEO, "New ADGROUP_GEO");
        expectedNew.put(DictDataCategory.ADGROUP_NAME, "New ADGROUP_NAME");
        expectedNew.put(DictDataCategory.ADGROUP_PATH,
                new ObjectPath.AdGroupPath(new ClientId(321L), new CampaignId(654L), new AdGroupId(987L)));
        expectedNew.put(DictDataCategory.CAMPAIGN_DISABLED_IPS, "New CAMPAIGN_DISABLED_IPS");
        expectedNew.put(DictDataCategory.CAMPAIGN_DISABLED_SSP, "New CAMPAIGN_DISABLED_SSP");
        expectedNew.put(DictDataCategory.CAMPAIGN_DONT_SHOW, "New CAMPAIGN_DONT_SHOW");
        expectedNew.put(DictDataCategory.CAMPAIGN_GEO, "New CAMPAIGN_GEO");
        expectedNew.put(DictDataCategory.CAMPAIGN_MINUS_WORDS, "New CAMPAIGN_MINUS_WORDS");
        expectedNew.put(DictDataCategory.CAMPAIGN_NAME, "New CAMPAIGN_NAME");
        expectedNew.put(DictDataCategory.CAMPAIGN_PATH,
                new ObjectPath.CampaignPath(new ClientId(321L), new CampaignId(654L)));
        expectedNew.put(DictDataCategory.CAMPAIGN_TIME_TARGET, "New CAMPAIGN_TIME_TARGET");
        expectedNew.put(DictDataCategory.HIERARCHICAL_MULTIPLIERS_RECORD, "New HIERARCHICAL_MULTIPLIERS_RECORD");
        expectedNew.put(DictDataCategory.RETARGETING_CONDITION_NAME, "New RETARGETING_CONDITION_NAME");
        expectedNew.put(DictDataCategory.CAMPAIGN_STRATEGY_DATA, "New CAMPAIGN_STRATEGY_DATA");
        TestUtils.assumeThat("В тестах должны быть все возможные категории",
                expectedNew.keySet(),
                Matchers.equalTo(EnumSet.allOf(DictDataCategory.class)));
        TestUtils.assumeThat("Новые и старые словарные данные не должны совпадать, иначе тест ничего не делает",
                expectedNew.keySet(),
                new AssertionMatcher<Set<DictDataCategory>>() {
                    @Override
                    public void assertion(Set<DictDataCategory> actual) throws AssertionError {
                        SoftAssertions softly = new SoftAssertions();
                        expectedOld.forEach((category, value) -> {
                            softly.assertThat(value)
                                    .describedAs("Category " + category)
                                    .isNotEqualTo(expectedNew.get(category));
                        });
                    }
                });

        final long id = 123321;
        final EnrichedRow oldRow = rowMock();
        final EnrichedRow newRow = rowMock();
        for (DictDataCategory category : DictDataCategory.values()) {
            baseRepository.addData(category, id, expectedOld.get(category));
        }
        CacheDictRepository cache = new CacheDictRepository(baseRepository);

        List<DictRequest> requests = Stream.of(DictDataCategory.values())
                .map(c -> new DictRequest(c, id))
                .collect(Collectors.toList());
        for (int attempt = 1; attempt <= 3; ++attempt) {
            Map<DictDataCategory, Object> actual = cache.getData(requests).entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().getCategory(),
                            Map.Entry::getValue));
            softly.assertThat(actual)
                    .describedAs("Fetching old data, attempt " + attempt)
                    .isEqualToComparingFieldByFieldRecursively(expectedOld);
        }

        cache.addData(expectedNew.entrySet().stream()
                .collect(Collectors.toMap(e -> new DictRequest(e.getKey(), id), Map.Entry::getValue)));

        requests = Stream.of(DictDataCategory.values())
                .map(c -> new DictRequest(c, id))
                .collect(Collectors.toList());

        Map<DictDataCategory, Object> actualFromBase = baseRepository.getData(requests).entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getCategory(),
                        Map.Entry::getValue));
        softly.assertThat(actualFromBase)
                .describedAs("New values was written into base repository")
                .isEqualToComparingFieldByFieldRecursively(expectedNew);

        for (int attempt = 1; attempt <= 3; ++attempt) {
            Map<DictDataCategory, Object> actual = cache.getData(requests).entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().getCategory(),
                            Map.Entry::getValue));
            softly.assertThat(actual)
                    .describedAs("Fetching new data, attempt " + attempt)
                    .isEqualToComparingFieldByFieldRecursively(expectedNew);
        }
    }
}
