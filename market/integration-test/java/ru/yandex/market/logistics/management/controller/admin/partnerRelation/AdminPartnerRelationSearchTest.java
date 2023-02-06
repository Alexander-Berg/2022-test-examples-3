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
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_RELATION_EDIT;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
@SuppressWarnings({"checkstyle:MagicNumber"})
class AdminPartnerRelationSearchTest extends BaseAdminPartnerRelationTest {
    /**
     * запрос без пагинации возвращает всё, что есть в репозитории (все 2 записи).
     */
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void partnerRelationFront() throws Exception {
        getPartnerRelationGrid()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_grid.json",
                true
            ));
    }

    /**
     * на первой странице должны быть все 2 результата из репозитория,
     * так как размер страницы по умолчанию 20.
     */
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void firstPagePartnerRelationGrid() throws Exception {
        getPartnerRelationGrid(0)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_grid.json",
                true
            ));
    }

    /**
     * на второй странице результатов быть не должно,
     * так как размер страницы по умолчанию 20, а в репозитории всего 2 записи.
     */
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION})
    void secondPagePartnerRelationGrid() throws Exception {
        getPartnerRelationGrid(1)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_grid_second_page.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationGridEditable() throws Exception {
        getPartnerRelationGrid()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_grid_editable.json",
                true
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationGridFilter() throws Exception {
        getPartnerRelationGrid(3, 4)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_grid_filtrable.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationGridFilterNoResult() throws Exception {
        getPartnerRelationGrid(1, 4)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_grid_filtrable_no_result.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_RELATION_EDIT})
    void partnerRelationGridFilterByWarehouseTo() throws Exception {
        getPartnerRelationGridByWarehouseTo(2)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partnerRelation/partner_relation_grid_filtrable_by_warehouse_to.json",
                false
            ));
    }

    @Test
    void partnerRelationGridUnauthorized() throws Exception {
        getPartnerRelationGrid()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void partnerRelationGridForbidden() throws Exception {
        getPartnerRelationGrid()
            .andExpect(status().isForbidden());
    }

    @Nonnull
    private ResultActions getPartnerRelationGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-relation")
        );
    }

    @Nonnull
    private ResultActions getPartnerRelationGrid(int page) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-relation")
                .param("page", String.valueOf(page))
        );
    }

    @Nonnull
    private ResultActions getPartnerRelationGrid(long fromPartner, long toPartner) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-relation")
                .param("partnerFrom", String.valueOf(fromPartner))
                .param("partnerTo", String.valueOf(toPartner))
        );
    }

    @Nonnull
    private ResultActions getPartnerRelationGridByWarehouseTo(long warehouseTo) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/partner-relation")
                .param("toPartnerLogisticsPoint", String.valueOf(warehouseTo))
        );
    }
}
