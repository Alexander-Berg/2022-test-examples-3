package ru.yandex.market.mbi.api.controller.abo.cutoff.open;

import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.core.cutoff.CutoffNotificationStatus;
import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.feature.model.FeatureMessage;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffRequest;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffResponse;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты на ручку
 * {@link ru.yandex.market.mbi.api.controller.AboCutoffController#openCutoff(long, OpenAboCutoffRequest)}.
 */
@DbUnitDataSet(before = "OpenAboCutoffFunctionalTest.before.csv")
class OpenAboCutoffFunctionalTest extends FunctionalTest {
    private static final long SHOP_ID = 774L;
    private static final long UID = 411814423L;

    /**
     * Пытаемся открыть уже существующее отключение, ожидаем статус {@link CutoffActionStatus#IGNORED}.
     */
    @Test
    @DbUnitDataSet(before = "testOpenPresentCutoff.before.csv")
    void testOpenPresentCutoff() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.CPC_QUALITY, "comment", null, null, null, UID
        );
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.IGNORED, CutoffNotificationStatus.NOT_SENT, new RuntimeException()));
    }

    /**
     * Успешное открытие отключения без нотификации.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenCutoffWithoutNotification.before.csv",
            after = "testOpenCutoffWithoutNotification.after.csv"
    )
    void testOpenCutoffWithoutNotification() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.CPC_QUALITY, "comment", null, null, null, UID
        );
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    /**
     * Успешное открытие отключения для магазина без программы размещения.
     */
    @Test
    @DbUnitDataSet(
            after = "testOpenCutoffWithoutPlacementProgram.after.csv"
    )
    void testOpenCutoffWithoutPlacementProgram() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.VERTICAL_OTHER, "comment", null, null, null, UID
        );
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    /**
     * Успешное открытие отключения за низкий операционный рейтинг ДСБС магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenCutoffForDsbs.before.csv",
            after = "testOpenLowRatingCutoffForDsbs.after.csv"
    )
    void testOpenLowRatingForDsbs() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.LOW_RATING,
                "comment",
                null,
                null,
                null,
                UID,
                List.of(new AboScreenshotDto(17L, "hash"))
        );
        request.setTid(105);
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
        checkNotificationWithComment("", FeatureMessage.DSBS_LOW_RATING);
    }


    /**
     * Успешное открытие отключения за клоновость ДСБС магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenCutoffForDsbs.before.csv",
            after = "testOpenCloneCutoffForDsbs.after.csv"
    )
    void testOpenCloneForDsbs() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.SHOP_CLONE,
                "comment",
                null,
                null,
                null,
                UID,
                List.of(new AboScreenshotDto(17L, "hash"))
        );
        request.setTid(105);
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
        checkNotificationWithComment("", FeatureMessage.CLONE_TEMPLATE);
    }
    /**
     * Успешное открытие отключения за клоновость ЦПЦ магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenCutoffForCpc.before.csv",
            after = "testOpenCloneCutoffForCpc.after.csv"
    )
    void testOpenCloneForCPC() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.SHOP_CLONE,
                "comment",
                null,
                null,
                null,
                UID,
                List.of(new AboScreenshotDto(17L, "hash"))
        );
        request.setTid(105);
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.SENT));
        checkNotificationWithComment("comment", 105L);
    }
    /**
     * Успешное открытие отключения за клоновость dropship магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenQualityCloneDropshipCutoff.after.csv"
    )
    void testOpenCloneForDropship() {
        OpenAboCutoffRequest request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.SHOP_CLONE)
                .uid(UID)
                .cutoffComment("comment")
                .mailBody("comment")
                .programType(PartnerPlacementProgramType.DROPSHIP)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(
                SHOP_ID,
                CutoffActionStatus.OK,
                CutoffNotificationStatus.NOT_SENT
        ));
    }

    /**
     * Успешное открытие отключения за неприемлемое качество dropship магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenCommonQualityDropshipCutoff.after.csv"
    )
    void testOpenQualityForDropship() {
        OpenAboCutoffRequest request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.COMMON_QUALITY)
                .uid(UID)
                .cutoffComment("comment")
                .mailBody("comment")
                .programType(PartnerPlacementProgramType.DROPSHIP)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(
                SHOP_ID,
                CutoffActionStatus.OK,
                CutoffNotificationStatus.NOT_SENT
        ));
    }

    /**
     * Успешное открытие отключения за неприемлемое качество dropship магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenCommonOtherDropshipCutoff.after.csv"
    )
    void testOpenCommonOtherForDropship() {
        OpenAboCutoffRequest request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.COMMON_OTHER)
                .uid(UID)
                .cutoffComment("comment")
                .mailBody("comment")
                .programType(PartnerPlacementProgramType.DROPSHIP)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(
                SHOP_ID,
                CutoffActionStatus.OK,
                CutoffNotificationStatus.NOT_SENT
        ));
    }

    /**
     * Успешное открытие отключения с нотификацией subject + body.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenCutoffForCpc.before.csv",
            after = "testOpenCutoffWithSubjectAndBodyNotification.after.csv"
    )
    void testOpenCutoffWithSubjectAndBodyNotification() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.SHOP_FRAUD,
                "comment",
                null,
                "subject",
                "body",
                UID,
                List.of(new AboScreenshotDto(17L, "hash"))
        );
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.SENT));
        checkNotificationWithComment("", 59L);
    }

    /**
     * Успешное открытие FRAUD для ДСБС.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenCutoffForDsbs.before.csv",
            after = "testOpenFraudCutoffForDsbs.after.csv"
    )
    void testOpenFraudForDsbs() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.SHOP_FRAUD,
                null,
                null,
                null,
                null,
                UID,
                List.of(new AboScreenshotDto(17L, "hash"))
        );
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
        checkNotificationWithComment("", FeatureMessage.QUALITY_SERIOUS_TEMPLATE);
    }

    /**
     * Успешное открытие отключения за клоновость dropship магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenQualityFraudDropshipCutoff.after.csv"
    )
    void testOpenFraudForDropship() {
        OpenAboCutoffRequest request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.SHOP_FRAUD)
                .uid(UID)
                .cutoffComment("comment")
                .mailBody("comment")
                .programType(PartnerPlacementProgramType.DROPSHIP)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(
                SHOP_ID,
                CutoffActionStatus.OK,
                CutoffNotificationStatus.NOT_SENT
        ));
    }

    @Test
    @DisplayName("Фиксируем запрос и ответ в XML-формате")
    @DbUnitDataSet(before = "testOpenCutoffForCpc.before.csv")
    void rawXmlSerializationAndDeserialization() {
        String request = //language=xml
                "<shop_abo_cutoffs_open_request>\n" +
                        "    <cutoff_period>2017-07-20T20:00:00</cutoff_period>\n" +
                        "    <mail_subject>subject</mail_subject>\n" +
                        "    <cutoff_comment>comment</cutoff_comment>\n" +
                        "    <cutoff_type>SHOP_FRAUD</cutoff_type>\n" +
                        "    <mail_body>body</mail_body>\n" +
                        "    <uid>411814423</uid>\n" +
                        "</shop_abo_cutoffs_open_request>";
        String response = FunctionalTestHelper.postForXml(
                "http://localhost:" + port + "/abo-cutoff/774/open",
                request
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<shop_abo_cutoffs_open_response>\n" +
                        "   <datasource_id>774</datasource_id>\n" +
                        "   <status>OK</status>\n" +
                        "   <notification_status>SENT</notification_status>\n" +
                        "</shop_abo_cutoffs_open_response>",
                response
        );
    }

    /**
     * Успешное отключение фичи {@link FeatureType#MARKETPLACE_SELF_DELIVERY} пингером.
     */
    @Test
    @DbUnitDataSet(before = "testOpenFeatureCutoff.before.csv")
    void dsbsCutoffPinger() {
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID,
                new OpenAboCutoffRequest(AboCutoff.PINGER, null, null, null, createRandom(5000), UID, null));
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        ArgumentCaptor<List> arguments = ArgumentCaptor.forClass(List.class);
        checkNotificationWithComment("", FeatureMessage.DSBS_PINGER_TEMPLATE);
    }

    /**
     * Успешное отключение фичи {@link FeatureType#MARKETPLACE_SELF_DELIVERY} фатально за качество.
     */
    @Test
    @DbUnitDataSet(before = "testOpenCutoffForDsbs.before.csv",
            after = "testOpenDsbsQuality.after.csv")
    void dsbsCutoffQuality() {
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID,
                new OpenAboCutoffRequest(AboCutoff.DSBS_QUALITY, "Фатально отключаем", null, null, "Публичный коммент",
                        UID,
                        List.of(new AboScreenshotDto(1L, "hash"))));
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationWithComment("Публичный коммент", FeatureMessage.QUALITY_SERIOUS_TEMPLATE);
    }

    /**
     * Успешное отключение фичи {@link FeatureType#MARKETPLACE_SELF_DELIVERY} фатально за качество
     * при наличии катофа NEED_TESTING.
     */
    @Test
    @DbUnitDataSet(before = "testOpenCutoffForDsbsWithOpenedCutoff.before.csv",
            after = "testOpenDsbsQuality.after.csv")
    void dsbsCutoffQualityWithOpenedNeedTesting() {
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID,
                new OpenAboCutoffRequest(AboCutoff.DSBS_QUALITY, "Фатально отключаем", null, null, "Публичный коммент",
                        UID,
                        List.of(new AboScreenshotDto(1L, "hash"))));
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationWithComment("Публичный коммент", FeatureMessage.QUALITY_SERIOUS_TEMPLATE);
    }

    /**
     * Успешное отключение фичи {@link FeatureType#MARKETPLACE_SELF_DELIVERY} за другие проблемы с качеством.
     */
    @Test
    @DbUnitDataSet(before = "testOpenCutoffForDsbs.before.csv")
    void dsbsCutoffOther() {
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID,
                new OpenAboCutoffRequest(AboCutoff.DSBS_OTHER, "Нефатально отключаем", null, null, "Нефатально отключаем", UID,
                        List.of(new AboScreenshotDto(1L, "hash"))));
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationWithComment("Нефатально отключаем", FeatureMessage.DSBS_QUALITY_OTHER_TEMPLATE);
    }

    /**
     * Открытие ДСБС катоффа за достижение лимита заказов.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenFeatureCutoff.before.csv",
            after = "testOpenLimitOrderCutoff.after.csv"
    )
    void testOpenLimitOrder() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.DSBS_LIMIT_ORDERS, "some comment", null, null, "some comment", UID
        );
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DbUnitDataSet(
            before = "testOpenDBSByPartnerCutoff.before.csv",
            after = "testOpenDBSByPartnerCutoff.after.csv"
    )
    void testOpenDsbsByPartner() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.BY_PARTNER, "Отключение партнером", null, null,
                "Отключение партнером", UID);

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DbUnitDataSet(
            before = "testOpenDailyOrderLimitCutoff.before.csv",
            after = "testOpenDailyOrderLimitCutoff.after.csv"
    )
    void testOpenDailyOrderLimitCutoff() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.DAILY_ORDER_LIMIT, "Превышение дневного лимита", null, null,
                "Превышение дневного лимита", UID);

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DbUnitDataSet(
            before = "testOpenModerationNeedInfoCutoff.before.csv",
            after = "testOpenModerationNeedInfoCutoff.after.csv"
    )
    void testOpenModerationNeedInfoCutoff() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.MODERATION_NEED_INFO, "Приостановка модерации", null, null,
                "Приостановка модерации", UID);

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    /**
     * Открытие ДСБС катоффа за карт дифф.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenFeatureCutoff.before.csv",
            after = "testOpenDsbsCartDiff.after.csv"
    )
    void testOpenDsbsCartDiffCutoff() {
        var request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.CART_DIFF)
                .programType(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                .cutoffComment("some comment")
                .mailBody("some comment")
                .uid(UID)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("Открытие катоффа для дропшипов за карт дифф")
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenDropshipCartDiff.after.csv"
    )
    void testOpenDropshipCartDiffCutoff() {
        var request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.CART_DIFF)
                .programType(PartnerPlacementProgramType.DROPSHIP)
                .cutoffComment("some comment")
                .mailBody("some comment")
                .uid(UID)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    /**
     * Открытие ДСБС катоффа за непринятый заказ.
     */
    @Test
    @DbUnitDataSet(
            before = "testOpenFeatureCutoff.before.csv",
            after = "testOpenOrderNotAcceptedCutoff.after.csv"
    )
    void testOpenOrderNotAccepted() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.DSBS_MISSED_ORDER, "some comment", null, null, "some comment", UID
        );
        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("В запросе АБО приходит невалидная программа размещения партнера")
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenFeatureCutoffDropship.before.csv" //не должно быть изменений в БД
    )
    void testOpenInvalidPartnerProgram() {
        var request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.CART_DIFF)
                .programType(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                .cutoffComment("some comment")
                .mailBody("some comment")
                .uid(UID)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.ERROR, CutoffNotificationStatus.NOT_SENT));
        assertThat(response.getError())
                .isNotBlank()
                .isEqualTo("IllegalArgumentException: Program type DROPSHIP_BY_SELLER is not enabled for partner 774");
    }

    @Test
    @DisplayName("В запросе АБО приходит программа размещения партнера, находящаяся в статусе DISABLED")
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship2.before.csv",
            after = "testOpenFeatureCutoffDropship2.before.csv" //не должно быть изменений в БД
    )
    void testOpenDisabledPartnerProgram() {
        var request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.CART_DIFF)
                .programType(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                .cutoffComment("some comment")
                .mailBody("some comment")
                .uid(UID)
                .build();

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.ERROR, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("Открытие катоффа дропшипу за непринятые заказы")
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenOrderNotAcceptedDropshipCutoff.after.csv"
    )
    void testOpenDropshipMissedOrderCutoff() {
        var request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.MISSED_ORDER)
                .uid(UID)
                .cutoffComment("comment")
                .mailBody("comment")
                .programType(PartnerPlacementProgramType.DROPSHIP)
                .build();

        var response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(
                SHOP_ID,
                CutoffActionStatus.OK,
                CutoffNotificationStatus.NOT_SENT
        ));
    }

    @Test
    @DisplayName("Открытие катоффа fbs express за несобранный в срок заказ")
    @DbUnitDataSet(
            before = "testOpenFeatureCutoffDropship.before.csv",
            after = "testOpenOrderPendingExpiredDropshipCutoff.after.csv"
    )
    void testOpenFbsExpressOrderPendingExpired() {
        var request = OpenAboCutoffRequest.builder()
                .cutoffType(AboCutoff.ORDER_PENDING_EXPIRED)
                .uid(UID)
                .cutoffComment("comment")
                .mailBody("comment")
                .programType(PartnerPlacementProgramType.DROPSHIP)
                .build();

        var response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(
                SHOP_ID,
                CutoffActionStatus.OK,
                CutoffNotificationStatus.NOT_SENT
        ));
    }

    @Test
    @DbUnitDataSet(
            before = "testOpenOrderFinalStatusNotSetCutoff.before.csv",
            after = "testOpenOrderFinalStatusNotSetCutoff.after.csv"
    )
    void testOpenOrderFinalStatusNotSetCutoff() {
        OpenAboCutoffRequest request = new OpenAboCutoffRequest(
                AboCutoff.ORDER_FINAL_STATUS_NOT_SET, "Не проставлены финальные статусы заказов", null, null,
                "Не проставлены финальные статусы заказов", UID);

        OpenAboCutoffResponse response = mbiApiClient.openAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new OpenAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    private static String createRandom(int strLength) {
        return RandomStringUtils.random(strLength, true, true);
    }

    /**
     * Проверить что было отправлено уведомление c типом templateId,
     * содержащее комментарий {@link FeatureMessage#getComment()}.
     */
    private void checkNotificationWithComment(String comment, long templateId) {
        var reqCaptor = verifySentNotificationType(partnerNotificationClient, 1, templateId);
        assertThat(reqCaptor.getValue().getData()).contains(comment);
    }
}
