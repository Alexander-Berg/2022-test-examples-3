package step;

import java.util.List;

import client.BlueFApiClient;
import dto.responses.bluefapi.ResolveLink;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;

public class BlueFApiSteps {

    private static final BlueFApiClient BLUE_F_API = new BlueFApiClient();

    @Step("Получаем ссылку для вызова курьера в такси")
    public ResolveLink.Collection.OnDemandUrl resolveOnDemandLink(String externalId) {
        ResolveLink resolveLink = BLUE_F_API.resolveOnDemandLink(externalId);
        Assertions.assertNotNull(resolveLink, "Пустой объект резолв линка");
        List<ResolveLink.Collection.OnDemandUrl> onDemandUrls = BLUE_F_API.resolveOnDemandLink(externalId)
            .getCollections()
            .getOnDemandUrl();
        Assertions.assertFalse(onDemandUrls.isEmpty(), "Список ссылок пуст");
        return onDemandUrls.get(0);
    }
}
