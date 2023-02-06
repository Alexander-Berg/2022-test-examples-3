package ru.yandex.market.pvz.internal.controller.lms;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LmsBannerControllerTest extends BaseShallowTest {

    @Test
    void createBannerThenSuccess() throws Exception {
        mockMvc.perform(post("/lms/banner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/banner/request_create_banner.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("lms/banner/response_create_banner.json"), false));
    }

    @Test
    void createSameBannerTwiceThenError() throws Exception {
        mockMvc.perform(post("/lms/banner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/banner/request_create_banner.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("lms/banner/response_create_banner.json"), false));

        mockMvc.perform(post("/lms/banner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/banner/request_create_banner.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createClosableBannerWithoutFrequency() throws Exception {
        mockMvc.perform(post("/lms/banner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/banner/request_create_banner_without_frequency.json")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPopupThenSuccess() throws Exception {
        mockMvc.perform(post("/lms/banner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/banner/request_create_popup.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(
                        getFileContent("lms/banner/response_create_popup.json"), false
                ));
    }

    @Test
    void createWithEmptyFields() throws Exception {
        mockMvc.perform(post("/lms/banner")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPopupWithoutButton() throws Exception {
        mockMvc.perform(post("/lms/banner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("lms/banner/request_create_popup_without_button.json")))
                .andExpect(status().isBadRequest());
    }

}
