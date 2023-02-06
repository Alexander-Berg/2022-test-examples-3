package ru.yandex.market.global.checkout.clients;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.config.properties.TaxiB2bApiProperties;
import ru.yandex.mj.generated.client.taxi_v1_intergration.api.PerformerApiClient;
import ru.yandex.mj.generated.client.taxi_v1_intergration.model.PerformerPositionResponse;

@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PerformerPositionLocalTest extends BaseLocalTest {

    private final PerformerApiClient performerApiClient;
    private final TaxiB2bApiProperties clientProperties;

    @Test
    public void test() {

        //указать валидный claimID
        String claimId = "";

        PerformerPositionResponse response = null;
        try {
            response = performerApiClient.integrationV1ClaimsPerformerPosition(claimId,
                    clientProperties.getAuthorization()).schedule().join();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return;
        }

        System.out.println("LAT " + response.getPosition().getLat());
        System.out.println("LON " + response.getPosition().getLon());
    }

}
