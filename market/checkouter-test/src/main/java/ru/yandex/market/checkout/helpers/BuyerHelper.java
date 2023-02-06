package ru.yandex.market.checkout.helpers;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.json.PersonalDataGatheredHolder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.buyer.PersonalDataStatus;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.RGB;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOP_ID;

@WebTestHelper
public class BuyerHelper extends MockMvcAware {

    public BuyerHelper(WebApplicationContext webApplicationContext,
                       TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public ResultActions setPersonalDataGatheredForActions(long orderId,
                                                           PersonalDataStatus status,
                                                           Color color,
                                                           ClientInfo clientInfo) throws Exception {
        MockHttpServletRequestBuilder builder = put("/orders/{orderId}/buyer/personal-data-gathered", orderId)
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, clientInfo.getId() == null ? null : clientInfo.getId().toString())
                .param(SHOP_ID, clientInfo.getShopId() == null ? null : clientInfo.getShopId().toString())
                .param(RGB, color == null ? null : color.name())
                .content(testSerializationService.serializeCheckouterObject(new PersonalDataGatheredHolder(status)))
                .contentType(MediaType.APPLICATION_JSON_UTF8);

        return mockMvc.perform(builder)
                .andDo(log());
    }

    public void setPersonalDataGathered(long orderId,
                                        PersonalDataStatus status,
                                        Color color,
                                        ClientInfo clientInfo) throws Exception {
        setPersonalDataGatheredForActions(orderId, status, color, clientInfo).andExpect(status().isOk());
    }
}
