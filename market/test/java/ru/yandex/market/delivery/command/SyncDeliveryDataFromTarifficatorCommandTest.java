package ru.yandex.market.delivery.command;

import java.io.PrintWriter;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

@ExtendWith(MockitoExtension.class)
public class SyncDeliveryDataFromTarifficatorCommandTest extends FunctionalTest {

    @Autowired
    private SyncDeliveryDataFromTarifficatorCommand command;
    @Autowired
    private WireMockServer tarificatorWireMockServer;
    private Terminal terminal;

    @BeforeEach
    public void configs() {
        terminal = Mockito.mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));

        tarificatorWireMockServer.resetMappings();
        tarificatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());
        prepareMocksForTarifficator();
    }

    @Test
    @DbUnitDataSet(
            before = "testSaveShopDeliveryState.cpc.before.csv",
            after = "testSaveShopDeliveryState.after.csv"
    )
    void testSyncCpcDeliveryData() {
        CommandInvocation commandInvocation = new CommandInvocation(
                SyncDeliveryDataFromTarifficatorCommand.COMMAND_NAME,
                new String[]{"cpc"},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "testSaveShopDeliveryState.dsbs.before.csv",
            after = "testSaveShopDeliveryState.after.csv"
    )
    void testSyncDsbsDeliveryData() {
        CommandInvocation commandInvocation = new CommandInvocation(
                SyncDeliveryDataFromTarifficatorCommand.COMMAND_NAME,
                new String[]{"dropship_by_seller"},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
    }

    @Test
    @DbUnitDataSet(
            before = "testSaveShopDeliveryState.cpc.before.csv",
            after = "testSaveShopDeliveryState.after.csv"
    )
    void testSyncByShopIdData() {
        CommandInvocation commandInvocation = new CommandInvocation(
                SyncDeliveryDataFromTarifficatorCommand.COMMAND_NAME,
                new String[]{"1000,2000"},
                Collections.emptyMap());

        command.executeCommand(commandInvocation, terminal);
    }

    private void prepareMocksForTarifficator() {
        tarificatorWireMockServer.stubFor(post("/v2/shops/delivery/state")
                .withRequestBody(equalToJson("{\"shopIds\": [1000, 2000]}"))
                .willReturn(aResponse()
                        .withBody(getString(this.getClass(), "json/deliveryStateForShops.response.json"))
                        .withStatus(200)));
    }
}
