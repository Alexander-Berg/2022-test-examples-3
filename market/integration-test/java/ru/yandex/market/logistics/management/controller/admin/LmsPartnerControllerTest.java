package ru.yandex.market.logistics.management.controller.admin;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
@ParametersAreNonnullByDefault
class LmsPartnerControllerTest extends AbstractContextualTest {
    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;

    @Autowired
    private TestableClock clock;

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
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partner/set_sync_stocks_enabled.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerSetSyncStocksEnabled() throws Exception {
        putPartnerDetail("data/controller/admin/partnerRelation/partner.json").andExpect(status().isOk());

        checkLogbrokerEvent("data/controller/admin/partner/logbrokerEvent/set_sync_stocks_enabled.json");
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partner/set_frozen.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerSetFrozen() throws Exception {
        putPartnerDetail("data/controller/admin/partnerRelation/partner_frozen.json").andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partner/set_subtype_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerSetSubtypeId() throws Exception {
        putPartnerDetail("data/controller/admin/subtype/subtype_id.json").andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partner/set_location_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerSetLocationId() throws Exception {
        putPartnerDetail("data/controller/admin/partnerRelation/partner_location_id.json").andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerSetWrongSubtypeId() throws Exception {
        putPartnerDetail("data/controller/admin/subtype/subtype_wrong_id.json").andExpect(status().isNotAcceptable());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partner/set_subtype_id_null.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partnerSetSubtypeIdNull() throws Exception {
        putPartnerDetail("data/controller/admin/subtype/subtype_id_null.json").andExpect(status().isOk());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER})
    void partnerSubtypeGrid() throws Exception {
        mockMvc.perform(
            get("/admin/lms/partner/1/get-subtype-options")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/subtype/subtype_grid.json", true));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER})
    void partnerSubtypeItem() throws Exception {
        mockMvc.perform(
            get("/admin/lms/partner/1/get-subtype-options").param("subtypeId", "1")
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson("data/controller/admin/subtype/subtype_item.json", true));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/admin/partnerRelation/settings_method_addon.xml",
        connection = "dbUnitQualifiedDatabaseConnection",
        type = DatabaseOperation.INSERT
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    void syncPickupPoints() throws Exception {
        mockMvc.perform(
            post("/admin/lms/partner/sync-pickup-points")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\": 1}")
        )
            .andExpect(status().isOk());
    }

    @Nonnull
    private ResultActions putPartnerDetail(String requestPath) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/partner/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(requestPath))
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
