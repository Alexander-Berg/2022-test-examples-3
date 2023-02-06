package ru.yandex.market.checkout.helpers;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.tariff.TariffData;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class TariffDataHelper extends MockMvcAware {

    public static final String PUT_DELIVERY_TARIFF_URL = "/orders/{orderId}/delivery/tariff";

    public TariffDataHelper(WebApplicationContext webApplicationContext,
                            TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public void putTariffData(long orderId, TariffData tariffData) throws Exception {
        putTariffData(orderId, tariffData, ClientInfo.SYSTEM);
    }

    public void putTariffData(long orderId, TariffData tariffData, ClientInfo clientInfo) throws Exception {
        putTariffDataForActions(orderId, tariffData, clientInfo)
                .andExpect(status().isOk());
    }

    public ResultActions putTariffDataForActions(long orderId, TariffData tariffData) throws Exception {
        return putTariffDataForActions(orderId, tariffData, ClientInfo.SYSTEM);
    }

    public ResultActions putTariffDataForActions(long orderId, TariffData tariffData, ClientInfo clientInfo)
            throws Exception {
        MockHttpServletRequestBuilder builder = put(PUT_DELIVERY_TARIFF_URL, orderId)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(tariffData));

        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);


        return mockMvc.perform(builder)
                .andDo(log());
    }
}
