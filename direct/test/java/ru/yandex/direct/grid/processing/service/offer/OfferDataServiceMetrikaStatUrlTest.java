package ru.yandex.direct.grid.processing.service.offer;

import java.time.LocalDate;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOfferId;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class OfferDataServiceMetrikaStatUrlTest {
    private static final Long COUNTER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2021, 11, 1);
    private static final GdiOfferId OFFER_ID = new GdiOfferId()
            .withBusinessId(1L)
            .withShopId(2L)
            .withOfferYabsId(123L);

    public static Object[] parameters() {
        return CampaignAttributionModel.values();
    }

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "parameters")
    public void testAllAttributionModels(CampaignAttributionModel attributionModel) {
        String metrikaStatUrl = OfferDataService.getMetrikaStatUrl(COUNTER_ID, DATE, DATE, attributionModel, OFFER_ID);

        assertThat(metrikaStatUrl).isNotBlank();
    }

    @Test
    public void testUrl() {
        String metrikaStatUrl = OfferDataService.getMetrikaStatUrl(38019L, LocalDate.of(2021, 9, 28),
                LocalDate.of(2021, 9, 29), CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK, new GdiOfferId()
                        .withBusinessId(2369811L)
                        .withShopId(2491640L)
                        .withOfferYabsId(Long.parseUnsignedLong("11013634747140245719")));

        assertThat(metrikaStatUrl).isEqualTo("https://metrika.yandex.ru/stat/orders_products?id=38019&period=2021-09-28%3A2021-09-29&filter=%28ym%3As%3ALAST_YANDEX_DIRECT_CLICKDirectOfferComplexID%3D%3D%27ACQpEwAmBPiY2EpzJTGI1w%3D%3D%27%29");
    }
}
