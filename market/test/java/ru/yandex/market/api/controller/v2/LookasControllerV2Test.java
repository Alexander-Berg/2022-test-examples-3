package ru.yandex.market.api.controller.v2;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.ModelOrOfferV2;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.ResultContextV2;
import ru.yandex.market.api.domain.v2.LookasResultV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.computervision.CbirdResolver;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.httpclient.clients.ComputerVisionTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * @author dimkarp93
 */
@WithContext
public class LookasControllerV2Test extends BaseTest {
    @Inject
    private LookasControllerV2 similarController;
    @Inject
    private ComputerVisionTestClient computerVisionTestClient;
    @Inject
    private ReportTestClient reportTestClient;

    @Test
    public void similarAllByUrls() {
        String url = "http://superoffers.json";
        computerVisionTestClient.looksas(url, CbirdResolver.OTHER_PARTNER_CBIRD, "success-computer-vision-response-model.json");
        reportTestClient.getModelInfoById(1, "modelinfo_1-lookas.json");

        OfferId[] offerIds = new OfferId[] {
            new OfferId("2", null),
            new OfferId("3", null)
        };
        reportTestClient.getOffersV2(offerIds, "offer-2-3.json");

        LookasResultV2 result = similarController
            .lookasByUrl(
                Collections.emptyList(),
                url,
                GenericParams.DEFAULT
            ).waitResult();
        List<? extends ModelOrOfferV2> entities = result.getItems();

        Assert.assertEquals(3, entities.size());

        ModelV2 model = (ModelV2) entities.get(0);
        Assert.assertEquals(1, model.getId());
        OfferV2 offer1 = (OfferV2) entities.get(1);
        Assert.assertEquals("2", offer1.getId().getWareMd5());
        OfferV2 offer2 = (OfferV2) entities.get(2);
        Assert.assertEquals("3", offer2.getId().getWareMd5());

        Assert.assertTrue(result.getContext() instanceof ResultContextV2);
        ResultContextV2 contextV2 = (ResultContextV2) result.getContext();
        Assert.assertEquals("https://m.market.yandex.ru/picsearch?cbir_id=1&pp=37", contextV2.getLink());
    }

}
