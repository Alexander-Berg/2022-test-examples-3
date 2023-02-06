package ru.yandex.market.mbi.api.controller.feature;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureMessage;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.ShopFeatureListItem;
import ru.yandex.market.core.message.MessageService;
import ru.yandex.market.core.message.PartnerNotificationMessageServiceTest;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.stocks.FF4ShopsClient;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.GenericStatusResponse;
import ru.yandex.market.mbi.api.client.entity.feature.CheckFeatureResult;
import ru.yandex.market.mbi.api.client.entity.shops.FeatureCutoffMessageDTO;
import ru.yandex.market.mbi.api.client.entity.shops.ProgramState;
import ru.yandex.market.mbi.api.client.entity.shops.ShopFeatureInfoDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.notification.model.WebContent;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты на логику работу {@link FeatureController}.
 *
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@DbUnitDataSet(before = "getFeatureInfos.before.csv")
class FeatureControllerFunctionalTest extends FunctionalTest {
    private static final List<Long> TEST_SHOP_IDS = List.of(1L, 2L);
    private static final List<Long> TEST_SHOP_ID = List.of(1L);
    private static final String COMMENT = "I'm just a comment";
    private static final List<AboScreenshotDto> SCREENSHOTS = List.of(
            new AboScreenshotDto(17L, "hash1"),
            new AboScreenshotDto(19L, "hash2"));

    @Autowired
    MessageService messageService;
    @Autowired
    LMSClient lmsClient;
    @Autowired
    FF4ShopsClient ff4ShopsClient;
    @Autowired
    EnvironmentService environmentService;
    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    private static Stream<Arguments> templateCashBack() {
        return Stream.of(
                Arguments.of(CheckFeatureResult.Status.OK, null, ""),
                Arguments.of(CheckFeatureResult.Status.FAIL, 1551686997, COMMENT),
                Arguments.of(CheckFeatureResult.Status.HALT, 1551687100, COMMENT));
    }

    private static WebContent createPersistentNotificationStub(String subject, String body) {
        var wc = new WebContent();
        wc.setSubject(subject);
        wc.setBody(body);
        return wc;
    }

    private static Stream<Arguments> templateB2BSeller() {
        return Stream.of(
                Arguments.of(CheckFeatureResult.Status.OK, List.of(4L)),
                Arguments.of(CheckFeatureResult.Status.FAIL, List.of()),
                Arguments.of(CheckFeatureResult.Status.HALT, List.of()));
    }

    private static Stream<Arguments> templateB2CSeller() {
        return Stream.of(
                Arguments.of(CheckFeatureResult.Status.OK, 306L),
                Arguments.of(CheckFeatureResult.Status.FAIL, -306L),
                Arguments.of(CheckFeatureResult.Status.HALT, -306L));
    }

    @BeforeEach
    void before() {
        when(lmsClient.getPartner(anyLong())).thenReturn(
                Optional.of(EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class).build()));
        PartnerNotificationMessageServiceTest.mockPN(partnerNotificationClient);
    }

    /**
     * Получение FeatureInfo одного типа.
     */
    @Test
    void getFeatureInfos() {
        List<ShopFeatureInfoDTO> infos = getFeatureInfos(List.of(1L, 2L), FeatureType.SUBSIDIES);

        checkResponseContainsFeatures(infos,
                new ShopFeatureInfoDTO(1, FeatureType.SUBSIDIES, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(2, FeatureType.SUBSIDIES, "DONT_WANT", false, null, ProgramState.DONT_WANT, null)
        );
    }

    /**
     * Получение FeatureInfo типа DROPSHIP.
     */
    @DisplayName("Включение фичи DROPSHIP происходит с открытием эксперимента")
    @Test
    void createDropshipAndGetSupplierFeatureInfos() {
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(true)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        List<ShopFeatureInfoDTO> infos = getFeatureInfos(List.of(100L),
                FeatureType.DROPSHIP, FeatureType.MARKETPLACE);

        checkResponseContainsFeatures(infos,
                new ShopFeatureInfoDTO(100L, FeatureType.DROPSHIP, "SUCCESS", true, null, ProgramState.OFF, null),
                new ShopFeatureInfoDTO(100L, FeatureType.MARKETPLACE, "SUCCESS", false, null, ProgramState.ON, null)
        );
    }

    @DisplayName("Перевести склад в INACTIVE при переключении фичи из SUCCESS")
    @Test
    void checkLmsPartnerSetInactiveOnFeatureDropshipSuccess() {
        mockMarketId();
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(createLmsPartnerResponse(1001L,
                PartnerStatus.ACTIVE)));
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.FAIL)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        verify(lmsClient, times(1)).changePartnerStatus(1001L, PartnerStatus.INACTIVE);
        verify(lmsClient, times(0)).updatePartnerSettings(any(), any());
    }

    @DisplayName("Перевести склад в ACTIVE при переключении фичи в SUCCESS")
    @Test
    @DbUnitDataSet(before = "checkLmsPartnerSetActiveOnFeatureDropshipSuccess.before.csv")
    void checkLmsPartnerSetActiveOnFeatureDropshipSuccess() {
        long logisticPartnerId = 1001L;
        PartnerResponse partnerResponse = createLmsPartnerResponseNoSync(logisticPartnerId, PartnerStatus.INACTIVE);
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(partnerResponse));
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(true)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        verify(lmsClient, times(1)).changePartnerStatus(logisticPartnerId, PartnerStatus.TESTING);
        verify(lmsClient, times(1)).updatePartnerSettings(logisticPartnerId,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(true)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(false)
                        .korobyteSyncEnabled(partnerResponse.getKorobyteSyncEnabled())
                        .build());
        reset(lmsClient);
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(partnerResponse));
        GenericStatusResponse responseExperiment = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );
        verify(lmsClient, times(1)).changePartnerStatus(logisticPartnerId, PartnerStatus.ACTIVE);
        verify(lmsClient, times(1)).updatePartnerSettings(logisticPartnerId,
                PartnerSettingDto.newBuilder()
                        .locationId(partnerResponse.getLocationId())
                        .trackingType(partnerResponse.getTrackingType())
                        .stockSyncEnabled(true)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(false)
                        .korobyteSyncEnabled(partnerResponse.getKorobyteSyncEnabled())
                        .build());
        assertThat(responseExperiment.getStatus()).isEqualTo(CutoffActionStatus.OK);
    }

    @DisplayName("Перевести склад в ACTIVE при переключении фичи в SUCCESS")
    @Test
    void checkLmsPartnerSetActiveOnFeatureDropshipAPISuccess() {
        long logisticPartnerId = 1001L;
        PartnerResponse partnerResponse = createLmsPartnerResponse(logisticPartnerId, PartnerStatus.INACTIVE);
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(partnerResponse));
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(true)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        verify(lmsClient, times(1)).changePartnerStatus(logisticPartnerId, PartnerStatus.TESTING);
        reset(lmsClient);
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(partnerResponse));
        GenericStatusResponse responseExperiment = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );
        verify(lmsClient, times(1)).changePartnerStatus(logisticPartnerId, PartnerStatus.ACTIVE);
        assertThat(responseExperiment.getStatus()).isEqualTo(CutoffActionStatus.OK);
    }

    @DisplayName("Перевести склад в ACTIVE при переключении фичи в SUCCESS, partner interface = true")
    @Test
    void checkLmsPartnerSetActiveOnFeatureCpaIsPartnerInterfaceDropshipSuccess() {
        long logisticPartnerId = 1115L;
        PartnerResponse logisticPartner = createLmsPartnerResponse(logisticPartnerId, PartnerStatus.INACTIVE);

        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(logisticPartner));
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(115L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        verify(lmsClient, times(1)).changePartnerStatus(logisticPartnerId, PartnerStatus.TESTING);
        verify(lmsClient, times(1)).updatePartnerSettings(logisticPartnerId,
                PartnerSettingDto.newBuilder()
                        .locationId(logisticPartner.getLocationId())
                        .trackingType(logisticPartner.getTrackingType())
                        .stockSyncEnabled(true)
                        .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
                        .autoSwitchStockSyncEnabled(false)
                        .korobyteSyncEnabled(logisticPartner.getKorobyteSyncEnabled())
                        .build()
        );
        mbiApiClient.closeFeatureCutoff(115L, FeatureType.DROPSHIP.getId(), FeatureCutoffType.EXPERIMENT.getId());
        reset(lmsClient);
        when(lmsClient.getPartner(anyLong())).thenReturn(Optional.of(logisticPartner));
        GenericStatusResponse responseExperiment = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(115L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(responseExperiment.getStatus()).isEqualTo(CutoffActionStatus.OK);
        verify(lmsClient, times(1)).changePartnerStatus(logisticPartnerId, PartnerStatus.ACTIVE);
    }

    /**
     * Получение FeatureInfo типа DROPSHIP.
     */
    @DisplayName("При переходе DROPSHIP из FAIL в SUCCESS, на фиче DROPSHIP открывается катофф эксперимента")
    @Test
    void createDropshipFailAndGetSupplierFeatureInfos() {
        mockMarketId();
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.FAIL)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        List<ShopFeatureInfoDTO> infos = getFeatureInfos(List.of(100L),
                FeatureType.DROPSHIP, FeatureType.MARKETPLACE);

        checkResponseContainsFeatures(infos,
                new ShopFeatureInfoDTO(100L, FeatureType.DROPSHIP, "FAIL", false, null, ProgramState.OFF, null),
                new ShopFeatureInfoDTO(100L, FeatureType.MARKETPLACE, "SUCCESS", false, null, ProgramState.ON, null)
        );
        response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(100L)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        infos = getFeatureInfos(List.of(100L),
                FeatureType.DROPSHIP, FeatureType.MARKETPLACE);

        checkResponseContainsFeatures(infos,
                new ShopFeatureInfoDTO(100L, FeatureType.DROPSHIP, "SUCCESS", true, null, ProgramState.OFF, null),
                new ShopFeatureInfoDTO(100L, FeatureType.MARKETPLACE, "SUCCESS", false, null, ProgramState.ON, null)
        );
    }

    /**
     * Получение FeatureInfo нескольких типов.
     */
    @Test
    @DbUnitDataSet(before = "getFeatureInfosTwoShopsTwoFeatures.before.csv")
    void getFeatureInfosTwoShopsTwoFeatures() {
        doReturn(Map.of(
                1001L, createPersistentNotificationStub("subj1", "body1")
        )).when(messageService).getCutoffNotificationMessages(anyCollection());

        List<ShopFeatureInfoDTO> infos = getFeatureInfos(TEST_SHOP_IDS, FeatureType.PROMO_CPC, FeatureType.SUBSIDIES);

        checkResponseContainsFeatures(infos,
                new ShopFeatureInfoDTO(1, FeatureType.SUBSIDIES, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(2, FeatureType.SUBSIDIES, "DONT_WANT", false, null, ProgramState.DONT_WANT,
                        null),
                new ShopFeatureInfoDTO(1, FeatureType.PROMO_CPC, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(2, FeatureType.PROMO_CPC, "FAIL", false, new FeatureCutoffMessageDTO("subj1",
                        "body1", List.of()), ProgramState.OFF, null)
        );

    }

    /**
     * Получение всех FeatureInfo одного магазина.
     * При добавлении новой фичи требует изменения.
     */
    @Test
    void getFeatureInfosAllFeatures() {
        List<ShopFeatureInfoDTO> infos = getFeatureInfos(TEST_SHOP_ID);

        checkResponseContainsFeatures(infos,
                new ShopFeatureInfoDTO(1, FeatureType.SUBSIDIES, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(1, FeatureType.PROMO_CPC, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(1, FeatureType.CPA_20, "DONT_WANT", false, null, ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.CASHBACK, "DONT_WANT", false, null, ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.SHOP_LOGO, "DONT_WANT", false, null, ProgramState.DONT_WANT,
                        null),
                new ShopFeatureInfoDTO(1, FeatureType.ALCOHOL, "DONT_WANT", false, null, ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.CUT_PRICE, "DONT_WANT", false, null, ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.CREDITS, "DONT_WANT", false, null, ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.MARKETPLACE_PARTNER, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.DIRECT_CATEGORY_MAPPING, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.MEDICINE_COURIER, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.TURBO_PLUS, "DONT_WANT", false, null, ProgramState.DONT_WANT,
                        null),
                new ShopFeatureInfoDTO(1, FeatureType.MARKETPLACE, "FAIL", false, null, ProgramState.OFF, null),
                new ShopFeatureInfoDTO(1, FeatureType.MARKETPLACE_SELF_DELIVERY, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.DROPSHIP_BY_SELLER, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.MARKETPLACE_AUCTION, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.CIS, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(1, FeatureType.DIRECT_SEARCH_SNIPPET_GALLERY, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.DIRECT_STANDBY, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.DIRECT_GOODS_ADS, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.VERTICAL_SHARE, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.DIRECT_STATUS, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.SELLS_MEDICINE, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.SELLS_JEWELRY, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.ORDER_AUTO_ACCEPT, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.B2B_SELLER, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null),
                new ShopFeatureInfoDTO(1, FeatureType.B2C_SELLER, "SUCCESS", false, null,
                        ProgramState.ON, null),
                new ShopFeatureInfoDTO(1, FeatureType.SELLS_ON_DEMAND, "DONT_WANT", false, null,
                        ProgramState.DONT_WANT, null)
        );
    }

    /**
     * Некорректный запрос отправки результатов проверки программы.
     */
    @Test
    void errorSendFeatureModerationResult() {
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(new CheckFeatureResult());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);
    }

    /**
     * Пытаемся сохранить результаты проверки программы магазину, у кого они уже сохранены.
     */
    @Test
    void ignoreSendFeatureModerationResult() {
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(1)
                        .featureId(FeatureType.SUBSIDIES.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()
        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.IGNORED);
    }

    /**
     * Сохраняем результаты проверки программы.
     */
    @Test
    @DbUnitDataSet(after = "csv/okSendFeatureModerationResult.after.csv")
    void okSendFeatureModerationResult() {
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(1)
                        .featureId(FeatureType.SUBSIDIES.getId())
                        .status(CheckFeatureResult.Status.FAIL)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(new FeatureMessage(1551686997, COMMENT))
                        .screenshots(SCREENSHOTS)
                        .build()
        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationWithComment(COMMENT);
    }

    @Test
    void okSendFeatureModerationResultPromoRevokeToSuccess() {
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(4)
                        .featureId(FeatureType.PROMO_CPC.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(new FeatureMessage(1551686997, COMMENT))
                        .screenshots(SCREENSHOTS)
                        .build()
        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        //не прокидываем комментарий при закрытии отключения
        checkNotificationWithComment("");
    }

    /**
     * Сохраняем результаты проверки программы, в случае использования default value
     * для {@link ru.yandex.market.core.feature.model.FeatureDescription}.
     */
    @Test
    void okSendFeatureModerationResultWithDefaultParamCheckStatus() {
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(1L)
                        .featureId(FeatureType.PROMO_CPC.getId())
                        .status(CheckFeatureResult.Status.FAIL)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(new FeatureMessage(null, null))
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
    }

    /**
     * Проверка отправки уведомления для кампании типа {@link CampaignType#FMCG}
     */
    @Test
    void checkNotificationSendForFmcg() {
        mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(200L)
                        .featureId(FeatureType.ALCOHOL.getId())
                        .status(CheckFeatureResult.Status.FAIL)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()

        );

        verifySentNotificationType(partnerNotificationClient, 1, 1556092991L);
    }

    /**
     * Получаем магазины с включённой программой SUBSIDIES.
     */
    @Test
    void testGetShopsWithEnabledFeatureSubsidies() {
        List<Long> shopIds = mbiApiClient.getShopsWithEnabledFeature(FeatureType.SUBSIDIES.getId());
        assertThat(shopIds).containsExactlyInAnyOrder(1L);
    }

    /**
     * Получаем магазины с включённой программой PROMO_CPC в том числе по умолчанию.
     */
    @Test
    void testGetShopsWithEnabledFeaturePromoCpc() {
        List<Long> shopIds = mbiApiClient.getShopsWithEnabledFeature(FeatureType.PROMO_CPC.getId());
        // Для программы PROMO_CPC по-умолчанию статус SUCCESS, поэтому должны приехать все магазины,
        // у кого программа явно не отключена.
        assertThat(shopIds).containsExactlyInAnyOrder(1L, 3L);
    }

    /**
     * Получаем пустой список магазинов с включённой программой FULFILLMENT.
     */
    @Test
    void testGetShopsWithEnabledFeatureFulfillment() {
        List<Long> shopIds = mbiApiClient.getShopsWithEnabledFeature(FeatureType.FULFILLMENT.getId());
        assertThat(shopIds).isEmpty();
    }

    /**
     * Получаем список магазинов, у которых программа FULFILLMENT на модерации.
     */
    @Test
    void testGetShopsForFeatureTesting() {
        List<Long> shopIds = mbiApiClient.getShopsForFeatureTesting(FeatureType.FULFILLMENT.getId());
        assertThat(shopIds).containsExactlyInAnyOrder(2L);
    }

    @Test
    @DisplayName("Получение поставщиков для модерации, дождавшись индексации")
    void getSuppliersForFeatureTestingWithPreview() {
        List<Long> supplierIds = mbiApiClient.getShopsForFeatureTesting(FeatureType.CROSSDOCK.getId());
        assertThat(supplierIds).containsExactlyInAnyOrder(300L, 302L);
    }

    @Test
    @DisplayName("Получение поставщиков для модерации")
    void getSuppliersForFeatureTestingWithoutPreview() {
        List<Long> supplierIds = mbiApiClient.getShopsForFeatureTesting(FeatureType.DROPSHIP.getId());
        assertThat(supplierIds).containsExactlyInAnyOrder(100L, 115L, 305L);
    }

    @Test
    @DisplayName("Получение магазинов участвующих в программе (подключены или на модерации)")
    void getShopsWithFeatureDropship() {
        List<ShopFeatureListItem> list = mbiApiClient.getShopsWithFeature(FeatureType.DROPSHIP.getId());
        checkResponseContainsFeatures(list,
                new ShopFeatureListItem(100, FeatureType.DROPSHIP, ParamCheckStatus.NEW, false),
                new ShopFeatureListItem(102, FeatureType.DROPSHIP, ParamCheckStatus.SUCCESS, false),
                new ShopFeatureListItem(103, FeatureType.DROPSHIP, ParamCheckStatus.SUCCESS, true),
                new ShopFeatureListItem(115, FeatureType.DROPSHIP, ParamCheckStatus.NEW, true),
                new ShopFeatureListItem(305, FeatureType.DROPSHIP, ParamCheckStatus.NEW, false)
        );
    }

    @Test
    @DisplayName("Получение магазинов участвующих в программе (подключены или на модерации)")
    void getShopsWithFeatureSubsidies() {
        List<ShopFeatureListItem> list = mbiApiClient.getShopsWithFeature(FeatureType.SUBSIDIES.getId());
        checkResponseContainsFeatures(list,
                new ShopFeatureListItem(1, FeatureType.SUBSIDIES, ParamCheckStatus.SUCCESS, true)
        );
    }

    @Test
    @DisplayName("Получение магазинов участвующих в программе, нет магазинов по программе")
    void getShopsWithFeatureNotFound() {
        List<ShopFeatureListItem> list = mbiApiClient.getShopsWithFeature(FeatureType.ALCOHOL.getId());
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("Получение магазинов участвующих в программе с несуществующей программой")
    void getShopsWithFeatureDoesNotExist() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> mbiApiClient.getShopsWithFeature(115))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Получение фичи под экспериментом")
    void getMarketplaceExperiment() {
        List<ShopFeatureInfoDTO> response = mbiApiClient.getFeatureInfos(
                List.of(101L),
                List.of(FeatureType.MARKETPLACE.getId())
        );
        ShopFeatureInfoDTO shopFeatureInfoDTO = new ShopFeatureInfoDTO(
                101,
                FeatureType.MARKETPLACE,
                ParamCheckStatus.SUCCESS.name(),
                true,
                null,
                ProgramState.OFF,
                null
        );
        assertThat(response).singleElement().usingRecursiveComparison().isEqualTo(shopFeatureInfoDTO);
    }

    @Test
    @DisplayName("Переключение эксперимента через mbi-api")
    void switchExperiment() {
        // Включаем выключенную фичу
        mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(101)
                        .featureId(FeatureType.MARKETPLACE.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()
        );
        List<ShopFeatureInfoDTO> response = mbiApiClient.getFeatureInfos(List.of(101L),
                List.of(FeatureType.MARKETPLACE.getId()));
        ShopFeatureInfoDTO shopFeatureInfoDTO = new ShopFeatureInfoDTO(
                101,
                FeatureType.MARKETPLACE,
                ParamCheckStatus.SUCCESS.name(),
                false,
                null,
                ProgramState.ON,
                null
        );
        assertThat(response).singleElement().usingRecursiveComparison().isEqualTo(shopFeatureInfoDTO);

        // Выключаем включенную фичу
        mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(101)
                        .featureId(FeatureType.MARKETPLACE.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(true)
                        .userId(1)
                        .message(null)
                        .build()
        );
        response = mbiApiClient.getFeatureInfos(List.of(101L),
                List.of(FeatureType.MARKETPLACE.getId()));
        assertThat(response).singleElement().usingRecursiveComparison().isEqualTo(
                new ShopFeatureInfoDTO(
                        101,
                        FeatureType.MARKETPLACE,
                        ParamCheckStatus.SUCCESS.name(),
                        true,
                        null,
                        ProgramState.OFF,
                        null)
        );
    }

    @Test
    @DisplayName("Выключение экперимента фичи маркетплейс для дропшип магазина")
    void turnOnOffMarketPlaceExperiment() {
        // Включаем фичу дропшип, при этом автоматически включается экперимент.
        mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(305)
                        .featureId(FeatureType.DROPSHIP.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(true)
                        .userId(1)
                        .message(null)
                        .build()
        );
        List<ShopFeatureInfoDTO> response = mbiApiClient.getFeatureInfos(List.of(305L),
                List.of(FeatureType.MARKETPLACE.getId(), FeatureType.DROPSHIP.getId()));
        checkResponseContainsFeatures(response,
                new ShopFeatureInfoDTO(305, FeatureType.MARKETPLACE, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(305, FeatureType.DROPSHIP, "SUCCESS", true, null, ProgramState.OFF, null)
        );

        // Выключаем эксперимент у фичи маркетплей, при этом автоматически выключается экперимент на фиче дропшип.
        mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(305)
                        .featureId(FeatureType.MARKETPLACE.getId())
                        .status(CheckFeatureResult.Status.OK)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(null)
                        .build()
        );
        response = mbiApiClient.getFeatureInfos(List.of(305L),
                List.of(FeatureType.MARKETPLACE.getId(), FeatureType.DROPSHIP.getId()));
        checkResponseContainsFeatures(response,
                new ShopFeatureInfoDTO(305, FeatureType.MARKETPLACE, "SUCCESS", false, null, ProgramState.ON, null),
                new ShopFeatureInfoDTO(305, FeatureType.DROPSHIP, "SUCCESS", true, null, ProgramState.OFF, null)
        );
    }

    /**
     * Успешное изменение статуса фичи CASHBACK с отправкой уведомления.
     */
    @ParameterizedTest
    @MethodSource("templateCashBack")
    void featureCashbackModerationResult(CheckFeatureResult.Status status, Integer templateId, String comment) {
        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(4L)
                        .featureId(FeatureType.CASHBACK.getId())
                        .status(status)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .message(new FeatureMessage(templateId, COMMENT))
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationWithComment(comment);
    }

    @Test
    void getShopWithFeature() {
        ShopFeatureListItem item = mbiApiClient.getShopWithFeature(301, FeatureType.CROSSDOCK.getId());
        assertThat(item.getStatus()).isEqualTo(ParamCheckStatus.FAIL);
    }

    @Test
    void getShopWithDefaultFeature() {
        ShopFeatureListItem item = mbiApiClient.getShopWithFeature(1, FeatureType.CROSSDOCK.getId());
        assertThat(item.getStatus()).isEqualTo(ParamCheckStatus.DONT_WANT);
    }

    @Test
    void getShopWithFeatureNoShop() {
        Assertions.assertNull(mbiApiClient.getShopWithFeature(9876, FeatureType.DROPSHIP.getId()));
    }

    @Test
    void getShopWithWrongFeature() {
        assertThatExceptionOfType(HttpClientErrorException.class)
                .isThrownBy(() -> mbiApiClient.getShopWithFeature(9876, 100500));
    }

    @Test
    void testEnablingFeature() {
        ShopFeatureInfoDTO result = mbiApiClient.enableShopFeature(306, FeatureType.DROPSHIP.getId());
        assertThat(result.getStatus()).isEqualTo(ParamCheckStatus.NEW.name());
    }

    @Test
    void testSuccessFeature() {
        ShopFeatureInfoDTO result = mbiApiClient.successShopFeature(305, FeatureType.DROPSHIP.getId());
        assertThat(result.getStatus()).isEqualTo(ParamCheckStatus.SUCCESS.name());
    }

    @Test
    void failEnableNotDropshipFeature() {
        var ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.enableShopFeature(306, FeatureType.B2B_SELLER.getId())
        );
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    /**
     * Изменение статуса фичи B2B_SELLER через модерацию
     */
    @ParameterizedTest
    @MethodSource("templateB2BSeller")
    void featureB2BSellerModerationResult(CheckFeatureResult.Status status, List<Long> shopsList) {
        List<Long> shopIds = mbiApiClient.getShopsWithEnabledFeature(FeatureType.B2B_SELLER.getId());
        assertThat(shopIds).isEmpty();

        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(4L)
                        .featureId(FeatureType.B2B_SELLER.getId())
                        .status(status)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        shopIds = mbiApiClient.getShopsWithEnabledFeature(FeatureType.B2B_SELLER.getId());
        assertThat(shopIds).containsExactlyInAnyOrderElementsOf(shopsList);
    }

    /**
     * Изменение статуса фичи B2C_SELLER через модерацию
     */
    @ParameterizedTest
    @MethodSource("templateB2CSeller")
    void featureB2CSellerModerationResult(CheckFeatureResult.Status status, Long shopId) {
        List<Long> shopIds = mbiApiClient.getShopsWithEnabledFeature(FeatureType.B2C_SELLER.getId());
        assertThat(shopIds).doesNotContain(306L);

        GenericStatusResponse response = mbiApiClient.sendFeatureModerationResult(
                CheckFeatureResult.builder()
                        .shopId(306L)
                        .featureId(FeatureType.B2C_SELLER.getId())
                        .status(status)
                        .reason(null)
                        .experiment(false)
                        .userId(1)
                        .build()

        );
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        shopIds = mbiApiClient.getShopsWithEnabledFeature(FeatureType.B2C_SELLER.getId());
        if (shopId > 0) {
            assertThat(shopIds).contains(shopId);
        } else {
            assertThat(shopIds).doesNotContain(-shopId);
        }
    }

    private List<ShopFeatureInfoDTO> getFeatureInfos(List<Long> shopIds, FeatureType... featureTypes) {
        List<Integer> featureIds = null;
        if (featureTypes != null) {
            featureIds = Stream.of(featureTypes).map(FeatureType::getId).collect(Collectors.toList());
        }

        return mbiApiClient.getFeatureInfos(shopIds, featureIds);
    }

    private static <T> void checkResponseContainsFeatures(List<T> featureInfos, T... expectedInfos) {
        assertThat(featureInfos)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedInfos);
    }

    /**
     * Проверить что было отправлено уведомление, содержащее комментарий {@link FeatureMessage#getComment()}.
     */
    private void checkNotificationWithComment(String comment) {
        var reqCaptor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(partnerNotificationClient).sendNotification(reqCaptor.capture());
        assertThat(reqCaptor.getValue().getData()).contains(comment);
    }

    private static PartnerResponse createLmsPartnerResponse(long partnerId, PartnerStatus partnerStatus) {
        return EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                .status(partnerStatus)
                .id(partnerId)
                .build();
    }

    private static PartnerResponse createLmsPartnerResponseNoSync(long partnerId, PartnerStatus partnerStatus) {
        return EnhancedRandom.random(PartnerResponse.PartnerResponseBuilder.class)
                .status(partnerStatus)
                .id(partnerId)
                .stockSyncEnabled(false)
                .build();
    }

    private void mockMarketId() {
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(MarketAccount.newBuilder().setMarketId(100500L).build())
                    .setSuccess(true)
                    .build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(any(GetByPartnerRequest.class), any());
    }
}
