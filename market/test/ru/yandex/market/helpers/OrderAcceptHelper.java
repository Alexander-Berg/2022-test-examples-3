package ru.yandex.market.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.util.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebTestHelper
public class OrderAcceptHelper extends MockMvcAware {

    private static final Logger logger = LoggerFactory.getLogger(OrderAcceptHelper.class);

    public OrderAcceptHelper(MockMvc mockMvc,
                             TestSerializationService testSerializationService) {
        super(mockMvc, testSerializationService);
    }

    public ResultActions orderAccept(OrderAcceptParameters orderAcceptParameters) throws Exception {
        String requestBody = testSerializationService.serializeXml(orderAcceptParameters.getOrderAcceptRequest());
        logger.info("Request: {}", requestBody);

        return mockMvc.perform(
                post("/{shopId}/order/accept", orderAcceptParameters.getShopId())
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_XML)
        );
    }
}
