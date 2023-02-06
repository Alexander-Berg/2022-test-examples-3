package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminDropoffDisablingRequestStatus;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminDropoffDisablingSearchFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Поиск запросов на отключение дропоффов")
@DatabaseSetup("/repository/dropoff/disabling/before/disabling_dropoff_request.xml")
@ParametersAreNonnullByDefault
class DropoffDisablingSearchTest extends AbstractContextualTest {

    private static final Long REASON_ID = 1L;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        when(lmsClient.getLogisticsPoints(any())).thenAnswer(invocation ->
            invocation.<LogisticsPointFilter>getArgument(0).getIds().stream()
                .map(
                    id -> LogisticsPointResponse.newBuilder()
                        .id(id)
                        .name("point" + id)
                        .build()
                )
                .collect(Collectors.toList())
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @DisplayName("Поиск запросов на отключение дропоффов")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    void search(
        @SuppressWarnings("unused") String caseName,
        AdminDropoffDisablingSearchFilter.AdminDropoffDisablingSearchFilterBuilder builder,
        String expectedResultJsonPath,
        Set<Long> logisticPointsIds
    ) throws Exception {
        mockMvc.perform(get("/admin/dropoff-disabling").params(toParams(builder.build())))
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedResultJsonPath, false));

        verifyGetLogisticPoints(logisticPointsIds);
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск без указания фильтров",
                AdminDropoffDisablingSearchFilter.builder(),
                "controller/admin/dropoff-disabling/all.json",
                Set.of(1L, 2L, 3L, 4L)
            ),
            Arguments.of(
                "Поиск с фильтром по ID логистической точки",
                AdminDropoffDisablingSearchFilter.builder().logisticPointId(1L),
                "controller/admin/dropoff-disabling/1.json",
                Set.of(1L)
            ),
            Arguments.of(
                "Поиск с фильтром по времени начала отключения",
                AdminDropoffDisablingSearchFilter.builder()
                    .startClosingDateTimeFrom(LocalDateTime.of(2021, 11, 28, 15, 0))
                    .startClosingDateTimeTo(LocalDateTime.of(2021, 11, 28, 15, 0)),
                "controller/admin/dropoff-disabling/1_2.json",
                Set.of(1L, 2L)
            ),
            Arguments.of(
                "Поиск с фильтром по времени отключения",
                AdminDropoffDisablingSearchFilter.builder()
                    .closingDateTimeFrom(LocalDateTime.of(2021, 12, 8, 15, 0))
                    .closingDateTimeTo(LocalDateTime.of(2021, 12, 8, 15, 0)),
                "controller/admin/dropoff-disabling/1_2.json",
                Set.of(1L, 2L)
            ),
            Arguments.of(
                "Поиск с фильтром по причине отключения",
                AdminDropoffDisablingSearchFilter.builder().reasonId(REASON_ID),
                "controller/admin/dropoff-disabling/all.json",
                Set.of(1L, 2L, 3L, 4L)
            ),
            Arguments.of(
                "Поиск с фильтром по статусу запроса",
                AdminDropoffDisablingSearchFilter.builder().status(AdminDropoffDisablingRequestStatus.SCHEDULED),
                "controller/admin/dropoff-disabling/1_2.json",
                Set.of(1L, 2L)
            ),
            Arguments.of(
                "Пустой ответ",
                AdminDropoffDisablingSearchFilter.builder().status(AdminDropoffDisablingRequestStatus.ERROR),
                "controller/admin/dropoff-disabling/empty.json",
                Collections.emptySet()
            )
        );
    }

    private void verifyGetLogisticPoints(Set<Long> logisticPointIds) {
        if (!logisticPointIds.isEmpty()) {
            verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(logisticPointIds).build());
        }
    }
}
