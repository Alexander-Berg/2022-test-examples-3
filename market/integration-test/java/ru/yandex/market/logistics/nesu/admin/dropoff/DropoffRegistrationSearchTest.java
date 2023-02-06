package ru.yandex.market.logistics.nesu.admin.dropoff;

import java.time.LocalDate;
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
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminDropoffRegistrationStatus;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminDropoffRegistrationSearchFilter;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminDropoffRegistrationSearchFilter.AdminDropoffRegistrationSearchFilterBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Поиск запросов на регистрацию дропоффов")
@DatabaseSetup("/repository/dropoff/dropoff_registration_state.xml")
@ParametersAreNonnullByDefault
class DropoffRegistrationSearchTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setup() {
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

        when(lmsClient.searchPartners(any())).thenAnswer(invocation ->
            invocation.<SearchPartnerFilter>getArgument(0).getIds().stream()
                .map(
                    id -> PartnerResponse.newBuilder()
                        .id(id)
                        .name("partner" + id)
                        .build()
                )
                .collect(Collectors.toList())
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    void search(
        @SuppressWarnings("unused") String caseName,
        AdminDropoffRegistrationSearchFilterBuilder builder,
        String expectedResultJsonPath,
        Set<Long> logisticPointsIds,
        Set<Long> partnerIds
    ) throws Exception {
        mockMvc.perform(get("/admin/dropoff-registration").params(toParams(builder.build())))
            .andExpect(status().isOk())
            .andExpect(jsonContent(expectedResultJsonPath));

        verifyGetLogisticPoints(logisticPointsIds);
        verifyGetPartners(partnerIds);
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Поиск без указания фильтров",
                AdminDropoffRegistrationSearchFilter.builder(),
                "controller/admin/dropoff-registration/all.json",
                Set.of(100L, 200L, 300L),
                Set.of(101L, 201L, 301L)
            ),
            Arguments.of(
                "Поиск с фильтром по ID логистической точки",
                AdminDropoffRegistrationSearchFilter.builder().logisticPointId(100L),
                "controller/admin/dropoff-registration/1.json",
                Set.of(100L),
                Set.of(101L)
            ),
            Arguments.of(
                "Поиск с фильтром по ID партнёра дропоффа",
                AdminDropoffRegistrationSearchFilter.builder().partnerId(201L),
                "controller/admin/dropoff-registration/2.json",
                Set.of(200L),
                Set.of(201L)
            ),
            Arguments.of(
                "Поиск с фильтром по статусу запроса",
                AdminDropoffRegistrationSearchFilter.builder().status(AdminDropoffRegistrationStatus.ERROR),
                "controller/admin/dropoff-registration/3.json",
                Set.of(300L),
                Set.of(301L)
            ),
            Arguments.of(
                "Поиск с фильтром по дате создания",
                AdminDropoffRegistrationSearchFilter.builder().created(LocalDate.of(2020, 1, 1)),
                "controller/admin/dropoff-registration/1_2.json",
                Set.of(100L, 200L),
                Set.of(101L, 201L)
            )
        );
    }

    private void verifyGetLogisticPoints(Set<Long> logisticPointIds) {
        if (!logisticPointIds.isEmpty()) {
            verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(logisticPointIds).build());
        }
    }

    private void verifyGetPartners(Set<Long> partnerIds) {
        if (!partnerIds.isEmpty()) {
            verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(partnerIds).build());
        }
    }
}
