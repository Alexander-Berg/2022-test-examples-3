package ru.yandex.market.mbi.bot.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import com.google.gson.Gson;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

import ru.yandex.market.mbi.bot.tg.service.TgBotAccountService;
import ru.yandex.market.mbi.bot.tg.service.UpdateProcessor;

@Controller
@Profile("!production")
public class TestController {

    private static final Gson GSON = new Gson();

    private final UpdateProcessor updateProcessor;
    private final TgBotAccountService accountService;

    @Autowired
    public TestController(UpdateProcessor updateProcessor, TgBotAccountService accountService) {
        this.updateProcessor = updateProcessor;
        this.accountService = accountService;
    }

    @PostMapping("/test/update")
    public ResponseEntity<String> testUpdate(HttpServletRequest request) throws IOException {
        String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        TestUpdate testUpdate = GSON.fromJson(body, TestUpdate.class);
        updateProcessor.processUpdate(
                accountService.getAccount(testUpdate.getBotId()),
                testUpdate.getUser(),
                testUpdate.getChat(),
                testUpdate.getMessage(),
                testUpdate.getText()
        );
        return ResponseEntity.ok("ok");
    }

    private static class TestUpdate {
        private String botId;
        private User user;
        private Chat chat;
        private Message message;
        private String text;

        public String getBotId() {
            return botId;
        }

        public void setBotId(String botId) {
            this.botId = botId;
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public Chat getChat() {
            return chat;
        }

        public void setChat(Chat chat) {
            this.chat = chat;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
