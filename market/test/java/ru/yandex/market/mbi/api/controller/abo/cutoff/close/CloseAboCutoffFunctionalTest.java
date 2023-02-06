package ru.yandex.market.mbi.api.controller.abo.cutoff.close;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.core.cutoff.CutoffNotificationStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.abo.CloseAboCutoffRequest;
import ru.yandex.market.mbi.api.client.entity.abo.CloseAboCutoffResponse;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.abo.AboCutoff.LOW_RATING;
import static ru.yandex.market.core.abo.AboCutoff.SHOP_CLONE;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

@DbUnitDataSet(before = "CloseAboCutoffFunctionalTest.before.csv")
class CloseAboCutoffFunctionalTest extends FunctionalTest {
    private static final long SHOP_ID = 774L;
    private static final long SUPPLIER_ID = 555L;
    private static final long UID = 411814423L;

    /**
     * Ручка должна возвращать ошибку при попытке закрытия отключения, которого нет у магазина
     */
    @Test
    @DbUnitDataSet(before = "testCloseNonExistentCutoff.before.csv")
    void testCloseNonExistentCutoff() {
        CloseAboCutoffRequest request = new CloseAboCutoffRequest(AboCutoff.COMMON_OTHER, "comment", UID, false);
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.ERROR, CutoffNotificationStatus.NOT_SENT));
    }

    /**
     * Успешное закрытие cpc-отключения без нотификации
     */
    @Test
    @DbUnitDataSet(
            before = "testCloseCpcCutoffWithoutNotification.before.csv",
            after = "testCloseCpcCutoffWithoutNotification.after.csv"
    )
    void testCloseCpcCutoffWithoutNotification() {
        CloseAboCutoffRequest request = new CloseAboCutoffRequest(AboCutoff.COMMON_OTHER, "comment", UID, false);
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    /**
     * Успешное закрытие отключения с нотификацией
     */
    @Test
    @DbUnitDataSet(
            before = "testCloseCutoffWithNotification.before.csv",
            after = "testCloseCutoffWithNotification.after.csv"
    )
    void testCloseCutoffWithNotification() {
        CloseAboCutoffRequest request = new CloseAboCutoffRequest(SHOP_CLONE, "comment", UID, true);
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.SENT));
    }

    /**
     * Успешное закрытие отключения за клоность ДСБС магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testCloseCloneForDsbs.before.csv",
            after = "testCloseCloneForDsbs.after.csv"
    )
    void testCloseCloneForDsbs() {
        CloseAboCutoffRequest request = new CloseAboCutoffRequest(SHOP_CLONE, "comment", UID, true);
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    /**
     * Успешное закрытие отключения за низкий операционный рейтинг ДСБС магазина.
     */
    @Test
    @DbUnitDataSet(
            before = "testCloseLowRatingForDsbs.before.csv",
            after = "testCloseLowRatingForDsbs.after.csv"
    )
    void testCloseLowRatingForDsbs() {
        CloseAboCutoffRequest request = new CloseAboCutoffRequest(LOW_RATING, "comment", UID, true);
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID, request);
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("Request - response в XML формате")
    @DbUnitDataSet(
            before = "testCloseCutoffWithNotification.before.csv",
            after = "testCloseCutoffWithNotification.after.csv"
    )
    void rawXmlSerializationAndDeserialization() {
        String request = //language=xml
                "<shop_abo_cutoffs_close_request>\n" +
                        "    <send_message>true</send_message>\n" +
                        "    <cutoff_type >SHOP_CLONE</cutoff_type>\n" +
                        "    <cutoff_comment>comment</cutoff_comment>\n" +
                        "    <uid>411814423</uid >\n" +
                        "</shop_abo_cutoffs_close_request>";
        String response = FunctionalTestHelper.postForXml(
                "http://localhost:" + port + "/abo-cutoff/774/close",
                request
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<shop_abo_cutoffs_close_response>\n" +
                        "   <datasource_id>774</datasource_id>\n" +
                        "   <status>OK</status>\n" +
                        "   <notification_status>SENT</notification_status>\n" +
                        "</shop_abo_cutoffs_close_response>",
                response
        );

    }

    /**
     * Успешное закрытие отключения для фичи.
     */
    @Test
    @DbUnitDataSet(
            before = "testCloseFeatureCutoff.before.csv",
            after = "testCloseFeatureCutoff.after.csv"
    )
    void testCloseFeatureCutoff() {
        mbiApiClient.openAboCutoff(SHOP_ID, new OpenAboCutoffRequest(
                AboCutoff.PINGER, null, null, null, null, UID
        ));
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID, new CloseAboCutoffRequest(
                AboCutoff.PINGER, null, UID, false
        ));
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
        verifySentNotificationType(partnerNotificationClient, 2, 108L, 1606103905L);
    }

    @Test
    @DisplayName("Успешное закрытие отключения за cart diff для партнера с программой DSBS")
    @DbUnitDataSet(
            before = "testCloseFeatureCutoffDsbs.before.csv",
            after = "testCloseFeatureCutoffDsbs.after.csv"
    )
    void testCloseCartdiffCutoffDropship() {
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID,
                CloseAboCutoffRequest.builder()
                        .cutoffType(AboCutoff.CART_DIFF)
                        .programType(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                        .sendMessage(false)
                        .uid(UID)
                        .build());
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("АБО присылает невалидную программу размещения партнера")
    @DbUnitDataSet(
            before = "testCloseFeatureCutoffDsbs.before.csv",
            after = "testCloseFeatureCutoffDsbs.before.csv" //не должно быть изменений в БД
    )
    void testCloseCartdiffCutoffDropship2() {
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID,
                CloseAboCutoffRequest.builder()
                        .cutoffType(AboCutoff.CART_DIFF)
                        .programType(PartnerPlacementProgramType.DROPSHIP)
                        .sendMessage(false)
                        .uid(UID)
                        .build());
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.ERROR, CutoffNotificationStatus.NOT_SENT));
        assertThat(response.getError())
                .isNotBlank()
                .isEqualTo("IllegalArgumentException: Program type DROPSHIP is not enabled for partner 774");
    }

    @Test
    @DisplayName("Успешное закрытие катофа за массовый картдифф")
    @DbUnitDataSet(
            before = "testCloseCartdiffCutoffDropship.before.csv",
            after = "testCloseCartdiffCutoffDropship.after.csv"
    )
    void testCloseCartiffCutoffDropship3() {
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SUPPLIER_ID,
                CloseAboCutoffRequest.builder()
                        .cutoffType(AboCutoff.CART_DIFF)
                        .programType(PartnerPlacementProgramType.DROPSHIP)
                        .sendMessage(false)
                        .uid(UID)
                        .build());
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SUPPLIER_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("Успешное закрытие отключения за непроставленные финальные статусы заказов")
    @DbUnitDataSet(
            before = "testCloseOrderFinalStatusNotSetCutoff.before.csv",
            after = "testCloseOrderFinalStatusNotSetCutoff.after.csv"
    )
    void testCloseOrderFinalStatusNotSetCutoff() {
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID,
                CloseAboCutoffRequest.builder()
                        .cutoffType(AboCutoff.ORDER_FINAL_STATUS_NOT_SET)
                        .programType(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                        .sendMessage(false)
                        .uid(UID)
                        .build());
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("Успешное закрытие отключения за непроставленные финальные статусы заказов, но остается " +
            "катофф, блокирующий перевод фичи в саксесс")
    @DbUnitDataSet(
            before = "testCloseOrderFinalStatusNotSetCutoffPingerOpen.before.csv",
            after = "testCloseOrderFinalStatusNotSetCutoffPingerOpen.after.csv"
    )
    void testCloseOrderFinalStatusNotSetCutoffPingerRemainsOpen() {
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID,
                CloseAboCutoffRequest.builder()
                        .cutoffType(AboCutoff.ORDER_FINAL_STATUS_NOT_SET)
                        .programType(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                        .sendMessage(false)
                        .uid(UID)
                        .build());
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }

    @Test
    @DisplayName("Закрытие катофа, требующего самопроверку")
    @DbUnitDataSet(
            before = "testCloseSelfcheckRequired.before.csv",
            after = "testCloseSelfcheckRequired.after.csv"
    )
    void testCloseSelfcheckRequired() {
        CloseAboCutoffResponse response = mbiApiClient.closeAboCutoff(SHOP_ID,
                CloseAboCutoffRequest.builder()
                        .cutoffType(AboCutoff.SELFCHECK_REQUIRED)
                        .programType(PartnerPlacementProgramType.DROPSHIP_BY_SELLER)
                        .sendMessage(false)
                        .uid(UID)
                        .build());
        assertThat(response).isEqualTo(new CloseAboCutoffResponse(SHOP_ID,
                CutoffActionStatus.OK, CutoffNotificationStatus.NOT_SENT));
    }
}
