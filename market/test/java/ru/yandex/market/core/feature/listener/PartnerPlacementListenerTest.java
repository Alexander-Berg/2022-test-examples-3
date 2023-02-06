package ru.yandex.market.core.feature.listener;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.meta.PartnerApplicationDAO;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.feature.model.ShopFeatureInfo;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgram;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.partner.placement.resolver.PartnerPlacementFeatureStatusResolver;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.SystemActionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP_BY_SELLER;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE;

@DbUnitDataSet(before = "partnerPlacementListener.before.csv")
public class PartnerPlacementListenerTest extends FunctionalTest {
    private static final long DROPSHIP_BY_SELLER_ID = 1001L;
    private static final long FULFILLMENT_ID = 2000L;
    private static final long CROSSDOCK_ID = 2001L;
    private static final long DROPSHIP_ID = 2002L;

    private static final Map<FeatureCutoffType, PartnerPlacementProgramStatus> CUTOFF_TYPES_WITH_PROGRAM_STATUSES;

    private static final List<FeatureCutoffType> CUSTOM_LOGIC_CUTOFFS = List.of(
            //Эти катоффы не влияют на статус программы, либо программа высчитывается из статуса фичи
            FeatureCutoffType.MANAGER,
            FeatureCutoffType.PARTNER,
            FeatureCutoffType.QUALITY,
            FeatureCutoffType.LOW_RATING,

            //Для этих катоффов программа проставляется по отдельной логике и тестируется отдельно
            FeatureCutoffType.PRECONDITION,

            //Для Товарных Вертикалей не нужно отображать магазины в ПИ, так что модель размещения не требуется
            FeatureCutoffType.VERTICAL_SHARE_QUALITY,
            FeatureCutoffType.VERTICAL_SHARE_OTHER
    );

    static {
        ImmutableMap.Builder<FeatureCutoffType, PartnerPlacementProgramStatus> builder = ImmutableMap.builder();
        builder.put(FeatureCutoffType.EXPERIMENT, PartnerPlacementProgramStatus.TESTED);
        builder.put(FeatureCutoffType.PINGER, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.LIMIT_ORDERS, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.MANUAL, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.HIDDEN, PartnerPlacementProgramStatus.DISABLED);
        builder.put(FeatureCutoffType.LOGISTIC_POINT_SWITCH, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.TESTING, PartnerPlacementProgramStatus.TESTED);
        builder.put(FeatureCutoffType.MARKETPLACE_CART_DIFF, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.MARKETPLACE_ORDER_NOT_ACCEPTED, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.BY_PARTNER, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.BALANCE_SUSPENDED, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.MARKETPLACE_PLACEMENT, PartnerPlacementProgramStatus.CONFIGURE);
        builder.put(FeatureCutoffType.MARKETPLACE_ORDER_PENDING_EXPIRED, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.QUALITY_FRAUD, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.QUALITY_CLONE, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.QUALITY_COMMON, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.QUALITY_COMMON_OTHER, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.PINGER_MASS_ERRORS, PartnerPlacementProgramStatus.FAIL);
        builder.put(FeatureCutoffType.SELFCHECK_REQUIRED, PartnerPlacementProgramStatus.FAIL);
        CUTOFF_TYPES_WITH_PROGRAM_STATUSES = builder.build();
    }

    @Autowired
    private FeatureService featureService;
    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private PartnerApplicationDAO partnerApplicationDAO;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    @Autowired
    @Qualifier("dropshipResolver")
    PartnerPlacementFeatureStatusResolver dropshipResolver;

    @Autowired
    private Clock clock;

    @BeforeEach
    void initMock() {
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
    }

    static Stream<Arguments> getFeatureInfoArguments() {
        return Stream.of(
                //FAIL CROSSDOCK
                Arguments.of(CROSSDOCK_ID, FeatureType.CROSSDOCK,
                        ParamCheckStatus.FAIL, Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.CROSSDOCK)
                                .partnerId(CROSSDOCK_ID)
                                .status(PartnerPlacementProgramStatus.FAIL)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //TESTED CROSSDOCK
                Arguments.of(CROSSDOCK_ID, FeatureType.CROSSDOCK,
                        ParamCheckStatus.NEW, Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.CROSSDOCK)
                                .partnerId(CROSSDOCK_ID)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //SUCCESS CROSSDOCK
                Arguments.of(CROSSDOCK_ID, FeatureType.CROSSDOCK,
                        ParamCheckStatus.SUCCESS, Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.CROSSDOCK)
                                .partnerId(CROSSDOCK_ID)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //DISABLED CROSSDOCK
                Arguments.of(CROSSDOCK_ID, FeatureType.CROSSDOCK,
                        ParamCheckStatus.REVOKE,
                        Set.of()), // не была активирована до конца
                //FAIL DROPSHIP
                Arguments.of(DROPSHIP_ID, DROPSHIP,
                        ParamCheckStatus.FAIL,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.DROPSHIP)
                                .partnerId(DROPSHIP_ID)
                                .status(PartnerPlacementProgramStatus.FAIL)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //TESTED DROPSHIP
                Arguments.of(DROPSHIP_ID, DROPSHIP,
                        ParamCheckStatus.NEW,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.DROPSHIP)
                                .partnerId(DROPSHIP_ID)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //SUCCESS DROPSHIP
                Arguments.of(DROPSHIP_ID, DROPSHIP,
                        ParamCheckStatus.SUCCESS,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.DROPSHIP)
                                .partnerId(DROPSHIP_ID)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //DISABLED DROPSHIP
                Arguments.of(DROPSHIP_ID, DROPSHIP,
                        ParamCheckStatus.REVOKE,
                        Set.of()), // не была активирована до конца
                //FAIL FULFILLMENT
                Arguments.of(FULFILLMENT_ID, FeatureType.MARKETPLACE,
                        ParamCheckStatus.FAIL,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.FULFILLMENT)
                                .partnerId(FULFILLMENT_ID)
                                .status(PartnerPlacementProgramStatus.FAIL)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //CONFIGURE FULFILLMENT
                Arguments.of(FULFILLMENT_ID, FeatureType.MARKETPLACE,
                        ParamCheckStatus.DONT_WANT,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.FULFILLMENT)
                                .partnerId(FULFILLMENT_ID)
                                .status(PartnerPlacementProgramStatus.CONFIGURE)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //TESTED FULFILLMENT
                Arguments.of(FULFILLMENT_ID, FeatureType.MARKETPLACE,
                        ParamCheckStatus.NEW,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.FULFILLMENT)
                                .partnerId(FULFILLMENT_ID)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),

                //DISABLED FULFILLMENT
                Arguments.of(FULFILLMENT_ID, FeatureType.MARKETPLACE,
                        ParamCheckStatus.REVOKE,
                        Set.of()),
                //FAIL DROPSHIP_BY_SELLER
                Arguments.of(DROPSHIP_BY_SELLER_ID, FeatureType.MARKETPLACE_SELF_DELIVERY,
                        ParamCheckStatus.FAIL,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .partnerId(DROPSHIP_BY_SELLER_ID)
                                .status(PartnerPlacementProgramStatus.FAIL)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //TESTED DROPSHIP_BY_SELLER
                Arguments.of(DROPSHIP_BY_SELLER_ID, FeatureType.MARKETPLACE_SELF_DELIVERY,
                        ParamCheckStatus.NEW,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .partnerId(DROPSHIP_BY_SELLER_ID)
                                .status(PartnerPlacementProgramStatus.TESTED)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //TESTED DROPSHIP_BY_SELLER
                Arguments.of(DROPSHIP_BY_SELLER_ID, FeatureType.MARKETPLACE_SELF_DELIVERY,
                        ParamCheckStatus.SUCCESS,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .partnerId(DROPSHIP_BY_SELLER_ID)
                                .status(PartnerPlacementProgramStatus.SUCCESS)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(true)
                                .build())),
                //FAIL DROPSHIP_BY_SELLER
                Arguments.of(DROPSHIP_BY_SELLER_ID, FeatureType.MARKETPLACE_SELF_DELIVERY,
                        ParamCheckStatus.REVOKE,
                        Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                                .partnerId(DROPSHIP_BY_SELLER_ID)
                                .status(PartnerPlacementProgramStatus.FAIL)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build())),
                //NEW CROSSDOCK and MARKETPLACE FAILED
                Arguments.of(2003L, FeatureType.CROSSDOCK,
                        ParamCheckStatus.NEW, Set.of(PartnerPlacementProgram.builder()
                                .program(PartnerPlacementProgramType.CROSSDOCK)
                                .partnerId(2003L)
                                .status(PartnerPlacementProgramStatus.CONFIGURE)
                                .updateAt(LocalDateTime.now())
                                .createAt(LocalDateTime.now())
                                .everActivated(false)
                                .build()))
        );
    }

    @Test
    @DisplayName("Проверка, что все катоффы учтены в ресолвере PARTNER_PLACEMENT_PROGRAM")
    void testAllCutoffsAreResolved() {
        for (FeatureCutoffType cutoffType : FeatureCutoffType.values()) {
            if (!CUTOFF_TYPES_WITH_PROGRAM_STATUSES.containsKey(cutoffType) &&
                    !CUSTOM_LOGIC_CUTOFFS.contains(cutoffType)) {
                Assertions.fail("Ошибка для катоффа " + cutoffType + ": " +
                        "при добавлении нового катоффа (особенно в случае, если он должен менять состояние " +
                        "фичи),проверьте, что он учтен в условии подсчета " +
                        "кол-ва катоффов в v_program_marketplace_status и v_program_feature_status и его поведение " +
                        "отражено  в PartnerPlacementFeatureStatusResolver. Затем добавьте катофф в мапу" +
                        "CUTOFF_TYPES_WITH_PROGRAM_STATUSES, если наличие катоффа прямо влияет на программу, либо в" +
                        "список CUSTOM_LOGIC_CUTOFFS, если статус программы вычисляется каким-то особым образом. " +
                        "В последнем случае стоит написать тест на эту логику в этом классе."
                );
            }
        }
    }

    @ParameterizedTest(name = "Cutoff: {0}, Needed status: {1}")
    @DisplayName("Проверка логики по всем катоффам")
    @DbUnitDataSet(before = "partnerPlacementListener.testAllCutoffs.before.csv")
    @MethodSource("getAllCutoffsTestArguments")
    void testAllCutoffs(FeatureCutoffType cutoff, PartnerPlacementProgramStatus expectedStatus) {
        final long dropshipId = 6002L;

        //Устанавливаем нужный катофф
        PartnerPlacementProgramStatus programStatus =
                dropshipResolver.calcStatus(partnerTypeAwareService.getPartnerTypeAwareInfo(dropshipId),
                        List.of(new ShopFeatureInfo(
                                dropshipId,
                                DROPSHIP,
                                cutoff.getTargetStatus().orElse(ParamCheckStatus.SUCCESS),
                                null,
                                true,
                                List.of(),
                                List.of(new FeatureCutoffInfo.Builder()
                                        .setFeatureType(DROPSHIP)
                                        .setFeatureCutoffType(cutoff)
                                        .setStartDate(new Date())
                                        .setDatasourceId(dropshipId)
                                        .build()
                                )
                        )));

        assertThat(programStatus).isEqualTo(expectedStatus);
    }

    private static Stream<Arguments> getAllCutoffsTestArguments() {
        return CUTOFF_TYPES_WITH_PROGRAM_STATUSES.entrySet().stream()
                .map((entry) -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    @Test
    @DisplayName("Катофф MARKETPLACE PRECONDITION с неподтвержденной заявкой -> программа в CONFIGURE")
    @DbUnitDataSet(before = "partnerPlacementListener.testAllCutoffs.before.csv")
    void testPreconditionCutoffWithInitRequest() {
        long partnerId = 6003L;

        PartnerPlacementProgramStatus programStatus =
                dropshipResolver.calcStatus(partnerTypeAwareService.getPartnerTypeAwareInfo(partnerId),
                        List.of(new ShopFeatureInfo(
                                partnerId,
                                MARKETPLACE,
                                ParamCheckStatus.FAIL,
                                null,
                                true,
                                List.of(),
                                List.of(new FeatureCutoffInfo.Builder()
                                        .setFeatureType(MARKETPLACE)
                                        .setFeatureCutoffType(FeatureCutoffType.PRECONDITION)
                                        .setStartDate(new Date())
                                        .setDatasourceId(partnerId)
                                        .build()
                                )
                        )));

        assertThat(programStatus).isEqualTo(PartnerPlacementProgramStatus.CONFIGURE);
    }

    @Test
    @DisplayName("Катофф MARKETPLACE PRECONDITION с подтвержденной заявкой -> программа в FAIL")
    @DbUnitDataSet(before = "partnerPlacementListener.testAllCutoffs.before.csv")
    void testPreconditionCutoffWithCompletedRequest() {
        long partnerId = 6004L;

        PartnerPlacementProgramStatus programStatus =
                dropshipResolver.calcStatus(partnerTypeAwareService.getPartnerTypeAwareInfo(partnerId),
                        List.of(new ShopFeatureInfo(
                                partnerId,
                                MARKETPLACE,
                                ParamCheckStatus.FAIL,
                                null,
                                true,
                                List.of(),
                                List.of(new FeatureCutoffInfo.Builder()
                                        .setFeatureType(MARKETPLACE)
                                        .setFeatureCutoffType(FeatureCutoffType.PRECONDITION)
                                        .setStartDate(new Date())
                                        .setDatasourceId(partnerId)
                                        .build()
                                )
                        )));

        assertThat(programStatus).isEqualTo(PartnerPlacementProgramStatus.FAIL);
    }

    @Test
    @DisplayName("CROSSDOCK под экспериментом")
    void testExperiment() {
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.FEATURE_MANAGEMENT),
                (transactionStatus, actionId) -> {
                    featureService.changeStatus(1L, new ShopFeature(
                                    ShopFeature.NON_PERSISTED_FEATURE_ID, CROSSDOCK_ID, FeatureType.CROSSDOCK,
                                    ParamCheckStatus.SUCCESS),
                            true, null, null
                    );
                });
        Map<PartnerPlacementProgramType, PartnerPlacementProgram> actualTestedMap =
                partnerPlacementProgramService.getPartnerPlacementPrograms(CROSSDOCK_ID);
        assertEquals(1, actualTestedMap.size());
        PartnerPlacementProgram actualTested = actualTestedMap.get(PartnerPlacementProgramType.CROSSDOCK);
        assertEquals(Long.valueOf(CROSSDOCK_ID), actualTested.getPartnerId());
        assertEquals(PartnerPlacementProgramType.CROSSDOCK, actualTested.getProgram());
        assertEquals(PartnerPlacementProgramStatus.TESTED, actualTested.getStatus());
        assertFalse(actualTested.isEverActivated());
        // снимаем экспермент
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.FEATURE_MANAGEMENT),
                (transactionStatus, actionId) -> {
                    featureService.changeStatus(1L, new ShopFeature(
                                    ShopFeature.NON_PERSISTED_FEATURE_ID, CROSSDOCK_ID, FeatureType.CROSSDOCK,
                                    ParamCheckStatus.SUCCESS),
                            false, null, null
                    );
                });
        Map<PartnerPlacementProgramType, PartnerPlacementProgram> actualSuccessMap =
                partnerPlacementProgramService.getPartnerPlacementPrograms(CROSSDOCK_ID);
        assertEquals(1, actualSuccessMap.size());
        PartnerPlacementProgram actualSuccess = actualSuccessMap.get(PartnerPlacementProgramType.CROSSDOCK);
        assertEquals(Long.valueOf(CROSSDOCK_ID), actualSuccess.getPartnerId());
        assertEquals(PartnerPlacementProgramType.CROSSDOCK, actualSuccess.getProgram());
        assertEquals(PartnerPlacementProgramStatus.SUCCESS, actualSuccess.getStatus());
        assertTrue(actualSuccess.isEverActivated());

    }

    @ParameterizedTest(name = "partnerId: {0}, featureType: {1}, status: {2}")
    @MethodSource("getFeatureInfoArguments")
    void changeStatus(long partnerId, FeatureType featureType, ParamCheckStatus status,
                      Set<PartnerPlacementProgram> programs) {
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.FEATURE_MANAGEMENT),
                (transactionStatus, actionId) -> {
                    featureService.changeStatus(1L, new ShopFeature(
                            ShopFeature.NON_PERSISTED_FEATURE_ID, partnerId, featureType, status)
                    );
                });
        Map<PartnerPlacementProgramType, PartnerPlacementProgram> actualMap =
                partnerPlacementProgramService.getPartnerPlacementPrograms(partnerId);

        assertEquals(programs.size(), actualMap.size());
        for (PartnerPlacementProgram expected : programs) {
            assertTrue("Not found status:" + expected.getProgram(), actualMap.containsKey(expected.getProgram()));
            PartnerPlacementProgram actual = actualMap.get(expected.getProgram());
            assertEquals(expected.getPartnerId(), actual.getPartnerId());
            assertEquals(expected.getProgram(), actual.getProgram());
            assertEquals(expected.getStatus(), actual.getStatus());
            assertEquals(expected.isEverActivated(), actual.isEverActivated());
        }
    }

    @Test
    @DisplayName("DROPSHIP PINGER")
    void testPinger() {
        final long dropshipId = 2004L;
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.FEATURE_MANAGEMENT),
                (transactionStatus, actionId) ->
                        featureService.openCutoff(1L, dropshipId, DROPSHIP, FeatureCutoffType.PINGER)
        );
        Map<PartnerPlacementProgramType, PartnerPlacementProgram> actualFailMap =
                partnerPlacementProgramService.getPartnerPlacementPrograms(dropshipId);
        assertEquals(1, actualFailMap.size());
        PartnerPlacementProgram actualFail = actualFailMap.get(PartnerPlacementProgramType.DROPSHIP);
        assertEquals(Long.valueOf(dropshipId), actualFail.getPartnerId());
        assertEquals(PartnerPlacementProgramType.DROPSHIP, actualFail.getProgram());
        assertEquals(PartnerPlacementProgramStatus.FAIL, actualFail.getStatus());


        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.FEATURE_MANAGEMENT),
                (transactionStatus, actionId) ->
                        featureService.closeCutoff(1L, dropshipId, DROPSHIP, FeatureCutoffType.PINGER)
        );
        Map<PartnerPlacementProgramType, PartnerPlacementProgram> actualSuccessMap =
                partnerPlacementProgramService.getPartnerPlacementPrograms(dropshipId);
        assertEquals(1, actualSuccessMap.size());
        PartnerPlacementProgram actualSuccess = actualSuccessMap.get(PartnerPlacementProgramType.DROPSHIP);
        assertEquals(Long.valueOf(dropshipId), actualSuccess.getPartnerId());
        assertEquals(PartnerPlacementProgramType.DROPSHIP, actualSuccess.getProgram());
        assertEquals(PartnerPlacementProgramStatus.SUCCESS, actualSuccess.getStatus());
    }

    @Test
    @DisplayName("Отключение програмы ДСБС")
    void testDropshipBySellerSwitchOff() {
        long shopId = 2005L;

        Map<PartnerPlacementProgramType, PartnerPlacementProgram> actualMap =
                partnerPlacementProgramService.getPartnerPlacementPrograms(shopId);
        assertEquals(1, actualMap.size());
        assertNotNull(actualMap.get(PartnerPlacementProgramType.DROPSHIP_BY_SELLER));
        assertEquals(PartnerPlacementProgramStatus.SUCCESS,
                actualMap.get(PartnerPlacementProgramType.DROPSHIP_BY_SELLER).getStatus());

        // Отключаем магазину программу ДСБС
        protocolService.operationInTransaction(
                new SystemActionContext(ActionType.FEATURE_MANAGEMENT),
                (transactionStatus, actionId) ->
                        featureService.changeStatus(1L, ShopFeature.of(
                                shopId,
                                DROPSHIP_BY_SELLER,
                                ParamCheckStatus.DONT_WANT)));
        actualMap =
                partnerPlacementProgramService.getPartnerPlacementPrograms(shopId);
        assertEquals(1, actualMap.size());
        assertNotNull(actualMap.get(PartnerPlacementProgramType.DROPSHIP_BY_SELLER));
        assertEquals(PartnerPlacementProgramStatus.DISABLED,
                actualMap.get(PartnerPlacementProgramType.DROPSHIP_BY_SELLER).getStatus());
    }
}
