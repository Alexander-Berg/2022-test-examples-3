package ru.yandex.market.tpl.partner.carrier.controller.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.telegram.TelegramChat;
import ru.yandex.market.tpl.carrier.core.domain.telegram.TelegramChatRepository;
import ru.yandex.market.tpl.partner.carrier.BaseTplPartnerCarrierWebIntTest;
import ru.yandex.market.tpl.partner.carrier.model.telegram.TokenDto;
import ru.yandex.market.tpl.partner.carrier.web.PartnerCompanyHandler;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RequiredArgsConstructor(onConstructor_ = { @Autowired })
class TelegramAuthControllerTest extends BaseTplPartnerCarrierWebIntTest {

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final TelegramChatRepository chatRepository;

    private Company company;
    private String token;
    private String invalidToken;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        token = "weriseekrettoocen";
        invalidToken = "someunknownstuff";
        TelegramChat chat = new TelegramChat();
        chat.setId(1L);
        chat.setTitle("Test");
        chat.setToken(token);
        chatRepository.saveAndFlush(chat);

    }


    @SneakyThrows
    @Test
    void shouldRegisterChatCompanyRelation() {

        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(token);
        mockMvc.perform(post("/internal/partner/telegram")
                .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(tokenDto)))
        .andExpect(status().isOk());

        Assertions.assertTrue(chatRepository.findByToken(token).isEmpty());

        List<TelegramChat> chats = chatRepository.findAllByCompany(company);
        Assertions.assertEquals(1, chats.size());


        mockMvc.perform(get("/internal/partner/telegram")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }


    @SneakyThrows
    @Test
    void shouldResponseWith400WhenUnknownToken() {

        TokenDto tokenDto = new TokenDto();
        tokenDto.setToken(invalidToken);
        mockMvc.perform(post("/internal/partner/telegram")
                        .header(PartnerCompanyHandler.COMPANY_HEADER, company.getCampaignId())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(tplObjectMapper.writeValueAsString(tokenDto)))
                .andExpect(status().isBadRequest());

        TelegramChat chat = chatRepository.findByToken(token).orElseThrow();
        Assertions.assertNull(chat.getCompany());

    }


    @SneakyThrows
    @Test
    void shouldValidateToken() {
        mockMvc.perform(get("/internal/partner/telegram")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        mockMvc.perform(get("/internal/partner/telegram")
                        .param("token", invalidToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

}
