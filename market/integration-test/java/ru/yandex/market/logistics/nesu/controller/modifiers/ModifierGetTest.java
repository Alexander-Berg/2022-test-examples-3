package ru.yandex.market.logistics.nesu.controller.modifiers;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение модификаторов опций доставки")
@DatabaseSetup({
    "/repository/shop-deliveries-availability/setup.xml",
    "/controller/modifier/modifier_setup.xml"
})
class ModifierGetTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setupLMSClient() {

        List<PlatformClientDto> activePlatformClientDtos = List.of(
            createPlatformClient(1L, "1", PartnerStatus.ACTIVE),
            createPlatformClient(3L, "3", PartnerStatus.ACTIVE),
            createPlatformClient(5L, "5", PartnerStatus.ACTIVE)
        );

        List<PlatformClientDto> inactivePlatformClientDtos = List.of(
            createPlatformClient(1L, "1", PartnerStatus.ACTIVE),
            createPlatformClient(3L, "3", PartnerStatus.INACTIVE),
            createPlatformClient(5L, "5", PartnerStatus.ACTIVE)
        );

        List<PlatformClientDto> testingPlatformClientDtos = List.of(
            createPlatformClient(1L, "1", PartnerStatus.ACTIVE),
            createPlatformClient(3L, "3", PartnerStatus.TESTING),
            createPlatformClient(5L, "5", PartnerStatus.ACTIVE)
        );

        var firstPartner = createPartner(
            42L,
            "First Partner",
            PartnerStatus.ACTIVE,
            activePlatformClientDtos
        );

        var secondPartner = createPartner(
            43L,
            "Second Partner",
            PartnerStatus.ACTIVE,
            inactivePlatformClientDtos
        );

        var thirdPartner = createPartner(
            44L,
            "Third Partner",
            PartnerStatus.INACTIVE,
            activePlatformClientDtos
        );

        var fourthPartner = createPartner(
            45L,
            "Fourth Partner",
            PartnerStatus.INACTIVE,
            testingPlatformClientDtos
        );

        var fifthPartner = createPartner(
            46L,
            "Fifth Partner",
            PartnerStatus.TESTING,
            testingPlatformClientDtos
        );

        when(lmsClient.searchPartners(any(SearchPartnerFilter.class)))
            .thenReturn(List.of(firstPartner, secondPartner, thirdPartner, fourthPartner, fifthPartner));
    }

    @Nonnull
    private PartnerResponse createPartner(
        Long id,
        String name,
        PartnerStatus status,
        List<PlatformClientDto> platformClientDto
    ) {
        return PartnerResponse.newBuilder()
            .id(id)
            .marketId(1L)
            .partnerType(PartnerType.DELIVERY)
            .name(name)
            .status(status)
            .platformClients(platformClientDto)
            .build();
    }

    @Nonnull
    private PlatformClientDto createPlatformClient(Long id, String name, PartnerStatus status) {
        return PlatformClientDto.newBuilder().id(id).name(name).status(status).build();
    }

    @Test
    @DisplayName("Получение модификаторов по идентификатору сендера")
    @DatabaseSetup("/controller/modifier/multiple_delivery_services_modifier.xml")
    void getBySender() throws Exception {
        getModifiers(1)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/modifier/get_full_modifier_response.json"));

        verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(Set.of(42L, 43L, 44L, 45L, 46L)).build());
    }

    @Test
    @DisplayName("Получение минимально заполненного модификатора")
    @DatabaseSetup("/controller/modifier/updated_modifier.xml")
    void getMinimalModifier() throws Exception {
        getModifiers(1)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/modifier/get_minimal_modifier_response.json"));
    }

    @Test
    @DisplayName("Получение пустого списка модификаторов")
    void getEmptyList() throws Exception {
        getModifiers(1)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/modifier/empty_modifier_response.json"));
    }

    @Test
    @DisplayName("Не найден сендер при получении")
    void getBySenderNotFound() throws Exception {
        getModifiers(2)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [2]"));
    }

    @Nonnull
    private ResultActions getModifiers(long senderId) throws Exception {
        return mockMvc.perform(
            get("/back-office/settings/modifiers")
                .param("shopId", "1")
                .param("senderId", String.valueOf(senderId))
                .param("userId", "1")
        );
    }
}
