package ru.yandex.market.logistics.tarifficator.admin.tag;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.dto.TagRelationDto;
import ru.yandex.market.logistics.tarifficator.admin.dto.TagRelationDtoListWrapper;
import ru.yandex.market.logistics.tarifficator.service.partner.PartnerSubtype;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Активация/деактивация программы для тарифа через админку")
@DatabaseSetup("/controller/tags/db/before/tags.xml")
class TagsActivationTest extends AbstractContextualTest {

    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Активировать одну программу")
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/single_tag_activation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateSingleTag() throws Exception {
        activateTags(List.of(new TagRelationDto().setName("BERU").setTariffId(3L)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Активировать одну программу вдобавок к существующей DAAS")
    @ExpectedDatabase(
        value = "/controller/tags/db/before/tags.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateSingleTagAddForExistingDaas() throws Exception {
        activateTags(List.of(new TagRelationDto().setName("BERU").setTariffId(2L)))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("DAAS tag and the others must be mutually exclusive"));
    }

    @Test
    @DisplayName("Активировать программу DAAS вдобавок к существующим")
    @ExpectedDatabase(
        value = "/controller/tags/db/before/tags.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateSingleTagDaasAddForExistingOnes() throws Exception {
        activateTags(List.of(new TagRelationDto().setName("DAAS").setTariffId(3L)))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("DAAS tag and the others must be mutually exclusive"));
    }

    @Test
    @DisplayName("Активировать только программу DAAS")
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/daas_tag_activation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateOnlyDaasTag() throws Exception {
        try (var ignored = mockLmsSearchPartners(Set.of())) {
            activateTags(List.of(new TagRelationDto().setName("DAAS").setTariffId(4L)))
                .andExpect(status().isOk())
                .andExpect(noContent());
        }
    }

    @Test
    @DisplayName("Активировать несколько программ")
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/multiple_tags_activation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateMultipleTags() throws Exception {
        activateTags(List.of(
            new TagRelationDto().setName("BERU").setTariffId(3L),
            new TagRelationDto().setName("WHITE").setTariffId(3L)
        ))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Деактивировать одну программу")
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/single_tag_deactivation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deactivateSingleTag() throws Exception {
        deactivateTags(List.of(new TagRelationDto().setName("BERU").setTariffId(1L)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Деактивировать несколько программ")
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/multiple_tags_deactivation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deactivateMultipleTags() throws Exception {
        try (var ignored = mockLmsSearchPartners(Set.of())) {
            deactivateTags(List.of(
                new TagRelationDto().setName("BERU").setTariffId(1L),
                new TagRelationDto().setName("DAAS").setTariffId(1L)
            ))
                .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Активировать несуществующую программу")
    void activateTagNotFound() throws Exception {
        activateTags(List.of(new TagRelationDto().setName("SOME_PROGRAM").setTariffId(1L)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TAG] with ids [[SOME_PROGRAM]]"));
    }

    @Test
    @DisplayName("Активировать программу для несуществующего тарифа")
    void activateTagTariffNotFound() throws Exception {
        activateTags(List.of(new TagRelationDto().setName("SOME_PROGRAM").setTariffId(0L)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [[0]]"));
    }

    @Test
    @DisplayName("Активировать программы для разных тарифов")
    void activateTagForDifferentTariffs() throws Exception {
        activateTags(List.of(
            new TagRelationDto().setName("BERU").setTariffId(1L),
            new TagRelationDto().setName("WHITE").setTariffId(2L)
        ))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Multiple tariffs are unsupported"));
    }

    @Test
    @DisplayName("Деактивировать несуществующую программу")
    void deactivateTagNotFound() throws Exception {
        deactivateTags(List.of(new TagRelationDto().setName("SOME_PROGRAM").setTariffId(1L)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TAG] with ids [[SOME_PROGRAM]]"));
    }

    @Test
    @DisplayName("Деактивировать программу для несуществующего тарифа")
    void deactivateTagTariffNotFound() throws Exception {
        deactivateTags(List.of(new TagRelationDto().setName("SOME_PROGRAM").setTariffId(0L)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [[0]]"));
    }

    @Test
    @DisplayName("Деактивировать программы для разных тарифов")
    void deactivateTagForDifferentTariffs() throws Exception {
        deactivateTags(List.of(
            new TagRelationDto().setName("BERU").setTariffId(1L),
            new TagRelationDto().setName("WHITE").setTariffId(2L)
        ))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Multiple tariffs are unsupported"));
    }

    @Test
    @DisplayName("Активировать программу DAAS для тарифа типа MARKET_COURIER")
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/market_courier_tariff_daas_tag_activation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateDaasTagForMarketCourierTariff() throws Exception {
        try (var ignored = mockLmsSearchPartners(Set.of(100500L, 100501L))) {
            activateTags(List.of(new TagRelationDto().setName("DAAS").setTariffId(5L)))
                .andExpect(status().isOk())
                .andExpect(noContent());
        }
    }

    @Test
    @DisplayName(
        "Активировать программу DAAS для тарифа типа MARKET_COURIER, " +
            "отсутствуют партнеры подтипа GO_PARTNER_LOCKER"
    )
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/market_courier_tariff_daas_tag_activation_no_partners.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void activateDaasTagForMarketCourierTariffNoGoPartnerLockerPartners() throws Exception {
        try (var ignored = mockLmsSearchPartners(Set.of())) {
            activateTags(List.of(new TagRelationDto().setName("DAAS").setTariffId(5L)))
                .andExpect(status().isOk())
                .andExpect(noContent());
        }
    }

    @Test
    @DisplayName("Деактивировать программу DAAS для тарифа типа MARKET_COURIER")
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/market_courier_tariff_daas_tag_deactivation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deactivateDaasTagForMarketCourierTariff() throws Exception {
        try (var ignored = mockLmsSearchPartners(Set.of(100500L, 100501L))) {
            deactivateTags(List.of(new TagRelationDto().setName("DAAS").setTariffId(6L)))
                .andExpect(status().isOk())
                .andExpect(noContent());
        }
    }

    @Test
    @DisplayName(
        "Деактивировать программу DAAS для тарифа типа MARKET_COURIER, " +
            "отсутствуют партнеры подтипа GO_PARTNER_LOCKER"
    )
    @ExpectedDatabase(
        value = "/controller/admin/tags/activation/after/market_courier_tariff_daas_tag_deactivation_no_partners.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deactivateDaasTagForMarketCourierTariffNoGoPartnerLockerPartners() throws Exception {
        try (var ignored = mockLmsSearchPartners(Set.of())) {
            deactivateTags(List.of(new TagRelationDto().setName("DAAS").setTariffId(6L)))
                .andExpect(status().isOk())
                .andExpect(noContent());
        }
    }

    @Nonnull
    private ResultActions activateTags(List<TagRelationDto> tagRelationDtos) throws Exception {
        return mockMvc.perform(
            TestUtils.request(
                HttpMethod.POST,
                "/admin/tariffs/tags/activate",
                new TagRelationDtoListWrapper().setTagRelationDtos(tagRelationDtos))
        );
    }

    @Nonnull
    private ResultActions deactivateTags(List<TagRelationDto> tagRelationDtos) throws Exception {
        return mockMvc.perform(
            TestUtils.request(
                HttpMethod.POST,
                "/admin/tariffs/tags/deactivate",
                new TagRelationDtoListWrapper().setTagRelationDtos(tagRelationDtos))
        );
    }

    @Nonnull
    private AutoCloseable mockLmsSearchPartners(Set<Long> partnerIds) {
        SearchPartnerFilter filter = SearchPartnerFilter.builder()
            .setPartnerSubTypeIds(Set.of(PartnerSubtype.GO_PARTNER_LOCKER.getId()))
            .build();
        when(lmsClient.searchPartners(filter))
            .thenReturn(
                partnerIds.stream()
                    .map(id -> PartnerResponse.newBuilder().id(id).build())
                    .collect(Collectors.toList())
            );
        return () -> verify(lmsClient).searchPartners(filter);
    }
}
