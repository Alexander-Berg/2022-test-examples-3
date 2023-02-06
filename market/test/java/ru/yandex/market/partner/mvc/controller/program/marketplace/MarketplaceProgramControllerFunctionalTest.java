package ru.yandex.market.partner.mvc.controller.program.marketplace;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.Pair;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.abo.api.client.AboPublicRestClient;
import ru.yandex.market.abo.api.entity.order.limit.OrderLimitDTO;
import ru.yandex.market.abo.api.entity.order.limit.PublicOrderLimitReason;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feature.db.FeatureCutoffDao;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffReason;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.cutoff.CommonCutoffs;
import ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.feature.model.cutoff.UtilityCutoffs;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.program.partner.status.PartnerStatusService;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient;
import ru.yandex.market.mbi.logprocessor.client.model.PushApiLogStatsResponse;
import ru.yandex.market.mbi.partner.status.client.model.NeedTestingState;
import ru.yandex.market.mbi.partner.status.client.model.PartnerStatusInfo;
import ru.yandex.market.mbi.partner.status.client.model.PartnerSubStatusInfo;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverResults;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverType;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversRequest;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolversResponse;
import ru.yandex.market.mbi.partner.status.client.model.WizardStepStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Тест на статусы светофора для поставщика.
 */
@DbUnitDataSet(before = "db/testGetProgramsOK.before.csv")
class MarketplaceProgramControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private MboMappingsService patientMboMappingsService;

    @Autowired
    private MbiLogProcessorClient logProcessorClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private ObjectMapper jacksonMapper;

    @Autowired
    private AboPublicRestClient aboPublicRestClient;

    @Autowired
    private FeatureCutoffDao featureCutoffDao;

    @Autowired
    private PartnerStatusService partnerStatusService;

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
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"blocked\"," +
                        "\"newbie\":false}"),
                //Request status INIT
                Arguments.of(1012L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"required\"," +
                        "\"newbie\":false}"),
                //Request status IN_PROGRESS
                Arguments.of(1013L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"required\"," +
                        "\"newbie\":false}"),
                //Request status NEED_INFO
                Arguments.of(1014L, "{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                //Dropship feature status REVOKE
                Arguments.of(1015L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[], \"needTestingState\":\"blocked\",\"newbie\":false}"),
                //Dropship feature status FAIL
                Arguments.of(1016L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feature_cutoff_types\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //Dropship feature status NEW
                Arguments.of(1017L, "{\"program\":\"marketplace\",\"status\":\"enabling\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sandbox\"}], \"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
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
                // Successful dropship partner. Everything is OK.
                Arguments.of(1034L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"offline_test\"}],\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false,\"messageId\":4}"),
                //FF supplier, Request status NEW
                Arguments.of(1035L, "{\"program\":\"marketplace\",\"status\":\"empty\",\"isEnabled\":false," +
                        "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //FF supplier, Request status CANCELLED
                Arguments.of(1036L, "{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"blocked\"," +
                        "\"newbie\":false}"),
                //FF supplier, Request status INIT
                Arguments.of(1037L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"required\"," +
                        "\"newbie\":false}"),
                //FF supplier, Request status IN_PROGRESS
                Arguments.of(1038L, "{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"required\"," +
                        "\"newbie\":false}"),
                //FF supplier, Request status NEED_INFO
                Arguments.of(1039L, "{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"legal_info\"}], \"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                //FF supplier, MARKETPLACE feature status REVOKE
                Arguments.of(1040L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[], \"needTestingState\":\"blocked\",\"newbie\":false}"),
                //FF supplier, MARKETPLACE feature status FAIL
                Arguments.of(1041L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feature_cutoff_types\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //FF supplierm MARKETPLACE SUCCESS no SUPPLIER_FEED_STATE
                Arguments.of(1042L, "{\"program\":\"marketplace\",\"status\":\"enabling\",\"isEnabled\":false," +
                        "\"subStatuses\":[], " + "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //FF supplier, SUPPLIER_FEED_STATE noffers=0
                Arguments.of(1043L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feed_failed\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //FF supplier, SUPPLIER_FEED_STATE retcode >= 2
                Arguments.of(1044L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feed_failed\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //FF supplier, FEATURE CUTOFF experiment
                Arguments.of(1045L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"offline_test\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":5}"),
                //FF supplier, FEATURE CUTOFF MANUAL
                Arguments.of(1047L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"manual\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":6}"),
                //FF supplier, ALL is ok, status FULL
                Arguments.of(1046L, "{\"program\":\"marketplace\",\"status\":\"full\",\"isEnabled\":true," +
                        "\"subStatuses\":[], \"needTestingState\":\"not_required\",\"newbie\":true}"),
                //FF supplier, no supply requests, status enabling
                Arguments.of(1051L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feed_failed\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":true}"),
                //FEATURE CUTOFF limit-orders
                Arguments.of(1048L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"limit_orders\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":7}"),
                //FEATURE CUTOFF limit orders (abo exception)
                Arguments.of(1049L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"limit_orders\"}],\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false,\"messageId\":8}"),
                //NEW CUTOFF limit orders
                Arguments.of(1050L, "{\"program\":\"marketplace\",\"status\":\"restricted\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"limit_orders\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}"),
                //FEATURE CUTOFF logistic point change
                Arguments.of(1052L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"logistic_point_switch\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":9}"),
                //Feature cutoff - cart diff - dropship
                Arguments.of(1054L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"feature_cutoff_types\"},{\"code\":\"cart_diff\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":10}"),
                //Feature cutoff - order not accepted - dropship
                Arguments.of(1055L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"order_not_accepted\"}],\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false,\"messageId\":11}"),
                //Feature cutoff - disabled by partner - dropship
                Arguments.of(1056L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"by_partner\"}], " +
                        "\"needTestingState\":\"not_required\",\"newbie\":false,\"messageId\":12}"),
                //Feature cutoff - marketplace placement - dropship
                Arguments.of(1057L, "{\"program\":\"marketplace\",\"status\":\"suspended\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"marketplace_placement\"}], " +
                        "\"needTestingState\":\"not_required\", \"newbie\":true}"),
                //Feature cutoff - order pending expired - dropship
                Arguments.of(1058L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"order_pending_expired\"}]," +
                        "\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                //No prepay request
                Arguments.of(1059L, "{\"program\":\"marketplace\",\"status\":\"empty\",\"isEnabled\":false," +
                        "\"subStatuses\":[],\"needTestingState\":\"not_required\"," +
                        "\"newbie\":true}"),
                // Dropship partner without sorting center.
                Arguments.of(1060L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sort_center_not_configured\"}]," +
                        "\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                Arguments.of(1061L, "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"no_loaded_offers\"}]," +
                        "\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                Arguments.of(1062L, "{\"program\":\"marketplace\",\"status\":\"failed\"," +
                        "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"low_rating\"}," +
                        "{\"code\":\"feature_cutoff_types\"}],\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                //selfemployed with unavailable npd
                Arguments.of(1063L, "{\"program\":\"marketplace\",\"status\":\"failed\"," +
                        "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"npd_unavailable\"}]," +
                        "\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                //selfemployed with available npd but no offers
                Arguments.of(1064L, "{\"program\":\"marketplace\",\"status\":\"failed\"," +
                        "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"no_loaded_offers\"}]," +
                        "\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}"),
                //fulfillment without offers
                Arguments.of(1065L, "{\"program\":\"marketplace\",\"status\":\"failed\"," +
                        "\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"no_loaded_offers\"}]," +
                        "\"needTestingState\":\"not_required\"," +
                        "\"newbie\":true}"),
                Arguments.of(1066L, "{\"program\":\"marketplace\",\"status\":\"failed\"," +
                        "\"isEnabled\":true," +
                        "\"subStatuses\":[{\"code\":\"quality_other\"}]," +
                        "\"needTestingState\":\"required\",\"newbie\":false}")
        );
    }

    private static Stream<Arguments> businessMarketplacesWithExpectedProgramStatuses() {
        return Stream.of(
                // Нет маркетплейсов
                Arguments.of(1001L, null, "{}"),
                // Ограничения по айдишникам партнеров
                Arguments.of(1002L, "{\"partnerIds\": [11,13,15]}",
                        "{\"11\":{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                                "\"subStatuses\":[{\"code\":\"legal_info\"}],\"needTestingState\":\"blocked\"," +
                                "\"newbie\":false}," +
                                "\"13\":{\"program\":\"marketplace\",\"status\":\"testing\",\"isEnabled\":false," +
                                "\"subStatuses\":[{\"code\":\"legal_info\"}],\"needTestingState\":\"required\"," +
                                "\"newbie\":false}," +
                                "\"15\":{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                                "\"subStatuses\":[],\"needTestingState\":\"blocked\",\"newbie\":false}}"),
                Arguments.of(1003L, null,
                        "{\"16\":{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
                                "\"subStatuses\":[{\"code\":\"feature_cutoff_types\"}]," +
                                "\"needTestingState\":\"not_required\",\"newbie\":false}," +
                                "\"17\":{\"program\":\"marketplace\",\"status\":\"enabling\",\"isEnabled\":false," +
                                "\"subStatuses\":[{\"code\":\"sandbox\"}],\"needTestingState\":\"not_required\"," +
                                "\"newbie\":false}}"),
                // Пустое тело partnersRequest
                Arguments.of(1004L, "", "{\"18\":{\"program\":\"marketplace\",\"status\":\"disabled\"," +
                        "\"isEnabled\":false,\"subStatuses\":[],\"needTestingState\":\"not_required\"," +
                        "\"newbie\":false}}"),
                // поставщик с id 19 - (FBY, FBY+), 20 - (C&C, DROPSHIP)
                Arguments.of(1005L, null, "{\"19\":{\"program\":\"marketplace\",\"status\":\"failed\"," +
                        "\"isEnabled\":false,\"subStatuses\":[],\"needTestingState\":\"blocked\",\"newbie\":false}," +
                        "\"20\":{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                        "\"subStatuses\":[{\"code\":\"sandbox\"},{\"code\":\"feed_failed\"}]," +
                        "\"needTestingState\":\"not_required\",\"newbie\":false}}")
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

        Mockito.when(patientMboMappingsService.searchMappingsByShopId(any()))
                .thenReturn(MboMappings.SearchMappingsResponse.newBuilder().setTotalCount(0).build());
        Mockito.when(lmsClient.getPartner(eq(1030L))).thenReturn(
                Optional.of(
                        PartnerResponse
                                .newBuilder()
                                .stockSyncEnabled(Boolean.TRUE)
                                .build()
                ));
        OrderLimitDTO testOrderLimitDTO48 = new OrderLimitDTO(48, 10,
                PublicOrderLimitReason.OPERATIONAL_RATING,
                1593430446257L, 100);
        OrderLimitDTO testOrderLimitDTO50 = new OrderLimitDTO(50, 10,
                PublicOrderLimitReason.OPERATIONAL_RATING,
                1593430446257L, 20);
        doReturn(testOrderLimitDTO48).when(aboPublicRestClient).getOrderLimit(eq(48L));
        doReturn(testOrderLimitDTO50).when(aboPublicRestClient).getOrderLimit(eq(50L));
        doThrow(new RuntimeException("")).when(aboPublicRestClient).getOrderLimit(eq(49L));
        doReturn(null).when(aboPublicRestClient).getOrderLimit(eq(30L));

        PushApiLogStatsResponse statsResponse = new PushApiLogStatsResponse().count(1L).errorCount(1L).successCount(0L);
        Mockito.when(logProcessorClient.getLogStatsFromLastEvent(eq(29L), any(Duration.class)))
                .thenReturn(statsResponse);
        Mockito.when(logProcessorClient.hasErrorsInHalfAnHour(eq(List.of(29L)))).thenReturn(List.of(29L));

        Mockito.when(partnerStatusService.getStatusResolvers(any()))
                .thenAnswer(invocation -> {
                    StatusResolversRequest request = invocation.getArgument(0, StatusResolversRequest.class);
                    if (CollectionUtils.isEmpty(request.getResolvers()) ||
                            CollectionUtils.isEmpty(request.getResolvers().get(0).getPartnerIds()) ||
                            request.getResolvers().get(0).getPartnerIds().get(0) != 60L) {
                        return CompletableFuture.completedFuture(new StatusResolversResponse());
                    }

                    return CompletableFuture.completedFuture(new StatusResolversResponse()
                            .addResolversItem(new StatusResolverResults().addResultsItem(new PartnerStatusInfo()
                                    .partnerId(60L)
                                    .status(WizardStepStatus.FAILED)
                                    .enabled(false)
                                    .addSubStatusesItem(new PartnerSubStatusInfo().code("sort_center_not_configured"))
                                    .needTestingState(NeedTestingState.NOT_REQUIRED)
                                    .newbie(false))
                                    .resolver(StatusResolverType.FBS_SORTING_CENTER)
                            )
                    );
                });

        Stream.of(Pair.of(1L, 1L), Pair.of(2L, 2L), Pair.of(3L, 3L), Pair.of(4L, 4L), Pair.of(5L, 5L), Pair.of(6L, 6L),
                        Pair.of(7L, 7L), Pair.of(8L, 8L), Pair.of(9L, 9L), Pair.of(10L, 10L), Pair.of(10L, 10L),
                        Pair.of(11L, 11L), Pair.of(12L, 12L))
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

    @ParameterizedTest
    @DbUnitDataSet(before = "db/marketplaceProgram.before.csv")
    @MethodSource("businessMarketplacesWithExpectedProgramStatuses")
    void businessProgramStatusChecks(long businessId, String body, String expected) throws IOException {
        ResponseEntity<String> entity;
        if (body != null) {
            entity = FunctionalTestHelper.post(businessProgramsStatusUrl(businessId), body);
        } else {
            entity = FunctionalTestHelper.post(businessProgramsStatusUrl(businessId));
        }
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
        Mockito.verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DbUnitDataSet(before = "db/marketplaceProgram.before.csv")
    void businessProgramStatusWithPartnersChecks() throws IOException {
        long businessId = 1010L;
        String expected = "{\"10\":{\"program\":\"marketplace\",\"status\":\"empty\",\"isEnabled\":false," +
                "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}}";
        String body = "{\"partnerIds\":[13,10,11]}";
        ResponseEntity<String> entity = FunctionalTestHelper.post(businessProgramsStatusUrl(businessId), body);
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
        Mockito.verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DbUnitDataSet(before = {"db/marketplaceProgram.before.csv", "db/businessProgram.before.csv"})
    void businessProgramStatusWithSeveralPartnersAndChecks() throws IOException {
        long businessId = 2001L;
        String expected = "{\"2210\":{\"program\":\"marketplace\",\"status\":\"empty\",\"isEnabled\":false," +
                "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}," +
                "\"2211\":{\"program\":\"marketplace\",\"status\":\"testing_failed\",\"isEnabled\":false," +
                "\"subStatuses\":[{\"code\":\"legal_info\"}],\"needTestingState\":\"blocked\",\"newbie\":false}," +
                "\"2221\":{\"program\":\"dropship_by_seller\",\"status\":\"failed\",\"isEnabled\":false," +
                "\"subStatuses\":[{\"code\":\"quality_failed\"}],\"needTestingState\":\"not_required\"," +
                "\"newbie\":false}}";
        String body = "{\"partnerIds\":[13,10,11,2210,2211,2221]}";
        ResponseEntity<String> entity = FunctionalTestHelper.post(businessProgramsStatusUrl(businessId), body);
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
        Mockito.verifyNoMoreInteractions(aboPublicRestClient);
    }

    @Test
    @DbUnitDataSet(before = "db/marketPlaceProgram.enable.dbs.before.csv",
            after = "db/marketPlaceProgram.enable.dbs.after.csv")
    void testShouldEnableDropShipBySellerProgram() throws IOException {
        final String expected = "{\"program\":\"dropship_by_seller\",\"status\":\"enabling\",\"isEnabled\":false," +
                "\"subStatuses\":[],\"needTestingState\":\"not_required\",\"newbie\":false}";
        final String body = "{\"partnerIds\":[999]}";
        final ResponseEntity<String> entity = FunctionalTestHelper.post(marketplaceEnableUrl(9090), body);
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "db/marketPlaceProgram.enable.part.dbs.before.csv",
            after = "db/marketPlaceProgram.enable.part.dbs.after.csv")
    void testShouldNotEnableButUpdateStatusDropShipBySellerProgram() throws IOException {
        final String expected = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\",\"isEnabled\":false," +
                "\"subStatuses\":[{\"code\":\"delivery_not_configured\"}],\"needTestingState\":\"not_required\"," +
                "\"newbie\":false}";
        final String body = "{\"partnerIds\":[999]}";
        final ResponseEntity<String> entity = FunctionalTestHelper.post(marketplaceEnableUrl(9090), body);
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "db/marketPlaceProgram.enable.dbs.before.csv",
            after = "db/marketPlaceProgram.fail.dbs.after.csv")
    void testFailedPreconditionWhileEnableDropShipBySellerProgram() throws IOException {
        final String expected = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\",\"isEnabled\":false," +
                "\"subStatuses\":[{\"code\":\"work_mode\"}, {\"code\":\"delivery_not_configured\"}]," +
                "\"needTestingState\":\"not_required\",\"newbie\":false}";
        final String body = "{\"partnerIds\":[888]}";
        final ResponseEntity<String> entity = FunctionalTestHelper.post(marketplaceEnableUrl(8080), body);
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
    }

    @Test
    @DbUnitDataSet(before = "db/marketPlaceProgram.enable.dbs.before.csv")
    void testFailedPreconditionWhileEnableDropShipBySellerProgram2() throws IOException {
        final String expected = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\",\"isEnabled\":false," +
                "\"subStatuses\":[{\"code\":\"npd_unavailable\"}]," +
                "\"needTestingState\":\"not_required\",\"newbie\":false}";
        final ResponseEntity<String> entity = FunctionalTestHelper.get(campaignProgramsUrl(7070, "dropship_by_seller"));
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(expected, actual);
    }

    private final Map<ParamCheckStatus, Integer> statusToPartnerIdForDBS = Map.of(
            ParamCheckStatus.SUCCESS, 30,
            ParamCheckStatus.REVOKE, 31,
            ParamCheckStatus.FAIL, 32,
            ParamCheckStatus.NEW, 33,
            ParamCheckStatus.DONT_WANT, 34
    );

    /**
     * Для катофов, которые не перечислены
     * <a href="https://wiki.yandex-team.ru/users/dmitryman/statusy-svetofora/#statusyskladovdljadbs">
     * в табличке со статусами складов для DBS</a>
     */
    private final String STORE_STATUSES_FOR_DBS = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"quality_failed\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String STORE_STATUSES_FOR_DBS_REQUIRED = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"quality_failed\"}]," +
            "\"needTestingState\":\"required\",\"newbie\":false}";

    /**
     * Для катофов, которые перечислены
     * <a href="https://wiki.yandex-team.ru/users/dmitryman/statusy-svetofora/#statusyskladovdljadbs">в этой табличке</a>
     * с именем статуса DBS_QUALITY_ISSUES
     */
    private final String DBS_QUALITY_ISSUES = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"quality_failed\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String DBS_QUALITY_ISSUES_REQUIRED = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"quality_failed\"}]," +
            "\"needTestingState\":\"required\",\"newbie\":false}";

    /**
     * Для катофов, которые перечислены
     * <a href="https://wiki.yandex-team.ru/users/dmitryman/statusy-svetofora/#statusyskladovdljadbs">в этой табличке</a>
     * с именем статуса DBS_MANUALLY_SUSPENDED
     */
    private final String DBS_MANUALLY_SUSPENDED = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"quality_failed\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String DBS_SELFCHECK_REQUIRED = "{\"program\":\"dropship_by_seller\",\"status\":\"suspended\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"selfcheck_required\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String CUTOFF_CLONE_EXPECTED_ANSWER = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"clone\"}],\"needTestingState\":\"required\"," +
            "\"newbie\":false}";

    private final String CUTOFF_QUALITY_SERIOUS_EXPECTED_ANSWER = "{\"program\":\"dropship_by_seller\"," +
            "\"status\":\"failed\",\"isEnabled\":false,\"subStatuses\":[{\"code\":\"quality_serious\"}]," +
            "\"needTestingState\":\"required\",\"newbie\":false}";

    private final String CUTOFF_DAILY_ORDER_LIMIT_EXPECTED_ANSWER = "{\"program\":\"dropship_by_seller\"," +
            "\"status\":\"failed\",\"isEnabled\":false,\"subStatuses\":[{\"code\":\"daily_order_limit\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String CUTOFF_ORDER_FINAL_STATUS_NOT_SET_EXPECTED_ANSWER = "{\"program\":\"dropship_by_seller\"," +
            "\"status\":\"failed\",\"isEnabled\":false,\"subStatuses\":[{\"code\":\"order_final_status_not_set\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String DBS_LIMIT_ORDERS = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"limit_orders\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String DBS_LOW_RAITING = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"low_rating\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String API_MASS_ERRORS_FOR_DBS = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"api_mass_errors\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String MASS_CART_DIFF_FOR_DBS = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"cart_diff\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final String QUALITY_OTHER_FOR_DBS = "{\"program\":\"dropship_by_seller\",\"status\":\"failed\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"quality_other\"}]," +
            "\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final Map<FeatureCustomCutoffType, String> expectedAnswersDBS = Map.ofEntries(
            Map.entry(CommonCutoffs.FEED, STORE_STATUSES_FOR_DBS),
            Map.entry(CommonCutoffs.FRAUD, DBS_QUALITY_ISSUES_REQUIRED),
            Map.entry(CommonCutoffs.CLONE, CUTOFF_CLONE_EXPECTED_ANSWER),
            Map.entry(CommonCutoffs.CART_DIFF, DBS_QUALITY_ISSUES_REQUIRED),
            Map.entry(FeatureCutoffType.QUALITY, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.MANAGER, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.PRECONDITION, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.BALANCE_SUSPENDED, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.EXPERIMENT, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.PINGER, DBS_QUALITY_ISSUES),
            Map.entry(FeatureCutoffType.LIMIT_ORDERS, DBS_LIMIT_ORDERS),
            Map.entry(FeatureCutoffType.MANUAL, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.HIDDEN, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.LOGISTIC_POINT_SWITCH, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.LOW_RATING, DBS_LOW_RAITING),
            Map.entry(FeatureCutoffType.MARKETPLACE_CART_DIFF, MASS_CART_DIFF_FOR_DBS),
            Map.entry(FeatureCutoffType.MARKETPLACE_ORDER_NOT_ACCEPTED, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.BY_PARTNER, DBS_MANUALLY_SUSPENDED),
            Map.entry(FeatureCutoffType.MARKETPLACE_PLACEMENT, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.VERTICAL_SHARE_QUALITY, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.VERTICAL_SHARE_OTHER, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.MARKETPLACE_ORDER_PENDING_EXPIRED, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.QUALITY_FRAUD, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.QUALITY_CLONE, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.QUALITY_COMMON, STORE_STATUSES_FOR_DBS),
            Map.entry(FeatureCutoffType.QUALITY_COMMON_OTHER, QUALITY_OTHER_FOR_DBS),
            Map.entry(FeatureCutoffType.PINGER_MASS_ERRORS, API_MASS_ERRORS_FOR_DBS),
            Map.entry(DSBSCutoffs.ORDER_NOT_ACCEPTED, DBS_QUALITY_ISSUES),
            Map.entry(DSBSCutoffs.QUALITY_SERIOUS, CUTOFF_QUALITY_SERIOUS_EXPECTED_ANSWER),
            Map.entry(DSBSCutoffs.QUALITY_OTHER, DBS_QUALITY_ISSUES_REQUIRED),
            Map.entry(DSBSCutoffs.DAILY_ORDER_LIMIT, CUTOFF_DAILY_ORDER_LIMIT_EXPECTED_ANSWER),
            Map.entry(DSBSCutoffs.MODERATION_NEED_INFO, STORE_STATUSES_FOR_DBS),
            Map.entry(DSBSCutoffs.ORDER_FINAL_STATUS_NOT_SET, CUTOFF_ORDER_FINAL_STATUS_NOT_SET_EXPECTED_ANSWER),
            Map.entry(DSBSCutoffs.QUALITY_CHECK_FAILED, STORE_STATUSES_FOR_DBS_REQUIRED),
            Map.entry(FeatureCutoffType.SELFCHECK_REQUIRED, DBS_SELFCHECK_REQUIRED),
            Map.entry(UtilityCutoffs.NEED_TESTING, DBS_QUALITY_ISSUES_REQUIRED)
    );

    private final Map<ParamCheckStatus, Integer> statusToPartnerIdForFBS = Map.of(
            ParamCheckStatus.SUCCESS, 20,
            ParamCheckStatus.REVOKE, 21,
            ParamCheckStatus.FAIL, 22,
            ParamCheckStatus.NEW, 23,
            ParamCheckStatus.DONT_WANT, 24
    );

    private Integer getPartnerByParamCheckStatus(String program, Optional<ParamCheckStatus> maybeParamCheckStatus) {
        var statusToPartner = program.equals("dropship_by_seller") ? statusToPartnerIdForDBS : statusToPartnerIdForFBS;
        if (maybeParamCheckStatus.isEmpty()) {
            // Катоф не влияет на статус
            // Возвращаю того, у которого указан статус FAIL
            return statusToPartner.get(ParamCheckStatus.FAIL);
        }
        return statusToPartner.get(maybeParamCheckStatus.get());
    }

    // Названия брал из этой таблицы
    // https://wiki.yandex-team.ru/users/dmitryman/statusy-svetofora/#statusyskladovdljafbs
    private final String DROPSHIP_REVOKED = "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
            "\"subStatuses\":[],\"needTestingState\":\"blocked\",\"newbie\":false}";

    private final String DROPSHIP_FAILED = "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
            "\"subStatuses\":[{\"code\":\"feature_cutoff_types\"}],\"needTestingState\":\"not_required\"," +
            "\"newbie\":false}";

    private final String DROPSHIP_SANDBOXED = "{\"program\":\"marketplace\",\"status\":\"testing\"," +
            "\"isEnabled\":false,\"subStatuses\":[{\"code\":\"sandbox\"}],\"needTestingState\":\"required\"," +
            "\"newbie\":false}";

    // Нет вот в этой таблице https://wiki.yandex-team.ru/users/dmitryman/statusy-svetofora/#statusyskladovdljafbs
    private final String NOT_IN_TABLE = "{\"program\":\"marketplace\",\"status\":\"failed\",\"isEnabled\":false," +
            "\"subStatuses\":[{\"code\":\"no_loaded_offers\"}],\"needTestingState\":\"not_required\",\"newbie\":false}";

    private final Map<FeatureCustomCutoffType, String> expectedAnswersFBS = Map.ofEntries(
            Map.entry(DSBSCutoffs.ORDER_NOT_ACCEPTED, NOT_IN_TABLE),
            Map.entry(DSBSCutoffs.QUALITY_SERIOUS, DROPSHIP_REVOKED),
            Map.entry(DSBSCutoffs.QUALITY_OTHER, DROPSHIP_FAILED),
            Map.entry(DSBSCutoffs.DAILY_ORDER_LIMIT, NOT_IN_TABLE),
            Map.entry(DSBSCutoffs.MODERATION_NEED_INFO, DROPSHIP_SANDBOXED),
            Map.entry(DSBSCutoffs.ORDER_FINAL_STATUS_NOT_SET, NOT_IN_TABLE),
            Map.entry(DSBSCutoffs.QUALITY_CHECK_FAILED, DROPSHIP_FAILED),
            Map.entry(CommonCutoffs.FEED, DROPSHIP_FAILED),
            Map.entry(CommonCutoffs.FRAUD, DROPSHIP_REVOKED),
            Map.entry(CommonCutoffs.CLONE, DROPSHIP_REVOKED),
            Map.entry(CommonCutoffs.CART_DIFF, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.QUALITY, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.PRECONDITION, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.MANAGER, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.BALANCE_SUSPENDED, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.EXPERIMENT, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.PINGER, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.LIMIT_ORDERS, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.MANUAL, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.HIDDEN, DROPSHIP_SANDBOXED),
            Map.entry(FeatureCutoffType.LOGISTIC_POINT_SWITCH, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.LOW_RATING, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.MARKETPLACE_CART_DIFF, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.MARKETPLACE_ORDER_NOT_ACCEPTED, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.BY_PARTNER, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.MARKETPLACE_PLACEMENT, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.VERTICAL_SHARE_QUALITY, DROPSHIP_REVOKED),
            Map.entry(FeatureCutoffType.VERTICAL_SHARE_OTHER, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.MARKETPLACE_ORDER_PENDING_EXPIRED, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.QUALITY_FRAUD, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.QUALITY_CLONE, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.QUALITY_COMMON, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.QUALITY_COMMON_OTHER, NOT_IN_TABLE),
            Map.entry(FeatureCutoffType.PINGER_MASS_ERRORS, DROPSHIP_FAILED),
            Map.entry(FeatureCutoffType.SELFCHECK_REQUIRED, NOT_IN_TABLE),
            Map.entry(UtilityCutoffs.NEED_TESTING, DROPSHIP_FAILED)
    );


    private final Map<String, Map<FeatureCustomCutoffType, String>> mapExpectedResultByNameProgram =
            Map.of(
                    "dropship_by_seller", expectedAnswersDBS,
                    "marketplace", expectedAnswersFBS
            );

    private static Stream<Arguments> testedCutoffsWithProgramArgumentsStream() {
        var testedCutoffs =
                (new Reflections("ru.yandex.market.core.feature.model"))
                        .getSubTypesOf(FeatureCustomCutoffType.class).stream()
                        .map(cutoffEnum -> Arrays.stream(cutoffEnum.getEnumConstants()))
                        .reduce(Stream.of(), Stream::concat)
                        .filter(type -> !isStartCutoff(type))
                        .collect(Collectors.toList());
        var listProgram = List.of("dropship_by_seller", "marketplace");
        List<Arguments> argumentsList = new ArrayList<>();
        for (var cutoff : testedCutoffs) {
            for (String program : listProgram) {
                argumentsList.add(Arguments.of(cutoff, program));
            }
        }
        return argumentsList.stream();
    }

    @ParameterizedTest
    @DbUnitDataSet(before = "db/marketplaceProgram.testCheckAllCutoffsWithFailPartner.csv")
    @MethodSource("testedCutoffsWithProgramArgumentsStream")
    void testCheckAllCutoffsWithFailPartner(FeatureCustomCutoffType cutoff, String program) throws IOException {
        var partnerId = getPartnerByParamCheckStatus(program, cutoff.getTargetStatus());
        featureCutoffDao.openCutoff(
                buildFeatureCutoffInfo(
                        partnerId,
                        FeatureType.MARKETPLACE,
                        cutoff,
                        cutoff.name(),
                        new Date(),
                        FeatureCutoffReason.MODERATION),
                100500L
        );
        ResponseEntity<String> entity = FunctionalTestHelper.get(campaignProgramsUrl(1000 + partnerId, program));
        JsonNode jsonNode = jacksonMapper.readTree(entity.getBody());
        String actual = jsonNode.findValue("result").toString();
        MbiAsserts.assertJsonEquals(mapExpectedResultByNameProgram.get(program).get(cutoff), actual);
    }

    private static boolean isStartCutoff(FeatureCustomCutoffType type) {
        if (type instanceof FeatureCutoffType) {
            return type == FeatureCutoffType.TESTING || type == FeatureCutoffType.PARTNER;
        }
        return false;
    }

    private static FeatureCutoffInfo buildFeatureCutoffInfo(long shopId,
                                                            FeatureType featureType,
                                                            FeatureCustomCutoffType featureCutoffType,
                                                            String comment,
                                                            Date startDate,
                                                            FeatureCutoffReason reason) {
        return new FeatureCutoffInfo.Builder()
                .setDatasourceId(shopId)
                .setFeatureType(featureType)
                .setFeatureCutoffType(featureCutoffType)
                .setComment(comment)
                .setStartDate(startDate)
                .setReason(reason)
                .build();
    }

    private String campaignProgramsUrl(long campaignId, String typePartner) {
        return baseUrl + String.format("/campaigns/programs/%s?_user_id=123&id=%d", typePartner, campaignId);
    }

    private String marketplaceEnableUrl(long campaignId) {
        return baseUrl + String.format("/campaigns/programs/dropship_by_seller/enable?_user_id=123&id=%d", campaignId);
    }

    private String marketplaceStatusUrl(long campaignId) {
        return baseUrl + String.format("/campaigns/programs/marketplace?_user_id=12345&id=%d", campaignId);
    }

    private String marketplaceFieldsUrl(int campaignId) {
        return baseUrl + String.format("/campaigns/programs/marketplace/fields?_user_id=12345&id=%d", campaignId);
    }

    private String businessProgramsStatusUrl(long businessId) {
        return baseUrl + String.format("/business/%d/programs?_user_id=12345", businessId);
    }
}
