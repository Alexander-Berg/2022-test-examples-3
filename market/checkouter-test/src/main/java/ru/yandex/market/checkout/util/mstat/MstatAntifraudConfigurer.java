package ru.yandex.market.checkout.util.mstat;

import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * Конфигуратор мока обращения к антифроду в MSTAT
 *
 * @author hronos
 * Created on: 24.04.19
 */
@TestComponent
public class MstatAntifraudConfigurer {

    private static final String ANTIFRAUD_DETECT = "/antifraud/detect";
    private static final String CRM_INFO = "/crm/buyer/info";

    @Autowired
    private WireMockServer mstatAntifraudOrdersMock;
    @Autowired
    private TestSerializationService testSerializationService;

    public void resetAll() {
        mstatAntifraudOrdersMock.resetAll();
    }

    public void mockOk() {
        mstatAntifraudOrdersMock.stubFor(
                post(urlPathMatching(ANTIFRAUD_DETECT))
                        .withName(ANTIFRAUD_DETECT)
                        .willReturn(okJson(testSerializationService.serializeAntifraudObject(OrderVerdict.EMPTY)))
        );
        mstatAntifraudOrdersMock.stubFor(
                get(urlPathMatching(CRM_INFO))
                        .withName(CRM_INFO)
                        .willReturn(okJson("{\"refundPolicy\": \"SIMPLE\"}"))
        );
    }

    public void mockVerdict(OrderVerdict orderVerdict) {
        mstatAntifraudOrdersMock.stubFor(
                post(urlPathMatching(ANTIFRAUD_DETECT))
                        .withName(ANTIFRAUD_DETECT)
                        .willReturn(okJson(testSerializationService.serializeAntifraudObject(orderVerdict)))
        );
    }

    public void mockUntrustedUser() {
        mstatAntifraudOrdersMock.stubFor(
                get(urlPathMatching("/crm/buyer/info"))
                        .willReturn(okJson("{\"refundPolicy\": \"FULL\"}"))
        );
    }

    public List<LoggedRequest> collectDetectEvents() {
        return mstatAntifraudOrdersMock.findAll(postRequestedFor(urlEqualTo(ANTIFRAUD_DETECT)));
    }
}
