package ru.yandex.market.checkout.pushapi.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class PushApiOrderAcceptHelper {

    private static final Logger logger = LoggerFactory.getLogger(PushApiOrderAcceptHelper.class);

    private final MockMvc mockMvc;
    private final PushApiTestSerializationService testSerializationService;
    private final SettingsService settingsService;
    private final SettingsProvider settingsProvider;

    public PushApiOrderAcceptHelper(MockMvc mockMvc,
                                    PushApiTestSerializationService testSerializationService,
                                    SettingsService settingsService,
                                    SettingsProvider settingsProvider) {
        this.mockMvc = mockMvc;
        this.testSerializationService = testSerializationService;
        this.settingsService = settingsService;
        this.settingsProvider = settingsProvider;
    }

    public ResultActions orderAcceptForActions(PushApiOrderParameters parameters) throws Exception {
        return orderAcceptForActions(parameters, 200);
    }

    public ResultActions orderAcceptForActions(PushApiOrderParameters parameters,
                                               int expectedStatus) throws Exception {
        return orderAcceptForActions(parameters, expectedStatus, false);
    }

    public ResultActions orderAcceptForActions(PushApiOrderParameters parameters,
                                               int expectedStatus,
                                               boolean syncResponse) throws Exception {
        settingsService.updateSettings(
                parameters.getShopId(),
                settingsProvider.buildXmlSettings(parameters.isPartnerInterface(), parameters.getDataType()).toBuilder()
                        .features(parameters.getFeatures())
                        .build(),
                parameters.isSandbox()
        );

        String requestBody = testSerializationService.serialize(parameters.getOrder());

        logger.debug("requestBody: {}", requestBody);

        if (syncResponse) {
            // Если ответ произошел сразу (например из-за ошибки входных данных)
            return mockMvc.perform(post("/shops/{shopId}/order/accept", parameters.getShopId())
                            .content(requestBody)
                            .contentType(MediaType.APPLICATION_XML))
                    .andExpect(status().is(expectedStatus));
        } else {
            var result = mockMvc.perform(post("/shops/{shopId}/order/accept", parameters.getShopId())
                            .content(requestBody)
                            .contentType(MediaType.APPLICATION_XML))
                    .andExpect(request().asyncStarted())
                    .andReturn();

            return mockMvc.perform(asyncDispatch(result)).andExpect(status().is(expectedStatus));
        }
    }
}
