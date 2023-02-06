package ru.yandex.market.logistics.management.controller.admin.partnerRelation;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_RELATION;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
class AdminPartnerRelationGetDetailTest extends BaseAdminPartnerRelationTest {
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void partnerRelationDetail1() throws Exception {
        getPartnerRelationDetail(1)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_detail_1.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void partnerRelationDetail2() throws Exception {
        getPartnerRelationDetail(2)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_detail_2.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void partnerRelationDetailWithTransferAndInboundTime() throws Exception {
        getPartnerRelationDetail(3)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_detail_with_transfer_and_inbound_time.json",
                false
            ));
    }

    @Nonnull
    private ResultActions getPartnerRelationDetail(long id) throws Exception {
        return mockMvc.perform(get("/admin/lms/partner-relation/" + id));
    }
}
