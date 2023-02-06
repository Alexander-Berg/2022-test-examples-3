package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.QueryParameter;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

public class CheckoutDebugCourierOptionsTest extends AbstractWebTestBase {

    @Test
    public void shouldPassDebugAllCourierOptionsInCart() {
        Parameters parameters = new Parameters();
        parameters.setDebugAllCourierOptions(true);

        orderCreateHelper.cart(parameters);

        assertDebugAllCourierOptions();
    }

    @Test
    public void shouldPassDebugAllCourierOptionsInCheckout() {
        Parameters parameters = new Parameters();
        parameters.setDebugAllCourierOptions(true);

        orderCreateHelper.cart(parameters);

        assertDebugAllCourierOptions();
    }

    private void assertDebugAllCourierOptions() {
        List<LoggedRequest> requests = reportMockWhite.findAll(WireMock.getRequestedFor(WireMock.anyUrl())
                .withQueryParam("place", WireMock.equalTo("actual_delivery")));

        assertThat(requests, hasSize(2));
        requests.forEach(request -> {
            QueryParameter queryParameter = request.getQueryParams().get("debug-all-courier-options");
            assertThat(queryParameter, notNullValue());
            assertThat(queryParameter.isSingleValued(), is(true));
            assertThat(queryParameter.values().get(0), is("1"));
        });
    }
}
