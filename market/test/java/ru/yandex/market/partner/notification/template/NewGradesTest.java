package ru.yandex.market.partner.notification.template;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.xml.impl.NamedContainer;
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient;
import ru.yandex.market.mbi.open.api.client.model.PartnerPlacementProgramTypesResponse;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.NotificationSendContext;
import ru.yandex.market.partner.notification.service.NotificationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NewGradesTest extends AbstractFunctionalTest {
    @Autowired
    NotificationService notificationService;

    @Test
    @DbUnitDataSet(after = "newGrades/absentGrades.after.csv")
    public void absentGradesTest() {
        when(mbiOpenApiClient.providePartnerPlacementProgramTypes(any())).thenReturn(
                new PartnerPlacementProgramTypesResponse().programTypes(List.of())
        );

        CampaignInfo campaignInfo = new CampaignInfo(654321, 123456);

        List<Object> data = new ArrayList<>();
        data.add(campaignInfo);
        data.add(new NamedContainer("date", "4 марта 2022"));
        data.add(new NamedContainer("partnerName", "Мой Магазин"));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(1644411076)
                .setShopId(123456L)
                .setData(data)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    @Test
    @DbUnitDataSet(after = "newGrades/absentBadGrades.after.csv")
    public void absentBadGradesTest() {
        when(mbiOpenApiClient.providePartnerPlacementProgramTypes(any())).thenReturn(
                new PartnerPlacementProgramTypesResponse().programTypes(List.of())
        );

        CampaignInfo campaignInfo = new CampaignInfo(654321, 123456);

        List<Object> data = new ArrayList<>();
        data.add(campaignInfo);
        data.add(new NamedContainer("date", "4 марта 2022"));
        data.add(new NamedContainer("partnerName", "Мой Магазин"));
        data.add(new NamedContainer("gradesCount", 7));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(1644411076)
                .setShopId(123456L)
                .setData(data)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    @Test
    @DbUnitDataSet(after = "newGrades/hasBadGrades.after.csv")
    public void hasBadGradesTest() {
        when(mbiOpenApiClient.providePartnerPlacementProgramTypes(any())).thenReturn(
                new PartnerPlacementProgramTypesResponse().programTypes(List.of())
        );

        CampaignInfo campaignInfo = new CampaignInfo(654321, 123456);

        List<Object> data = new ArrayList<>();
        data.add(campaignInfo);
        data.add(new NamedContainer("date", "4 марта 2022"));
        data.add(new NamedContainer("partnerName", "Мой Магазин"));
        data.add(new NamedContainer("gradesCount", 7));
        data.add(new NamedContainer("badGradesCount", 2));

        var ctx = new NotificationSendContext.Builder()
                .setTypeId(1644411076)
                .setShopId(123456L)
                .setData(data)
                .setRenderOnly(true)
                .build();

        notificationService.send(ctx);
    }

    public static final class CampaignInfo implements Serializable {
        private final long id;
        private final long datasourceId;

        public CampaignInfo(long id, long datasourceId) {
            this.id = id;
            this.datasourceId = datasourceId;
        }

        public long getDatasourceId() {
            return datasourceId;
        }

        public long getId() {
            return id;
        }
    }
}
