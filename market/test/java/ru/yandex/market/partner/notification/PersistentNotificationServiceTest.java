package ru.yandex.market.partner.notification;


import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypesResponse;
import ru.yandex.market.partner.notification.service.NotificationSendContext;
import ru.yandex.market.partner.notification.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PersistentNotificationServiceTest extends AbstractFunctionalTest {

    public static final Integer MUSTACHE_TEMPLATE_ID = 2;

    public static final Integer XSL_TEMPLATE_ID = 1627032762;

    @Autowired
    NotificationService notificationService;

    @Autowired
    MbiOpenApiClient mbiOpenApiClient;

    @Test
    @DbUnitDataSet(after = "shouldSavePersistentNotification.after.csv")
    public void shouldSavePersistentNotification() {
        when(mbiOpenApiClient.providePartnerPlacementProgramTypes(any())).thenReturn(
                new PartnerPlacementProgramTypesResponse().programTypes(List.of())
        );

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(MUSTACHE_TEMPLATE_ID)
                .setShopId(123456L)
                .setData(List.of(
                        new NamedContainer("shop-name", "The Shop"),
                        new CampaignInfo(654321L, 123456L)
                ))
                .setShopId(123456L)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    @Test
    @DbUnitDataSet(after = "formattedEmailBody.after.csv")
    public void formattedEmailBodyTest() {
        when(mbiOpenApiClient.providePartnerPlacementProgramTypes(any())).thenReturn(
                new PartnerPlacementProgramTypesResponse().programTypes(List.of())
        );

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(MUSTACHE_TEMPLATE_ID)
                .setShopId(123456L)
                .setData(List.of(
                        new NamedContainer("shop-name", "The Shop"),
                        new CampaignInfo(654321L, 123456L)
                ))
                .setShopId(123456L)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }


    @Test
    @DbUnitDataSet(after = "shouldSavePersistentXslNotification.after.csv")
    public void shouldSavePersistentXslNotification() {
        when(mbiOpenApiClient.providePartnerPlacementProgramTypes(any())).thenReturn(
                new PartnerPlacementProgramTypesResponse().programTypes(List.of())
        );

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(XSL_TEMPLATE_ID)
                .setShopId(123456L)
                .setData(List.of(
                        new NamedContainer("shop-name", "The Shop"),
                        new CampaignInfo(654321L, 123456L)
                ))
                .setShopId(123456L)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    public static final class CampaignInfo {
        private final long id;
        private final long datasourceId;

        private CampaignInfo(long id, long datasourceId) {
            this.id = id;
            this.datasourceId = datasourceId;
        }

        public long getId() {
            return id;
        }

        public long getDatasourceId() {
            return datasourceId;
        }
    }

}
