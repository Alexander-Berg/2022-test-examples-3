package ru.yandex.market.wms.receiving.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.wms.common.spring.utils.columnFilters.UuidFieldFilter;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;
import ru.yandex.market.wms.receiving.config.ServiceBusConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@ContextConfiguration(classes = ServiceBusConfiguration.class)
public class InboundIdentityRpcTest extends ReceivingIntegrationTest {

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/db/receipt-detail-identities-new.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = UuidFieldFilter.class)
    public void createNew() throws Exception {
        createInboundOkCall(
                "inbound/create/request/new-instances.json",
                "inbound/create/response/first-receipt.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/db/identities-empty.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void duplicatedCis() throws Exception {
        callCreateInboundBadRequest(
                "inbound/create/request/duplicated-instances.json",
                "400 BAD_REQUEST \\\"Duplicated identities in request: IdentityFrontInfoDto"
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/db/identities-empty.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void emptyInstanceList() throws Exception {
        createInboundOkCall(
                "inbound/create/request/empty-instances.json",
                "inbound/create/response/first-receipt.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/db/identities-empty.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void emptyIdentityMap() throws Exception {
        createInboundOkCall(
                "inbound/create/request/empty-identity-maps.json",
                "inbound/create/response/first-receipt.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-partner-facility-control.xml")
    @ExpectedDatabase(value = "/inbound/db/identities-empty.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    public void unsupportedIdentityNameRegexImei() throws Exception {
        callCreateInboundBadRequest(
                "inbound/create/request/wrong-regex-cis.json",
                "400 BAD_REQUEST \\\"Invalid format of identity: regex not matched\\\""
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-partner-facility-control.xml")
    @ExpectedDatabase(value = "/inbound/db/receipt-detail-identities-new.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = UuidFieldFilter.class)
    public void unsupportedIdentityName() throws Exception {
        createInboundOkCall(
                "inbound/create/request/unsupported-identity.json",
                "inbound/create/response/first-receipt.json"
        );
    }

    @Test
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "enterpriseConnection")
    @DatabaseSetup(value = "/items/db/initial.xml", connection = "wmwhseConnection")
    @DatabaseSetup(value = "/inbound/db/storer-sku-and-related.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/inbound/db/receipt-detail-identities-new.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED,
            columnFilters = UuidFieldFilter.class)
    public void emptyIdentityKeyOrValue() throws Exception {
        createInboundOkCall(
                "inbound/create/request/empty-identity-key-or-value.json",
                "inbound/create/response/first-receipt.json"
        );
    }

    private void createInboundOkCall(String request, String expectedResponse) throws Exception {
        okRequest("/INFOR_SCPRD_wmwhse1/inbound", request, expectedResponse);
    }

    private void callCreateInboundBadRequest(String request, String expectedMessage) throws Exception {
        badRequest("/INFOR_SCPRD_wmwhse1/inbound", request, expectedMessage);
    }

    private void okRequest(String path, String request, String expectedResponse) throws Exception {
        final String result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertEquals(getFileContent(expectedResponse), result, JSONCompareMode.NON_EXTENSIBLE);
    }

    private void badRequest(String path, String request, String expectedMessage) throws Exception {
        final String result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat("Response body", result, containsString(expectedMessage));
    }
}
