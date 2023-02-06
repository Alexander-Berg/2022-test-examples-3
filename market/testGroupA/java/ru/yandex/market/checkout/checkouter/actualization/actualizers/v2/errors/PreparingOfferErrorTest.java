package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2.errors;

import java.util.List;

import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.common.report.model.Color;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

public class PreparingOfferErrorTest extends AbstractWebTestBase {

    @Autowired
    private ReportConfigurer reportConfigurer;
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Test
    void shouldFailOnOfferInfoFailedCall() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().skip(MarketReportPlace.OFFER_INFO);
        parameters.turnOffErrorChecks();

        reportConfigurer.mockReportPlaceError(MarketReportPlace.OFFER_INFO, parameters.getReportParameters(),
                Fault.MALFORMED_RESPONSE_CHUNK);

        var response = orderCreateHelper.multiCartActualize(parameters);

        assertThat(response.getCartFailures(), not(empty()));

        var failure = response.getCartFailures().get(0);

        assertThat(failure.getErrorCode(), is(OrderFailure.Code.UNKNOWN_ERROR));
        assertThat(failure.getErrorDetails(), is("Error occurred while searching items in Report"));
    }

    @Test
    void shouldFailOnShopInfoFailedCall() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().skip(MarketReportPlace.SHOP_INFO);
        parameters.turnOffErrorChecks();

        reportConfigurer.mockReportPlaceError(MarketReportPlace.SHOP_INFO, parameters.getReportParameters(),
                Fault.MALFORMED_RESPONSE_CHUNK);

        var response = orderCreateHelper.multiCartActualize(parameters);

        assertThat(response.getCartFailures(), not(empty()));

        var failure = response.getCartFailures().get(0);

        assertThat(failure.getErrorCode(), is(OrderFailure.Code.UNKNOWN_ERROR));
        assertThat(failure.getErrorDetails(),
                is("All market report calls failed, main report exception is: " +
                        "ru.yandex.market.common.report.model.ReportException: IO"));
    }

    @Test
    void shouldFailOnOfferColorMix() {
        checkouterProperties.setSetOrderColorUsingReportOfferColor(true);

        var item1 = OrderItemProvider.orderItemWithSortingCenter()
                .offer("some offer")
                .build();
        var item2 = OrderItemProvider.orderItemWithSortingCenter()
                .offer("another offer")
                .build();
        var parameters = BlueParametersProvider.defaultBlueOrderParameters(OrderProvider.orderBuilder()
                .configure(OrderProvider::applyDefaults)
                .item(item1)
                .item(item2)
                .build());
        parameters.getReportParameters().setOffers(List.of(
                FoundOfferBuilder.createFrom(item1)
                        .color(Color.WHITE)
                        .build(),
                FoundOfferBuilder.createFrom(item2)
                        .color(Color.BLUE)
                        .build()
        ));
        parameters.turnOffErrorChecks();

        var response = orderCreateHelper.multiCartActualize(parameters);

        assertThat(response.getCartFailures(), not(empty()));

        var failure = response.getCartFailures().get(0);

        assertThat(failure.getErrorCode(), is(OrderFailure.Code.UNKNOWN_ERROR));
        assertThat(failure.getErrorDetails(), startsWith("Mixed offer colors in order: "));
    }

    @Test
    void shouldFailOnStockRetrieve() {
        var parameters = BlueParametersProvider.defaultBlueOrderParameters();
        var mockConfig = parameters.configuration().cart(parameters.getOrder());
        parameters.turnOffErrorChecks();

        mockConfig.setShouldMockStockStorageGetAmountResponse(false);
        stockStorageConfigurer.mockErrorForGetAvailableCount();

        var response = orderCreateHelper.multiCartActualize(parameters);

        assertThat(response.getCartFailures(), not(empty()));

        var failure = response.getCartFailures().get(0);

        assertThat(failure.getErrorCode(), is(OrderFailure.Code.UNKNOWN_ERROR));
        assertThat(failure.getErrorDetails(),
                is("Unable to actualize items count in StockStorage"));
    }

}
