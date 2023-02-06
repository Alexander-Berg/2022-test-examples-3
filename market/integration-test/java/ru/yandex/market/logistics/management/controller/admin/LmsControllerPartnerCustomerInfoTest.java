package ru.yandex.market.logistics.management.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.PartnerCustomerInfo;
import ru.yandex.market.logistics.management.domain.entity.type.TrackCodeSource;
import ru.yandex.market.logistics.management.repository.PartnerCustomerInfoRepository;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@Sql("/data/controller/admin/customerInfo/prepare_data.sql")
class LmsControllerPartnerCustomerInfoTest extends AbstractContextualTest {

    @Autowired
    private PartnerCustomerInfoRepository partnerCustomerInfoRepository;

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CUSTOMER_INFO})
    void getPartnerCustomerInfoGrid() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/admin/lms/" + LMSPlugin.SLUG_PARTNER_CUSTOMER_INFO);

        ResultActions perform = mockMvc.perform(requestBuilder);
        perform
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/customerInfo/partner_customer_info_grid.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CUSTOMER_INFO})
    void getPartnerCustomerInfoDetail() throws Exception {
        getPartnerCustomerInfoDetail(1L)
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/customerInfo/partner_customer_info_detail.json"));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CUSTOMER_INFO})
    void getPartnerCustomerInfoDetailWithRelatedPartners() throws Exception {
        getPartnerCustomerInfoDetail(2L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/customerInfo/partner_customer_info_detail_with_related_partners.json"
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CUSTOMER_INFO})
    void getPartnerHandlingTimeNotFound() throws Exception {
        getPartnerCustomerInfoDetail(0)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CUSTOMER_INFO_EDIT})
    void createPartnerHandlingTime() throws Exception {
        postPartnerCustomerInfo("data/controller/admin/customerInfo/partner_customer_info_create_request.json")
            .andExpect(status().isCreated());

        PartnerCustomerInfo partnerCustomerInfo = partnerCustomerInfoRepository.findByIdOrThrow(3L);

        softly.assertThat(partnerCustomerInfo.getName()).isEqualTo("PartnerThree");
        softly.assertThat(partnerCustomerInfo.getTrackOrderSite()).isEqualTo("www.partner3-site.ru");
        softly.assertThat(partnerCustomerInfo.getTrackCodeSource()).isEqualTo(TrackCodeSource.ORDER_NO);
        softly.assertThat(partnerCustomerInfo.getPhones()).containsOnly("8-(800)-123-45-67");
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_PARTNER_CUSTOMER_INFO_EDIT})
    void updatePartnerCustomerInfo() throws Exception {
        putPartnerCustomerInfo(1L, "data/controller/admin/customerInfo/partner_customer_info_update_request.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/customerInfo/partner_customer_info_update_response.json"));

        PartnerCustomerInfo partnerCustomerInfo = partnerCustomerInfoRepository.findByIdOrThrow(1L);

        softly.assertThat(partnerCustomerInfo.getName()).isEqualTo("PartnerOne");
        softly.assertThat(partnerCustomerInfo.getTrackOrderSite()).isEqualTo("www.partner1-site.ru");
        softly.assertThat(partnerCustomerInfo.getTrackCodeSource()).isEqualTo(TrackCodeSource.ORDER_NO);
        softly.assertThat(partnerCustomerInfo.getPhones()).containsOnly("+7-(912)-345-67-89", "8-(800)-123-45-68");
    }


    private ResultActions getPartnerCustomerInfoDetail(long id) throws Exception {
        return mockMvc.perform(get("/admin/lms/" + LMSPlugin.SLUG_PARTNER_CUSTOMER_INFO + "/{id}", id));
    }

    private ResultActions postPartnerCustomerInfo(String fileName) throws Exception {
        return mockMvc.perform(post("/admin/lms/" + LMSPlugin.SLUG_PARTNER_CUSTOMER_INFO)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(fileName)));
    }

    private ResultActions putPartnerCustomerInfo(long id, String fileName) throws Exception {
        return mockMvc.perform(put("/admin/lms/" + LMSPlugin.SLUG_PARTNER_CUSTOMER_INFO + "/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(pathToJson(fileName)));
    }
}
