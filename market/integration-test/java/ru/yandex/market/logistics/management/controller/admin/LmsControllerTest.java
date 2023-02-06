package ru.yandex.market.logistics.management.controller.admin;

import java.time.LocalDate;
import java.time.ZoneId;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_CUT_OFF;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_CUT_OFF_EDIT;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_RATING;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_RATING_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@DatabaseSetup("/data/controller/admin/partnerRelation/prepare_data.xml")
@ParametersAreNonnullByDefault
class LmsControllerTest extends AbstractContextualTest {

    @Autowired
    private TestableClock clock;

    @BeforeEach
    void setup() {
        LocalDate today = LocalDate.of(2018, 10, 29);
        clock.setFixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_CUT_OFF})
    void cutoffGrid() throws Exception {
        getCutoffGrid()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/cutoff/cutoff_grid.json",
                true
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_CUT_OFF})
    void cutoffGridFilter() throws Exception {
        getCutoffGrid(2)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/cutoff/cutoff_grid_filtrable.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_CUT_OFF})
    void cutoffGridFilterNoResult() throws Exception {
        getCutoffGrid(10)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/cutoff/cutoff_grid_filtrable_no_result.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_CUT_OFF})
    void cutoffDetail() throws Exception {
        getCutoffDetail()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/cutoff/cutoff_detail.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_CUT_OFF_EDIT})
    void cutoffNew() throws Exception {
        getCutoffNew()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/cutoff/cutoff_new.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_CUT_OFF_EDIT})
    void cutoffNewInvalidPackagingDuration() throws Exception {
        postCutoffNew("cutoff_invalid_packaging_duration")
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_CUT_OFF_EDIT})
    void cutoffNewCreate() throws Exception {
        postCutoffNew("cutoff_create").andExpect(status().isCreated());
        checkBuildWarehouseSegmentTask(1L, 2L, 3L, 4L);
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_RATING})
    void productRatingGrid() throws Exception {
        getProductRatingGrid()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/productRating/product_rating_grid.json",
                true
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_RATING})
    void productRatingGridFilter() throws Exception {
        getProductRatingGrid(1, 1)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/productRating/product_rating_grid_filtrable.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_RATING})
    void productRatingGridFilterNullResult() throws Exception {
        getProductRatingGrid(2, 2)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/productRating/product_rating_grid_filtrable_null_result.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_RATING})
    void productRatingDetail() throws Exception {
        getProductRatingDetail()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/productRating/product_rating_detail.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_RATING, AUTHORITY_ROLE_RATING_EDIT})
    void productRatingDetailEditable() throws Exception {
        getProductRatingDetail()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/productRating/product_rating_detail_editable.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_RATING_EDIT})
    void productRatingNew() throws Exception {
        getProductRatingNew()
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/productRating/product_rating_new.json",
                false
            ));
    }

    @Nonnull
    private ResultActions getCutoffGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/cut-off")
        );
    }

    @Nonnull
    private ResultActions getCutoffGrid(int locationId) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/cut-off")
                .param("partnerRelation", "2")
                .param("locationId", String.valueOf(locationId))
        );
    }

    @Nonnull
    private ResultActions getCutoffDetail() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/cut-off/1")
        );
    }

    @Nonnull
    private ResultActions getCutoffNew() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/cut-off/new")
        );
    }

    @Nonnull
    private ResultActions postCutoffNew(String fileName) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/cut-off")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/cutoff/" + fileName + ".json"))
        );
    }

    @Nonnull
    private ResultActions getProductRatingGrid() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/rating")
        );
    }

    @Nonnull
    private ResultActions getProductRatingGrid(long partnerRelation, int locationId) throws Exception {
        return mockMvc.perform(
            get("/admin/lms/rating")
                .param("partnerRelation", String.valueOf(partnerRelation))
                .param("locationId", String.valueOf(locationId))
        );
    }

    @Nonnull
    private ResultActions getProductRatingDetail() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/rating/1")
        );
    }

    @Nonnull
    private ResultActions getProductRatingNew() throws Exception {
        return mockMvc.perform(
            get("/admin/lms/rating/new")
        );
    }
}
