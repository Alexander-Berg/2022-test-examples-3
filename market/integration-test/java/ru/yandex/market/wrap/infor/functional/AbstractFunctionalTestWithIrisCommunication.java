package ru.yandex.market.wrap.infor.functional;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClient;
import ru.yandex.market.logistics.iris.client.configuration.IrisClientModule;
import ru.yandex.market.logistics.iris.client.model.request.TrustworthyInfoRequest;
import ru.yandex.market.logistics.iris.client.model.response.TrustworthyInfoResponse;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;

public abstract class AbstractFunctionalTestWithIrisCommunication extends AbstractFunctionalTest {

    @Autowired
    private TrustworthyInfoClient trustworthyInfoClient;
    private final ObjectMapper objectMapper = IrisClientModule.getConstructedMapper();

    protected void mockIrisCommunication(String requestPath, String responsePath) {
        TrustworthyInfoRequest request;
        TrustworthyInfoResponse response;
        try {
            request = objectMapper.readValue(extractFileContent(requestPath), TrustworthyInfoRequest.class);
            response = objectMapper.readValue(extractFileContent(responsePath), TrustworthyInfoResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        when(trustworthyInfoClient.getTrustworthyInfo(refEq(request)))
            .thenReturn(response);
    }
}
