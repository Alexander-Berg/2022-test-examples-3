package ru.yandex.market.logistics.management.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.front.partner.AdminPartnerType;
import ru.yandex.market.logistics.management.domain.dto.front.partner.PartnerNewDto;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.producer.PartnerBillingRegistrationTaskProducer;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.logging.jdbc.json.JsonJdbcExceptionLogger.OBJECT_MAPPER;

@DisplayName("Контроллер для создания партнеров")
@DatabaseSetup("/data/controller/admin/partner/prepare_data.xml")
public class AdminPartnerControllerCreateTest extends AbstractContextualAspectValidationTest {

    private static final String PARTNER_URL = "/admin/lms/partner";

    private final PartnerNewDto partnerNewDto = new PartnerNewDto()
        .setPartnerId(1000L)
        .setName("Test")
        .setReadableName("test")
        .setPartnerType(AdminPartnerType.DELIVERY);

    @Autowired
    @Qualifier("partnerBillingClientCreationTaskProducer")
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> clientCreationTaskProducer;
    @Autowired
    @Qualifier("partnerBillingClientLinkingTaskProducer")
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> clientLinkingTaskProducer;

    @BeforeEach
    void setup() {
        Mockito.reset(clientCreationTaskProducer, clientLinkingTaskProducer);
        Mockito.doNothing().when(clientCreationTaskProducer).produceTask(any());
        Mockito.doNothing().when(clientLinkingTaskProducer).produceTask(any());
    }

    @Test
    @DisplayName("Создать партнера - Неавторизованный пользователь")
    void testCreatePartner_unauthorized() throws Exception {
        mockMvc.perform(post(PARTNER_URL))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Создать партнера - Недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER})
    void testCreatePartner_forbidden() throws Exception {
        mockMvc.perform(post(PARTNER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(
                    partnerNewDto.setLegalInfoId(1L))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Создать партнера - Bad Request")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT})
    void testCreatePartner_badRequest() throws Exception {
        mockMvc.perform(post(PARTNER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(partnerNewDto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Юридическая информация не найдена")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT)
    void testCreatePartner_legalInfoNotFound() throws Exception {
        mockMvc.perform(post(PARTNER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(
                    partnerNewDto.setLegalInfoId(3L))))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Партнер создан успешно")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/partner/after/create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreatePartner_success() throws Exception {
        mockMvc.perform(post(PARTNER_URL).contentType(MediaType.APPLICATION_JSON)
                .content(OBJECT_MAPPER.writeValueAsBytes(
                    partnerNewDto.setLegalInfoId(1L))))
            .andExpect(status().isCreated());
    }
}
