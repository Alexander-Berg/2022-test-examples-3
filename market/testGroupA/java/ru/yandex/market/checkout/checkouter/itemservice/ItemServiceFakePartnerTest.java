package ru.yandex.market.checkout.checkouter.itemservice;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.yauslugi.model.YaServiceDto;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICE_REGISTRATION;

/**
 * @author zagidullinri
 * @date 19.11.2021
 */
public class ItemServiceFakePartnerTest extends AbstractWebTestBase {

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private WireMockServer yaUslugiMock;

    @Autowired
    private ObjectMapper yaUslugiObjectMapper;

    @BeforeEach
    public void setUp() {
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .willReturn(okJson("{}")));
    }

    @Test
    public void createShouldAddExpFlagsWhenExperimentEnabled() throws IOException {
        createOrderWithService(Experiments.of(Experiments.ENABLE_FAKE_SERVICE_PARTNER_EXP,
                Experiments.ENABLE_FAKE_SERVICE_PARTNER_EXP_VALUE));

        YaServiceDto yaServiceDto = getCreateServiceRequestBody();

        assertNotNull(yaServiceDto.getExpFlags());
        assertTrue(yaServiceDto.getExpFlags().getForceFakePartner());
    }

    @Test
    public void createShouldNotAddExpFlagsWhenExperimentDisabled() throws IOException {
        createOrderWithService(Experiments.of(Experiments.ENABLE_FAKE_SERVICE_PARTNER_EXP, ""));

        YaServiceDto yaServiceDto = getCreateServiceRequestBody();

        assertNull(yaServiceDto.getExpFlags());
    }

    @Test
    public void createShouldNotAddExpFlagsWhenEmptyExperiments() throws IOException {
        createOrderWithService(Experiments.empty());

        YaServiceDto yaServiceDto = getCreateServiceRequestBody();

        assertNull(yaServiceDto.getExpFlags());
    }

    private void createOrderWithService(Experiments experiments) {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        parameters.setExperiments(experiments);
        orderCreateHelper.createOrder(parameters);
        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);
    }

    private YaServiceDto getCreateServiceRequestBody() throws java.io.IOException {
        String createServiceBodyRequest = Iterables.getOnlyElement(yaUslugiMock.getAllServeEvents())
                .getRequest()
                .getBodyAsString();
        return yaUslugiObjectMapper.readValue(createServiceBodyRequest, YaServiceDto.class);
    }
}
