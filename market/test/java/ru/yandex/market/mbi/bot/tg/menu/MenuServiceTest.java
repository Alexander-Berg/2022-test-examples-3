package ru.yandex.market.mbi.bot.tg.menu;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bot.FunctionalTest;
import ru.yandex.market.mbi.bot.tg.service.TgBotAccountService;

import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static ru.yandex.market.mbi.bot.tg.TelegramTestUtils.createOkResponse;

public class MenuServiceTest extends FunctionalTest {

    @Autowired
    private MenuService menuService;

    @Autowired
    private WireMockServer tgApiMock;

    @Autowired
    private TgBotAccountService tgBotAccountService;

    @Test
    void testMenuCommandsInitialization() {
        tgApiMock.resetRequests();
        tgApiMock.stubFor(any(urlPathMatching("/(.*)/setMyCommands"))
                .willReturn(ok(createOkResponse())));

        String menuJson = "{" +
                "    \"menu\": [" +
                "        {" +
                "            \"command\": \"/command1\"," +
                "            \"description\": \"Сделайте мне хорошо\"," +
                "            \"scenario\": \"makeMeOk\"" +
                "        }" +
                "    ]" +
                "}";
        menuService.initMenu(new ByteArrayInputStream(menuJson.getBytes(StandardCharsets.UTF_8)));

        int botsCount = tgBotAccountService.getAllAccounts().size();
        tgApiMock.verify(botsCount, RequestPatternBuilder.newRequestPattern(
                RequestMethod.POST,
                urlPathMatching("/(.*)/setMyCommands")
        ));
    }
}
