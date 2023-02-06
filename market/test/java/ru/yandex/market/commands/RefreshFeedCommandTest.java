package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.StringUtils;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.shop.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Date: 07.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class RefreshFeedCommandTest extends FunctionalTest {

    @Autowired
    private RefreshFeedCommand refreshFeedCommand;
    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;
    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @BeforeEach
    void init() {
        when(terminal.getWriter())
                .thenReturn(printWriter);
    }

    @DisplayName("Принудительное обновление информации по фидам через самовар для белых")
    @DbUnitDataSet(before = "RefreshFeedCommandTest.before.csv")
    @ParameterizedTest(name = "feedId = {0}")
    @CsvSource({
            "1,ASSORTMENT,774,SHOP,test.login,test.password,test.feed.url",
            "4,ASSORTMENT,774,SHOP,,,test.feed.url",
            "5,ASSORTMENT,775,SHOP,,,test.feed.url",
            "5,STOCKS,775,SHOP,,,https://market.net/stocks_shop",
            "5,PRICES,775,SHOP,42,no,https://market.net/price_shop",
            "12,ASSORTMENT,113,SUPPLIER,,,test.feed.url",
            "15,ASSORTMENT,115,SUPPLIER,,,test.feed.url",
            "15,STOCKS,115,SUPPLIER,,,https://market.net/stocks",
            "15,PRICES,115,SUPPLIER,42,no,https://market.net/price_100"
    })
    void executeCommand_testCorrectShopFeedForSamovar_invokeLogbroker(String feedId, String feedType,
                                                                      String partnerId, String campaignType,
                                                                      String login, String password,
                                                                      String url) {
        CommandInvocation commandInvocation = new CommandInvocation("refresh",
                new String[]{feedId, feedType, partnerId, campaignType},
                Collections.emptyMap());

        refreshFeedCommand.executeCommand(commandInvocation, terminal);

        var argumentCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1))
                .publishEvent(argumentCaptor.capture());
        assertEquals(1, argumentCaptor.getAllValues().size());

        FeedProcessorUpdateRequestEvent samovarEvent = argumentCaptor.getValue();
        assertTrue(samovarEvent.toString().contains(url));

        var eventLogin = samovarEvent.getPayload().getFeed().getLogin();
        var eventPassword = samovarEvent.getPayload().getFeed().getPassword();
        if (login == null) {
            assertEquals(StringUtils.EMPTY, eventLogin);
            assertEquals(StringUtils.EMPTY, eventPassword);
        } else {
            assertEquals(login, eventLogin);
            assertEquals(password, eventPassword);
        }
    }

    @DisplayName("Невозможность принудительного обновления информации по фидам через самовар для белых")
    @DbUnitDataSet(before = "RefreshFeedCommandTest.before.csv")
    @ParameterizedTest(name = "feedId = {0}")
    @CsvSource({
            "2,ASSORTMENT,774,SHOP",
            "3,ASSORTMENT,774,SHOP",
            "6,ASSORTMENT,776,SHOP",
            "7,ASSORTMENT,777,SHOP"
    })
    void executeCommand_testWrongShopFeedForSamovar_threeTimeInvokeLogbroker(String feedId, String feedType,
                                                                             String partnerId, String campaignType) {
        CommandInvocation commandInvocation = new CommandInvocation("refresh",
                new String[]{feedId, feedType, partnerId, campaignType},
                Collections.emptyMap());

        refreshFeedCommand.executeCommand(commandInvocation, terminal);

        verify(feedProcessorUpdateLogbrokerEventPublisher, never()).publishEvent(any());
    }
}
