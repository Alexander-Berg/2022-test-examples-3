package ru.yandex.market.billing.api.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.billing.api.config.MvcConfig;
import ru.yandex.market.billing.core.distribution.DistributionHardcodedClidsDao;
import ru.yandex.market.billing.core.distribution.HardcodedClid;
import ru.yandex.market.billing.core.distribution.PartnerSegment;
import ru.yandex.market.billing.security.config.PassportConfig;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DistributionHardcodedClidsController.class)
@AutoConfigureMockMvc(secure = false)
@ActiveProfiles("functionalTest")
class DistributionHardcodedClidsControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PassportConfig passportConfig;
    @MockBean
    MvcConfig mvcConfig;

    @MockBean
    DistributionHardcodedClidsDao dao;


    @Test
    void testUpdate() throws Exception {
        System.out.println(mockMvc.perform(post("/distribution/hardcoded-clids/set")));
        MockMultipartFile mmf = new MockMultipartFile(
                "file", "", "text/plain", getInputStreamForUpdate());

        mockMvc.perform(multipart("/distribution/hardcoded-clids/set")
                        .file(mmf))
                .andExpect(status().isOk());
        verify(dao).upsert(
                List.of(
                        new HardcodedClid(22, PartnerSegment.CLOSER, "Купонный агрегатор"),
                        new HardcodedClid(44, PartnerSegment.CLOSER, "Не определено")));
    }

    @Test
    void testUpdateBadInput() throws Exception {
        System.out.println(mockMvc.perform(post("/distribution/hardcoded-clids/set")));
        MockMultipartFile mmf = new MockMultipartFile(
                "file", null, "text/plain", getBrokenInputStreamForUpdate());

        mockMvc.perform(multipart("/distribution/hardcoded-clids/set")
                        .file(mmf)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDelete() throws Exception {
        System.out.println(mockMvc.perform(post("/distribution/hardcoded-clids/delete")));
        mockMvc.perform(post("/distribution/hardcoded-clids/delete")
                        .param("clids", "33,81")
                )
                .andExpect(status().isOk());
        verify(dao).delete(List.of(33L, 81L));
    }

    private static InputStream getInputStreamForUpdate() {
        return new ByteArrayInputStream(
                ("clid,partner_segment,place_type\n" +
                "22,closer,Купонный агрегатор\n" +
                "44,closer,Не определено").getBytes());
    }

    private static InputStream getBrokenInputStreamForUpdate() {
        return new ByteArrayInputStream(
                ("clid,partner_segment,place_type\n" +
                        "22,closer\n" +
                        "44,closer,Не определено").getBytes());
    }
}
