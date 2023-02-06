package ru.yandex.market.tpl.internal.client;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.internal.client.model.DeliveryServiceByLocationRequestDto;
import ru.yandex.market.tpl.internal.client.model.DeliveryServiceByLocationResponseDto;
import ru.yandex.market.tpl.internal.client.model.GeoCoordinates;
import ru.yandex.market.tpl.internal.client.model.SortingCenterInfoDto;
import ru.yandex.market.tpl.internal.client.model.clientreturn.ClientReturnCreateDto;
import ru.yandex.market.tpl.internal.client.model.clientreturn.ClientReturnSystemCreated;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TplInternalClientTest extends AbstractTest {
    @Test
    @DisplayName("Контекст поднимается")
    void context() {
        softly.assertThat(tplInternalClient).isNotNull();
    }

    @Test
    @DisplayName("Получение идентификатора партнера")
    void getDeliveryServiceByLocation() {
        mock.expect(requestTo("http://localhost:8080/internal/ds-by-location"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(resourceContent("get_delivery_service_by_location/request.json"), true))
            .andRespond(
                withStatus(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                    .body(resourceContent("get_delivery_service_by_location/response.json"))
            );

        DeliveryServiceByLocationResponseDto partnerByLocation = tplInternalClient.getDeliveryServiceByLocation(
            DeliveryServiceByLocationRequestDto.builder()
                .addressString("Москва, Льва Толстого 16")
                .geoCoordinates(
                    GeoCoordinates.builder()
                        .latitude(new BigDecimal("55.733969"))
                        .longitude(new BigDecimal("37.587093"))
                        .build()
                )
                .build()
        );

        softly.assertThat(partnerByLocation).isEqualToComparingFieldByFieldRecursively(
            DeliveryServiceByLocationResponseDto.builder().deliveryServiceId(239L).regionId(120542).build()
        );
    }

    @Test
    @DisplayName("Создание клиентского возврата")
    void createClientReturn() {
        mock.expect(requestTo("http://localhost:8080/internal/orders/return"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json(resourceContent("create_client_return/request.json"), true))
            .andRespond(withStatus(HttpStatus.OK));

        tplInternalClient.createClientReturn(
            new ClientReturnCreateDto(
                1L,
                1L,
                "7432234",
                "VOZVRAT_SF_PVZ_7432234",
                LocalDate.parse("2021-03-26"),
                ClientReturnSystemCreated.LRM
            )
        );
    }

    @Test
    @DisplayName("Отмена клиентского возврата")
    void cancelClientReturn() {
        mock.expect(requestTo("http://localhost:8080/internal/orders/return/7432234/cancel"))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withStatus(HttpStatus.OK));

        tplInternalClient.cancelClientReturn("7432234");
    }

    @Test
    @DisplayName("Успешное получение сц")
    void getSortingCenterSuccess() {
        mock.expect(requestTo("http://localhost:8080/internal/ds/12345/sc"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resourceContent("get_sorting_center/response.json"))
            );

        SortingCenterInfoDto sortingCenterInfoDto = tplInternalClient.getSortingCenter(12345L);
        softly.assertThat(sortingCenterInfoDto).isNotNull();
        softly.assertThat(sortingCenterInfoDto.getId()).isEqualTo(345L);
    }

    @Test
    @DisplayName("Получение сц: сц не найден")
    void getSortingCenterNoSortingCenter() {
        mock.expect(requestTo("http://localhost:8080/internal/ds/12345/sc"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withStatus(HttpStatus.OK)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(resourceContent("get_sorting_center/empty_response.json"))
            );

        SortingCenterInfoDto sortingCenterInfoDto = tplInternalClient.getSortingCenter(12345L);
        softly.assertThat(sortingCenterInfoDto).isNotNull();
        softly.assertThat(sortingCenterInfoDto.getId()).isNull();
    }
}
