package ru.yandex.market.logistics.management.controller.admin.partnerRelation;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_RELATION_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
@SuppressWarnings({"checkstyle:MagicNumber"})
class AdminPartnerRelationUpdateTest extends BaseAdminPartnerRelationTest {
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationWithReturnPartnerUpdate() throws Exception {
        putPartnerRelationDetail(1, "updatePartnerRelationWithReturnPartner")
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/admin/partnerRelation/updatePartnerRelationWithReturnPartnerResponse.json"
            )));

        Optional<PartnerRelation> partnerRelationOptional =
            partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 2L);

        assertReturnPartner(partnerRelationOptional);
        checkBuildWarehouseSegmentTask(1L, 2L, 3L, 4L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationWithoutIntakeDeadlineUpdate() throws Exception {
        putPartnerRelationDetail(1, "updatePartnerRelationWithoutIntakeDeadline")
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/admin/partnerRelation/updatePartnerRelationWithoutIntakeDeadlineResponse.json"
            )));

        Optional<PartnerRelation> partnerRelationOptional =
            partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 2L);

        assertReturnPartner(partnerRelationOptional);
        checkBuildWarehouseSegmentTask(1L, 2L, 3L, 4L);
    }

    /**
     * Не удается обновить связку партнеров с возвратным складом,
     * который не является фулфиллментом или дропшипом.
     */
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationWithReturnPartnerUpdateError() throws Exception {
        putPartnerRelationDetail(1, "updatePartnerRelationWithReturnPartnerError")
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Return partner 2 doesn't exist."));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationShipmentTypeIsNullUpdateError() throws Exception {
        putPartnerRelationDetail(1, "updatePartnerRelationShipmentTypeNull")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field", Matchers.is("shipmentType")))
            .andExpect(jsonPath("errors[0].code", Matchers.is("NotNull")));
    }

    @Nonnull
    private ResultActions putPartnerRelationDetail(long id, String fileName) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/partner-relation/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/partnerRelation/" + fileName + ".json"))
        );
    }
}
