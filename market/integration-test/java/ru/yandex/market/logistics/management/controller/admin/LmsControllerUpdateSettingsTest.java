package ru.yandex.market.logistics.management.controller.admin;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.repository.PartnerExternalParamRepository;
import ru.yandex.market.logistics.management.repository.PartnerExternalParamTypeRepository;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

class LmsControllerUpdateSettingsTest extends AbstractContextualTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;

    @Autowired
    private PartnerExternalParamRepository valueRepository;

    @Autowired
    private PartnerExternalParamTypeRepository typeRepository;

    @BeforeEach
    void setup() {
        Mockito.doNothing().when(logbrokerEventTaskProducer).produceTask(Mockito.any());
        clock.setFixed(Instant.parse("2021-08-05T13:45:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
        Mockito.verifyNoMoreInteractions(logbrokerEventTaskProducer);
    }

    @Test
    @DisplayName("Успешное включение синхронизации ВГХ у партнера, у которого она была выключена")
    @DatabaseSetup("/data/controller/admin/partnerExternalParam/before_update_partner_with_korobyte_sync.xml")
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/after_update_partner_with_korobyte_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    void testUpdatePartnerSettingsExistingKorobyteSyncParamType() throws Exception {
        updatePartner()
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/partnerExternalParam/partner_update_response.json"));

        checkLogbrokerEvent("data/controller/admin/partnerExternalParam/logbrokerEvent/partner_update_response.json");
    }

    @Test
    @DisplayName("Успешное включение синхронизации ВГХ у партнера, у которого она еще не настраивалась")
    @DatabaseSetup("/data/controller/admin/partnerExternalParam/before_update_partner_without_korobyte_sync.xml")
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerExternalParam/after_update_partner_without_korobyte_sync.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    void testUpdatePartnerSettingsNonExistingKorobyteSyncParamType() throws Exception {
        updatePartner()
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/partnerExternalParam/partner_update_response.json"));

        checkLogbrokerEvent("data/controller/admin/partnerExternalParam/logbrokerEvent/partner_update_response.json");
    }

    @Nonnull
    private ResultActions updatePartner() throws Exception {
        return mockMvc.perform(
            put("/admin/lms/partner/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/partnerExternalParam/partner_update_request.json"))
        );
    }

    private void checkLogbrokerEvent(String jsonPath) throws IOException {
        ArgumentCaptor<EventDto> argumentCaptor =
            ArgumentCaptor.forClass(EventDto.class);
        Mockito.verify(logbrokerEventTaskProducer)
            .produceTask(argumentCaptor.capture());
        assertThatJson(argumentCaptor.getValue())
            .isEqualTo(objectMapper.readValue(pathToJson(jsonPath), EventDto.class));
    }
}
