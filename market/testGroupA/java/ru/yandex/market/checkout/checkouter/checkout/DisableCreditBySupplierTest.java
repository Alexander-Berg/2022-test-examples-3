package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

public class DisableCreditBySupplierTest extends AbstractWebTestBase {

    @BeforeEach
    void init() {
        reportMock.resetRequests();
        checkouterProperties.setEnableShowCredits(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void checkSendingSupplierIdToCreditInfo(boolean hideCredit) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.HIDE_CREDIT_FOR_BLACKLISTED_SUPPLIERS, hideCredit);
        OrderItem item1 = OrderItemProvider.defaultOrderItem();
        OrderItem item2 = OrderItemProvider.defaultOrderItem();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParametersWithItems(item1, item2);
        parameters.setShowCredits(true);

        orderCreateHelper.cart(parameters);

        assertSupplierIdAsCreditInfoParameter(hideCredit);
    }

    private void assertSupplierIdAsCreditInfoParameter(boolean hideCredit) {
        List<ServeEvent> creditInfoEvents = reportMock.getServeEvents().getServeEvents()
                .stream()
                .filter(se -> se.getRequest()
                        .queryParameter("place")
                        .containsValue(MarketReportPlace.CREDIT_INFO.getId()))
                .collect(Collectors.toList());
        assertThat(creditInfoEvents, hasSize(1));

        LoggedRequest creditInfoRequest = creditInfoEvents.get(0).getRequest();
        String offersListParam = creditInfoRequest.getQueryParams().get("offers-list").values().get(0);
        assertThat(offersListParam.contains(";supplier_id:"), is(hideCredit));
    }
}
