package ru.yandex.market.logistics.management.controller.partner;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.EntityManager;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.partner.ChangePartnerStatusDto;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.service.client.PartnerRelationService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.DynamicValidationAspect;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.DynamicValidationService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.rule.PartnerWithoutRequiredDataCantBeActive;
import ru.yandex.market.logistics.management.util.tskv.ValidationExceptionLogger;
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.util.TestUtil.validationErrorMatcher;

@ParametersAreNonnullByDefault
@Import(ChangePartnerStatusTest.DynamicValidationConfiguration.class)
@DisplayName("Изменение статуса партнёра")
class ChangePartnerStatusTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешная смена статуса")
    @DatabaseSetup("/data/controller/partner/changeStatus/before/correct_partner_setup.xml")
    @ExpectedDatabase(
        value = "/data/controller/partner/changeStatus/after/partner_activated.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successChangeStatus() throws Exception {
        changeStatus(
            getChangePartnerStatusToActiveRequest(),
            "data/controller/partner/changeStatus/response/change_partner_status_success.json"
        );
    }

    @Test
    @DisplayName("Смена статуса в ACTIVE для партнера без marketId")
    @DatabaseSetup("/data/controller/partner/changeStatus/before/partner_without_market_id_setup.xml")
    @ExpectedDatabase(
        value = "/data/controller/partner/changeStatus/before/partner_without_market_id_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeStatusToActiveWithoutMarketId() throws Exception {
        changeStatus(getChangePartnerStatusToActiveRequest())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status().reason(
                "Ошибка валидации для партнера [id=1]: "
                    + "Partner can't be ACTIVE with empty fields [marketId], partnerId 1."
            ));
    }

    @Test
    @DisplayName("Смена статуса в ACTIVE для партнера-дропшипа с недозаполненным адресом склада")
    @DatabaseSetup("/data/controller/partner/changeStatus/before/partner_dropship_without_warehouse_address_setup.xml")
    @ExpectedDatabase(
        value = "/data/controller/partner/changeStatus/before/partner_dropship_without_warehouse_address_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeStatusToActiveDropshipWithoutAddress() throws Exception {
        changeStatus(getChangePartnerStatusToActiveRequest())
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status().reason(
                "Ошибка валидации для партнера [id=1]: "
                    + "Partner can't be ACTIVE with empty fields [house, postCode], partnerId 1."
            ));
    }

    @Test
    @DisplayName("Невалидное тело запроса на смену статуса партнёра")
    @DatabaseSetup("/data/controller/partner/changeStatus/before/correct_partner_setup.xml")
    @ExpectedDatabase(
        value = "/data/controller/partner/changeStatus/before/correct_partner_setup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void invalidRequestBodyForChangePartnerStatus() throws Exception {
        changeStatus(ChangePartnerStatusDto.newBuilder().build())
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                "changePartnerStatusDto",
                "status",
                "NotNull",
                "must not be null"
            ));
    }

    @Test
    @CleanDatabase
    @DisplayName("Обновление статуса несуществующего партнёра")
    void partnerNotFound() throws Exception {
        changeStatus(getChangePartnerStatusToActiveRequest())
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=1"));
    }

    private void changeStatus(
        ChangePartnerStatusDto changeStatusDto,
        String responsePath
    ) throws Exception {
        mockMvc.perform(post("/externalApi/partners/1/changeStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(changeStatusDto)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private ResultActions changeStatus(ChangePartnerStatusDto changeStatusDto) throws Exception {
        return mockMvc.perform(post("/externalApi/partners/1/changeStatus")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(changeStatusDto)));
    }

    @Nonnull
    private ChangePartnerStatusDto getChangePartnerStatusToActiveRequest() {
        return ChangePartnerStatusDto.newBuilder()
            .status(PartnerStatus.ACTIVE)
            .build();
    }

    @Configuration
    public static class DynamicValidationConfiguration {
        @Bean
        public ValidationRule partnerStatusValidationRule() {
            return new PartnerWithoutRequiredDataCantBeActive();
        }

        @Bean
        public DynamicValidationAspect dynamicValidationAspect(PartnerRelationService partnerRelationService) {
            List<ValidationRule> rules = List.of(partnerStatusValidationRule());
            DynamicValidationService validationService = new DynamicValidationService(rules);

            return new DynamicValidationAspect(
                Mockito.mock(EntityManager.class),
                partnerRelationService,
                validationService,
                Mockito.mock(ValidationExceptionLogger.class),
                true
            );
        }
    }
}
