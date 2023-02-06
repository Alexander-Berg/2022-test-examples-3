package ru.yandex.market.global.checkout.clients;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.config.properties.TaxiB2bApiProperties;
import ru.yandex.mj.generated.client.taxi_v2_intergration.api.ClaimsApiClient;
import ru.yandex.mj.generated.client.taxi_v2_intergration.model.InlineObject1;
import ru.yandex.mj.generated.client.taxi_v2_intergration.model.InlineResponse200;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ClaimIdExtractorLocalTest extends BaseLocalTest {

    private final ClaimsApiClient claimsApiClient;
    private final TaxiB2bApiProperties clientProperties;

    @Test
    public void test() {

        //указать валидный orderId
        long orderId = 0L;

        InlineObject1 object1 = new InlineObject1().externalOrderId(String.valueOf(orderId)).offset(0).limit(1);

        InlineResponse200 response = claimsApiClient.integrationV2ClaimsSearch(
                clientProperties.getAuthorization(),
                clientProperties.getAcceptLanguage(),
                object1).schedule().join();

        System.out.println(response.getClaims().size());
        if (!response.getClaims().isEmpty()) {
            System.out.println(response.getClaims().get(0).getId());
        }
    }
}
