package ru.yandex.market.logistics.management.client;

import java.time.LocalDate;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.management.entity.request.partner.PartnerHolidaysDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonContent;

@DisplayName("Метод установки выходных партнёра")
class LmsClientSetPartnerHolidaysTest extends AbstractClientTest {
    private static final LocalDate DATE_FROM = LocalDate.of(2020, 7, 12);
    private static final LocalDate DATE_TO = LocalDate.of(2020, 7, 16);

    @Test
    @DisplayName("Непустой набор выходных")
    void notEmptyDays() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/holidays")
                    .queryParam("dateTo", DATE_TO)
                    .queryParam("dateFrom", DATE_FROM)
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonContent("data/controller/partnerHolidays/set_request_not_empty.json"))
            .andRespond(withStatus(HttpStatus.OK));

        client.setPartnerHolidays(
            1L,
            DATE_FROM,
            DATE_TO,
            PartnerHolidaysDto.newBuilder()
                .days(ImmutableList.of(
                    LocalDate.of(2020, 7, 13),
                    LocalDate.of(2020, 7, 14)
                ))
                .build()
        );
    }

    @Test
    @DisplayName("Пустой набор выходных")
    void emptyDays() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/1/holidays")
                    .queryParam("dateTo", DATE_TO)
                    .queryParam("dateFrom", DATE_FROM)
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonContent("data/controller/partnerHolidays/set_request_empty.json"))
            .andRespond(withStatus(HttpStatus.OK));

        client.setPartnerHolidays(
            1L,
            DATE_FROM,
            DATE_TO,
            PartnerHolidaysDto.newBuilder()
                .days(ImmutableList.of())
                .build()
        );
    }

    @Test
    @DisplayName("Множественная установка выходных")
    void setHolidaysForMultiplePartners() {
        mockServer.expect(requestTo(
                getBuilder(uri, "/externalApi/partners/holidays")
                    .queryParam("dateTo", DATE_TO)
                    .queryParam("dateFrom", DATE_FROM)
                    .toUriString()
            ))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(jsonContent("data/controller/partnerHolidays/multiple_set_request.json"))
            .andRespond(withStatus(HttpStatus.OK));

        client.setPartnersHolidays(
            DATE_FROM,
            DATE_TO,
            Map.of(
                1L,
                PartnerHolidaysDto.newBuilder()
                    .days(ImmutableList.of(
                        LocalDate.of(2020, 7, 13),
                        LocalDate.of(2020, 7, 14)
                    ))
                    .build(),
                2L,
                PartnerHolidaysDto.newBuilder()
                    .days(ImmutableList.of(
                        LocalDate.of(2020, 7, 15),
                        LocalDate.of(2020, 7, 16)
                    ))
                    .build()
            )
        );
    }
}
