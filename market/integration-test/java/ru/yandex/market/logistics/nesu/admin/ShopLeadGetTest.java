package ru.yandex.market.logistics.nesu.admin;

import java.time.LocalDate;
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

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminShopFilter;
import ru.yandex.market.logistics.nesu.utils.CommonsConstants;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.nesu.ContactDto;
import ru.yandex.market.mbi.api.client.entity.nesu.DeliveryPartnerRegistrationDto;
import ru.yandex.market.mbi.api.client.entity.nesu.PagedDeliveryPartnerRegistrationDto;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

class ShopLeadGetTest extends AbstractContextualTest {
    private static final ContactDto CONTACT = ContactDto.of(
        "Петров",
        "Петр",
        "+7 111 111 1111",
        "contact-11@yandex.ru"
    );
    private static final List<DeliveryPartnerRegistrationDto> LEADS = List.of(
        DeliveryPartnerRegistrationDto.of(
            1L,
            10001L,
            "Тестовый магазин Яндекс.Доставки 1",
            LocalDate.of(2019, 10, 1).atStartOfDay(CommonsConstants.MSK_TIME_ZONE).toInstant(),
            Collections.nCopies(5, CONTACT)
        ),
        DeliveryPartnerRegistrationDto.of(
            4L,
            10004L,
            "Тестовый магазин Яндекс.Доставки 4",
            LocalDate.of(2019, 10, 4).atStartOfDay(CommonsConstants.MSK_TIME_ZONE).toInstant(),
            List.of()
        )
    );

    @Autowired
    private MbiApiClient mbiApiClient;

    @Test
    @DisplayName("Получить лид по идентификатору")
    void getShopLead() throws Exception {
        long shopId = 1;
        AdminShopFilter filter = new AdminShopFilter().setShopId(Set.of(shopId));
        when(mbiApiClient.getNewDeliveryPartners(0, 1, Set.of(shopId), null))
            .thenReturn(createResponse(filter));

        mockMvc.perform(get("/admin/shops/leads/" + shopId))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/leads-get/1_details.json"));
    }


    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    @DisplayName("Получить лиды")
    void getShopLeads(
        @SuppressWarnings("unused") String caseName,
        AdminShopFilter filter,
        String responsePath
    ) throws Exception {
        when(
            mbiApiClient.getNewDeliveryPartners(
                0,
                20,
                Optional.ofNullable(filter.getShopId()).orElse(null),
                Optional.ofNullable(filter.getClientId()).map(Set::of).orElse(null)
            )
        )
            .thenReturn(createResponse(filter));

        mockMvc.perform(get("/admin/shops/leads").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminShopFilter(),
                "controller/admin/leads-get/response.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору магазина",
                new AdminShopFilter().setShopId(Set.of(1L)),
                "controller/admin/leads-get/1.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору клиента",
                new AdminShopFilter().setClientId(10001L),
                "controller/admin/leads-get/1.json"
            ),
            Arguments.of(
                "Фильтрация по идентификатору клиента возвращает пустой ответ",
                new AdminShopFilter().setClientId(10002L),
                "controller/admin/leads-get/empty_response.json"
            )
        );
    }

    @Nonnull
    private PagedDeliveryPartnerRegistrationDto createResponse(AdminShopFilter filter) {
        List<DeliveryPartnerRegistrationDto> leads = LEADS.stream()
            .filter(
                lead -> Optional.ofNullable(filter.getShopId())
                    .map(id -> id.contains(lead.getId()))
                    .orElse(true)
            )
            .filter(
                lead -> Optional.ofNullable(filter.getClientId())
                    .map(id -> id.equals(lead.getClientId()))
                    .orElse(true)
            )
            .sorted(Comparator.comparing(DeliveryPartnerRegistrationDto::getId).reversed())
            .collect(Collectors.toList());

        return PagedDeliveryPartnerRegistrationDto.of(leads, (long) leads.size());
    }
}
