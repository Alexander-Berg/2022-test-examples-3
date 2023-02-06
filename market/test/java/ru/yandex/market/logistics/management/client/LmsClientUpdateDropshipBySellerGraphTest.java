package ru.yandex.market.logistics.management.client;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.dropshipBySeller.UpdateDeliveryServiceCalendarRequest;
import ru.yandex.market.logistics.management.entity.request.dropshipBySeller.UpdateDropshipBySellerGraphRequest;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientUpdateDropshipBySellerGraphTest extends AbstractClientTest {
    @Test
    @DisplayName("Обновить логистический граф DBS-партнера")
    void updateGraph() {
        mockServer.expect(requestTo(startsWith(
                getBuilder(uri, "/externalApi/dropship-by-seller/49850/graph").toUriString()
            )))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/dropshipBySellerGraph/update.json")))
            .andRespond(withStatus(OK));

        client.updateDropshipBySellerGraph(
            49850,
            UpdateDropshipBySellerGraphRequest.newBuilder()
                .updateDeliveryServiceCalendar(
                    UpdateDeliveryServiceCalendarRequest.newBuilder()
                        .dateFrom(LocalDate.of(2021, 1, 1))
                        .dateTo(LocalDate.of(2021, 1, 31))
                        .holidayDates(Set.of(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2)))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Обновить множество логистических графов DBS-партнеров")
    void updateMultipleGraphs() {
        mockServer.expect(requestTo(startsWith(
                getBuilder(uri, "/externalApi/dropship-by-seller/graphs").toUriString()
            )))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().json(jsonResource("data/controller/dropshipBySellerGraph/multipleUpdate.json")))
            .andRespond(withStatus(OK));

        client.updateDropshipBySellerGraphs(
            List.of(
                UpdateDropshipBySellerGraphRequest.newBuilder()
                    .partnerId(49850L)
                    .updateDeliveryServiceCalendar(
                        UpdateDeliveryServiceCalendarRequest.newBuilder()
                            .dateFrom(LocalDate.of(2021, 1, 1))
                            .dateTo(LocalDate.of(2021, 1, 31))
                            .holidayDates(Set.of(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 2)))
                            .build()
                    )
                    .build(),
                UpdateDropshipBySellerGraphRequest.newBuilder()
                    .partnerId(49851L)
                    .updateDeliveryServiceCalendar(
                        UpdateDeliveryServiceCalendarRequest.newBuilder()
                            .dateFrom(LocalDate.of(2021, 1, 1))
                            .dateTo(LocalDate.of(2021, 1, 31))
                            .holidayDates(Set.of(LocalDate.of(2021, 1, 3), LocalDate.of(2021, 1, 4)))
                            .build()
                    )
                    .build()
            )
        );
    }
}
