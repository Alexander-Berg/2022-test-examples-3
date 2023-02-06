package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.shop.FunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

public class SetDeliveryRegionPaymentSettingsCommandTest extends FunctionalTest {
    @Autowired
    private SetDeliveryRegionPaymentSettingsCommand command;

    @Autowired
    protected WireMockServer tarifficatorWireMockServer;

    @BeforeEach
    protected void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
    }

    @Test
    @DbUnitDataSet(
            before = "SetDeliveryRegionPaymentSettingsCommand/testSetDeliveryRegionPaymentSettings.before.csv"
    )
    void testSetDeliveryRegionPaymentSettings() {
        // multiple groups
        mockGetRegionGroups(1000L);
        mockGetPaymentTypes(1000L, 1001L);
        mockPostPaymentTypes(1000L, 1001L);
        mockPostPaymentTypes(1000L, 1002L);

        // failed dbs partner
        mockGetRegionGroups(2000L);
        mockGetPaymentTypes(2000L, 2001L);
        mockPostPaymentTypes(2000L, 2001L);

        // cash only
        mockGetRegionGroups(5000L);
        mockGetPaymentTypes(5000L, 5001L);

        CommandInvocation commandInvocation = new CommandInvocation("set-delivery-region-payment-settings",
                new String[]{},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        command.executeCommand(commandInvocation, terminal);
    }

    private void mockGetRegionGroups(long shopId) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "SetDeliveryRegionPaymentSettingsCommand/getRegionGroups" + shopId + ".response.json"));
        tarifficatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups")
                .willReturn(response));
    }

    private void mockGetPaymentTypes(long shopId, long groupId) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "SetDeliveryRegionPaymentSettingsCommand/getPaymentTypes" + groupId + ".response.json"));
        tarifficatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups/" + groupId + "/payment-types?_user_id=11")
                .willReturn(response));
    }

    private void mockPostPaymentTypes(long shopId, long groupId) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200);
        String request = getString(this.getClass(),
                "SetDeliveryRegionPaymentSettingsCommand/postPaymentTypes" + groupId + ".request.json");
        tarifficatorWireMockServer.stubFor(post("/v2/shops/" + shopId + "/region-groups/" + groupId + "/payment-types?_user_id=11")
                .withRequestBody(containing(new String(request.getBytes(StandardCharsets.UTF_8))))
                .willReturn(response));
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }
}
