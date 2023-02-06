package ru.yandex.market.mbo.db.recipes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.gwt.models.recipe.Recipe;
import ru.yandex.market.mbo.gwt.models.recipe.RecipeGoodState;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ReportResultCheckerTest {
    private static final String REPORT_PATH = "http://tst.host.ru";
    private ReportResultChecker checker;
    private CloseableHttpClient httpClient = Mockito.mock(CloseableHttpClient.class);

    @Before
    public void setUp() throws Exception {
        checker = new ReportResultChecker(REPORT_PATH, "some user agent");
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test
    public void testMakeUrlBySearchQueryWithSpace() {
        Recipe recipe = new Recipe();
        recipe.setHid(1);
        recipe.setWithoutFilters(true);
        recipe.setSearchQuery("Сковородки и кастрюли");

        String url = checker.makeUrl(recipe, 123, ReportResultChecker.CheckType.TOTAL);

        String expectedUrl = REPORT_PATH + "/yandsearch?place=prime&pp=18&rids=123&"
            + "hid=1&text=%D0%A1%D0%BA%D0%BE%D0%B2%D0%BE%D1%80%D0%BE%D0%B4%D0%BA%D0%B8+%D0%B8+%D0%BA%D0%B0"
            + "%D1%81%D1%82%D1%80%D1%8E%D0%BB%D0%B8"
            + "&nosearchresults=1&onstock=1&allow-collapsing=1";

        Assertions.assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void testEncodeURLQueryStringOneParam() {
        String expected = "oneParam=one+param";
        String res = checker.encodeURLQueryString("oneParam=one param");
        Assertions.assertThat(res).isEqualTo(expected);
    }

    @Test
    public void testEncodeURLQueryStringNoParam() {
        String expected = "noParam";
        String res = checker.encodeURLQueryString(expected);
        Assertions.assertThat(res).isEqualTo(expected);
    }

    @Test
    public void testEncodeURLQueryStringTwoParam() {
        String expected = "oneParam=one+param&twoParam";
        String res = checker.encodeURLQueryString("oneParam=one param&twoParam");
        Assertions.assertThat(res).isEqualTo(expected);
    }

    @Test
    public void testEncodeURLQueryStringManyEqualSigns() {
        String expected = "oneParam=one+param%3Dtwo+param&treeParam=three+param";
        String res = checker.encodeURLQueryString("oneParam=one param=two param&treeParam=three param");
        Assertions.assertThat(res).isEqualTo(expected);
    }

    @Test
    public void updateEmptyReportInfoBreakWorksInNotMoscow() throws IOException {

        // First response with offers
        HttpEntity entityWithOffers = mock(HttpEntity.class);
        doReturn(buildStreamWithOffers()).when(entityWithOffers).getContent();
        CloseableHttpResponse responseWithOffers = mock(CloseableHttpResponse.class);
        doReturn(entityWithOffers).when(responseWithOffers).getEntity();

        HttpEntity entityWithNoOffers = mock(HttpEntity.class);
        doReturn(buildStreamWithNoOffers()).when(entityWithNoOffers).getContent();
        CloseableHttpResponse responseWithNoOffers = mock(CloseableHttpResponse.class);
        doReturn(entityWithNoOffers).when(responseWithNoOffers).getEntity();

        doReturn(responseWithOffers, responseWithNoOffers).when(httpClient).execute(any());

        Recipe recipe = Mockito.mock(Recipe.class);
        doReturn(RecipeGoodState.ANY).when(recipe).getGoodState();
        ReflectionTestUtils.setField(checker, "httpClient", httpClient);
        checker.updateEmptyReportInfo(recipe, ReportResultChecker.CheckType.SHOW_REVIEWS);
        verify(httpClient, times(2)).execute(any());

        verify(recipe, times(1)).setContainsReviews(false);
    }

    @Test
    public void updateEmptyReportInfoBreakWorksInMoscow() throws IOException {

        HttpEntity entityWithNoOffers = mock(HttpEntity.class);
        doReturn(buildStreamWithNoOffers()).when(entityWithNoOffers).getContent();
        CloseableHttpResponse responseWithNoOffers = mock(CloseableHttpResponse.class);
        doReturn(entityWithNoOffers).when(responseWithNoOffers).getEntity();

        doReturn(responseWithNoOffers).when(httpClient).execute(any());

        Recipe recipe = Mockito.mock(Recipe.class);
        doReturn(RecipeGoodState.ANY).when(recipe).getGoodState();
        ReflectionTestUtils.setField(checker, "httpClient", httpClient);
        checker.updateEmptyReportInfo(recipe, ReportResultChecker.CheckType.SHOW_REVIEWS);
        verify(httpClient, times(1)).execute(any());
        verify(recipe, times(1)).setContainsReviews(false);
        verify(recipe, times(0)).setTotalModelsMsk(anyLong());
        verify(recipe, times(0)).setTotalOffersMsk(anyLong());
    }

    @Test
    public void updateEmptyReportCheckMoscowSpecialFields() throws IOException {

        HttpEntity entityWithNoOffers = mock(HttpEntity.class);
        doReturn(buildStreamWithNoOffers()).when(entityWithNoOffers).getContent();
        CloseableHttpResponse responseWithNoOffers = mock(CloseableHttpResponse.class);
        doReturn(entityWithNoOffers).when(responseWithNoOffers).getEntity();

        doReturn(responseWithNoOffers).when(httpClient).execute(any());

        Recipe recipe = Mockito.mock(Recipe.class);
        doReturn(RecipeGoodState.ANY).when(recipe).getGoodState();
        ReflectionTestUtils.setField(checker, "httpClient", httpClient);
        checker.updateEmptyReportInfo(recipe, ReportResultChecker.CheckType.TOTAL);
        verify(recipe, times(1)).setTotalModelsMsk(anyLong());
        verify(recipe, times(1)).setTotalOffersMsk(anyLong());
    }

    private InputStream buildStreamWithOffers() {
        return new ByteArrayInputStream(new String(
     "{\"search\":{\"total\":7,\"totalOffers\":5,\"totalFreeOffers\":0,\"totalOffersBeforeFilters\":0," +
         "\"totalModels\":0,\"totalPassedAllGlFilters\":0,\"adult\":false,\"view\":\"list\",\"salesDetected\":false," +
         "\"maxDiscountPercent\":0,\"shops\":2,\"totalShopsBeforeFilters\":2,\"cpaCount\":0," +
         "\"isParametricSearch\":false,\"category\":{\"cpaType\":\"cpc_and_cpa\"},\"isDeliveryIncluded\":false," +
         "\"isPickupIncluded\":false,\"results\":[]}}")
            .getBytes(StandardCharsets.UTF_8));
    }

    private InputStream buildStreamWithNoOffers() {
        return new ByteArrayInputStream(new String(
            "{\"search\":{\"total\":0,\"totalOffers\":0,\"totalFreeOffers\":0,\"totalOffersBeforeFilters\":0," +
                "\"totalModels\":0,\"totalPassedAllGlFilters\":0,\"adult\":false,\"view\":\"list\"," +
                "\"salesDetected\":false,\"maxDiscountPercent\":0,\"shops\":2,\"totalShopsBeforeFilters\":2," +
                "\"cpaCount\":0,\"isParametricSearch\":false,\"category\":{\"cpaType\":\"cpc_and_cpa\"}," +
                "\"isDeliveryIncluded\":false,\"isPickupIncluded\":false,\"results\":[]}}")
            .getBytes(StandardCharsets.UTF_8));
    }
}
