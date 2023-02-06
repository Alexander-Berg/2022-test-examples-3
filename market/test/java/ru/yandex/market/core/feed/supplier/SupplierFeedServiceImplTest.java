package ru.yandex.market.core.feed.supplier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.feed.datacamp.FeedParsingType;
import ru.yandex.market.core.feed.event.DataCampCreateUpdateFeedEventListener;
import ru.yandex.market.core.feed.event.PartnerParsingFeedEvent;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.supplier.model.SupplierFeed;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link SupplierFeedService}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class SupplierFeedServiceImplTest extends FunctionalTest {

    @Autowired
    private SupplierFeedService supplierFeedService;

    @Autowired
    private FeedFileStorage feedFileStorage;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Autowired
    private DataCampCreateUpdateFeedEventListener feedEventListener;

    private static Stream<Arguments> params() {
        return Stream.of(
                Arguments.of(new HashSet<>(Arrays.asList(1000L, 1001L)), 2),
                Arguments.of(new HashSet<>(Collections.singletonList(1009L)), 1),
                Arguments.of(new HashSet<>(), 0)
        );
    }

    @DisplayName("Выбор feedId в зависимости от ff_link")
    @Test
    @DbUnitDataSet(before = "SupplierFeedServiceImplTest.before.csv",
            after = "SupplierFeedServiceImplTest.createOrUpdateFeedForUpload.after.csv")
    void createOrUpdateFeedForUpload() {
        when(feedFileStorage.getUrl(any())).thenReturn("http://nowhere.local/1");
        // 1 ффлинк без основного feed_id, берём из линка
        supplierFeedService.updateFeedForUpload(1, 10, 101L, FeedParsingType.COMPLETE_FEED, 1L, null);
        // 2 ффлинка без основного feed_id, генерим новый, обновляем линки
        supplierFeedService.updateFeedForUpload(2, 20, 102L, FeedParsingType.UPDATE_FEED, 2L, null);
        // нет ффлинков, генерим новый
        supplierFeedService.updateFeedForUpload(3, 30, 103L, FeedParsingType.COMPLETE_FEED, 3L, null);
        ArgumentCaptor<PartnerParsingFeedEvent> captor = ArgumentCaptor.forClass(PartnerParsingFeedEvent.class);
        verify(feedEventListener, times(3)).onApplicationEvent(captor.capture());
        long count = captor.getAllValues().stream()
                .filter(e -> e.getBusinessId() != null)
                .count();
        assertEquals(3, count);
    }

    @DisplayName("Прокидывание parsingFields")
    @Test
    @DbUnitDataSet(before = "SupplierFeedServiceImplTest.before.csv")
    void createOrUpdateFeedForUploadWithParsingFields() {
        when(feedFileStorage.getUrl(any())).thenReturn("http://nowhere.local/1");
        supplierFeedService.updateFeedForUpload(1, 10, 101L, FeedParsingType.COMPLETE_FEED, 1L,
                List.of("id", "price", "adult"));
        ArgumentCaptor<PartnerParsingFeedEvent> captor = ArgumentCaptor.forClass(PartnerParsingFeedEvent.class);
        verify(feedEventListener).onApplicationEvent(captor.capture());
        Assertions.assertThat(captor.getValue().getParsingFields())
                .isEqualTo(List.of("id", "price", "adult"));
    }

    @ParameterizedTest
    @MethodSource("params")
    @DbUnitDataSet(before = "FeedSupplierDaoTest.before.csv")
    void getSupplierFeedMap(Set<Long> supplierIds, int expectedSize) {
        Map<Long, Long> supplierFeedMap = supplierFeedService.getSuppliersFeedIdList(supplierIds);
        assertEquals(expectedSize, supplierFeedMap.size());
    }

    @Test
    @DbUnitDataSet(before = "FeedSupplierDaoTest.before.csv")
    void getSupplierIdByFeedIdTest() {
        assertEquals(1000L, supplierFeedService.getSupplierIdByFeedId(10L).get());
        assertEquals(1001L, supplierFeedService.getSupplierIdByFeedId(11L).get());
        assertTrue(supplierFeedService.getSupplierIdByFeedId(56L).isEmpty());
    }

    @Test
    @DisplayName("Выгрузка фидов для самовара. Живые партнеры")
    @DbUnitDataSet(before = {"testGetFeedsWithPushSchema.csv"})
    void getAliveSupplierFeedsWithPushSchemaTest() {
        checkFeedsForSamovar(Set.of(774L, 222L, 888L, 669L));
    }

    private void checkFeedsForSamovar(Set<Long> expected) {
        Map<Long, SupplierFeed> supplierFeeds = supplierFeedService.getAllSupplierFeedsForSamovar()
                .stream()
                .collect(Collectors.toMap(SupplierFeed::getSupplierId, Function.identity()));

        assertEquals(expected, supplierFeeds.keySet());

        assertEquals(180, supplierFeeds.get(669L).getPeriod());
        assertEquals(33, supplierFeeds.get(888L).getPeriod());
        assertEquals(null, supplierFeeds.get(774L).getPeriod());
        assertEquals(180, supplierFeeds.get(222L).getPeriod());
    }

    @DisplayName("Проверка на то, что фиды, которые загружаются по пуш схеме выдаются через " +
            "getSupplierFeedWithPushSchema")
    @Test
    @DbUnitDataSet(before = "testGetFeedsWithPushSchema.csv")
    void getSupplierFeedWithPushSchemaPositiveTest() {
        assertEquals(774L, supplierFeedService.getSupplierFeedForSamovarById(10L).get().getSupplierId());
        assertEquals(888L, supplierFeedService.getSupplierFeedForSamovarById(20L).get().getSupplierId());
        assertEquals(669L, supplierFeedService.getSupplierFeedForSamovarById(56L).get().getSupplierId());
    }

    @Test
    @DisplayName("Удаление фида. Фид становится дефолтным")
    @DbUnitDataSet(before = "FeedSupplierDaoTest.before.csv", after = "SupplierFeedService.testDeleteFeed.after.csv")
    void testDeleteFeed() {
        mockCamunda();
        supplierFeedService.deleteFeed(PartnerId.partnerId(1000L, CampaignType.SUPPLIER), 101);
    }

    private void mockCamunda() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        Mockito.when(mbiBpmnClient.postProcess(any())).thenReturn(response);
    }

    @Test
    @DisplayName("Перемещение фида в таблицу служебных фидов. PRICE. URL.")
    @DbUnitDataSet(
            before = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/partner.before.csv",
            after = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/url.after.csv"
    )
    void fixPartnerUtilityFeeds_existUrlFeed_successful() {
        supplierFeedService.fixPartnerUtilityFeeds(1000L, 105L);
    }

    @Test
    @DisplayName("Перемещение фида в таблицу служебных фидов. PRICE. Upload.")
    @DbUnitDataSet(
            before = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/partner.before.csv",
            after = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/upload.after.csv"
    )
    void fixPartnerUtilityFeeds_existUploadFeed_successful() {
        supplierFeedService.fixPartnerUtilityFeeds(1001L, 105L);
    }

    @Test
    @DisplayName("Перемещение фида в таблицу служебных фидов, если там уже есть фид по ссылке. PRICE. URL.")
    @DbUnitDataSet(
            before = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/partner.before.csv",
            after = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/urlExist.after.csv"
    )
    void fixPartnerUtilityFeeds_existUrlFeedAndPartnerUtilityFeed_successful() {
        supplierFeedService.fixPartnerUtilityFeeds(1006L, 105L);
    }

    @Test
    @DisplayName("Перемещение фида в таблицу служебных фидов. PRICE. Дефолтный.")
    @DbUnitDataSet(
            before = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/partner.before.csv",
            after = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/default.after.csv"
    )
    void fixPartnerUtilityFeeds_existDefaultFeed_nothing() {
        supplierFeedService.fixPartnerUtilityFeeds(1009L, 105L);
    }

    @Test
    @DisplayName("Перемещение фида в таблицу служебных фидов. PRICE. Фид не найден.")
    @DbUnitDataSet(
            before = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/partner.before.csv",
            after = "SupplierFeedServiceImpl/fixPartnerUtilityFeeds/partner.before.csv"
    )
    void fixPartnerUtilityFeeds_unknownPartner_nothing() {
        supplierFeedService.fixPartnerUtilityFeeds(1010L, 105L);
    }
}
