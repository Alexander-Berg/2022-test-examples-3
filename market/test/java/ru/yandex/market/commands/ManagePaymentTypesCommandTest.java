package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.test.util.StringTestUtil.getString;

@DbUnitDataSet(
        before = "ManagePaymentTypesCommand/testManagePaymentTypes.before.csv"
)
public class ManagePaymentTypesCommandTest extends FunctionalTest {
    @Autowired
    private ManagePaymentTypesCommand command;

    @Autowired
    protected WireMockServer tarifficatorWireMockServer;

    @Autowired
    protected EnvironmentService environmentService;

    @BeforeEach
    protected void beforeEach() {
        tarifficatorWireMockServer.resetMappings();
        tarifficatorWireMockServer.removeServeEventsMatching(RequestPattern.everything());

        mockTarifficatorResponse();
    }

    @Test
    @DisplayName("Обновляем по shop_id всем, кроме cash-only")
    void testManagePaymentTypesByShopId() {
        CommandInvocation commandInvocation = new CommandInvocation("manage-payment-types",
                new String[]{"SHOP_ID", "1000,2000,5000", "PREPAYMENT_CARD,COURIER_CASH", "true", "true", "false"},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        command.executeCommand(commandInvocation, terminal);

        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1002/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups/5001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, postRequestedFor(urlEqualTo("/v2/shops/5000/region-groups/5001/payment-types?_user_id=11")));
    }

    @Test
    @DisplayName("Обновляем по shop_id всем, включая cash-only")
    void testManagePaymentTypesByShopIdWithCashOnly() {
        CommandInvocation commandInvocation = new CommandInvocation("manage-payment-types",
                new String[]{"SHOP_ID", "1000,2000,5000", "PREPAYMENT_CARD,COURIER_CASH", "true", "true", "true"},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        command.executeCommand(commandInvocation, terminal);

        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1002/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups/5001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/5000/region-groups/5001/payment-types?_user_id=11")));
    }

    @Test
    @DisplayName("Обновляем region_id всем, кроме cash-only")
    void testManagePaymentTypesByRegionId() {
        CommandInvocation commandInvocation = new CommandInvocation("manage-payment-types",
                new String[]{"LOCAL_REGION_ID", "3,213", "PREPAYMENT_CARD,COURIER_CASH", "true", "false"},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        command.executeCommand(commandInvocation, terminal);

        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1002/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups")));
        tarifficatorWireMockServer.verify(1, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(1, postRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups/5001/payment-types?_user_id=11")));
    }

    @Test
    @DisplayName("Никому не обновляем (все в блэклисте)")
    void testManagePaymentTypesByShopIdWithBlacklist() {
        environmentService.setValues("mbi.tms.payment.types.blacklist", List.of("1000", "2000", "5000"));

        CommandInvocation commandInvocation = new CommandInvocation("manage-payment-types",
                new String[]{"SHOP_ID", "1000,2000,5000", "PREPAYMENT_CARD,COURIER_CASH", "true", "false"},
                Collections.emptyMap());
        Terminal terminal = createTerminal();

        command.executeCommand(commandInvocation, terminal);

        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, postRequestedFor(urlEqualTo("/v2/shops/1000/region-groups/1002/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, postRequestedFor(urlEqualTo("/v2/shops/2000/region-groups/2001/payment-types?_user_id=11")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups")));
        tarifficatorWireMockServer.verify(0, getRequestedFor(urlEqualTo("/v2/shops/5000/region-groups/5001/payment-types?_user_id=11")));
    }

    private void mockTarifficatorResponse() {
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
    }

    private void mockGetRegionGroups(long shopId) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "ManagePaymentTypesCommand/getRegionGroups" + shopId + ".response.json"));
        tarifficatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups")
                .willReturn(response));
    }

    private void mockGetPaymentTypes(long shopId, long groupId) {
        ResponseDefinitionBuilder response = aResponse().withStatus(200)
                .withBody(getString(this.getClass(), "ManagePaymentTypesCommand/getPaymentTypes" + groupId + ".response.json"));
        tarifficatorWireMockServer.stubFor(get("/v2/shops/" + shopId + "/region-groups/" + groupId + "/payment-types?_user_id=11")
                .willReturn(response));
    }

    private void mockPostPaymentTypes(long shopId, long groupId) {
        ResponseDefinitionBuilder response = aResponse()
                .withBody(getString(this.getClass(), "ManagePaymentTypesCommand/postPaymentTypes" + groupId + ".response.json"))
                .withStatus(200);
        String request = getString(this.getClass(),
                "ManagePaymentTypesCommand/postPaymentTypes" + groupId + ".request.json");
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
