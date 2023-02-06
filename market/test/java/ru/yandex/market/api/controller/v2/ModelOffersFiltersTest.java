package ru.yandex.market.api.controller.v2;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;

import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.FiltersResult;
import ru.yandex.market.api.domain.v2.ResultFieldV2;
import ru.yandex.market.api.domain.v2.offers.GetOffersByModelResult;
import ru.yandex.market.api.domain.v2.option.AvailableReportSort;
import ru.yandex.market.api.integration.ParameterizedTestBase;
import ru.yandex.market.api.internal.report.ReportSort;
import ru.yandex.market.api.internal.report.ReportSortType;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.offer.GetOffersByModelRequest;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.option;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.optionHow;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.optionId;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.options;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.sort;
import static ru.yandex.market.api.matchers.ReportSortsMatcher.text;

/**
 * Created by vdorogin on 18.05.17.
 */
public class ModelOffersFiltersTest extends ParameterizedTestBase {

    @Inject
    ModelsControllerV2 controller;
    @Inject
    ReportTestClient reportTestClient;

    public long id;
    public String modelinfoFile;
    public String productoffersFile;

    @Parameterized.Parameters
    public static Object[] testData() {
        return new Object[]{
            new Object[]{13953515L, "modelinfo_guru_13953515.json", "productoffers_guru_13953515.json"},
            new Object[]{1366969686L, "modelinfo_visual_1366969686.json", "productoffers_visual_1366969686.json"},
            new Object[]{11007864L, "modelinfo_group_11007864.json", "productoffers_group_11007864.json"},
            new Object[]{10972776L, "modelinfo_modif_10972776.json", "productoffers_modif_10972776.json"},
            new Object[]{11620757L, "modelinfo_book_11620757.json", "productoffers_book_11620757.json"}
        };
    }

    public ModelOffersFiltersTest(long id, String modelinfoFile, String productoffersFile) {
        this.id = id;
        this.modelinfoFile = modelinfoFile;
        this.productoffersFile = productoffersFile;

    }

    /**
     * Проверка отсутствия фильтра "-3" в v2/model/{}/offers
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3563">MARKETAPI-3563: Фильтр "-3" в v2/model/{}/offers</a>
     */
    @Test
    public void offerFilters() {
        // настройка системы
        reportTestClient.getModelInfoById(id, modelinfoFile);
        reportTestClient.getModelOffers(id, productoffersFile);
        // вызов системы
        GetOffersByModelResult result = controller.getOffers(
                new GetOffersByModelRequest()
                        .setModelId(id)
                        .setShopRegions(IntLists.EMPTY_LIST)
                        .setFields(Arrays.asList(ResultFieldV2.FILTERS))
                        .setPageInfo(PageInfo.DEFAULT)
                        .setSort(new ReportSort(ReportSortType.RELEVANCY, SortOrder.DESC))
                        .setLocalDelivery(false)
                        .setDeliveryIncluded(false)
                        .setGroupBy(null)
                        .setFilterParameters(Collections.emptyMap())
                        .setWithModel(true)
                        .setSkuIds(Collections.emptyList())
                        .setGenericParams(genericParams)
        ).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getFilters());
        Assert.assertFalse(result.getFilters().isEmpty());
        Assert.assertFalse(result.getFilters().stream().anyMatch(f -> "-3".equals(f.getId())));
    }

    /**
     * Проверка сортировки в v2/models/{}/offers/filters
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3535">MARKETAPI-3535: (тест) Проверка сортировки в v2/models/{}/offers/filters</a>
     */
    @Test
    public void sortFields() {
        // настройка системы
        reportTestClient.getModelInfoById(id, modelinfoFile);
        reportTestClient.getModelOffers(id, productoffersFile);
        // вызов системы
        FiltersResult result = controller.getModelOffersFilters(
            id,
            Arrays.asList(ResultFieldV2.SORTS),
            null,
            null,
            genericParams
        ).waitResult();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getSorts());

        Assert.assertThat(
            result.getSorts(),
            containsInAnyOrder(
                byPopularitySort(),
                byPrice(),
                byRatingAndPrice(),
                byDiscount()
            )
        );

    }

    private static Matcher<AvailableReportSort> byPopularitySort() {
        return sort(
            text("по популярности")
        );
    }

    private static Matcher<AvailableReportSort> byPrice() {
        return sort(
            text("по цене"),
            options(
                option(
                    optionId("aprice"), optionHow(SortOrder.ASC)
                ),
                option(
                    optionId("dprice"),
                    optionHow(SortOrder.DESC)
                )
            )
        );
    }

    private static Matcher<AvailableReportSort> byRatingAndPrice() {
        return sort(
            text("по рейтингу и цене"),
            options(
                option(optionId("rorp"), optionHow(SortOrder.DESC))
            )
        );
    }

    private static Matcher<AvailableReportSort> byDiscount() {
        return sort(
            text("по размеру скидки"),
            options(option(optionId("discount_p"), optionHow(SortOrder.DESC)))
        );
    }

}
