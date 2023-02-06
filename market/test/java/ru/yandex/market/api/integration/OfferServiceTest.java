package ru.yandex.market.api.integration;

import java.util.Collections;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.internal.report.ReportSort;
import ru.yandex.market.api.internal.report.ReportSortType;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.offer.GetOffersByModelRequest;
import ru.yandex.market.api.offer.OfferService;
import ru.yandex.market.api.offer.Offers;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

/**
 * Created by tesseract on 01.03.17.
 */
public class OfferServiceTest extends BaseTest {

    @Inject
    OfferService offerService;

    @Inject
    ReportTestClient reportClient;

    /**
     * В проде NPE при получении модели
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3336">MARKETAPI-3336: NPE в проде</a>
     */
    @Test
    public void npe1716297601() {
        // настройка системы
        reportClient.getModelInfoById(1716297601, "modelinfo_1716297601.json");
        reportClient.searchOffersByModelId(1716297601, "productoffers_1716297601.json");

        //  вызов системы
        GetOffersByModelRequest offerRequest = new GetOffersByModelRequest()
                .setModelId(1716297601)
                .setSort(new ReportSort(ReportSortType.PRICE, SortOrder.ASC))
                .setPageInfo(new PageInfo(1, 1))
                .setFields(Collections.emptyList())
                .setGenericParams(genericParams);

        Offers offers = Futures.waitAndGet(offerService.getModelOffersV1(offerRequest));

        // проверка утверждений
        Assert.assertNotNull(offers);
        Assert.assertEquals(0, offers.getCount());
    }

    /**
     * Получение офферов для групповой модели
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3614">MARKETAPI-3614: В model/{id}/offers при запросе на productoffers для групповой модели явно передаем id модификаций</a>
     */
    @Test
    public void groupModel() {
        long id = 11007864L;
        // настройка системы
        reportClient.getModelInfoById(id, "modelinfo_11007864.json");
        reportClient.getModelOffers(id, "productoffers_11007864.json");
        reportClient.getModelModifications(id, "modelmodifications_11007864.json");

        //  вызов системы
        GetOffersByModelRequest offerRequest = new GetOffersByModelRequest()
                .setModelId(id)
                .setPageInfo(new PageInfo(1, 1))
                .setFields(Collections.emptyList())
                .setGenericParams(genericParams);

        Offers offers = Futures.waitAndGet(offerService.getModelOffersV1(offerRequest));

        // проверка утверждений
        Assert.assertNotNull(offers);
        Assert.assertNotNull(offers.getItems());
        Assert.assertFalse(offers.getItems().isEmpty());
    }
}
