package ru.yandex.market.logistics.management.client;

import java.time.LocalDate;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ResponseActions;

import ru.yandex.market.logistics.management.entity.request.capacity.PartnerDaysOffFilter;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerDaysOffFilterPartner;
import ru.yandex.market.logistics.management.entity.response.capacity.PartnerCapacityDayOffSearchResult;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Поиск дэй-оффов партнеров")
public class LmsClientSearchDaysOffTest extends AbstractClientTest {

    @Test
    @DisplayName("Сериализация фильтра")
    void searchFilterSerialization() {
        defaultSearch()
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/capacity/days_off_search_result.json"))
            );

        client.searchDaysOff(defaultFilter());
    }

    @Test
    @DisplayName("Пустой результат")
    void searchEmptyResult() {
        defaultSearch()
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/empty_entities.json"))
            );

        softly.assertThat(client.searchDaysOff(defaultFilter())).isEmpty();
    }

    @Test
    @DisplayName("Десериализация результата")
    void searchResultDeserialization() {
        defaultSearch()
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/capacity/days_off_search_result.json"))
            );

        softly.assertThat(client.searchDaysOff(defaultFilter()))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(1L)
                    .deliveryType(DeliveryType.COURIER)
                    .days(ImmutableList.of(
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 1, 2),
                        LocalDate.of(2020, 1, 3)
                    ))
                    .build(),
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(2L)
                    .deliveryType(DeliveryType.PICKUP)
                    .days(ImmutableList.of(
                        LocalDate.of(2020, 1, 10),
                        LocalDate.of(2020, 1, 11),
                        LocalDate.of(2020, 1, 12)
                    ))
                    .build(),
                PartnerCapacityDayOffSearchResult.builder()
                    .partnerId(3L)
                    .deliveryType(null)
                    .days(ImmutableList.of(
                        LocalDate.of(2020, 1, 31),
                        LocalDate.of(2020, 2, 1),
                        LocalDate.of(2020, 2, 2)
                    ))
                    .build()
            );
    }

    @Nonnull
    private ResponseActions defaultSearch() {
        return mockServer.expect(requestTo(uri + "/externalApi/partner-capacities/days-off/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonContent("data/controller/capacity/days_off_search_filter.json"));
    }

    @Nonnull
    private PartnerDaysOffFilter defaultFilter() {
        return PartnerDaysOffFilter.builder()
            .platformClientId(3L)
            .locationFrom(213)
            .locationsTo(Set.of(2))
            .dateFrom(LocalDate.of(2020, 1, 1))
            .dateTo(LocalDate.of(2020, 2, 2))
            .partners(ImmutableList.of(
                PartnerDaysOffFilterPartner.builder()
                    .partnerId(1L)
                    .deliveryType(DeliveryType.COURIER)
                    .build(),
                PartnerDaysOffFilterPartner.builder()
                    .partnerId(2L)
                    .deliveryType(DeliveryType.PICKUP)
                    .build(),
                PartnerDaysOffFilterPartner.builder()
                    .partnerId(3L)
                    .deliveryType(DeliveryType.POST)
                    .build()
            ))
            .build();
    }

}
