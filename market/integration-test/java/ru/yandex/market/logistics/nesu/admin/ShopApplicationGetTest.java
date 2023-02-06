package ru.yandex.market.logistics.nesu.admin;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminShopFilter;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.nesu.ContactDto;
import ru.yandex.market.mbi.api.client.entity.nesu.DeliveryPartnerApplicationDto;
import ru.yandex.market.mbi.api.client.entity.nesu.PagedDeliveryPartnerApplicationDto;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

class ShopApplicationGetTest extends AbstractContextualTest {
    private static final ContactDto CONTACT = ContactDto.of(
        "Александров",
        "Александр",
        "+7 222 222 2222",
        "contact-12@yandex.ru"
    );
    private static final List<DeliveryPartnerApplicationDto> APPLICATIONS = List.of(
        DeliveryPartnerApplicationDto.of(
            2L,
            10002L,
            Instant.parse("2020-01-01T12:00:00Z"),
            "Тестовый магазин Яндекс.Доставки 2",
            "ИП Александров Александр",
            OrganizationType.IP,
            Collections.nCopies(5, CONTACT),
            1L,
            PartnerApplicationStatus.INIT,
            null
        ),
        DeliveryPartnerApplicationDto.of(
            5L,
            10005L,
            Instant.parse("2020-01-01T12:00:00Z"),
            "Тестовый магазин Яндекс.Доставки 5",
            "ООО Тестовый магазин Яндекс.Доставки",
            OrganizationType.OOO,
            List.of(),
            2L,
            PartnerApplicationStatus.NEED_INFO,
            "Требуется фотография котика"
        )
    );

    @Autowired
    private MbiApiClient mbiApiClient;

    @Test
    @DisplayName("Получить заявку по идентификатору")
    void getShopApplication() throws Exception {
        long applicationId = 1;
        AdminShopFilter filter = new AdminShopFilter().setApplicationId(applicationId);

        when(mbiApiClient.getDeliveryPartnersApplications(0, 1, null, null, Set.of(applicationId)))
            .thenReturn(createResponse(filter));

        mockMvc.perform(get("/admin/shops/applications/" + applicationId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/applications-get/2_details.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    @DisplayName("Получить заявки на подключение к Яндекс.Доставке")
    void getShopApplications(
        @SuppressWarnings("unused") String caseName,
        AdminShopFilter filter,
        String responsePath
    ) throws Exception {
        when(
            mbiApiClient.getDeliveryPartnersApplications(
                0,
                20,
                Optional.ofNullable(filter.getShopId()).orElse(null),
                Optional.ofNullable(filter.getClientId()).map(Set::of).orElse(null),
                Optional.ofNullable(filter.getApplicationId()).map(Set::of).orElse(null)
            )
        )
            .thenReturn(createResponse(filter));

        mockMvc.perform(get("/admin/shops/applications").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminShopFilter(),
                "controller/admin/applications-get/response.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору магазина",
                new AdminShopFilter().setShopId(Set.of(2L)),
                "controller/admin/applications-get/2.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору клиента",
                new AdminShopFilter().setClientId(10002L),
                "controller/admin/applications-get/2.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору заявки",
                new AdminShopFilter().setApplicationId(1L),
                "controller/admin/applications-get/2.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору заявки, которой не существует, получаем пустой ответ",
                new AdminShopFilter().setApplicationId(3L),
                "controller/admin/applications-get/empty_response.json"
            )
        );
    }

    @Nonnull
    private PagedDeliveryPartnerApplicationDto createResponse(AdminShopFilter filter) {
        List<DeliveryPartnerApplicationDto> applications = APPLICATIONS.stream()
            .filter(
                application -> Optional.ofNullable(filter.getShopId())
                    .map(ids -> ids.contains(application.getId()))
                    .orElse(true)
            )
            .filter(
                application -> Optional.ofNullable(filter.getApplicationId())
                    .map(id -> id.equals(application.getRequestId()))
                    .orElse(true)
            )
            .filter(
                application -> Optional.ofNullable(filter.getClientId())
                    .map(id -> id.equals(application.getClientId()))
                    .orElse(true)
            )
            .sorted(Comparator.comparing(DeliveryPartnerApplicationDto::getId).reversed())
            .collect(Collectors.toList());

        return PagedDeliveryPartnerApplicationDto.of(applications, (long) applications.size());
    }
}
