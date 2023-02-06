package ru.yandex.market.partner.mvc.controller.business;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.saas.impl.attributes.SaasDocType;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasOfferFilter;
import ru.yandex.market.mbi.datacamp.saas.impl.model.SaasSearchResult;
import ru.yandex.market.mbi.web.paging.SeekSliceRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link BusinessSummaryController}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "BusinessSummaryControllerTest.csv")
public class BusinessSummaryControllerTest extends FunctionalTest {

    @Autowired
    private SaasService saasService;

    /**
     * Проверяет, что статистика по заказам считается корректо для бизнеса. В частности, что оффера dsbs считаются
     * как cpa.
     *
     * @see BusinessSummaryController#getBusinessAssortmentSummary(long)
     */
    @Test
    void testGetBusinessAssortmentSummary() {
        mockSaasService(500);
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/10/summary/assortment");
        JsonTestUtil.assertEquals(response, "" +
                "{" +
                "\"cpcOffersCount\": 100," +
                "\"cpaOffersCount\": 700," +
                "\"totalOffersCount\": 800" +
                "}");

        verify(saasService).searchBusinessOffers(
                SaasOfferFilter.newBuilder()
                        .setPrefix(10L)
                        .setBusinessId(10)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .setDocType(SaasDocType.OFFER)
                        .setUnitedCatalog(true)
                        .build());
        verifyNoMoreInteractions(saasService);
    }

    /**
     * Проверяет, что для бизнеса без услуг возвращется нулевая статистика по офферам.
     *
     * @see BusinessSummaryController#getBusinessAssortmentSummary(long)
     */
    @Test
    void testGetBusinessAssortmentSummary_Empty() {
        mockSaasService(0);
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/11/summary/assortment");
        JsonTestUtil.assertEquals(response, "" +
                "{" +
                "\"cpcOffersCount\": 0," +
                "\"cpaOffersCount\": 0," +
                "\"totalOffersCount\": 0" +
                "}");

        verify(saasService).searchBusinessOffers(
                SaasOfferFilter.newBuilder()
                        .setPrefix(11L)
                        .setBusinessId(11)
                        .setPageRequest(SeekSliceRequest.firstN(0))
                        .setDocType(SaasDocType.OFFER)
                        .setUnitedCatalog(true)
                        .build());
        verifyNoMoreInteractions(saasService);
    }

    /**
     * Проверяет определение, есть ли у бизнеса метрика.
     * <ul>
     *     <li>10 - магазины с метрикой</li>
     *     <li>12 - один магазин с метрикой, второй нет</li>
     *     <li>13 - только поставщик</li>
     * </ul>
     */
    @ParameterizedTest
    @CsvSource({"10, true", "11, true", "12, false", "13, true"})
    void testIsMetrikaEnabled(long businessId, boolean expectedResult) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "businesses/" + businessId + "/summary/metrika/is-enabled");
        JsonTestUtil.assertEquals(response, Boolean.toString(expectedResult));
    }

    /**
     * Проверяет, что баланс считается только по белым не DSBS магазинам.
     *
     * @see BusinessSummaryController#getBusinessBalance(long)
     */
    @ParameterizedTest
    @CsvSource({"10, 10", "11, 0"})
    void testBusinessBalance(long businessId, int expectedResult) {
        ResponseEntity<String> response = FunctionalTestHelper.get(baseUrl + "businesses/" + businessId + "/balance");
        JsonTestUtil.assertEquals(response, Integer.toString(expectedResult));
    }

    private void mockSaasService(int result) {
        SaasSearchResult resultMock = SaasSearchResult.builder()
                .setTotalCount(result)
                .setOffers(List.of())
                .build();
        when(saasService.searchBusinessOffers(any()))
                .thenReturn(resultMock);
    }
}
