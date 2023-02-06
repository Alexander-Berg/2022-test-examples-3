package ru.yandex.market.logistics.tarifficator.admin.tariff;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.DeliveryMethod;
import ru.yandex.market.logistics.tarifficator.model.enums.PlatformClient;
import ru.yandex.market.logistics.tarifficator.model.enums.TariffType;
import ru.yandex.market.logistics.tarifficator.model.filter.AdminTariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск тарифов через админку")
@DatabaseSetup("/controller/tariffs/db/search_prepare.xml")
class SearchTariffsTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск тарифов")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminTariffSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockLmsClient();

        mockMvc.perform(get("/admin/tariffs").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
                Triple.of(
                    "Пустой фильтр",
                    new AdminTariffSearchFilter(),
                    "controller/admin/tariffs/response/all.json"
                ),
                Triple.of(
                    "Фильтр по идентификатору",
                    new AdminTariffSearchFilter().setTariffId(1L),
                    "controller/admin/tariffs/response/id_1.json"
                ),
                Triple.of(
                    "Фильтр по идентификатору партнёра",
                    new AdminTariffSearchFilter().setPartner(1L),
                    "controller/admin/tariffs/response/id_1_4.json"
                ),
                Triple.of(
                    "Фильтр по способу доставки",
                    new AdminTariffSearchFilter().setDeliveryMethod(DeliveryMethod.POST),
                    "controller/admin/tariffs/response/id_2_3.json"
                ),
                Triple.of(
                    "Фильтр по типу тарифа",
                    new AdminTariffSearchFilter().setType(TariffType.GENERAL),
                    "controller/admin/tariffs/response/id_1_2.json"
                ),
                Triple.of(
                    "Фильтр включенных тарифов",
                    new AdminTariffSearchFilter().setEnabled(true),
                    "controller/admin/tariffs/response/id_1_2_3.json"
                ),
                Triple.of(
                    "Фильтр по названию тарифа",
                    new AdminTariffSearchFilter().setName("Первый"),
                    "controller/admin/tariffs/response/id_1.json"
                ),
                Triple.of(
                    "Фильтр по поисковому запросу (название тарифа)",
                    new AdminTariffSearchFilter().setSearchQuery("первый"),
                    "controller/admin/tariffs/response/id_1.json"
                ),
                Triple.of(
                    "Фильтр по поисковому запросу (идентификатор тарифа)",
                    new AdminTariffSearchFilter().setSearchQuery("1"),
                    "controller/admin/tariffs/response/id_1.json"
                ),
                Triple.of(
                    "Фильтр по нескольким идентификаторам",
                    new AdminTariffSearchFilter().setTariffIds(Set.of(1L, 2L, 3L)),
                    "controller/admin/tariffs/response/id_1_2_3.json"
                ),
                Triple.of(
                    "Фильтр по ID группы",
                    new AdminTariffSearchFilter().setTariffGroupId(18L),
                    "controller/admin/tariffs/response/id_1.json"
                ),
                Triple.of(
                    "Фильтр по всем параметрам",
                    new AdminTariffSearchFilter()
                        .setPartner(1L)
                        .setTariffId(1L)
                        .setDeliveryMethod(DeliveryMethod.PICKUP)
                        .setName("Первый")
                        .setType(TariffType.GENERAL)
                        .setTariffGroupId(18L),
                    "controller/admin/tariffs/response/id_1.json"
                )
            )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchWithPlatformArgument")
    @DisplayName("Поиск тарифов указанной платформы")
    void searchWithPlatform(
        @SuppressWarnings("unused") String displayName,
        AdminTariffSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockLmsClient();

        mockMvc.perform(get("/admin/tariffs").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    @Nonnull
    private static Stream<Arguments> searchWithPlatformArgument() {
        return Stream.of(
            Arguments.of(
                "Фильтр с платформой",
                new AdminTariffSearchFilter()
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1_3.json"
            ),
            Arguments.of(
                "Фильтр с несколькими платформами",
                new AdminTariffSearchFilter()
                    .setPlatformClients(Set.of(
                        PlatformClient.YANDEX_DELIVERY,
                        PlatformClient.BERU,
                        PlatformClient.B2B
                    )),
                "controller/admin/tariffs/response/id_1_3_4.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору DAAS тарифа",
                new AdminTariffSearchFilter()
                    .setTariffId(1L)
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору партнёра",
                new AdminTariffSearchFilter()
                    .setPartner(1L)
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по способу доставки",
                new AdminTariffSearchFilter()
                    .setDeliveryMethod(DeliveryMethod.POST)
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_3.json"
            ),
            Arguments.of(
                "Фильтр по типу тарифа",
                new AdminTariffSearchFilter()
                    .setType(TariffType.GENERAL)
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр включенных тарифов",
                new AdminTariffSearchFilter()
                    .setEnabled(true)
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1_3.json"
            ),
            Arguments.of(
                "Фильтр по названию тарифа",
                new AdminTariffSearchFilter()
                    .setName("Первый")
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по поисковому запросу (название тарифа)",
                new AdminTariffSearchFilter()
                    .setSearchQuery("первый")
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по поисковому запросу (идентификатор тарифа)",
                new AdminTariffSearchFilter()
                    .setSearchQuery("1")
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по нескольким идентификаторам",
                new AdminTariffSearchFilter()
                    .setTariffIds(Set.of(1L, 2L, 3L))
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1_3.json"
            ),
            Arguments.of(
                "Фильтр по всем параметрам",
                new AdminTariffSearchFilter()
                    .setSearchQuery("первый")
                    .setPartner(1L)
                    .setTariffId(1L)
                    .setDeliveryMethod(DeliveryMethod.PICKUP)
                    .setName("Первый")
                    .setType(TariffType.GENERAL)
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY)),
                "controller/admin/tariffs/response/id_1.json"
            )
        );
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchWithoutPlatformArgument")
    @DisplayName("Поиск тарифов без платформы")
    void searchWithoutPlatform(
        @SuppressWarnings("unused") String displayName,
        AdminTariffSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockLmsClient();

        mockMvc.perform(get("/admin/tariffs").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
        verify(lmsClient).searchPartners(any(SearchPartnerFilter.class));
    }

    @Nonnull
    private static Stream<Arguments> searchWithoutPlatformArgument() {
        return Stream.of(
            Arguments.of(
                "Фильтр без платформы",
                new AdminTariffSearchFilter()
                    .setPlatformClients(Set.of()),
                "controller/admin/tariffs/response/id_2.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору 2 тарифа",
                new AdminTariffSearchFilter()
                    .setTariffId(2L)
                    .setPlatformClients(Set.of()),
                "controller/admin/tariffs/response/id_2.json"
            ),
            Arguments.of(
                "Фильтр по поисковому запросу (название тарифа)",
                new AdminTariffSearchFilter()
                    .setSearchQuery("второй")
                    .setPlatformClients(Set.of()),
                "controller/admin/tariffs/response/id_2.json"
            ),
            Arguments.of(
                "Фильтр по поисковому запросу (идентификатор тарифа)",
                new AdminTariffSearchFilter()
                    .setSearchQuery("2")
                    .setPlatformClients(Set.of()),
                "controller/admin/tariffs/response/id_2.json"
            ),
            Arguments.of(
                "Фильтр по нескольким идентификаторам",
                new AdminTariffSearchFilter()
                    .setTariffIds(Set.of(1L, 2L, 3L))
                    .setPlatformClients(Set.of()),
                "controller/admin/tariffs/response/id_2.json"
            )
        );
    }

    @Test
    @DisplayName("Партнёры, указанные в тарифах, не найдены")
    void partnersNotFoundTest() throws Exception {
        mockMvc.perform(get("/admin/tariffs").params(TestUtils.toParams(new AdminTariffSearchFilter())))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/tariffs/response/partners_are_not_found.json"));

        verify(lmsClient).searchPartners(
            SearchPartnerFilter.builder()
                .setIds(ImmutableSet.of(1L, 2L, 3L))
                .build()
        );
    }

    @Test
    @DisplayName("Фильтр по идентификатору не DAAS тарифа")
    void searchNondaasPartners() throws Exception {
        mockMvc.perform(get("/admin/tariffs").params(TestUtils.toParams(
                new AdminTariffSearchFilter()
                    .setTariffId(2L)
                    .setPlatformClients(Set.of(PlatformClient.YANDEX_DELIVERY))
            )))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/tariffs/response/empty.json"));
    }

    private void mockLmsClient() {
        when(lmsClient.searchPartners(any(SearchPartnerFilter.class))).thenAnswer(
            invocation -> (invocation.getArgument(0, SearchPartnerFilter.class)).getIds().stream()
                .map(
                    id -> PartnerResponse.newBuilder()
                        .id(id)
                        .readableName("partner_" + id)
                        .name("partner_" + id)
                        .build()
                )
                .collect(Collectors.toList())
        );
    }
}
