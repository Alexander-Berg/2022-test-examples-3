package ru.yandex.market.tsum.api;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.market.tsum.balancer.form.FqdnNameValidator;
import ru.yandex.market.tsum.balancer.form.JugglerRespsValidator;
import ru.yandex.market.tsum.balancer.form.SubmitResult;
import ru.yandex.market.tsum.balancer.submit.FormField;
import ru.yandex.market.tsum.clients.staff.StaffApiClient;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.misc.test.Assert.assertContains;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class FormValidatorControllerTest {
    private MockMvc mockMvc;

    @Configuration
    static class TestConfiguration {
        @Bean
        public StaffApiClient staffApiClient() {
            return new StaffApiClient("https://staff-api.yandex-team.ru/", "");
        }

        @Bean
        public Session startrekSession() {
            return Mockito.mock(Session.class);
        }

        @Bean
        public FqdnNameValidator fqdnNameValidator() {
            FqdnNameValidator validatorMock = Mockito.spy(FqdnNameValidator.class);
            doReturn("").when(validatorMock).checkDomainNameNonExists(anyString());
            return validatorMock;
        }

        @Bean
        public JugglerRespsValidator jugglerRespsValidator() {
            JugglerRespsValidator validatorMock = Mockito.spy(JugglerRespsValidator.class);
            return validatorMock;
        }
    }

    @Autowired
    private Session startrekSession;

    @Autowired
    private FqdnNameValidator fqdnNameValidator;

    @Autowired
    private JugglerRespsValidator jugglerRespsValidator;

    private FormValidatorController controller;

    @Before
    public void init() {
        controller = new FormValidatorController(
            startrekSession, fqdnNameValidator, jugglerRespsValidator
        );
        ReflectionTestUtils.setField(controller, "startrekUrl", "https://st.yandex-team.ru");
        ReflectionTestUtils.setField(controller, "tsumUrl", "https://tsum.yandex-team.ru/");
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .build();
    }

    @Test
    public void requestIsValid() throws Exception {

        String jsonRequest = readFile("formvalidator/validatorRequestDefault.json");
        String expectedAnswer = "{\"status\":\"OK\"}";

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(expectedAnswer, content);

    }

    @Test
    public void requestIsNotValid() throws Exception {

        String jsonRequest = readFile("formvalidator/validatorRequestNotValid.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"errors\":{\"fqdn\":[\"Enter FQDN as one line, separated by commas. Do not use spaces, tabs" +
                " or symbols of new line.\"],\"httpPort\":[\"Http port and https port both have the same value, " +
                "but have to is different.\"],\"realServers\":[\"Enter a list of services in one line, separated " +
                "by commas. Do not use spaces, tabs or symbols of new line.\"],\"sslAltnames\":[\"Enter a list " +
                "of aliases in one line, separated by commas. Do not use spaces, tabs or symbol of new lines.\"]}," +
                "\"status\":\"ERROR\"}",
            content
        );
    }

    @Test
    public void requestSslAliasesEmpty() throws Exception {

        String jsonRequest = readFile("formvalidator/validatorRequestSslAliasesIsEmpty.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("{\"status\":\"OK\"}", result.getResponse().getContentAsString());
    }

    @Test
    public void fqdnNotResolve() throws Exception {

        String jsonRequest = readFile("formvalidator/validatorRequestNotResolveFqdn.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"status\":\"OK\"}",
            content
        );
    }

    @Test
    public void wrongReals() throws Exception {

        String jsonRequest = readFile("formvalidator/validatorRequestWrongReals.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"errors\":{\"realServers\":[\"Name of real !production_cs_dashboard_sas has " +
                "wrong symbol. Use only: a-z0-9_.-\",\"Name of real productions_cs_dashboard_vl!a has wrong symbol. " +
                "Use only: a-z0-9_.-\"]},\"status\":\"ERROR\"}",
            content
        );
    }

    @Test
    public void conductorNiceReals() throws Exception {

        String jsonRequest = readFile(
            "formvalidator/validatorRequestConductorReals.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"status\":\"OK\"}",
            content
        );
    }

    @Test
    public void nannyNiceReals() throws Exception {

        String jsonRequest = readFile("formvalidator/validatorRequestNannyReals.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"status\":\"OK\"}",
            content
        );
    }

    @Test
    public void fqdnNiceReals() throws Exception {

        String jsonRequest = readFile(
            "formvalidator/validatorRequestFqdnReals.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"status\":\"OK\"}",
            content
        );
    }

    @Test
    public void staffGroupNice() throws Exception {
        doReturn(true).when(jugglerRespsValidator).checkStaffGroup(anyString());
        String jsonRequest = readFile("formvalidator/validatorRequestStaffNice.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"status\":\"OK\"}",
            content
        );
    }

    @Test
    public void staffGroupBad() throws Exception {
        doReturn(false).when(jugglerRespsValidator).checkStaffGroup(anyString());
        String jsonRequest = readFile("formvalidator/validatorRequestStaffBad.json");

        MvcResult result = mockMvc.perform(post("/sre/form/validate")
            .contentType(MediaType.APPLICATION_JSON)
            .characterEncoding("utf-8")
            .content(jsonRequest))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(
            "{\"errors\":{\"resps\":" +
                "[\"Group unknown_group_name not found on staff service.\"]},\"status\":\"ERROR\"}",
            content
        );
    }

    @Test
    public void testSubmitBalancerRequest() throws Exception {
        Gson gson = new Gson();
        String jsonRequest = readFile("formvalidator/submitData.json");
        Type type = new TypeToken<HashMap<String, FormField>>(){}.getType();
        HashMap<String, FormField> formFields = gson.fromJson(jsonRequest, type);
        Issue issue = new Issue(
            "1111",
            new URI("https://st.yandex-team.ru/TEST-1111"),
            "TEST-1111",
            "Test issue",
            12345,
            Cf.map("SomeData", "OtherData"),
            startrekSession
        );
        given(startrekSession.issues()).willReturn(Mockito.mock(Issues.class));
        given(startrekSession.issues().create(any())).willReturn(issue);
        MockHttpServletRequestBuilder resultBuilder = multipart("/sre/form/submit")
            .contentType("multipart/form-data")
            .characterEncoding("utf-8")
            .accept(MediaType.APPLICATION_JSON)
            .header("X-Validation-Status", "success");

        for (Map.Entry<String, FormField> entry : formFields.entrySet()) {
            resultBuilder.param(entry.getKey(), gson.toJson(entry.getValue()));
        }

        MvcResult result = mockMvc.perform(resultBuilder)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        SubmitResult submitResult = new Gson().fromJson(content, SubmitResult.class);
    }

    @Test
    public void testSubmitBalancerRequestHttpsPortOnly() throws Exception {
        Gson gson = new Gson();
        String jsonRequest = readFile("formvalidator/submitDataHttpsPortOnly.json");
        Type type = new TypeToken<HashMap<String, FormField>>(){}.getType();
        HashMap<String, FormField> formFields = gson.fromJson(jsonRequest, type);
        Issue issue = new Issue(
            "1111",
            new URI("https://st.yandex-team.ru/TEST-1111"),
            "TEST-1111",
            "Test issue",
            12345,
            Cf.map("SomeData", "OtherData"),
            startrekSession
        );
        given(startrekSession.issues()).willReturn(Mockito.mock(Issues.class));
        given(startrekSession.issues().create(any())).willReturn(issue);
        MockHttpServletRequestBuilder resultBuilder = multipart("/sre/form/submit")
            .contentType("multipart/form-data")
            .characterEncoding("utf-8")
            .accept(MediaType.APPLICATION_JSON)
            .header("X-Validation-Status", "success");

        for (Map.Entry<String, FormField> entry : formFields.entrySet()) {
            resultBuilder.param(entry.getKey(), gson.toJson(entry.getValue()));
        }

        MvcResult result = mockMvc.perform(resultBuilder)
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        SubmitResult submitResult = new Gson().fromJson(content, SubmitResult.class);
    }

    @Test
    public void testSubmitBalancerDescription() throws Exception {
        Gson gson = new Gson();
        String jsonRequest = readFile("formvalidator/submitData.json");
        Type type = new TypeToken<HashMap<String, FormField>>(){}.getType();
        Map<String, FormField> formFields = gson.fromJson(jsonRequest, type);
        Map<String, String> formData = new HashMap<>();
        for (Map.Entry<String, FormField> entry: formFields.entrySet()) {
            formData.put(entry.getKey(), gson.toJson(entry.getValue()));
        }
        Map<String, String> templateArgs = controller.getTemplateArgs(formData);
        Map<String, String> pipelineUriParams = controller.getPipelineUriParams(formData, "TEST-11111");
        String pipelineUri = controller.getPipelineUri(pipelineUriParams);
        templateArgs.put("pipelineUrl", pipelineUri);
        String description = controller.getDescription(templateArgs);

        assertContains(description, "slb.description = balancer form test");
        assertContains(description, "slb.fqdn = test10.tst.vs.market.yandex.net");
        assertContains(description, "slb.port = 80");
        assertContains(description, "slb.type = Внутренний");
        assertContains(description, "slb.httpsPort = ");
        assertContains(description, "slb.realPort = ");
        assertContains(description, "slb.redirect_to_https = Нет");
        assertContains(description, "slb.ssl_backends = Нет");
        assertContains(description, "slb.ssl_externalca = Нет");
        assertContains(description, "slb.cnames = ");
        assertContains(description, "slb.ssl_altnames = ");
        assertContains(description, "slb.type_backends = HOST");
        assertContains(description, "slb.real_servers = dev-null01vd.market.yandex.net");
        assertContains(description, "slb.offset_port = ");
        assertContains(description, "slb.health_check_url = /ping");
        assertContains(description, "slb.health_check_type = 200-ый код ответа");
        assertContains(description, "slb.health_check_text = ");
        assertContains(description, "slb.rps = 1");
        assertContains(description, "slb.ip_version = IPv6-only");
        assertContains(description, "access.human = ");
        assertContains(description, "access.machine = ");
        assertContains(description, "monitor.needMonitor = Нет");
        assertContains(description, "monitor.resps = ");
        assertContains(description, "https://tsum.yandex-team.ru/pipe/projects/sre/release/new/balancer?formData=%7B%2217952fb1-d0e7-4da5-b044-e96025450fd5.healthCheckUrl%22%3A%22%2Fping%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.humanAccess%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.httpsPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.sslExternalCa%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.description%22%3A%22balancer+form+test%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.answer_choices_133374%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.realServers%22%3A%22dev-null01vd.market.yandex.net%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.rps%22%3A%221%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.sslAltnames%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.realPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.balancerType%22%3A%22%D0%92%D0%BD%D1%83%D1%82%D1%80%D0%B5%D0%BD%D0%BD%D0%B8%D0%B9%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.healthCheckText%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.jugglerMonitor%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.typeOfBackends%22%3A%22HOST%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.httpPort%22%3A%2280%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.startrekTicketKey%22%3A%22TEST-11111%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.offsetPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.ipVersion%22%3A%22IPv6-only%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.sslBackends%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.healthCheckType%22%3A%22200-%D1%8B%D0%B9+%D0%BA%D0%BE%D0%B4+%D0%BE%D1%82%D0%B2%D0%B5%D1%82%D0%B0%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.fqdn%22%3A%22test10.tst.vs.market.yandex.net%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.redirectToHttps%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.machineAccess%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.service_already_work_boolean%22%3A%22%D0%94%D0%B0%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.resps%22%3A%22%22%7D");
    }

    @Test
    public void testSubmitBalancerDescriptionHttpsPortOnly() throws Exception {
        Gson gson = new Gson();
        String jsonRequest = readFile("formvalidator/submitDataHttpsPortOnly.json");
        Type type = new TypeToken<HashMap<String, FormField>>(){}.getType();
        Map<String, FormField> formFields = gson.fromJson(jsonRequest, type);
        Map<String, String> formData = new HashMap<>();
        for (Map.Entry<String, FormField> entry: formFields.entrySet()) {
            formData.put(entry.getKey(), gson.toJson(entry.getValue()));
        }
        Map<String, String> templateArgs = controller.getTemplateArgs(formData);
        Map<String, String> pipelineUriParams = controller.getPipelineUriParams(formData, "TEST-11111");
        String pipelineUri = controller.getPipelineUri(pipelineUriParams);
        templateArgs.put("pipelineUrl", pipelineUri);
        String description = controller.getDescription(templateArgs);

        assertContains(description, "slb.description = balancer form test");
        assertContains(description, "slb.fqdn = test10.tst.vs.market.yandex.net");
        assertContains(description, "slb.port = ");
        assertContains(description, "slb.type = Внутренний");
        assertContains(description, "slb.httpsPort = 443");
        assertContains(description, "slb.realPort = ");
        assertContains(description, "slb.redirect_to_https = Да");
        assertContains(description, "slb.ssl_backends = Нет");
        assertContains(description, "slb.ssl_externalca = Нет");
        assertContains(description, "slb.cnames = ");
        assertContains(description, "slb.ssl_altnames = ");
        assertContains(description, "slb.type_backends = HOST");
        assertContains(description, "slb.real_servers = dev-null01vd.market.yandex.net");
        assertContains(description, "slb.offset_port = ");
        assertContains(description, "slb.health_check_url = /ping");
        assertContains(description, "slb.health_check_type = 200-ый код ответа");
        assertContains(description, "slb.health_check_text = ");
        assertContains(description, "slb.rps = 1");
        assertContains(description, "slb.ip_version = IPv6-only");
        assertContains(description, "access.human = ");
        assertContains(description, "access.machine = ");
        assertContains(description, "monitor.needMonitor = Нет");
        assertContains(description, "monitor.resps = ");
        assertContains(description, "https://tsum.yandex-team.ru/pipe/projects/sre/release/new/balancer?formData=%7B%2217952fb1-d0e7-4da5-b044-e96025450fd5.healthCheckUrl%22%3A%22%2Fping%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.humanAccess%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.httpsPort%22%3A%22443%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.sslExternalCa%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.description%22%3A%22balancer+form+test%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.answer_choices_133374%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.realServers%22%3A%22dev-null01vd.market.yandex.net%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.rps%22%3A%221%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.sslAltnames%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.realPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.balancerType%22%3A%22%D0%92%D0%BD%D1%83%D1%82%D1%80%D0%B5%D0%BD%D0%BD%D0%B8%D0%B9%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.healthCheckText%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.jugglerMonitor%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.typeOfBackends%22%3A%22HOST%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.httpPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.startrekTicketKey%22%3A%22TEST-11111%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.offsetPort%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.ipVersion%22%3A%22IPv6-only%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.sslBackends%22%3A%22%D0%9D%D0%B5%D1%82%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.healthCheckType%22%3A%22200-%D1%8B%D0%B9+%D0%BA%D0%BE%D0%B4+%D0%BE%D1%82%D0%B2%D0%B5%D1%82%D0%B0%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.fqdn%22%3A%22test10.tst.vs.market.yandex.net%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.redirectToHttps%22%3A%22%D0%94%D0%B0%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.machineAccess%22%3A%22%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.service_already_work_boolean%22%3A%22%D0%94%D0%B0%22%2C%2217952fb1-d0e7-4da5-b044-e96025450fd5.resps%22%3A%22%22%7D Адрес пайплайна для создания балансера");
    }

    private static String readFile(String path) throws IOException {
        return Resources.toString(Resources.getResource(path), Charsets.UTF_8);
    }
}
