package ru.yandex.market.logistics.management.controller.partner;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@DisplayName("Метод установки выходных дней партнёра")
@DatabaseSetup("/data/controller/partner/holidays/data/prepare_data.xml")
class PartnerControllerSetHolidaysTest extends AbstractContextualTest {

    @Test
    @DisplayName("Даты вне интервала запроса не учитываются")
    @ExpectedDatabase(
        value = "/data/controller/partner/holidays/data/result_data_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outOfInterval() throws Exception {
        setPartnersHolidays(
            "2020-07-14",
            "2020-07-19",
            "data/controller/partner/holidays/request/set_holidays_not_empty.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Добавление выходных дней партнёру без календаря")
    @ExpectedDatabase(
        value = "/data/controller/partner/holidays/data/result_data_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCalendarCreation() throws Exception {
        setPartnersHolidays(
            "2020-07-13",
            "2020-07-16",
            "data/controller/partner/holidays/request/set_holidays_no_calendar.json"
        )
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Множественное добавление выходных дней")
    @ExpectedDatabase(
        value = "/data/controller/partner/holidays/data/result_data_3.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleSetPartnerCalendars() throws Exception {
        setPartnersHolidays(
            "2020-07-12",
            "2020-07-19",
            "data/controller/partner/holidays/request/set_holidays_batch.json"
        )
            .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("queryValidationSource")
    @DisplayName("Валидация параметров запроса")
    void queryValidation(
        String dateFrom,
        String dateTo,
        String errorReason
    ) throws Exception {
        setPartnersHolidays(
            dateFrom,
            dateTo,
            "data/controller/partner/holidays/request/set_holidays_not_empty.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(errorReason))
            .andExpect(content().string(""));
    }

    private static Stream<Arguments> queryValidationSource() {
        return Stream.of(
            Arguments.of(
                null,
                null,
                "Required LocalDate parameter 'dateFrom' is not present"
            ),
            Arguments.of(
                null,
                "2020-07-16",
                "Required LocalDate parameter 'dateFrom' is not present"
            ),
            Arguments.of(
                "2020-07-12",
                null,
                "Required LocalDate parameter 'dateTo' is not present"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("bodyValidationSource")
    @DisplayName("Валидация тела запроса")
    void bodyValidation(
        String requestBodyPath,
        String field,
        String errorMessage
    ) throws Exception {
        setPartnersHolidays("2020-07-12", "2020-07-16", requestBodyPath)
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(String.join(" ", field, errorMessage)));
    }

    private static Stream<Arguments> bodyValidationSource() {
        return Stream.of(
            Arguments.of(
                "data/controller/partner/holidays/request/set_holidays_null_days.json",
                "days",
                "must not be null"
            ),
            Arguments.of(
                "data/controller/partner/holidays/request/set_holidays_null_date.json",
                "days.<list element>",
                "must not be null"
            )
        );
    }

    @Test
    @DisplayName("Несуществующий партнёр")
    void notExistingPartner() throws Exception {
        setPartnersHolidays(
            "2020-07-14",
            "2020-07-19",
            "data/controller/partner/holidays/request/set_holidays_partner_not_found.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=3"))
            .andExpect(content().string(""));
    }

    @Nonnull
    private ResultActions setPartnersHolidays(
        String dateFrom,
        String dateTo,
        String requestBodyPath
    ) throws Exception {
        return mockMvc.perform(
            put("/externalApi/partners/holidays")
                .param("dateFrom", dateFrom)
                .param("dateTo", dateTo)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestBodyPath))
        );
    }
}
