package ru.yandex.market.checkout.checkouter.report;

import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OptionalReportParametersTest extends AbstractWebTestBase {

    @Test
    public void shouldPassPostcodeToReportIfSpecifiedOnCart() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderCreateHelper.cart(parameters);

        assertPostCodeInReportRequests(AddressProvider.POSTCODE);
    }

    @Test
    public void shouldPassPostcodeToReportIfNoneSpecifiedOnCartButExistsInGeoCoder() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        //Адрес без посткода, но посткод есть в геокодере и будет обогащен при уточнении региона
        parameters.getOrder().getDelivery().setBuyerAddress(AddressProvider.getAddressWithoutPostcode());
        orderCreateHelper.cart(parameters);

        assertPostCodeInReportRequests(AddressProvider.GEOBASE_POSTCODE);
    }

    private void assertPostCodeInReportRequests(String postCode) {
        List<LoggedRequest> loggedRequests = getActualDeliveryRequests();
        loggedRequests.forEach(r -> {
            log.info(r.toString());
            assertEquals(Collections.singletonList(postCode), r.getQueryParams().get("post-index").values());
        });
    }

    private List<LoggedRequest> getActualDeliveryRequests() {
        return reportMock.findAll(
                getRequestedFor(anyUrl())
                        .withQueryParam("place", equalTo("actual_delivery"))
        );
    }
}
