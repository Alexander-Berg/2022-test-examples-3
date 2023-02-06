package ru.yandex.market.core.feature.impl;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.apache.commons.lang3.BooleanUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.cutoff.model.CutoffAction;
import ru.yandex.market.core.feature.FeatureCutoffService;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.exception.PreconditionFailedException;
import ru.yandex.market.core.feature.exception.WrongFeatureStatusException;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffMessage;
import ru.yandex.market.core.feature.model.FeatureCutoffMinorInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffReason;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeature;
import ru.yandex.market.core.feature.model.ShopFeatureInfo;
import ru.yandex.market.core.feature.model.ShopFeatureListItem;
import ru.yandex.market.core.feature.model.ShopFeatureWithCutoff;
import ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.message.MessageService;
import ru.yandex.market.core.message.PartnerNotificationMessageServiceTest;
import ru.yandex.market.core.moderation.ModerationService;
import ru.yandex.market.core.moderation.event.ModerationEvent;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.protocol.model.ActionType;
import ru.yandex.market.core.protocol.model.ProtocolFunction;
import ru.yandex.market.core.protocol.model.UIDActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.notification.model.WebContent;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.feature.model.FeatureCutoffReason.MANUAL;
import static ru.yandex.market.core.feature.model.FeatureCutoffReason.PERCENT_ARBITRAGE_USER_WIN;
import static ru.yandex.market.core.feature.model.FeatureCutoffReason.PINGER_API;
import static ru.yandex.market.core.feature.model.FeatureCutoffReason.TEST_ORDER;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.HIDDEN;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.MANAGER;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.PARTNER;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.PINGER;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.PRECONDITION;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.QUALITY;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.TESTING;
import static ru.yandex.market.core.feature.model.FeatureType.ALCOHOL;
import static ru.yandex.market.core.feature.model.FeatureType.B2B_SELLER;
import static ru.yandex.market.core.feature.model.FeatureType.B2C_SELLER;
import static ru.yandex.market.core.feature.model.FeatureType.CASHBACK;
import static ru.yandex.market.core.feature.model.FeatureType.CIS;
import static ru.yandex.market.core.feature.model.FeatureType.CPA_20;
import static ru.yandex.market.core.feature.model.FeatureType.CREDITS;
import static ru.yandex.market.core.feature.model.FeatureType.CROSSDOCK;
import static ru.yandex.market.core.feature.model.FeatureType.CUT_PRICE;
import static ru.yandex.market.core.feature.model.FeatureType.DIRECT_CATEGORY_MAPPING;
import static ru.yandex.market.core.feature.model.FeatureType.DIRECT_GOODS_ADS;
import static ru.yandex.market.core.feature.model.FeatureType.DIRECT_SEARCH_SNIPPET_GALLERY;
import static ru.yandex.market.core.feature.model.FeatureType.DIRECT_STANDBY;
import static ru.yandex.market.core.feature.model.FeatureType.DIRECT_STATUS;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP;
import static ru.yandex.market.core.feature.model.FeatureType.DROPSHIP_BY_SELLER;
import static ru.yandex.market.core.feature.model.FeatureType.FULFILLMENT;
import static ru.yandex.market.core.feature.model.FeatureType.FULFILLMENT_AS_A_SERVICE;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_AUCTION;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_PARTNER;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_SELF_DELIVERY;
import static ru.yandex.market.core.feature.model.FeatureType.MEDICINE_COURIER;
import static ru.yandex.market.core.feature.model.FeatureType.ORDER_AUTO_ACCEPT;
import static ru.yandex.market.core.feature.model.FeatureType.PREPAY;
import static ru.yandex.market.core.feature.model.FeatureType.PROMO_CPC;
import static ru.yandex.market.core.feature.model.FeatureType.SELLS_JEWELRY;
import static ru.yandex.market.core.feature.model.FeatureType.SELLS_MEDICINE;
import static ru.yandex.market.core.feature.model.FeatureType.SELLS_ON_DEMAND;
import static ru.yandex.market.core.feature.model.FeatureType.SHOP_LOGO;
import static ru.yandex.market.core.feature.model.FeatureType.SUBSIDIES;
import static ru.yandex.market.core.feature.model.FeatureType.TURBO_PLUS;
import static ru.yandex.market.core.feature.model.FeatureType.VERTICAL_SHARE;
import static ru.yandex.market.core.feature.model.cutoff.UtilityCutoffs.NEED_TESTING;
import static ru.yandex.market.core.param.model.ParamCheckStatus.DONT_WANT;
import static ru.yandex.market.core.param.model.ParamCheckStatus.FAIL;
import static ru.yandex.market.core.param.model.ParamCheckStatus.NEW;
import static ru.yandex.market.core.param.model.ParamCheckStatus.REVOKE;
import static ru.yandex.market.core.param.model.ParamCheckStatus.SUCCESS;

/**
 * Функциональные тесты на {@link DbFeatureService}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "DbFeatureServiceTest.before.csv")
class DbFeatureServiceTest extends FunctionalTest {

    private static final String DEFAULT_FEATURE_MESSAGE = "Feature Message!";

    private static final List<AboScreenshotDto> DEFAULT_SCREENSHOTS =
            List.of(new AboScreenshotDto(17L, "hash1"), new AboScreenshotDto(19L, "hash2"));

    private static final FeatureCutoffMinorInfo DEFAULT_CUTOFF = FeatureCutoffMinorInfo.builder()
            .setUserId(1L)
            .setFeatureCutoffType(QUALITY)
            .setReason(PINGER_API)
            .setMessage(DEFAULT_FEATURE_MESSAGE)
            .setScreenshots(DEFAULT_SCREENSHOTS)
            .build();
    public static final int SHOP_ID = 1;

    @Autowired
    private FeatureService featureService;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private CheckouterAPI checkouterClient;

    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @Autowired
    private FF4ShopsClient ff4ShopsClient;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private SandboxRepository sandboxRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private FeatureCutoffService featureCutoffService;

    @Autowired
    private Clock clock;

    @Autowired
    private FeatureCutoffInternalService featureCutoffInternalService;

    @Autowired
    EnvironmentService environmentService;

    private static WebContent createPersistentNotificationStub(String subject, String body) {
        var wc = new WebContent();
        wc.setSubject(subject);
        wc.setBody(body);
        return wc;
    }

    static Stream<Arguments> getFeatureInfoArguments() {
        return Stream.of(
                //включенная фича
                Arguments.of(1l, SUBSIDIES,
                        ShopFeatureInfo.builder()
                                .withShopId(1l)
                                .withFeatureId(SUBSIDIES)
                                .withStatus(SUCCESS)
                                .build()),
                //отключенная фича
                Arguments.of(1l, PROMO_CPC,
                        ShopFeatureInfo.builder()
                                .withShopId(1l)
                                .withFeatureId(PROMO_CPC)
                                .withStatus(FAIL)
                                .withCanEnable(true)
                                .withRecentMessage(new FeatureCutoffMessage(1l, PROMO_CPC, "sub", "body", List.of()))
                                .withCutoffs(List.of(
                                        new FeatureCutoffInfo.Builder()
                                                .setId(1l)
                                                .setDatasourceId(1l)
                                                .setFeatureCutoffType(QUALITY)
                                                .setFeatureType(PROMO_CPC)
                                                .setComment("q")
                                                .setReason(PINGER_API)
                                                .setRequiresModeration(false)
                                                .setRestrictsIndexation(false)
                                                .setStartDate(Timestamp.valueOf(LocalDateTime.of(2017, 1, 1, 0, 0)))
                                                .build(),
                                        new FeatureCutoffInfo.Builder()
                                                .setId(2l)
                                                .setDatasourceId(1l)
                                                .setFeatureCutoffType(MANAGER)
                                                .setFeatureType(PROMO_CPC)
                                                .setComment("q")
                                                .setRequiresModeration(true)
                                                .setRestrictsIndexation(true)
                                                .setStartDate(Timestamp.valueOf(LocalDateTime.of(2018, 1, 1, 0, 0)))
                                                .build()))
                                .build()));
    }

    @BeforeEach
    void setUp() {
        PersonStructure person = new PersonStructure();
        person.setPersonId(1001L);
        person.setIsPartner(BooleanUtils.toBoolean(PersonStructure.TYPE_GENERAL));
        when(balanceService.getClientPersons(eq(1L), eq(PersonStructure.TYPE_GENERAL)))
                .thenReturn(List.of(person));
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class).build()));
        when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        PartnerNotificationMessageServiceTest.mockPN(partnerNotificationClient);
        when(clock.instant()).thenReturn(Clock.systemDefaultZone().instant());
    }

    @Test
    @DbUnitDataSet(before = "datasource.csv")
    void getFeatureDefault() {
        ShopFeature feature = featureService.getFeature(1L, SUBSIDIES);
        assertThat(feature.getShopId()).isEqualTo(1L);
        assertThat(feature.getFeatureType()).isEqualTo(SUBSIDIES);
        assertThat(feature.getStatus()).isEqualTo(DONT_WANT);
    }

    /**
     * Тест проверяет, что при вызове
     * {@link DbFeatureService#lockFeature(long, ru.yandex.market.core.feature.model.FeatureType, long)} создается
     * запись в таблице {@code shops_web.feature} на которой берется блокировка.
     * <p>
     * Кейс дополнительно проверяет, что для программы {@link FeatureType#SUBSIDIES} вместе со статусом
     * {@code ru.yandex.market.core.param.model.ParamCheckStatus#DONT_WANT} открывается
     * {@code ru.yandex.market.core.feature.model.FeatureCutoffType#PARTNER} отключение.
     */
    @Test
    @DbUnitDataSet(before = "datasource.csv", after = "lockSubsidiesNonExistingFeature.after.csv")
    void lockSubsidiesNonExistingFeature() {
        ShopFeature feature = featureService.lockFeature(1, SUBSIDIES, 100500);
        assertThat(feature).isEqualTo(new ShopFeature(1L, 1, SUBSIDIES, DONT_WANT));
    }

    /**
     * Аналогично {@link DbFeatureServiceTest#lockSubsidiesNonExistingFeature()},
     * но для программы {@link FeatureType#PROMO_CPC}.
     * <p>
     * Кейс дополнительно проверяет, что для программы {@link FeatureType#PROMO_CPC} в статусе по-умолчанию
     * {@code ru.yandex.market.core.param.model.ParamCheckStatus#SUCCESS} отключений не открывается.
     */
    @Test
    @DbUnitDataSet(before = "datasource.csv", after = "lockPromoCpcNonExistingFeature.after.csv")
    void lockPromoCpcNonExistingFeature() {
        ShopFeature feature = featureService.lockFeature(1, PROMO_CPC, 100500);
        assertThat(feature).isEqualTo(new ShopFeature(1L, 1, PROMO_CPC, SUCCESS));
    }

    /**
     * Тест проверяет, что при блокировке программы, для которой уже есть запись в {@code shops_web.feature},
     * ничего кроме блокировки не происходит.
     */
    @Test
    @DbUnitDataSet(
            before = {"datasource.csv", "ensureNoActionWhenFeatureRecordExists.before.csv"},
            after = "ensureNoActionWhenFeatureRecordExists.before.csv"
    )
    void ensureNoActionWhenFeatureRecordExists() {
        ShopFeature feature = featureService.lockFeature(1, SUBSIDIES, 100500);
        assertThat(feature).isEqualTo(new ShopFeature(1L, 1, SUBSIDIES, DONT_WANT));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "feature.csv"})
    void getFeature() {
        ShopFeature feature = featureService.getFeature(1, SUBSIDIES);
        assertThat(feature).isEqualTo(new ShopFeature(1, 1, SUBSIDIES, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = "datasource.csv", after = "changeStatusAndNotify.after.csv")
    void changeStatusAndNotify() {
        featureService.changeStatusAndNotify(100500, ShopFeature.of(1, SUBSIDIES, FAIL), false, null, DEFAULT_CUTOFF);
    }

    @Test
    @DbUnitDataSet(before = "datasource.csv")
    void changeStatusAndNotifyWithExperimentSuccessMarketplaceTransition() {
        assertThat(featureService.getCutoff(2, MARKETPLACE, FeatureCutoffType.EXPERIMENT)).isEmpty();

        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(2, MARKETPLACE, SUCCESS),
                true,
                null,
                DEFAULT_CUTOFF);
        assertThat(featureService.getCutoff(2, MARKETPLACE, FeatureCutoffType.EXPERIMENT)).isPresent();
        assertThat(featureService.getCutoff(2, MARKETPLACE, PRECONDITION)).isEmpty();

        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(2, MARKETPLACE, SUCCESS),
                true,
                null,
                DEFAULT_CUTOFF);
        assertThat(featureService.getCutoff(2, MARKETPLACE, FeatureCutoffType.EXPERIMENT)).isPresent();
        assertThat(featureService.getCutoff(2, MARKETPLACE, PRECONDITION)).isEmpty();

        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(2, MARKETPLACE, SUCCESS),
                false,
                null,
                DEFAULT_CUTOFF);
        assertThat(featureService.getCutoff(2, MARKETPLACE, FeatureCutoffType.EXPERIMENT)).isEmpty();
        assertThat(featureService.getCutoff(2, MARKETPLACE, PRECONDITION)).isEmpty();

        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(2, MARKETPLACE, SUCCESS),
                true,
                null,
                DEFAULT_CUTOFF);
        assertThat(featureService.getCutoff(2, MARKETPLACE, FeatureCutoffType.EXPERIMENT)).isPresent();
        assertThat(featureService.getCutoff(2, MARKETPLACE, PRECONDITION)).isEmpty();
    }

    @Test
    @DbUnitDataSet(before = "datasource.csv")
    void changeStatusAndNotifyWithExperimentFail() {
        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(2, MARKETPLACE, SUCCESS),
                true,
                null,
                DEFAULT_CUTOFF);
        assertThat(featureService.getCutoff(2, MARKETPLACE, FeatureCutoffType.EXPERIMENT)).isPresent();
        Assertions.assertThrows(
                WrongFeatureStatusException.class,
                () -> featureService.changeStatusAndNotify(
                        1,
                        ShopFeature.of(2, MARKETPLACE, FAIL),
                        true,
                        null,
                        DEFAULT_CUTOFF)
        );
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failFeature.before.csv"}, after = "failFeature.after.csv")
    void failFeature() {
        featureService.changeStatus(100500, ShopFeature.of(1, SUBSIDIES, FAIL), QUALITY);
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "fixFeature.before.csv"}, after = "fixFeature.after.csv")
    void fixFeature() {
        featureService.changeStatus(100500, ShopFeature.of(1, SUBSIDIES, NEW));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "initialEnableFeature.before.csv"}, after = "initialEnableFeature" +
            ".after.csv")
    void initialEnableFeature() {
        featureService.changeStatus(100500, ShopFeature.of(1, SUBSIDIES, NEW));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "initialEnableFeaturePI.before.csv"},
            after = "initialEnableFeaturePI.after.csv")
    void initialEnableFeaturePI() {
        protocolService.actionInTransaction(
                new UIDActionContext(ActionType.FEATURE_MANAGEMENT, 100500L),
                (ProtocolFunction<Void>) (transactionStatus, actionId) -> {
                    featureService.changeStatus(actionId, ShopFeature.of(1, SUBSIDIES, NEW));
                    return null;
                });
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "disableEnabledFeature.before.csv"},
            after = "disableEnabledFeature.after.csv")
    void disableEnabledFeature() {
        featureService.changeStatus(100500, ShopFeature.of(1, SUBSIDIES, DONT_WANT));
    }

    @Test
    @DbUnitDataSet(before = "getEnabledShopIds.before.csv")
    void getEnabledShopIdsDefaultOff() {
        List<Long> result = featureService.getEnabledShopIds(SUBSIDIES);
        assertThat(result).isEqualTo(List.of(3L));
    }

    @Test
    @DbUnitDataSet(before = "getEnabledShopIds.before.csv")
    void getEnabledDropshipSupplierDefaultRevoke() {
        List<Long> result = featureService.getEnabledShopIds(DROPSHIP);
        assertThat(result).isEqualTo(List.of(100L));
    }

    @Test
    @DbUnitDataSet(before = "getShopIdsForModeration.before.csv")
    void getShopIdsForModerationInPreview() {
        List<Long> result = featureService.getShopIdsForModeration(PROMO_CPC);
        assertThat(result).isEqualTo(List.of(3L));
    }

    @Test
    @DbUnitDataSet(before = "getShopIdsForModeration.before.csv")
    void getShopIdsForModeration() {
        List<Long> result = featureService.getShopIdsForModeration(SUBSIDIES);
        assertThat(result).isEqualTo(List.of(2L));
    }

    @Test
    @DbUnitDataSet(before = "getEnabledShopIds.before.csv")
    void getEnabledShopIdsDefaultOn() {
        List<Long> result = featureService.getEnabledShopIds(PROMO_CPC);
        assertThat(result).isEqualTo(List.of(3L, 6L));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "singleCutoff.csv"})
    void getRecentMessagesSingleShop() {
        doReturn(Map.of(
                1101L, createPersistentNotificationStub("subj1", "body1")
        )).when(messageService).getCutoffNotificationMessages(anyCollection());
    }

    @Test
    @DbUnitDataSet(before = "getRecentMessagesMultiShop.csv")
    void getRecentMessagesMultiShop() {
        doReturn(Map.of(
                1011L, createPersistentNotificationStub("subj11", "body11"),
                1013L, createPersistentNotificationStub("subj13", "body13")
        )).when(messageService).getCutoffNotificationMessages(anyCollection());

        Collection<FeatureCutoffMessage> msgs = featureService.getRecentMessages(
                List.of(1L, 2L, 3L),
                Set.of(SUBSIDIES),
                List.of(QUALITY, MANAGER)
        ).values();
        assertThat(msgs).containsExactlyInAnyOrder(
                new FeatureCutoffMessage(1, SUBSIDIES, "subj11", "body11", List.of()),
                new FeatureCutoffMessage(2, SUBSIDIES, "subj13", "body13", List.of())
        );
    }

    @ParameterizedTest(name = "shopId: {0}, featureType: {1}")
    @MethodSource("getFeatureInfoArguments")
    @DbUnitDataSet(before = "getFeatureInfo.before.csv")
    void getFeatureInfo(long shopId, FeatureType featureType, ShopFeatureInfo expected) {
        if (shopId == 2) {
            return;
        }
        doReturn(Map.of(
                1L, createPersistentNotificationStub("sub", "body")
        )).when(messageService).getCutoffNotificationMessages(anyCollection());

        ShopFeatureInfo actual = featureService.getFeatureInfo(shopId, featureType);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DbUnitDataSet(before = "getExtendedInfo.before.csv")
    void getExtendedInfo() {
        List<ShopFeatureInfo> infos = featureService.getExtendedInfo(List.of(1L, 2L), Set.of(SUBSIDIES));
        assertThat(infos).containsExactlyInAnyOrder(
                makeFeatureInfo(1, null, SUBSIDIES, SUCCESS),
                makeFeatureInfo(2, Set.of(createTestSubsidiesCutoff()), SUBSIDIES, DONT_WANT)
        );
    }

    private static ShopFeatureWithCutoff makeFeatureWithCutoff(
            long id,
            int shopId,
            @Nullable Collection<FeatureCutoffInfo> cutoffs,
            FeatureType featureType,
            ParamCheckStatus paramCheckStatus
    ) {
        return ShopFeatureWithCutoff.builder()
                .cutoffs(cutoffs)
                .featureType(featureType)
                .shopId(shopId)
                .status(paramCheckStatus)
                .id(id)
                .build();
    }

    /**
     * Тест по 4 раза проверяет получение дефолтного статуса фичи для всех доступных фич.
     * При добавлении новой фичи этот тест падает, необходимо в конец списка ассерта добавить
     * 4 записи для магазинов с {@code id 1, 2, 100, 101} и дефолтным статусом фичи.
     *
     * @see DbFeatureServiceTest#getFeatureWithCutoffAllFeatures()
     */
    @Test
    @DbUnitDataSet(before = "getExtendedInfo.before.csv")
    void getExtendedInfoAllFeatures() {
        List<ShopFeatureInfo> infos = featureService.getExtendedInfo(List.of(1L, 2L, 100L, 101L),
                Arrays.asList(FeatureType.values()));
        assertThat(infos).containsExactlyInAnyOrder(
                makeFeatureInfo(1, null, SUBSIDIES, SUCCESS),
                makeFeatureInfo(2, Set.of(createTestSubsidiesCutoff()), SUBSIDIES, DONT_WANT),
                makeFeatureInfo(1, null, PROMO_CPC, SUCCESS),
                makeFeatureInfo(2, Set.of(createTestQualityCutoff(), createTestManagerCutoff()), PROMO_CPC, FAIL),
                makeFeatureInfo(100, null, FULFILLMENT, DONT_WANT),
                makeFeatureInfo(101, null, FULFILLMENT, DONT_WANT),
                makeFeatureInfo(1, null, CPA_20, DONT_WANT),
                makeFeatureInfo(2, null, CPA_20, DONT_WANT),
                makeFeatureInfo(100, null, DROPSHIP, DONT_WANT),
                makeFeatureInfo(100, null, PREPAY, SUCCESS),
                makeFeatureInfo(101, null, DROPSHIP, DONT_WANT),
                makeFeatureInfo(101, null, PREPAY, REVOKE),
                makeFeatureInfo(2, null, CASHBACK, DONT_WANT),
                makeFeatureInfo(1, null, CASHBACK, DONT_WANT),
                makeFeatureInfo(100, Set.of(createHiddenCutoff(CROSSDOCK, 100),
                        createPartnerCutoff(CROSSDOCK, 100)), CROSSDOCK, DONT_WANT),
                makeFeatureInfo(101, Set.of(createHiddenCutoff(CROSSDOCK, 101),
                        createPartnerCutoff(CROSSDOCK, 101)), CROSSDOCK, DONT_WANT),
                makeFeatureInfo(1, null, SHOP_LOGO, DONT_WANT),
                makeFeatureInfo(2, null, SHOP_LOGO, DONT_WANT),
                makeFeatureInfo(100, Set.of(createPreconditionCutoff(100)), MARKETPLACE, FAIL),
                makeFeatureInfo(101, Set.of(createPreconditionCutoff(101)), MARKETPLACE, FAIL),
                makeFeatureInfo(1, null, ALCOHOL, DONT_WANT),
                makeFeatureInfo(2, null, ALCOHOL, DONT_WANT),
                makeFeatureInfo(1, null, CUT_PRICE, DONT_WANT),
                makeFeatureInfo(2, null, CUT_PRICE, DONT_WANT),
                makeFeatureInfo(100, null, CUT_PRICE, DONT_WANT),
                makeFeatureInfo(101, null, CUT_PRICE, DONT_WANT),
                makeFeatureInfo(1, null, CREDITS, DONT_WANT),
                makeFeatureInfo(2, null, CREDITS, DONT_WANT),
                makeFeatureInfo(1, null, MARKETPLACE_PARTNER, DONT_WANT),
                makeFeatureInfo(2, null, MARKETPLACE_PARTNER, DONT_WANT),
                makeFeatureInfo(1, null, DIRECT_CATEGORY_MAPPING, DONT_WANT),
                makeFeatureInfo(2, null, DIRECT_CATEGORY_MAPPING, DONT_WANT),
                makeFeatureInfo(1, null, MEDICINE_COURIER, DONT_WANT),
                makeFeatureInfo(2, null, MEDICINE_COURIER, DONT_WANT),
                makeFeatureInfo(100, null, MEDICINE_COURIER, DONT_WANT),
                makeFeatureInfo(101, null, MEDICINE_COURIER, DONT_WANT),
                makeFeatureInfo(1, Set.of(createPartnerCutoff(TURBO_PLUS, 1)), TURBO_PLUS, DONT_WANT),
                makeFeatureInfo(2, Set.of(createPartnerCutoff(TURBO_PLUS, 2)), TURBO_PLUS, DONT_WANT),
                makeFeatureInfo(1, Set.of(createPreconditionCutoff(1)), MARKETPLACE, FAIL),
                makeFeatureInfo(2, Set.of(createPreconditionCutoff(2)), MARKETPLACE, FAIL),
                makeFeatureInfo(2, Set.of(createPartnerCutoff(MARKETPLACE_SELF_DELIVERY, 2)),
                        MARKETPLACE_SELF_DELIVERY, DONT_WANT),
                makeFeatureInfo(1, Set.of(createPartnerCutoff(MARKETPLACE_SELF_DELIVERY, 1)),
                        MARKETPLACE_SELF_DELIVERY, DONT_WANT),
                makeFeatureInfo(2, Set.of(createPartnerCutoff(DROPSHIP_BY_SELLER, 2)),
                        DROPSHIP_BY_SELLER, DONT_WANT),
                makeFeatureInfo(1, Set.of(createPartnerCutoff(DROPSHIP_BY_SELLER, 1)),
                        DROPSHIP_BY_SELLER, DONT_WANT),
                makeFeatureInfo(100, null, MARKETPLACE_AUCTION, DONT_WANT),
                makeFeatureInfo(101, null, MARKETPLACE_AUCTION, DONT_WANT),
                makeFeatureInfo(1, null, MARKETPLACE_AUCTION, DONT_WANT),
                makeFeatureInfo(2, null, MARKETPLACE_AUCTION, DONT_WANT),
                makeFeatureInfo(100, null, ORDER_AUTO_ACCEPT, DONT_WANT),
                makeFeatureInfo(101, null, ORDER_AUTO_ACCEPT, DONT_WANT),
                makeFeatureInfo(1, null, CIS, SUCCESS),
                makeFeatureInfo(2, null, CIS, SUCCESS),
                makeFeatureInfo(100, null, CIS, SUCCESS),
                makeFeatureInfo(101, null, CIS, SUCCESS),
                makeFeatureInfo(1, null, DIRECT_STANDBY, DONT_WANT),
                makeFeatureInfo(1, null, DIRECT_SEARCH_SNIPPET_GALLERY, DONT_WANT),
                makeFeatureInfo(1, null, DIRECT_GOODS_ADS, DONT_WANT),
                makeFeatureInfo(1, null, VERTICAL_SHARE, DONT_WANT),
                makeFeatureInfo(2, null, DIRECT_STANDBY, DONT_WANT),
                makeFeatureInfo(2, null, DIRECT_SEARCH_SNIPPET_GALLERY, DONT_WANT),
                makeFeatureInfo(2, null, DIRECT_GOODS_ADS, DONT_WANT),
                makeFeatureInfo(2, null, VERTICAL_SHARE, DONT_WANT),
                makeFeatureInfo(100, null, VERTICAL_SHARE, DONT_WANT),
                makeFeatureInfo(101, null, VERTICAL_SHARE, DONT_WANT),
                makeFeatureInfo(1, Set.of(createPartnerCutoff(DIRECT_STATUS, 1)), DIRECT_STATUS, DONT_WANT),
                makeFeatureInfo(2, Set.of(createPartnerCutoff(DIRECT_STATUS, 2)), DIRECT_STATUS, DONT_WANT),
                makeFeatureInfo(1, null, SELLS_JEWELRY, DONT_WANT),
                makeFeatureInfo(2, null, SELLS_JEWELRY, DONT_WANT),
                makeFeatureInfo(100, null, SELLS_JEWELRY, DONT_WANT),
                makeFeatureInfo(101, null, SELLS_JEWELRY, DONT_WANT),
                makeFeatureInfo(1, null, SELLS_MEDICINE, DONT_WANT),
                makeFeatureInfo(2, null, SELLS_MEDICINE, DONT_WANT),
                makeFeatureInfo(100, null, SELLS_MEDICINE, DONT_WANT),
                makeFeatureInfo(101, null, SELLS_MEDICINE, DONT_WANT),
                makeFeatureInfo(1, null, ORDER_AUTO_ACCEPT, DONT_WANT),
                makeFeatureInfo(2, null, ORDER_AUTO_ACCEPT, DONT_WANT),
                makeFeatureInfo(100, null, FULFILLMENT_AS_A_SERVICE, DONT_WANT),
                makeFeatureInfo(101, null, FULFILLMENT_AS_A_SERVICE, DONT_WANT),
                makeFeatureInfo(1, Set.of(createHiddenCutoff(B2B_SELLER, 1)), B2B_SELLER, DONT_WANT),
                makeFeatureInfo(2, Set.of(createHiddenCutoff(B2B_SELLER, 2)), B2B_SELLER, DONT_WANT),
                makeFeatureInfo(100, Set.of(createHiddenCutoff(B2B_SELLER, 100)), B2B_SELLER, DONT_WANT),
                makeFeatureInfo(101, Set.of(createHiddenCutoff(B2B_SELLER, 101)), B2B_SELLER, DONT_WANT),
                makeFeatureInfo(1, null, SELLS_ON_DEMAND, DONT_WANT),
                makeFeatureInfo(2, null, SELLS_ON_DEMAND, DONT_WANT),
                makeFeatureInfo(100, null, SELLS_ON_DEMAND, DONT_WANT),
                makeFeatureInfo(101, null, SELLS_ON_DEMAND, DONT_WANT),
                makeFeatureInfo(1, null, B2C_SELLER, SUCCESS),
                makeFeatureInfo(2, null, B2C_SELLER, SUCCESS),
                makeFeatureInfo(100, null, B2C_SELLER, SUCCESS),
                makeFeatureInfo(101, null, B2C_SELLER, SUCCESS)
        );
    }

    private static ShopFeatureInfo makeFeatureInfo(
            int shopId,
            @Nullable Collection<FeatureCutoffInfo> cutoffs,
            FeatureType featureType,
            ParamCheckStatus paramCheckStatus
    ) {
        return ShopFeatureInfo.builder()
                .withCutoffs(cutoffs)
                .withFeatureId(featureType)
                .withRecentMessage(null)
                .withShopId(shopId)
                .withStatus(paramCheckStatus)
                .build();
    }

    @Test
    @DbUnitDataSet(before = "getExtendedInfo.before.csv")
    void getFeatureWithCutoff() {
        List<ShopFeatureWithCutoff> infos = featureService.getFeatureWithCutoff(List.of(1L, 2L), Set.of(SUBSIDIES));
        assertThat(infos).containsExactlyInAnyOrder(
                makeFeatureWithCutoff(1001, 1, List.of(), SUBSIDIES, SUCCESS),
                makeFeatureWithCutoff(1003, 2, List.of(createTestSubsidiesCutoff()), SUBSIDIES, DONT_WANT)
        );
    }

    /**
     * Тест проверяет поведение метода {@link FeatureService#getFeatureWithCutoff} для дефолтного состояния каждой
     * из доступных фич.
     *
     * @see DbFeatureServiceTest#getExtendedInfoAllFeatures()
     */
    @Test
    @DbUnitDataSet(before = "getExtendedInfo.before.csv")
    void getFeatureWithCutoffAllFeatures() {
        List<ShopFeatureWithCutoff> infos = featureService.getFeatureWithCutoff(List.of(1L),
                List.of(FeatureType.values()));
        assertThat(infos).containsExactlyInAnyOrder(
                makeFeatureWithCutoff(1001, 1, List.of(), SUBSIDIES, SUCCESS),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), FULFILLMENT,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), PROMO_CPC, SUCCESS),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), CPA_20, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), CASHBACK,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), SHOP_LOGO,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), ALCOHOL, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), CUT_PRICE, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), CREDITS, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), MARKETPLACE_PARTNER,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(),
                        DIRECT_CATEGORY_MAPPING, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), MEDICINE_COURIER,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1,
                        List.of(createPartnerCutoff(TURBO_PLUS, 1)), TURBO_PLUS, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1,
                        List.of(createPreconditionCutoff(1)), MARKETPLACE, FAIL),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1,
                        List.of(createPartnerCutoff(MARKETPLACE_SELF_DELIVERY, 1)),
                        MARKETPLACE_SELF_DELIVERY, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1,
                        List.of(createPartnerCutoff(DROPSHIP_BY_SELLER, 1)),
                        DROPSHIP_BY_SELLER, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), MARKETPLACE_AUCTION,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), CIS, SUCCESS),

                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), DIRECT_STANDBY,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(),
                        DIRECT_SEARCH_SNIPPET_GALLERY, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), DIRECT_GOODS_ADS,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), VERTICAL_SHARE,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1,
                        List.of(createHiddenCutoff(CROSSDOCK, 1),
                                createPartnerCutoff(CROSSDOCK, 1)), CROSSDOCK, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(),
                        ORDER_AUTO_ACCEPT, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), PREPAY, SUCCESS),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1,
                        List.of(createPartnerCutoff(DROPSHIP, 1)), DROPSHIP, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), FeatureType.DAAS,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(),
                        FeatureType.RED_MARKET
                        , DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(),
                        FeatureType.FMCG_PARTNER, DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(createPartnerCutoff(DIRECT_STATUS, 1)), DIRECT_STATUS,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), SELLS_JEWELRY,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), SELLS_MEDICINE,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), FULFILLMENT_AS_A_SERVICE,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(createHiddenCutoff(B2B_SELLER, 1)), B2B_SELLER,
                        DONT_WANT),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), B2C_SELLER,
                        SUCCESS),
                makeFeatureWithCutoff(ShopFeature.NON_PERSISTED_FEATURE_ID, 1, List.of(), SELLS_ON_DEMAND,
                        DONT_WANT)
        );
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failFromRevokeSeveralCutoffs.before.csv"},
            after = "failFromRevokeSeveralCutoffs.after.csv")
    void failFromRevokeSeveralCutoffs() {
        featureService.changeStatus(100500, ShopFeature.of(1, SUBSIDIES, FAIL), QUALITY);
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failRevokeFail.before.csv"}, after = "failRevokeFail.after.csv")
    void failRevokeFail() {
        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(1, SUBSIDIES, FAIL),
                false,
                null,
                FeatureCutoffMinorInfo.builder()
                        .setFeatureCutoffType(QUALITY)
                        .setReason(MANUAL)
                        .setMessage(DEFAULT_FEATURE_MESSAGE)
                        .build());
        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(1, SUBSIDIES, REVOKE),
                false,
                null,
                FeatureCutoffMinorInfo.builder()
                        .setFeatureCutoffType(MANAGER)
                        .setReason(PERCENT_ARBITRAGE_USER_WIN)
                        .setMessage(DEFAULT_FEATURE_MESSAGE)
                        .build());
        featureService.changeStatusAndNotify(
                100500,
                ShopFeature.of(1, SUBSIDIES, FAIL),
                false,
                null,
                FeatureCutoffMinorInfo.builder()
                        .setFeatureCutoffType(QUALITY)
                        .setReason(PINGER_API)
                        .setMessage(DEFAULT_FEATURE_MESSAGE)
                        .build());

        // Отправляем по 1 уведомлению за каждое отключение.
        verify(partnerNotificationClient, times(3)).sendNotification(any());
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "disableEnabledFeature.before.csv"})
    void invalidateCacheWhenFeatureChanged() {
        //add to memcache
        featureService.getFeatureInfo(1, SUBSIDIES);

        featureService.changeStatus(100500, ShopFeature.of(1, SUBSIDIES, DONT_WANT));
        ShopFeatureInfo featureInfo = featureService.getFeatureInfo(1, SUBSIDIES);
        assertThat(featureInfo.getStatus()).isEqualTo(DONT_WANT);
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "disableEnabledFeature.before.csv"})
    void invalidateCacheWhenFeatureCheckPrecondition() {
        //add to memcache
        featureService.getFeatureInfo(1, SUBSIDIES);

        featureService.checkPreconditions(1, SUBSIDIES, 123);
        ShopFeatureInfo featureInfo = featureService.getFeatureInfo(1, SUBSIDIES);
        assertThat(featureInfo.getStatus()).isEqualTo(FAIL);
    }

    @Test
    @DbUnitDataSet(before = "openCloseCutoff.before.csv")
    void invalidateCacheWhenOpenCloseCutoff() {
        //add to memcache
        ShopFeatureInfo featureInfo = featureService.getFeatureInfo(1, MARKETPLACE);
        assertThat(featureInfo.getCutoffs()).isNull();

        featureService.openCutoff(123, 1, MARKETPLACE, PINGER);
        featureInfo = featureService.getFeatureInfo(1, MARKETPLACE);
        assertThat(featureInfo.getCutoffs()).hasSize(1);

        featureService.closeCutoff(123, 1, MARKETPLACE, PINGER);
        featureInfo = featureService.getFeatureInfo(1, MARKETPLACE);
        assertThat(featureInfo.getCutoffs()).isNull();
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failDropshipFeature.before.csv"},
            after = "failDropshipFeature.after.csv")
    void failDropship() {
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, FAIL), QUALITY);
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failNoFulfilmentLinkDropshipFeature.before.csv"})
    void dropshipHasNotFulfilment() {
        PreconditionFailedException exception = Assertions.assertThrows(
                PreconditionFailedException.class,
                () -> featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS)));
        assertThat(exception.getMessage()).isEqualTo("dropship-has-not-fulfilment");
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failApiNotReadyDropshipFeature.before.csv"})
    void dropshipApiNotReady() {
        PreconditionFailedException exception = Assertions.assertThrows(
                PreconditionFailedException.class,
                () -> featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS)));
        assertThat(exception.getMessage()).isEqualTo("api-not-ready");
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failDropshipNoPrices.before.csv"},
            after = "successDropshipFeature.after.csv")
    void dropshipNoPrices() {
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "successDropshipFeature.before.csv"},
            after = "successDropshipFeature.after.csv")
    void successDropship() {
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "successDropshipFeatureForPiSupplier.before.csv"},
            after = "successDropshipFeatureForPiSpplier.after.csv")
    void successDropshipWorkingWithPi() {
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "successDropshipSupplierWithIngoreStocks.before.csv"})
    void successDropshipSupplierWithIgnoreStocks() {
        reset(ff4ShopsClient);
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS));
        verifyNoMoreInteractions(ff4ShopsClient);
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failDropshipFeature.before.csv"},
            after = "successDropshipFeature.after.csv")
    void toggleDropshipTwice() {
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, FAIL), QUALITY);
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"},
            after = "successMarketplaceSelfDeliveryFeature.after.csv")
    void successMarketplaceSelfDelivery_deliveryConfigured() {
        featureService.changeStatus(100500, ShopFeature.of(1, MARKETPLACE_SELF_DELIVERY, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "successMarketplaceSelfDeliveryFeature.yaDoTariffs.before.csv"},
            after = "successMarketplaceSelfDeliveryFeature.after.csv")
    void successMarketplaceSelfDelivery_deliveryFromYaDoSettings() {
        featureService.changeStatus(100500, ShopFeature.of(1, MARKETPLACE_SELF_DELIVERY, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failMarketplaceSelfDelivery_noDeliverySettings.before.csv"})
    void failMarketplaceSelfDelivery_noDeliverySettings() {
        PreconditionFailedException exception = Assertions.assertThrows(
                PreconditionFailedException.class,
                () -> featureService.changeStatus(100500, ShopFeature.of(1, MARKETPLACE_SELF_DELIVERY, SUCCESS)));
        assertThat(exception.getMessage()).isEqualTo("delivery-not-configured");
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "doubleSuccess.before.csv"})
    void doubleSuccess() {
        Assertions.assertThrows(
                WrongFeatureStatusException.class,
                () -> featureService.changeStatusAndNotify(
                        100500,
                        ShopFeature.of(1, SUBSIDIES, SUCCESS),
                        false,
                        null,
                        FeatureCutoffMinorInfo.builder()
                                .setReason(TEST_ORDER)
                                .setMessage(DEFAULT_FEATURE_MESSAGE)
                                .build())
        );
    }

    @Test
    @DbUnitDataSet(before = {"datasource.csv", "cutoffHistory.csv"})
    void getCutoffHistory() {
        List<FeatureCutoffInfo> featureCutoffHistory = featureService.getFeatureCutoffHistory(
                1L, SUBSIDIES, 5, 2
        );
        List<Long> cutoffIds = featureCutoffHistory.stream()
                .map(FeatureCutoffInfo::getId)
                .collect(toList());
        MatcherAssert.assertThat(
                cutoffIds,
                Matchers.containsInAnyOrder(7L, 10L)
        );
    }

    @Test
    @DbUnitDataSet(before = "getShopWithFeature.before.csv")
    void getShopWithFeature() {
        ShopFeatureListItem item = featureService.getShopWithFeature(1, DROPSHIP);
        assertThat(item.getStatus()).isEqualTo(FAIL);
        assertThat(item.isCpaPartnerInterface()).isTrue();
    }

    @Test
    @DbUnitDataSet(before = "getShopWithFeature.before.csv")
    void getShopWithFeatureDefaultValue() {
        ShopFeatureListItem item = featureService.getShopWithFeature(2, CROSSDOCK);
        assertThat(item.getStatus()).isEqualTo(DONT_WANT);
        assertThat(item.isCpaPartnerInterface()).isFalse();
    }

    @Test
    @DbUnitDataSet(before = "getShopWithFeature.before.csv")
    void getShopWithFeatureNoShop() {
        ShopFeatureListItem item = featureService.getShopWithFeature(3, DROPSHIP);
        assertThat(item).isNull();
    }

    @Test
    @DisplayName("Возможность получения отключения по умолчанию")
    @DbUnitDataSet(before = "datasource.csv")
    void getDefaultBlueCutoff() {
        ShopFeatureInfo featureDropship = featureService.getFeatureInfo(2, DROPSHIP);
        ShopFeatureWithCutoff featureDropshipWithCutoff = featureService.getFeatureWithCutoff(2, DROPSHIP);

        assertThat(featureDropship.getCutoffs()).hasSize(1);
        FeatureCutoffInfo cutoffDropship = featureDropship.getCutoffs().iterator().next();
        assertThat(cutoffDropship.getId()).isNull();
        assertThat(cutoffDropship.getFeatureType()).isEqualTo(DROPSHIP);
        assertThat(cutoffDropship.getFeatureCutoffType()).isEqualTo(PARTNER);
        assertThat(featureDropship.getCutoffs()).isEqualTo(featureService.getCutoffs(2, DROPSHIP));

        assertFeatureEquals(featureDropship, featureDropshipWithCutoff);

        ShopFeatureInfo featureCrossdock = featureService.getFeatureInfo(2, CROSSDOCK);
        ShopFeatureWithCutoff featureCrossdockWithCutoff = featureService.getFeatureWithCutoff(2, CROSSDOCK);

        assertThat(featureCrossdock.getCutoffs()).isEqualTo(featureService.getCutoffs(2, CROSSDOCK));
        assertFeatureEquals(featureCrossdock, featureCrossdockWithCutoff);

        assertThat(featureCrossdock.getCutoffs()).hasSize(2);
        for (FeatureCutoffInfo cutoffCrossdock : featureCrossdock.getCutoffs()) {
            assertThat(cutoffCrossdock.getId()).isNull();
            assertThat(cutoffCrossdock.getFeatureType()).isEqualTo(CROSSDOCK);
        }
        assertThat(
                featureCrossdock.getCutoffs().stream()
                        .map(FeatureCutoffInfo::getFeatureCutoffType)
                        .collect(Collectors.toSet())
        ).isEqualTo(Set.of(HIDDEN, PARTNER));

        ShopFeatureInfo featureMarketplace = featureService.getFeatureInfo(2, MARKETPLACE);
        ShopFeatureWithCutoff featureMarketplaceWithCutoff = featureService.getFeatureWithCutoff(2, MARKETPLACE);

        assertThat(featureMarketplace.getCutoffs()).hasSize(1);
        FeatureCutoffInfo cutoffMarketplace = featureMarketplace.getCutoffs().iterator().next();
        assertThat(cutoffMarketplace.getId()).isNull();
        assertThat(cutoffMarketplace.getFeatureType()).isEqualTo(MARKETPLACE);
        assertThat(cutoffMarketplace.getFeatureCutoffType()).isEqualTo(PRECONDITION);
        assertThat(featureMarketplace.getCutoffs()).isEqualTo(featureService.getCutoffs(2, MARKETPLACE));
        assertFeatureEquals(featureMarketplace, featureMarketplaceWithCutoff);
    }

    @Test
    @DisplayName("При блокировке программы, отключения по умолчанию сохраняются в БД")
    @DbUnitDataSet(before = "datasource.csv")
    void lockFeatureWithDeafultCutoff() {
        ShopFeature shopFeature = featureService.lockFeature(2, DROPSHIP, 100500);
        Collection<FeatureCutoffInfo> cutoffInfos = featureService.getCutoffs(2, DROPSHIP);
        assertThat(cutoffInfos).hasSize(1);
        FeatureCutoffInfo cutoffDropship = cutoffInfos.iterator().next();
        assertThat(cutoffDropship.getId()).isNotNull();
        assertThat(cutoffDropship.getFeatureType()).isEqualTo(DROPSHIP);
        assertThat(cutoffDropship.getFeatureCutoffType()).isEqualTo(PARTNER);
    }

    @Test
    @DisplayName("При блокировке программы, программа и отключения по умолчанию сохраняются в БД")
    @DbUnitDataSet(before = "datasource.csv")
    void lockFeatureWithDefaultCutoffs() {
        ShopFeature shopFeature = featureCutoffService.lockFeature(100500, SHOP_ID, MARKETPLACE_SELF_DELIVERY);
        assertThat(shopFeature.getStatus()).isEqualTo(DONT_WANT);
        Collection<FeatureCustomCutoffType> actualOpenCutoffs = featureCutoffService.getOpenCutoffs(SHOP_ID,
                MARKETPLACE_SELF_DELIVERY);
        assertThat(actualOpenCutoffs).containsExactlyInAnyOrder(PARTNER);
    }

    @Test
    @DisplayName("Прогон фичи MARKETPLACE_SELF_DELIVERY по пайплайну")
    @DbUnitDataSet(before = {"datasource.csv", "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"})
    void testMarketplaceSelfDeliveryPipeline() {
        // Первоначально фича имеет только дефолтные катофы HIDDEN и PARTNER в статусе DONT_WANT
        // Закрываем катоф HIDDEN, статус не должен измениться т.к. висит катоф PARTNER
        closeCutoff(10, SHOP_ID, MARKETPLACE_SELF_DELIVERY, HIDDEN, DONT_WANT, Set.of(PARTNER));
        // Запускаем серию модераций
        startModeration(11, SHOP_ID, TestingType.CPA_PREMODERATION);
        startModeration(12, SHOP_ID, TestingType.SELF_CHECK);
        startModeration(13, SHOP_ID, TestingType.CPC_PREMODERATION);
        // Закрытие PARTNER переводит в статус NEW и открывается катоф TESTING, т.к. не пройдены проверки
        // CPA_PREMODERATION и SELF_CHECK
        closeCutoff(14, SHOP_ID, MARKETPLACE_SELF_DELIVERY, PARTNER, NEW, Set.of(TESTING));
        // завершаем модерацию
        finishModeration(15, SHOP_ID, TestingType.CPA_PREMODERATION);
        finishModeration(16, SHOP_ID, TestingType.SELF_CHECK);
        // после завершения модерации, фича должна перейти в статус SUCCESS, т.к. CPC_PREMODERATION не имеет отношения
        // к MARKETPLACE_SELF_DELIVERY
        assertFeatureStatus(SHOP_ID, MARKETPLACE_SELF_DELIVERY, SUCCESS, Set.of());
    }

    @Test
    @DisplayName("Открытие катофа PINGER не должно менять статус программы")
    @DbUnitDataSet(before = {"datasource.csv", "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"})
    void testOpenPingerCutoff() {
        prepareDsbsFeature(SHOP_ID, SUCCESS);
        // Открываем катоф PINGER, фича не должна изменить статус
        openCutoff(2, SHOP_ID, MARKETPLACE_SELF_DELIVERY, PINGER, SUCCESS,
                Set.of(PINGER));
        // Закрываем катоф PINGER, фича не должна изменить статус
        closeCutoff(3, SHOP_ID, MARKETPLACE_SELF_DELIVERY, PINGER, SUCCESS, Set.of());
    }

    @Test
    @DisplayName("Переход из статуса NEW должен закрывать катоф TESTING")
    @DbUnitDataSet(before = {"datasource.csv", "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"})
    void testLeaveNewStatus() {
        prepareDsbsFeature(SHOP_ID, NEW);
        // Открываем катоф QUALITY_SERIOUS, фича должна перейти в статус REVOKE, катоф TESTING закрыться,
        // откроется катоф REVOKE_MODERATION_REQUIRED
        openCutoff(2, SHOP_ID, MARKETPLACE_SELF_DELIVERY, DSBSCutoffs.QUALITY_SERIOUS, REVOKE,
                Set.of(DSBSCutoffs.QUALITY_SERIOUS, NEED_TESTING));
        // Закрываем катоф QUALITY_SERIOUS, фича должна остаться оказаться в статусе FAIL с открытым катофом
        // REVOKE_MODERATION_REQUIRED
        closeCutoff(3, SHOP_ID, MARKETPLACE_SELF_DELIVERY, DSBSCutoffs.QUALITY_SERIOUS, FAIL,
                Set.of(NEED_TESTING));
        // Закрываем катоф REVOKE_MODERATION_REQUIRED, фича должна перейти в статус NEW с открытием катофа TESTING
        closeCutoff(3, SHOP_ID, MARKETPLACE_SELF_DELIVERY, NEED_TESTING, NEW, Set.of(TESTING));
    }

    @Test
    @DisplayName("Открытие катофов, которые требуют модерацию, должны создавать запись в datasources_in_testing")
    @DbUnitDataSet(before = {"datasource.csv", "successMarketplaceSelfDeliveryFeature.selfTariffs.before.csv"})
    void testOpenCutoffsWithModeration() {
        prepareDsbsFeature(SHOP_ID, SUCCESS);
        // Открываем катоф QUALITY_OTHER, фича должна перейти в статус FAIL, открыться модерация DSBS_LITE_CHECK
        openCutoff(2, SHOP_ID, MARKETPLACE_SELF_DELIVERY, DSBSCutoffs.QUALITY_OTHER, FAIL,
                Set.of(DSBSCutoffs.QUALITY_OTHER));
        assertSandboxState(SHOP_ID, TestingType.DSBS_LITE_CHECK);
        // Закрываем катоф QUALITY_OTHER, фича должна перейти в статус NEW с открытием катофа TESTING
        closeCutoff(3, SHOP_ID, MARKETPLACE_SELF_DELIVERY, DSBSCutoffs.QUALITY_OTHER, NEW, Set.of(TESTING));
    }

    @Test
    @DisplayName("Нельзя включить программу кроссдок без склада")
    @DbUnitDataSet(before = "getShopWithFeature.before.csv")
    void failCrossdock() {
        Assertions.assertThrows(
                PreconditionFailedException.class,
                () -> featureService.changeStatus(100500, ShopFeature.of(2, CROSSDOCK, SUCCESS)));
    }

    @Test
    @DisplayName("Включать фичу Crossdok в SUCCESS всегда под экспериментом")
    @DbUnitDataSet(before = {"datasource.csv", "successCrossdockFeature.before.csv"})
    void enbableCrossdockWithExperiment() {
        featureService.changeStatus(100500, ShopFeature.of(2, CROSSDOCK, SUCCESS));
        Collection<FeatureCutoffInfo> cutoffInfos = featureService.getCutoffs(2, CROSSDOCK);
        assertThat(cutoffInfos).hasSize(1);
        FeatureCutoffInfo cutoffCrossdock = cutoffInfos.iterator().next();
        assertThat(cutoffCrossdock.getId()).isNotNull();
        assertThat(cutoffCrossdock.getFeatureType()).isEqualTo(CROSSDOCK);
        assertThat(cutoffCrossdock.getFeatureCutoffType()).isEqualTo(FeatureCutoffType.EXPERIMENT);
    }

    @Test
    @DisplayName("Не отправлять уведомление партнеру при снятии катофф PINGER, если партнер принимает заказы через ЛК")
    @DbUnitDataSet(before = "pingerCutoffPi.before.csv")
    void notSendNotificationForPingerCutoffClosing() {
        featureService.closeCutoff(100500, SHOP_ID, DROPSHIP, PINGER);
        verify(featureCutoffInternalService, times(0)).sendNotification(eq(PINGER), any(), any(), any(),
                eq(CutoffAction.CLOSE));
    }

    @Test
    @DisplayName("Отправлять уведомление партнеру при снятии катофф PINGER, если партнер принимает заказы через API")
    @DbUnitDataSet(before = "pingerCutoffApi.before.csv")
    void sendNotificationForPingerCutoffClosing() {
        featureService.closeCutoff(100500, SHOP_ID, DROPSHIP, PINGER);
        verify(featureCutoffInternalService, times(1)).sendNotification(eq(PINGER), any(), any(), any(),
                eq(CutoffAction.CLOSE));
    }


    @Test
    @DbUnitDataSet(before = {"datasource.csv", "failDropshipNoMapping.before.csv"},
            after = "successDropshipFeature.after.csv")
    void dropshipSaasSuccess() {
        featureService.changeStatus(100500, ShopFeature.of(2, DROPSHIP, SUCCESS));
    }

    @Test
    @DbUnitDataSet(before = "datasource.csv")
    void smokeTest() {
        featureService.getFeatureInfo(2L, DROPSHIP);
    }

    private FeatureCutoffInfo createTestSubsidiesCutoff() {
        return new FeatureCutoffInfo.Builder()
                .setId(2l)
                .setDatasourceId(2l)
                .setFeatureCutoffType(PARTNER)
                .setFeatureType(SUBSIDIES)
                .setComment("q")
                .setStartDate(Timestamp.valueOf(LocalDateTime.of(2018, 1, 1, 0, 0)))
                .build();
    }

    private FeatureCutoffInfo createTestQualityCutoff() {
        return new FeatureCutoffInfo.Builder()
                .setId(1l)
                .setDatasourceId(2l)
                .setFeatureCutoffType(QUALITY)
                .setFeatureType(PROMO_CPC)
                .setComment("q")
                .setReason(PINGER_API)
                .setRequiresModeration(false)
                .setRestrictsIndexation(false)
                .setStartDate(Timestamp.valueOf(LocalDateTime.of(2017, 1, 1, 0, 0)))
                .build();
    }

    private FeatureCutoffInfo createTestManagerCutoff() {
        return new FeatureCutoffInfo.Builder()
                .setId(3l)
                .setDatasourceId(2l)
                .setFeatureCutoffType(MANAGER)
                .setFeatureType(PROMO_CPC)
                .setComment("q")
                .setReason(FeatureCutoffReason.MASS_CART_DIFF)
                .setRequiresModeration(true)
                .setRestrictsIndexation(false)
                .setStartDate(Timestamp.valueOf(LocalDateTime.of(2019, 1, 1, 0, 0)))
                .build();
    }

    private FeatureCutoffInfo createHiddenCutoff(FeatureType type, long datasourceId) {
        return new FeatureCutoffInfo.Builder()
                .setFeatureType(type)
                .setFeatureCutoffType(HIDDEN)
                .setDatasourceId(datasourceId)
                .setStartDate(new Date(0))
                .setRequiresModeration(true)
                .setRestrictsIndexation(true)
                .build();
    }

    private FeatureCutoffInfo createPartnerCutoff(FeatureType type, long datasourceId) {
        return new FeatureCutoffInfo.Builder()
                .setFeatureType(type)
                .setFeatureCutoffType(PARTNER)
                .setDatasourceId(datasourceId)
                .setStartDate(new Date(0))
                .setRequiresModeration(true)
                .setRestrictsIndexation(true)
                .build();
    }

    private FeatureCutoffInfo createPreconditionCutoff(long datasourceId) {
        return new FeatureCutoffInfo.Builder()
                .setFeatureType(MARKETPLACE)
                .setFeatureCutoffType(PRECONDITION)
                .setDatasourceId(datasourceId)
                .setStartDate(new Date(0))
                .setRequiresModeration(false)
                .setRestrictsIndexation(true)
                .build();
    }

    private void closeCutoff(long actionId, long shopId, FeatureType featureType, FeatureCustomCutoffType cutoffType,
                             ParamCheckStatus expectedStatus, Set<FeatureCustomCutoffType> expectedOpenCutoffs) {
        featureCutoffService.closeCutoff(actionId, shopId, featureType, cutoffType);

        Set<FeatureCustomCutoffType> actualOpenCutoffs = featureCutoffService.getOpenCutoffs(shopId, featureType);
        assertThat(actualOpenCutoffs).isEqualTo(expectedOpenCutoffs);

        ParamCheckStatus actualStatus = featureService.getFeature(shopId, featureType).getStatus();
        assertThat(actualStatus).isEqualTo(expectedStatus);

    }

    private void openCutoff(long actionId, long shopId, FeatureType featureType, FeatureCustomCutoffType cutoffType,
                            ParamCheckStatus expectedStatus, Set<FeatureCustomCutoffType> expectedOpenCutoffs) {
        FeatureCutoffMinorInfo cutoffMinorInfo =
                FeatureCutoffMinorInfo.builder().setFeatureCutoffType(cutoffType).build();
        featureCutoffService.openCutoff(actionId, shopId, featureType, cutoffMinorInfo);
        assertFeatureStatus(shopId, featureType, expectedStatus, expectedOpenCutoffs);
    }

    private void assertFeatureStatus(long shopId,
                                     FeatureType featureType,
                                     ParamCheckStatus expectedStatus,
                                     Set<FeatureCustomCutoffType> expectedOpenCutoffs) {
        Set<FeatureCustomCutoffType> actualOpenCutoffs = featureService.getCutoffs(shopId, featureType).stream()
                .map(FeatureCutoffInfo::getFeatureCutoffType)
                .collect(Collectors.toSet());
        assertThat(actualOpenCutoffs).isEqualTo(expectedOpenCutoffs);

        ParamCheckStatus actualStatus = featureService.getFeature(shopId, featureType).getStatus();
        assertThat(actualStatus).isEqualTo(expectedStatus);
    }

    private void startModeration(long actionId, long shopId, TestingType testingType) {
        moderationService.setModerationType(new ShopActionContext(actionId, shopId),
                testingType.getShopProgram(), testingType);
    }

    private void finishModeration(long actionId, long shopId, TestingType testingType) {
        SandboxState sandboxState = sandboxRepository.load(shopId, testingType.getShopProgram());
        assertThat(sandboxState).isNotNull();
        sandboxState.delete();
        sandboxRepository.store(new ShopActionContext(actionId, shopId), sandboxState);

        eventPublisher.publishEvent(ModerationEvent.builder()
                .actionId(actionId)
                .partnerId(shopId)
                .testingType(testingType)
                .moderationEventType(ModerationEvent.ModerationEventType.PASS)
                .build());
    }

    private void assertSandboxState(long shopId, TestingType testingType) {
        SandboxState sandboxState = sandboxRepository.load(shopId, testingType.getShopProgram());
        assertThat(sandboxState).isNotNull();
        assertThat(sandboxState.getTestingType()).isEqualTo(testingType);
    }

    private void prepareDsbsFeature(int shopId, ParamCheckStatus status) {
        switch (status) {
            case SUCCESS:
                closeCutoff(100500, shopId, MARKETPLACE_SELF_DELIVERY, HIDDEN, DONT_WANT, Set.of(PARTNER));
                closeCutoff(100500, shopId, MARKETPLACE_SELF_DELIVERY, PARTNER, SUCCESS, Set.of());
                break;
            case NEW:
                closeCutoff(100500, shopId, MARKETPLACE_SELF_DELIVERY, HIDDEN, DONT_WANT, Set.of(PARTNER));
                startModeration(100500, shopId, TestingType.CPA_PREMODERATION);
                closeCutoff(100500, shopId, MARKETPLACE_SELF_DELIVERY, PARTNER, NEW, Set.of(TESTING));
        }
    }

    private void assertFeatureEquals(ShopFeatureInfo shopFeatureInfo, ShopFeatureWithCutoff shopFeatureWithCutoff) {
        assertEquals(shopFeatureInfo.getFeatureId(), shopFeatureWithCutoff.getFeatureType());
        assertEquals(shopFeatureInfo.getStatus(), shopFeatureWithCutoff.getStatus());
        assertEquals(shopFeatureInfo.getCutoffs(), shopFeatureWithCutoff.getCutoffs());
    }
}
