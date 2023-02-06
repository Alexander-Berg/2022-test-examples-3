package ru.yandex.market.adv.shop.integration.checkouter.interactor.sender;

import java.nio.charset.StandardCharsets;

import org.mockserver.client.MockServerClient;
import org.mockserver.matchers.MatchType;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationMockServerTest;
import ru.yandex.market.adv.shop.integration.checkouter.properties.SendToCheckouterProperties;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

/**
 * Date: 29.07.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
class AbstractCheckouterShopIntegrationTest extends AbstractShopIntegrationMockServerTest {

    private final String testName;

    @Autowired
    private SendToCheckouterProperties sendToCheckouterProperties;

    AbstractCheckouterShopIntegrationTest(MockServerClient server, String testName) {
        super(server);
        this.testName = testName;
    }

    protected void mockCheckouter(String testName, int statusCode) {
        server.when(
                        request()
                                .withMethod("PUT")
                                .withPath("/orders/items/edit-fee")
                                .withBody(
                                        json(
                                                loadFile(this.testName + "/json/request/" + testName + ".json"),
                                                StandardCharsets.UTF_8,
                                                MatchType.STRICT
                                        )
                                )
                )
                .respond(
                        response()
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(
                                        json(
                                                loadFile(this.testName + "/json/response/" + testName + ".json"),
                                                StandardCharsets.UTF_8,
                                                MatchType.STRICT
                                        )
                                )
                                .withStatusCode(statusCode)
                );
    }

    protected void run(Runnable runnable) {
        boolean oldSkipOrder = sendToCheckouterProperties.isSkipOrder();
        try {
            sendToCheckouterProperties.setSkipOrder(false);

            runnable.run();

            sendToCheckouterProperties.setSkipOrder(oldSkipOrder);
        } catch (Throwable e) {
            sendToCheckouterProperties.setSkipOrder(oldSkipOrder);
            throw e;
        }
    }
}
