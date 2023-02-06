package ru.yandex.market.tsum.api.telegram;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.market.tsum.telegrambot.bot.handlers.commands.startrek.checkouter.CommandMessage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class TsumBotMessagingControllerTest {

    private MockMvc mockMvc;

    @Configuration
    static class TestConfiguration {

        @Bean
        public TsumBotMessagingService tsumBotMessagingService() throws IOException {
            return mock(TsumBotMessagingService.class);
        }
    }


    @Autowired
    private TsumBotMessagingService tsumBotMessagingService;

    private TsumBotMessagingController controller;

    @Before
    public void init() {
        controller = new TsumBotMessagingController(tsumBotMessagingService);
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();
        Mockito.reset(tsumBotMessagingService);
    }

    @Test
    public void postCommandOnlyText() throws Exception {
        mockMvc.perform(
                post("/v1/bot/command")
                    .contentType("application/json")
                    .content("{\n" +
                        "  \"chat_id\": \"-1001324412277\",\n" +
                        "  \"text\": \"/newcheckouteralert {{alert.id}} {{alert.name}}\\n<b>{{status.code}}</b>:" +
                        " <a href=\\\"{{url}} \\\">{{alert.name}}</a>\\n\\nОписание: {{annotations.description}}\",\n" +
                        "  \"parse_mode\": \"HTML\"\n" +
                        "}"))
            .andExpect(status().isOk());

        ArgumentCaptor<CommandMessage> captor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(tsumBotMessagingService, times(1)).processCommand(captor.capture());
        CommandMessage msg = captor.getValue();

        Assert.assertEquals("-1001324412277", msg.getChatId());
        Assert.assertEquals("HTML", msg.getParseMode());
        Assert.assertNull(msg.getAddScreenShot());
        Assert.assertEquals("/newcheckouteralert {{alert.id}} {{alert.name}}\n<b>{{status.code}}</b>: <a " +
            "href=\"{{url}} \">{{alert.name}}</a>\n\nОписание: {{annotations.description}}", msg.getText());
    }

    @Test
    public void postCommandWithScreenshot() throws Exception {
        mockMvc.perform(
                post("/v1/bot/command")
                    .contentType("application/json")
                    .content("{\n" +
                        "  \"chat_id\": \"-1001324412277\",\n" +
                        "  \"text\": \"/newcheckouteralert {{alert.id}} {{alert.name}}\\n<b>{{status.code}}</b>:" +
                        " <a href=\\\"{{url}} \\\">{{alert.name}}</a>\\n\\nОписание: {{annotations.description}}\",\n" +
                        "  \"parse_mode\": \"HTML\",\n" +
                        "  \"add_screenshot\": \"true\"\n" +
                        "}"))
            .andExpect(status().isOk());

        ArgumentCaptor<CommandMessage> captor = ArgumentCaptor.forClass(CommandMessage.class);
        verify(tsumBotMessagingService, times(1)).processCommand(captor.capture());
        CommandMessage msg = captor.getValue();

        Assert.assertEquals("-1001324412277", msg.getChatId());
        Assert.assertEquals("HTML", msg.getParseMode());
        Assert.assertTrue(msg.getAddScreenShot());
        Assert.assertEquals("/newcheckouteralert {{alert.id}} {{alert.name}}\n<b>{{status.code}}</b>: <a " +
            "href=\"{{url}} \">{{alert.name}}</a>\n\nОписание: {{annotations.description}}", msg.getText());
    }

}
