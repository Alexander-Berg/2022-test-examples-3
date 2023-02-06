package ru.yandex.market.logistics.management.controller.admin.partnerRelation;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_RELATION_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
@SuppressWarnings({"checkstyle:MagicNumber"})
class AdminPartnerRelationCreateTest extends BaseAdminPartnerRelationTest {
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/data_after_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void partnerRelationCreate() throws Exception {
        postPartnerRelationDetail("newPartnerRelation")
            .andExpect(status().isCreated());
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    @ExpectedDatabase(
        value = "/data/controller/admin/partnerRelation/data_after_creation_without_intake_deadline.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void partnerRelationCreateWithoutIntakeDeadline() throws Exception {
        postPartnerRelationDetail("newPartnerRelationWithoutIntakeDeadline")
            .andExpect(status().isCreated());
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationWithReturnPartnerCreate() throws Exception {
        postPartnerRelationDetail("newPartnerRelationWithReturnPartner")
            .andExpect(status().isCreated());

        Optional<PartnerRelation> partnerRelationOptional =
            partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 4L);

        assertReturnPartner(partnerRelationOptional);
        checkBuildWarehouseSegmentTask(1L);
    }

    /**
     * Не удается создать связку партнеров с возвратным складом,
     * который не является фулфиллментом или дропшипом.
     */
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationWithReturnPartnerCreateError() throws Exception {
        postPartnerRelationDetail("newPartnerRelationWithReturnPartnerError")
            .andExpect(status().isBadRequest())
        .andExpect(status().reason("Return partner 2 doesn't exist."));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationWithTransferAndInboundTimeCreate() throws Exception {
        postPartnerRelationDetail("newPartnerRelationWithTransferAndInboundTime")
            .andExpect(status().isCreated());

        Optional<PartnerRelation> partnerRelationOptional =
            partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 4L);

        softly.assertThat(partnerRelationOptional)
            .as("Asserting that the partner relation exists and is valid")
            .hasValueSatisfying(partnerRelation -> {
                softly.assertThat(partnerRelation.getTransferTime().toNanos())
                    .as("Asserting that the transfer time value is valid")
                    .isEqualTo(1800000000000L);
                softly.assertThat(partnerRelation.getInboundTime().toNanos())
                    .as("Asserting that the inbound time is valid")
                    .isEqualTo(5400000000000L);
            });
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationCreateHandlingTimeNull() throws Exception {
        postPartnerRelationDetail("newPartnerRelationHandlingTimeNull")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field", Matchers.is("handlingTime")))
            .andExpect(jsonPath("errors[0].code", Matchers.is("NotNull")));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationCreateReturnPartnerNull() throws Exception {
        postPartnerRelationDetail("newPartnerRelationReturnPartnerNull")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field", Matchers.is("partnerReturnId")))
            .andExpect(jsonPath("errors[0].code", Matchers.is("NotNull")));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationCreateHandlingTimeString() throws Exception {
        postPartnerRelationDetail("newPartnerRelationHandlingTimeString")
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationCreateShipmentTypeNull() throws Exception {
        postPartnerRelationDetail("newPartnerRelationShipmentTypeNull")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field", Matchers.is("shipmentType")))
            .andExpect(jsonPath("errors[0].code", Matchers.is("NotNull")));
    }

    @Nonnull
    private ResultActions postPartnerRelationDetail(String fileName) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/partner-relation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/partnerRelation/" + fileName + ".json"))
        );
    }
}
