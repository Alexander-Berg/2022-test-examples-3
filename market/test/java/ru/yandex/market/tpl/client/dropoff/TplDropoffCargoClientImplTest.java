package ru.yandex.market.tpl.client.dropoff;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.client.BaseClientTest;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoCreateCommandDto;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
class TplDropoffCargoClientImplTest extends BaseClientTest {

    private final ObjectMapper objectMapper;
    private final TplDropoffCargoClientImpl dropoffCargoClient;

    @Test
    @SneakyThrows
    void createCargo_success() {
        var barcode = "12876";
        String logisticPointIdFrom = "34";
        String logisticPointIdTo = "67";

        var expected = new DropoffCargoDto();
        expected.setBarcode(barcode);
        expected.setLogisticPointIdFrom(logisticPointIdFrom);
        expected.setLogisticPointIdTo(logisticPointIdTo);
        expected.setStatus("CREATED");

        mock.expect(method(HttpMethod.PUT))
                .andExpect(requestToUriTemplate(tplIntUrl + "/dropoff/cargo/" + barcode))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(expected),
                        MediaType.APPLICATION_JSON
                ));

        DropoffCargoCreateCommandDto command = new DropoffCargoCreateCommandDto();
        command.setBarcode(barcode);
        command.setLogisticPointIdFrom(logisticPointIdFrom);
        command.setLogisticPointIdTo(logisticPointIdTo);
        DropoffCargoDto result = dropoffCargoClient.createCargo(command).getBody();

        assertThat(result).isEqualTo(expected);
    }

}
