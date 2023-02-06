package ru.yandex.market.checkout.checkouter.actualization.multicart.preprocessor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.fetchers.ReportOffersFetcher;
import ru.yandex.market.checkout.checkouter.actualization.flow.MultiCartFlowFactory;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MarketReportSearchService;
import ru.yandex.market.checkout.checkouter.order.ReportSearchParameters;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

class ReportOffersFetcherTest extends AbstractWebTestBase {

    @Autowired
    ExecutorService requestsExecutor;
    @Autowired
    ColorConfig colorConfig;
    @Autowired
    CheckouterProperties properties;

    @Test
    void preprocessShouldSearchItemsInReport() throws Throwable {
        var marketReportSearchServiceMock = Mockito.mock(MarketReportSearchService.class);
        var fetcher = new ReportOffersFetcher(marketReportSearchServiceMock,
                colorConfig, requestsExecutor, 5000, properties);
        var cart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());

        Mockito.when(marketReportSearchServiceMock.searchItems(any(), any())).thenReturn(List.of(
                FoundOfferBuilder.create()
                        .cpa("real")
                        .build()
        ));

        assertThat(MultiCartFlowFactory.fetch(fetcher)
                .apply(MultiCartFetchingContext.of(multiCartContext, cart)).await().orElseThrow()).hasSize(2);
        Mockito.verify(marketReportSearchServiceMock, times(2)).searchItems(any(), any());
    }

    @Test
    void preprocessThrowIfReportCallFails() {
        var marketReportSearchServiceMock = Mockito.mock(MarketReportSearchService.class);
        var fetcher = new ReportOffersFetcher(marketReportSearchServiceMock,
                colorConfig, requestsExecutor, 5000, properties);
        var cart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());

        Mockito.when(marketReportSearchServiceMock.searchItems(any(), any())).thenThrow(IllegalStateException.class);

        assertThatThrownBy(() -> MultiCartFlowFactory.fetch(fetcher)
                .apply(MultiCartFetchingContext.of(multiCartContext, cart)).await())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error occurred while searching items in Report");

        Mockito.verify(marketReportSearchServiceMock, times(2)).searchItems(any(), any());

    }

    @Test
    void preprocessShouldThrowIfSearchIsTooLong() {
        var marketReportSearchServiceMock = Mockito.mock(MarketReportSearchService.class);
        int reportSearchTimeout = 100;
        var fetcher = new ReportOffersFetcher(marketReportSearchServiceMock,
                colorConfig, requestsExecutor, reportSearchTimeout, properties);
        var cart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());

        Mockito.when(marketReportSearchServiceMock.searchItems(any(), any())).thenAnswer(
                new AnswersWithDelay(reportSearchTimeout + 50, invocation -> List.of(
                        FoundOfferBuilder.create()
                                .cpa("real")
                                .build()
                )));

        assertThatThrownBy(() -> MultiCartFlowFactory.fetch(fetcher)
                .apply(MultiCartFetchingContext.of(multiCartContext, cart)).await())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timeout searching items in Report");

        Mockito.verify(marketReportSearchServiceMock, times(2)).searchItems(any(), any());
    }

    @Test
    void testTurboPlusReportSearchParameters() throws Throwable {
        var marketReportSearchServiceMock = Mockito.mock(MarketReportSearchService.class);
        var fetcher = new ReportOffersFetcher(marketReportSearchServiceMock,
                colorConfig, requestsExecutor, 5000, properties);
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setColor(Color.TURBO_PLUS);
        MultiCart cart = orderCreateHelper.cart(parameters);
        ImmutableMultiCartParameters multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());

        Mockito.when(marketReportSearchServiceMock.searchItems(any(), any())).thenReturn(List.of(
                FoundOfferBuilder.create()
                        .cpa("real")
                        .build()
        ));

        assertThat(MultiCartFlowFactory.fetch(fetcher)
                .apply(MultiCartFetchingContext.of(multiCartContext, cart)).await().orElseThrow()).hasSize(1);
        ArgumentCaptor<ReportSearchParameters> captor = ArgumentCaptor.forClass(ReportSearchParameters.class);
        Mockito.verify(marketReportSearchServiceMock, times(1))
                .searchItems(captor.capture(), any());
        assertEquals(captor.getAllValues().size(), 1);
        assertTrue(captor.getAllValues().get(0).isIgnoreHiddenShops());
    }

    @Test
    void testDisabledPromoReportSearchParameters() throws Throwable {
        var marketReportSearchServiceMock = Mockito.mock(MarketReportSearchService.class);
        var fetcher = new ReportOffersFetcher(marketReportSearchServiceMock,
                colorConfig, requestsExecutor, 5000, properties);
        Parameters parameters = defaultBlueOrderParameters();
        MultiCart cart = orderCreateHelper.cart(parameters);
        final String promoThresholds = "blocked_1421525_3";
        ImmutableMultiCartParameters multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .withDisabledPromoThresholds(promoThresholds)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());

        Mockito.when(marketReportSearchServiceMock.searchItems(any(), any())).thenReturn(List.of(
                FoundOfferBuilder.create()
                        .cpa("real")
                .build()));

        assertThat(fetcher.fetch(ImmutableMultiCartContext.from(multiCartContext, cart))).hasSize(1);

        ArgumentCaptor<ReportSearchParameters> captor = ArgumentCaptor.forClass(ReportSearchParameters.class);
        Mockito.verify(marketReportSearchServiceMock, times(1))
                .searchItems(captor.capture(), any());

        assertEquals(captor.getAllValues().get(0).getDisabledPromoThresholds(), promoThresholds);
    }
}
