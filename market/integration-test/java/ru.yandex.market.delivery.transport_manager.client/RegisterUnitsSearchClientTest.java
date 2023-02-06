package ru.yandex.market.delivery.transport_manager.client;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.PartialIdDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.IdType;
import ru.yandex.market.delivery.transport_manager.model.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.model.filter.RegisterUnitSearchFilter;
import ru.yandex.market.delivery.transport_manager.model.page.Page;
import ru.yandex.market.delivery.transport_manager.model.page.PageRequest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class RegisterUnitsSearchClientTest extends AbstractClientTest {
    @Autowired
    private TransportManagerClient transportManagerClient;

    @Test
    @DisplayName("Поиск юнитов реестра")
    void searchRegisterUnits() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(startsWith(tmApiProperties.getUrl() + "/register-units/search")))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonRequestContent("request/register/search/by_everything.json"))
            .andExpect(queryParam("size", "10"))
            .andExpect(queryParam("page", "0"))
            .andRespond(
                withSuccess(
                    extractFileContent("response/register/search/by_everything.json"),
                    MediaType.APPLICATION_JSON
                )
            );

        RegisterUnitSearchFilter filter = RegisterUnitSearchFilter.builder()
            .unitType(UnitType.ITEM)
            .registerId(1L)
            .build();

        Page<RegisterUnitDto> actual = transportManagerClient.searchRegisterUnits(
            filter,
            new PageRequest(0, 10)
        );

        Page<RegisterUnitDto> expected = new Page<RegisterUnitDto>()
            .setData(
                List.of(
                    RegisterUnitDto.builder()
                        .id(6L)
                        .type(UnitType.ITEM)
                        .partialIds(
                            List.of(
                                PartialIdDto.builder()
                                    .idType(IdType.ORDER_ID)
                                    .value("12345")
                                    .build()
                            )
                        )
                        .build(),
                    RegisterUnitDto.builder()
                        .id(5L)
                        .type(UnitType.ITEM)
                        .partialIds(
                            List.of(
                                PartialIdDto.builder()
                                    .idType(IdType.ORDER_ID)
                                    .value("23456")
                                    .build()
                            )
                        )
                        .build()

                ))
            .setTotalPages(1)
            .setPage(0)
            .setTotalElements(2)
            .setSize(10);

        softly.assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
