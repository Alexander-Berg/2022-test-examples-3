package ru.yandex.market.partner.mvc.controller.program.marketplace;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.program.partner.status.PartnerStatusService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient;
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogStatsResponse;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversResponse;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.partner.notification.client.model.GetMessageHeadersResponse;
import ru.yandex.market.partner.notification.client.model.MessageHeaderDTO;
import ru.yandex.market.partner.notification.client.model.PagerDTO;
import ru.yandex.market.partner.notification.client.model.PriorityDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Тест на статусы светофора для поставщика.
 * В тестах включено получение push-api логов из компонента mbi-log-processor.
 */
@DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
class MarketplaceProgramControllerWithMbiLogProcessorFunctionalTest extends FunctionalTest {

    @Autowired
    private MboMappingsService patientMboMappingsService;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private ObjectMapper jacksonMapper;
    @Autowired
    private MbiLogProcessorClient logProcessorClient;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private PartnerStatusService partnerStatusService;

    @BeforeEach
    void setup() {
        environmentService.setValue("mbi-log-processor.enabled", "true");
        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenReturn(CompletableFuture.completedFuture(new StatusResolversResponse()));
    }

    /**
     * Параметры для тестов успешной установки параметра.
     */
    private static Stream<Arguments> supplierWithExpectedProgramStatuses() {
        return Stream.of(
                //Not supplier
                Arguments.of(1001L, "{\"program\":\"marketplace\",\"status\":\"none\",\"isEnabled\":false," +
                        "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Request status NEW
                Arguments.of(1010L, "{\"program\":\"marketplace\",\"status\":\"empty\",\"isEnabled\":false," +
                        "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Request status CANCELLED
                Arguments.of(1011L, "{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"blocked\",\"newbie\":false}"),
                //Request status INIT
                Arguments.of(1012L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"required\",\"newbie\":false}"),
                //Request status IN_PROGRESS
                Arguments.of(1013L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"required\",\"newbie\":false}"),
                //Request status NEED_INFO
                Arguments.of(1014L, "{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Dropship feature status REVOKE
                Arguments.of(1015L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[], \"needTestingState\":\"blocked\",\"newbie\":false}"),
                //Dropship feature status FAIL
                Arguments.of(1016L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feature_cutoff_types\"}], \"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Dropship feature status NEW
                Arguments.of(1017L, "{\"program\":\"marketplace\",\"status\":\"enabling\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sandbox\"}], \"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Dropship feature status DONT_WANT
                Arguments.of(1018L, "{\"program\":\"marketplace\",\"status\":\"disabled\",\"isEnabled\":false," +
                        "\"subStatuses\":[], \"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Dropship feature status FAIL_MANUAL
                Arguments.of(1019L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[], \"needTestingState\":\"blocked\",\"newbie\":false}"),
                //TEST_FEED_STATE noffers=0
                Arguments.of(1020L, "{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sandbox\"}, {\"code\":\"feed_failed\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //TEST_FEED_STATE retcode =2
                Arguments.of(1021L, "{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sandbox\"}, {\"code\":\"feed_failed\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //TEST_FEED_STATE noffers>0
                Arguments.of(1022L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sandbox\"}], " +
                        "\"needTestingState\":\"required\",\"newbie\":false}"),
                //MARKETPLACE feature no feature
                Arguments.of(1022L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sandbox\"}], " +
                        "\"needTestingState\":\"required\",\"newbie\":false}"),
                //MARKETPLACE SUCCESS no SUPPLIER_FEED_STATE
                Arguments.of(1023L, "{\"program\":\"marketplace\",\"status\":\"enabling\",\"isEnabled\":false," +
                        "\"subStatuses\":[], " + "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //SUPPLIER_FEED_STATE noffers=0
                Arguments.of(1024L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feed_failed\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //SUPPLIER_FEED_STATE retcode >= 2
                Arguments.of(1025L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feed_failed\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //SUPPLIER_FEED_STATE noffers>0
                Arguments.of(1026L, "{\"program\":\"marketplace\",\"status\":\"restricted\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"stocks\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //FEATURE CUTOFF experiment
                Arguments.of(1027L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"offline_test\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":1}"),
                //FEATURE CUTOFF pinger
                Arguments.of(1028L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"api_error\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":2}"),
                //Has push api error
                Arguments.of(1029L, "{\"program\":\"marketplace\",\"status\":\"restricted\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"api_error\"},{\"code\":\"stocks\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //ALL is ok, status FULL
                Arguments.of(1030L, "{\"program\":\"marketplace\",\"status\":\"full\",\"isEnabled\":true," +
                        "\"subStatuses\":[], \"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Cpa is partner interface true
                Arguments.of(1031L, "{\"program\":\"marketplace\",\"status\":\"full\",\"isEnabled\":true," +
                        "\"subStatuses\":[], \"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Has another cutoffs
                Arguments.of(1032L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Has another cutoffs
                Arguments.of(1032L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                // Successful dropship partner. Everything is OK.
                Arguments.of(1034L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"offline_test\"}],\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":4}")
        );
    }

    @BeforeEach
    void initMocks() {
        Mockito.when(patientMboMappingsService.searchOfferProcessingStatusesByShopId(any())).
                thenReturn(MboMappings.SearchOfferProcessingStatusesResponse.newBuilder()
                        .setStatus(MboMappings.SearchOfferProcessingStatusesResponse.Status.OK)
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.READY)
                                        .setOfferCount(3)
                                        .build()
                        )
                        .addOfferProcessingStatuses(
                                MboMappings.SearchOfferProcessingStatusesResponse.OfferProcessingStatusInfo.newBuilder()
                                        .setOfferProcessingStatus(SupplierOffer.OfferProcessingStatus.REVIEW)
                                        .setOfferCount(3)
                                        .build()
                        ).build());

        Mockito.when(lmsClient.getPartner(Mockito.eq(1030L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.TRUE)
                                .build()
                ));
        Mockito.when(logProcessorClient.getLogStatsFromLastEvent(29L, Duration.ofMinutes(30L)))
                .thenReturn(new PushApiLogStatsResponse().errorCount(1L));
        Mockito.when(logProcessorClient.hasErrorsInHalfAnHour(List.of(29L)))
                .thenReturn(List.of(29L));
        Stream.of(Pair.of(1L, 1L), Pair.of(2L, 2L), Pair.of(4L, 4L))
                .forEach(p -> mockGetMessageHeaders(p.left(), p.right()));
    }

    private void mockGetMessageHeaders(long messageId, long groupId) {
        doReturn(new GetMessageHeadersResponse().headers(
                                List.of(new MessageHeaderDTO()
                                        .messageId(messageId)
                                        .sentTime(OffsetDateTime.now())
                                        .priority(PriorityDTO.NORMAL))
                        )
                        .pager(new PagerDTO().currentPage(0).pageSize(2).itemCount(2))
        )
                .when(partnerNotificationClient)
                .getMessageHeaders(any(), any(), any(), any(), any(), any(), any(),
                        ArgumentMatchers.argThat(groupIds -> groupIds.contains(groupId)), any(), any());
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "db/marketplaceProgram.before.csv")
    @MethodSource("supplierWithExpectedProgramStatuses")
    void programStatusChecks(long campaignId, String expected) throws IOException {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceStatusUrl(campaignId));
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
    }

    @Test
    @DisplayName("Fulfillment ничего не заполняли")
    void testGetProgramMarketplaceFieldsFF() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceFieldsUrl(210));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/marketplace/fullFields.json");
    }

    @Test
    @DisplayName("Dropship ничего не заполняли")
    void testGetProgramMarketplaceFieldsDropship() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceFieldsUrl(211));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/marketplace/fullDropshipFields.json");
    }

    @Test
    @DisplayName("Crossdock ничего не заполняли")
    void testGetProgramMarketplaceFieldsCrossdock() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceFieldsUrl(311));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/marketplace/fullCrossdockFields.json");
    }

    /**
     * Тест, проверяющий поля программы marketplace для Drophip поставщика
     */
    @Test
    @DisplayName("Crossdock через ПИ все поля заполнены")
    void testGetProgramMarketplaceFieldsFFAllFilled() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceFieldsUrl(212));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/marketplace/crossdockWithPIAllFilled.json");
    }

    @Test
    @DisplayName("Dropship все поля заполнены")
    void testGetProgramMarketplaceFieldsDropshipAllFilled() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceFieldsUrl(213));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/marketplace/dropshipAllFilled.json");
    }

    @Test
    @DisplayName("Crossdock все поля заполнены")
    void testGetProgramMarketplaceFieldsCrossdockAllFilled() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceFieldsUrl(313));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/marketplace/crossdockAllFilled.json");
    }

    @Test
    @DisplayName("Fulfillment все поля заполнены")
    void testGetProgramMarketplaceFieldsFulfillmentAllFilled() {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceFieldsUrl(314));
        JsonTestUtil.assertEquals(entity, this.getClass(), "/mvc/program/marketplace/fulfillmentAllFilled.json");
    }

    @Test
    @DisplayName("Dropship все поля заполнены и закэширован поход в МБО")
    void testGetProgramMarketplaceFieldsDropshipAllFilledAndCached() {
        FunctionalTestHelper.get(marketplaceFieldsUrl(213));
        FunctionalTestHelper.get(marketplaceFieldsUrl(213));
        Mockito.verify(patientMboMappingsService, Mockito.times(1))
                .searchOfferProcessingStatusesByShopId(any());
    }

    @Test
    @DisplayName("C&C игнорируем стоки")
    @DbUnitDataSet(before = "db/marketplaceProgram.before.csv")
    void testIgnoreStocksCC() throws IOException {
        ResponseEntity<String> entity = FunctionalTestHelper.get(marketplaceStatusUrl(1033));
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals("{\"program\":\"marketplace\",\"status\":\"full\",\"isEnabled\":true," +
                "\"subStatuses\":[], \"needTestingState\":\"not_required\",\"newbie\":false}", actual);
        Mockito.verify(lmsClient, Mockito.times(0)).getPartner(1033L);
    }

    private String marketplaceStatusUrl(long campaignId) {
        return baseUrl + String.format("/campaigns/programs/marketplace?_user_id=12345&id=%d", campaignId);
    }

    private String marketplaceFieldsUrl(int campaignId) {
        return baseUrl + String.format("/campaigns/programs/marketplace/fields?_user_id=12345&id=%d", campaignId);
    }

}
