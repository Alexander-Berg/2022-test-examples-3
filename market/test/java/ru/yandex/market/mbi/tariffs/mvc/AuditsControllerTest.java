package ru.yandex.market.mbi.tariffs.mvc;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.TestUtils;
import ru.yandex.market.mbi.tariffs.matcher.AuditDTOMatcher;
import ru.yandex.market.mbi.tariffs.model.AuditDTO;
import ru.yandex.market.mbi.tariffs.model.AuditFindFilter;
import ru.yandex.market.mbi.tariffs.model.PagerResponseInfo;
import ru.yandex.market.mbi.tariffs.service.audit.AuditService;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.post;
import static ru.yandex.market.mbi.tariffs.TestUtils.parsePagerResponse;
import static ru.yandex.market.mbi.tariffs.matcher.ErrorInfoMatcher.hasCode;
import static ru.yandex.market.mbi.tariffs.matcher.ErrorInfoMatcher.hasMessage;

/**
 * Тесты для {@link ru.yandex.market.mbi.tariffs.mvc.controller.AuditsController}
 */
@ParametersAreNonnullByDefault
public class AuditsControllerTest extends FunctionalTest {

    private static final OffsetDateTime INITIAL_OFFSET_DATETIME = OffsetDateTime.of(
            LocalDateTime.of(2020, 11, 16, 10, 0, 0), ZoneOffset.UTC
    );

    @ParameterizedTest(name = "[{index}] {displayName}")
    @MethodSource("getAuditsTestData")
    @DbUnitDataSet(
            before = "audit/getAuditsWithFilter.before.csv"
    )
    @DisplayName("Тест на получение аудитов по фильтру")
    void testGetAuditsWithFilter(
            AuditFindFilter auditFindFilter,
            long expectedTotalCount,
            List<AuditDTO> expectedAudits
    ) {
        HttpEntity<String> bodyEntity = createHttpRequestEntity(auditFindFilter);
        ResponseEntity<String> response = post(baseUrl() + "/audits?page=0&pageSize=100&sortBy=time&sortType=asc", bodyEntity);
        assertOk(response);

        PagerResponseInfo pagerResponse = parsePagerResponse(response.getBody(), AuditDTO.class);
        assertEquals(expectedTotalCount, (long) pagerResponse.getTotalCount());
        if (expectedAudits.isEmpty()) {
            assertThat(pagerResponse.getItems(), is(empty()));
        } else {
            assertThat(
                    pagerResponse.getItems().stream()
                            .map(obj -> (AuditDTO) obj)
                            .collect(Collectors.toList()),
                    contains(
                            expectedAudits.stream()
                                    .map(AuditDTOMatcher::hasAllFields)
                                    .collect(Collectors.toList())
                    )
            );
        }

    }

    private static Stream<Arguments> getAuditsTestData() {
        return Stream.of(
                Arguments.of(
                        new AuditFindFilter().login("test"),
                        0L,
                        List.of()
                ),
                Arguments.of(
                        new AuditFindFilter().login("test0"),
                        3L,
                        List.of(generate("test0", "msg1", 0), generate("test0", "msg2", 1), generate("test0", "msg1", 4))
                ),
                Arguments.of(
                        new AuditFindFilter().startTime(INITIAL_OFFSET_DATETIME.plusHours(2)),
                        4L,
                        List.of(generate("test1", "msg3", 2), generate("test1", "msg4", 3), generate("test3", "msg5", 3), generate("test0", "msg1", 4))
                ),
                Arguments.of(
                        new AuditFindFilter().startTime(INITIAL_OFFSET_DATETIME.plusHours(3)).endTime(INITIAL_OFFSET_DATETIME.plusHours(4)),
                        2L,
                        List.of(generate("test1", "msg4", 3), generate("test3", "msg5", 3))
                ),
                Arguments.of(
                        new AuditFindFilter().startTime(INITIAL_OFFSET_DATETIME.plusHours(3)).endTime(INITIAL_OFFSET_DATETIME.plusHours(4)).login("test1"),
                        1L,
                        List.of(generate("test1", "msg4", 3))
                ),
                Arguments.of(
                        new AuditFindFilter().startTime(INITIAL_OFFSET_DATETIME.plusHours(3)).endTime(INITIAL_OFFSET_DATETIME.plusHours(4)).login("test0"),
                        0L,
                        List.of()
                ),
                Arguments.of(
                        new AuditFindFilter().payloadFilterFieldKey(AuditService.PayloadKey.TARIFF_ID).payloadFilterFieldValue("10"),
                        2L,
                        List.of(generate("test0", "msg1", 0), generate("test1", "msg3", 2))
                ),
                Arguments.of(
                        new AuditFindFilter().payloadFilterFieldKey(AuditService.PayloadKey.DRAFT_ID).payloadFilterFieldValue("20"),
                        1L,
                        List.of(generate("test1", "msg3", 2))
                ),
                Arguments.of(
                        new AuditFindFilter().payloadFilterFieldKey(AuditService.PayloadKey.DRAFT_ID).payloadFilterFieldValue("30"),
                        0L,
                        List.of()
                )
        );
    }

    private static AuditDTO generate(String login, String message, int plusHour) {
        return new AuditDTO()
                .login(login)
                .message(message)
                .time(INITIAL_OFFSET_DATETIME.plusHours(plusHour));
    }

    @ParameterizedTest(name = "[{index}] sortBy = {0} and isSuccess = {1}")
    @MethodSource("sortByTestData")
    @DbUnitDataSet(
            before = "audit/getAuditsWithFilter.before.csv",
            after = "audit/getAuditsWithFilter.before.csv" // явная проверка на то, что ничего не поменялось
    )
    @DisplayName("Тест на проверку получения аудитов по пустому фильтру (с возможной проверкой на валидность)")
    void testWithSortBy(
            @Nullable String sortBy,
            boolean isSuccess
    ) {
        String url = baseUrl() + "/audits";
        if (sortBy != null) {
            url += "?sortBy=" + sortBy;
        }

        String finalUrl = url;
        Supplier<ResponseEntity<String>> result = () -> post(finalUrl, createHttpRequestEntity("{}"));
        if (isSuccess) {
            ResponseEntity<String> response = result.get();
            assertOk(response);
        } else {
            HttpClientErrorException exception = Assertions.assertThrows(
                    HttpClientErrorException.class,
                    result::get
            );
            List<ErrorInfo> errors = TestUtils.getErrors(exception.getResponseBodyAsString());
            assertThat(errors, hasSize(1));
            assertThat(errors.get(0), allOf(
                    hasCode("BAD_PARAM"),
                    hasMessage("Unknown sortBy. Available are : [login, time]")
            ));
        }
    }

    private static Stream<Arguments> sortByTestData() {
        return Stream.of(
                Arguments.of("'--drop table mbi_tariffs.audit", false),
                Arguments.of("asd", false),
                Arguments.of(null, true),
                Arguments.of("login", true),
                Arguments.of("time", true)
        );
    }
}
