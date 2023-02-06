package ru.yandex.market.mbi.api.controller.feature.cutoff;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.cutoff.model.AboScreenshot;
import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureMessage;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.screenshot.AboScreenshotDao;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.GenericStatusResponse;
import ru.yandex.market.mbi.api.client.entity.shops.OpenFeatureCutoffRequest;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.partner.notification.client.model.SuccessResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

@DbUnitDataSet(before = "FeatureControllerFunctionalTest.cutoff.before.csv")
public class FeatureControllerOpenCloseCutoffTest extends FunctionalTest {
    @Autowired
    AboScreenshotDao aboScreenshotDao;
    @Autowired
    protected PartnerNotificationClient partnerNotificationClient;

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.open.after.csv")
    void testOpenCutoff() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(FeatureMessage.DROPSHIP_MODERATION_FAIL_TEMPLATE);

        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(FeatureMessage.DROPSHIP_MODERATION_FAIL_TEMPLATE);
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.open.after.csv")
    void testOpenCutoffWithCustomTemplate() {
        final int templateFROMDb = 1602665727;
        OpenFeatureCutoffRequest openFeatureCutoffRequest = OpenFeatureCutoffRequest.builder()
                .tid(templateFROMDb)
                .screenshots(List.of(new AboScreenshotDto(144L, "hash@1")))
                .message("<mailBody></mailBody>")
                .aboInfo("<customInfo></customInfo>")
                .build();
        when(partnerNotificationClient.isTemplateExists(1602665727L)).thenReturn(new SuccessResponse().result(true));
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId(),
                        openFeatureCutoffRequest);
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(templateFROMDb);
        checkScreenshots(openFeatureCutoffRequest);

        openFeatureCutoffRequest = OpenFeatureCutoffRequest.builder()
                .tid(FeatureMessage.DROPSHIP_MODERATION_REVOKE_TEMPLATE)
                .build();
        when(partnerNotificationClient.isTemplateExists(1600000000L)).thenReturn(new SuccessResponse().result(true));
        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId(),
                        openFeatureCutoffRequest);
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(FeatureMessage.DROPSHIP_MODERATION_REVOKE_TEMPLATE);
        checkScreenshots(openFeatureCutoffRequest);
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.open.failed.after.csv")
    void testOpenCutoffWithWrongCustomTemplate() {
        OpenFeatureCutoffRequest openFeatureCutoffRequest = OpenFeatureCutoffRequest.builder()
                .tid(-1)
                .build();
        when(partnerNotificationClient.isTemplateExists(-1L)).thenReturn(new SuccessResponse().result(false));
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId(),
                        openFeatureCutoffRequest);
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);
        assertThat(response.getMessage()).isEqualTo("Template with tId -1 is not supported");
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.limit_orders.open.after.csv")
    void testOpenCutoffLimitOrder() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.openFeatureCutoff(100, FeatureType.CROSSDOCK.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        //для всех катоффов, кроме PINGER, сейчас уведомлений нет
        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.manual.open.after.csv")
    void testOpenCutoffManual() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.openFeatureCutoff(100, FeatureType.CROSSDOCK.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        //для всех катоффов, кроме PINGER, сейчас уведомлений нет
        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.close.after.csv")
    void testCloseCutoff() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(FeatureMessage.DROPSHIP_MODERATION_FAIL_TEMPLATE);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(FeatureMessage.DROPSHIP_MODERATION_SUCCESS_TEMPLATE);

        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(FeatureMessage.DROPSHIP_MODERATION_FAIL_TEMPLATE);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        checkNotificationsSent(FeatureMessage.DROPSHIP_MODERATION_SUCCESS_TEMPLATE);
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.limit_orders.close.after.csv")
    void testCloseCutoffLimitOrder() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.MARKETPLACE.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.DROPSHIP.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.openFeatureCutoff(100, FeatureType.CROSSDOCK.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(100, FeatureType.CROSSDOCK.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.manual.close.after.csv")
    void testCloseCutoffManual() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.openFeatureCutoff(100, FeatureType.CROSSDOCK.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(100, FeatureType.CROSSDOCK.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.nothing.close.after.csv")
    void testCloseNotOpenCutoff() {
        GenericStatusResponse response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.MARKETPLACE.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.DROPSHIP.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(100, FeatureType.CROSSDOCK.getId(),
                        FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.closeFeatureCutoff(100, FeatureType.CROSSDOCK.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.unsupported.after.csv")
    void testOpenUnsupportedCutoff() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.QUALITY.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: MARKETPLACE, cutoff: QUALITY");

        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.QUALITY.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: DROPSHIP, cutoff: QUALITY");
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.unsupported.after.csv")
    void testOpenCutoffForWrongFeature() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.CPA_20.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);

        //feature has been created when locking, cutoff validation failed
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: CPA_20, cutoff: PINGER");

        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.CPA_20.getId(), FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);

        //feature has been created when locking, cutoff validation failed
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: CPA_20, cutoff: LIMIT_ORDERS");

        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.CPA_20.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);

        //feature has been created when locking, cutoff validation failed
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: CPA_20, cutoff: MANUAL");
    }

    @Test
    @DbUnitDataSet(after = "FeatureControllerFunctionalTest.cutoff.unsupported.after.csv")
    void testCloseCutoffForWrongFeature() {
        GenericStatusResponse response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.CPA_20.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);

        //feature has been created when locking, cutoff validation failed
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: CPA_20, cutoff: PINGER");
        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.CPA_20.getId(), FeatureCutoffType.LIMIT_ORDERS.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);

        //feature has been created when locking, cutoff validation failed
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: CPA_20, cutoff: LIMIT_ORDERS");

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.CPA_20.getId(), FeatureCutoffType.MANUAL.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.ERROR);

        //feature has been created when locking, cutoff validation failed
        assertThat(response.getMessage()).isEqualTo("Message: Cutoff is not allowed for manually modification, " +
                "shop-id: 99, " +
                "feature-type: CPA_20, cutoff: MANUAL");
    }

    @Test
    @DbUnitDataSet(before = "FeatureControllerFunctionalTest.several.cutoff.before.csv",
            after = "FeatureControllerFunctionalTest.several.cutoff.after.csv")
    void testSeveralOpenCutoff() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
    }

    @Test
    @DbUnitDataSet(before = "FeatureControllerFunctionalTest.several.cutoff.before.csv",
            after = "FeatureControllerFunctionalTest.several.cutoff.close.after.csv")
    void testSeveralCloseCutoff() {
        GenericStatusResponse response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.openFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);

        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.MARKETPLACE.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
        response =
                mbiApiClient.closeFeatureCutoff(99, FeatureType.DROPSHIP.getId(), FeatureCutoffType.PINGER.getId());
        assertThat(response.getStatus()).isEqualTo(CutoffActionStatus.OK);
    }

    private void checkNotificationsSent(int notificationType) {
        verifySentNotificationType(partnerNotificationClient, 1, notificationType);
        Mockito.clearInvocations(partnerNotificationClient);
    }

    private void checkScreenshots(OpenFeatureCutoffRequest r) {
        if (CollectionUtils.isEmpty(r.getScreenshots())) {
            return;
        }
        var captor = ArgumentCaptor.forClass(List.class);
        verify(aboScreenshotDao).save(captor.capture());
        List<AboScreenshot> screenshots = captor.getValue();
        List<AboScreenshotDto> screenshotsDto = screenshots.stream()
                .map(AboScreenshot::toDto)
                .collect(Collectors.toList());
        assertThat(r.getScreenshots()).isEqualTo(screenshotsDto);
    }
}
