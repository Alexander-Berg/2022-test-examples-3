package ru.yandex.market.checkout.util.balance;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingXPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.base.Throwables.propagate;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static ru.yandex.market.checkout.util.matching.XPathMatcher.xPath;

@TestComponent
public class BalanceMockHelper {

    private static final DocumentBuilder XMLDOC_BUILDER = initXmlDocBuilder();
    private static final String FIRST_HASH = "params/param[1]/value/";
    private static final String SECOND_HASH = "params/param[2]/value/";

    @Autowired
    protected WireMockServer balanceMock;

    public static String getRetBody(BalanceXMLRPCMethod method,
                                    BalanceResponseVariant variant,
                                    Map<ResponseVariable, Object> variables
    ) throws IOException {
        String fileName = method.name() + (variant != null ? "_" + variant.name().toLowerCase() : "") + ".xml";
        return getStringBodyFromFile(fileName, variables);
    }

    /**
     * Проблема текущей реализации ReturnTestHelper и BalanceMockHelper в завязке на порядок вызовов баланса.
     * Иногда этот порядок недетерминирован и зависит от порядка элементов в коллекциях.
     * Хак для обхода серий вызовов над коллекциями объектов.
     * Мы просто пытаемся проверить, что следующий вызов отвечает ожиданиям, а если нет, то мы ищем его по всем вызовам.
     * Данный метод продвигает итератор (чтобы не ломать работу последующих проверок).
     */
    public static ServeEvent checkNextOrAnyBalanceSimpleCall(BalanceXMLRPCMethod method,
                                                             OneElementBackIterator<ServeEvent> eventsIter,
                                                             Map<?, ?> expectedValues) throws Exception {
        try {
            return checkNextBalanceSimpleCall(method, eventsIter, expectedValues, true);
        } catch (AssertionError error) {
            return eventsIter.getSource().stream()
                    .filter(event -> {
                        try {
                            checkBalanceSimpleCall(method, event.getRequest(), expectedValues, true);
                            return true;
                        } catch (Exception | AssertionError e) {
                            return false;
                        }
                    }).findAny()
                    .orElseThrow(() -> new RuntimeException("No event with method " + method
                            + " matching " + expectedValues));
        }
    }

    public static ServeEvent checkNextBalanceSimpleCall(BalanceXMLRPCMethod method,
                                                        Iterator<ServeEvent> eventsIter,
                                                        Map<?, ?> expectedValues,
                                                        boolean fullCheck) throws Exception {
        assertThat("Missing Balance.Simple call to " + method, eventsIter.hasNext(), Matchers.is(true));
        ServeEvent event = eventsIter.next();
        checkBalanceSimpleCall(method, event.getRequest(), expectedValues, fullCheck);
        return event;
    }

    public static String extractBalanceRequestMethodName(LoggedRequest request) throws Exception {
        Document doc = parseXmlString(request.getBodyAsString());
        String xpath = "/methodCall/methodName/text()";
        XPathExpression expr = XPathFactory.newInstance().newXPath().compile(xpath);
        return expr.evaluate(doc);
    }

    public static MappingBuilder createForXMLRPCMethod(final BalanceXMLRPCMethod method) {
        String path = Objects.equals(method.apiType, "BalanceSimple")
                ? "/simple/xmlrpc"
                : "/xmlrpc";
        return post(urlPathEqualTo(path))
                .withRequestBody(matchingXPath("/methodCall/methodName[text()=\"" + method.fullName() + "\"]"));
    }

    private static String getStringBodyFromFile(String fileName, Map<ResponseVariable, Object> vars) throws
            IOException {
        final String[] template = {IOUtils.toString(BalanceMockHelper.class.getResourceAsStream(fileName))};
        stream(ResponseVariable.values()).forEach(
                var -> template[0] = template[0].replace("{{" + var.name() + "}}",
                        Objects.toString((vars != null) ? vars.getOrDefault(var, var.defaultValue) : var
                                .defaultValue)));
        return template[0];
    }

    private static Document parseXmlString(String xml) throws SAXException, IOException {
        return XMLDOC_BUILDER.parse(new InputSource(new StringReader(xml)));
    }

    //------------------------------------------------------------------------------------------------------------------
    @Nonnull
    private static DocumentBuilder initXmlDocBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void checkBalanceSimpleCall(BalanceXMLRPCMethod method,
                                              LoggedRequest request,
                                              Map<?, ?> expectedValues,
                                              boolean fullCheck) throws Exception {
        Document doc = parseXmlString(request.getBodyAsString());
        Element methodCall = doc.getDocumentElement();
        String assertReason = method.fullName()
                + " request check failed. Actual request body was:\n"
                + request.getBodyAsString();
        assertThat(assertReason, methodCall, hasXPath("methodName", is(method.fullName())));
        assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/text()")); // TODO: научиться сверять
        // само значение токена

        if (expectedValues != null) {
            checkBalanceSimpleCallParams(methodCall, "params/param[2]/value/", expectedValues, assertReason, fullCheck);
        }
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public static void checkBalanceCall(BalanceXMLRPCMethod method, LoggedRequest request,
                                        Map<String, String> variablesForExpectedValues)
            throws Exception {
        Document doc = parseXmlString(request.getBodyAsString());
        Element methodCall = doc.getDocumentElement();
        String assertReason = method.fullName()
                + " request check failed. Actual request body was:\n"
                + request.getBodyAsString();
        assertThat(assertReason, methodCall, hasXPath("methodName", is(method.fullName())));

        Map<String, Map<String, String>> expectedValues = new HashMap<>();
        expectedValues.put(FIRST_HASH, new HashMap<>());
        expectedValues.put(SECOND_HASH, new HashMap<>());

        switch (method) {
            case FindClient:
                assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/struct/member/name/text()",
                        is("PassportID")));
                assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/struct/member/value/text()",
                        is(variablesForExpectedValues.get("passport_id"))));
                expectedValues = null;
                break;
            case CreateClient:
                assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/text()",
                        is(variablesForExpectedValues.get("passport_id"))));
                expectedValues.get(SECOND_HASH).put("IS_AGENCY", "0");
                expectedValues.get(SECOND_HASH).put("PHONE", "+71234567891");
                expectedValues.get(SECOND_HASH).put("CLIENT_TYPE_ID", "0");
                expectedValues.get(SECOND_HASH).put("EMAIL", "a@b.com");
                expectedValues.get(SECOND_HASH).put("NAME", "Leo Tolstoy");
                break;
            case CreateUserClientAssociation:
                assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/text()",
                        is(variablesForExpectedValues.get("passport_id"))));
                assertThat(assertReason, methodCall, hasXPath("params/param[2]/value/i4/text()",
                        is(variablesForExpectedValues.get("client_id"))));
                assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/text()",
                        is(variablesForExpectedValues.get("passport_id"))));
                expectedValues = null;
                break;
            case GetClientContracts:
                expectedValues.get(FIRST_HASH).put("ClientID", "5835538");
                expectedValues.get(FIRST_HASH).put("ContractType", "SPENDABLE");
                break;
            case CreatePerson:
                assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/text()",
                        is(variablesForExpectedValues.get("passport_id"))));
                assertThat(assertReason, methodCall,
                        hasXPath("params/param[2]/value/struct/member[name='invalid_bankprops']/value/text()",
                                is("0")));
                expectedValues.get(SECOND_HASH).put("fname", "lastName");
                expectedValues.get(SECOND_HASH).put("bik", "000000009");
                expectedValues.get(SECOND_HASH).put("mname", "middleName");
                expectedValues.get(SECOND_HASH).put("type", "ph");
                expectedValues.get(SECOND_HASH).put("client_id", variablesForExpectedValues.get("client_id"));
                expectedValues.get(SECOND_HASH).put("bankcity", "bankCity");
                expectedValues.get(SECOND_HASH).put("lname", "firstName");
                expectedValues.get(SECOND_HASH).put("bank", "bank");
                expectedValues.get(SECOND_HASH).put("phone", "+71234567891");
                expectedValues.get(SECOND_HASH).put("is_partner", "0");
                expectedValues.get(SECOND_HASH).put("corraccount", "0123456789");
                expectedValues.get(SECOND_HASH).put("email", "a@b.com");
                expectedValues.get(SECOND_HASH).put("account", "00000000000000000009");
                break;
            case CreateOffer:
                assertThat(assertReason, methodCall, hasXPath("params/param[1]/value/text()",
                        is(variablesForExpectedValues.get("passport_id"))));
                expectedValues.get(SECOND_HASH).put("manager_uid", "23087041");
                expectedValues.get(SECOND_HASH).put("currency", "RUR");
                expectedValues.get(SECOND_HASH).put("external_id", variablesForExpectedValues.get("external_id"));
                expectedValues.get(SECOND_HASH).put("services", "613");
                expectedValues.get(SECOND_HASH).put("nds", "0");
                expectedValues.get(SECOND_HASH).put("firm_id", "111");
                expectedValues.get(SECOND_HASH).put("client_id", variablesForExpectedValues.get("client_id"));
                expectedValues.get(SECOND_HASH).put("person_id", "6306941");
                break;
        }

        if (expectedValues != null) {
            for (String pathToCheck : expectedValues.keySet()) {
                Map<String, String> valuesForCheck = expectedValues.get(pathToCheck);
                if (valuesForCheck != null) {
                    checkBalanceSimpleCallParams(methodCall, pathToCheck, valuesForCheck, assertReason, false);
                }
            }
        }
    }

    private static void checkBalanceSimpleCallParams(Element root, String path, Map<?, ?> expectedValues,
                                                     String assertReason, boolean fullCheck) {
        expectedValues.forEach((fieldName, expectedValue) -> {
            String fieldPath = path + "struct/member[name='" + fieldName + "']/value";
            if (expectedValue instanceof Map) {
                checkBalanceSimpleCallParams(
                        root,
                        fieldPath + "/",
                        (Map) expectedValue,
                        assertReason,
                        fullCheck
                );
            } else if (expectedValue instanceof Collection) {
                String arrayPath = fieldPath + "/array/data";
                Collection<Map> expectedValuesMaps = (Collection<Map>) expectedValue;
                for (Map expectedMap : expectedValuesMaps) {
                    checkBalanceSimpleCallArrayItem(root, arrayPath, expectedMap, assertReason);
                }
                assertThat(
                        assertReason,
                        root,
                        hasXPath("count(" + arrayPath + "/*)", is(String.valueOf(expectedValuesMaps.size())))
                );
            } else {
                assertThat(assertReason, root, hasXPath(fieldPath, expectedValueMatcher(expectedValue)));
            }
        });
        if (fullCheck) {
            assertThat(assertReason, root, hasXPath("count(" + path + "struct/*)",
                    is(String.valueOf(expectedValues.size()))));
        }
    }

    private static void checkBalanceSimpleCallArrayItem(Element root, String arrayPath, Map<?, ?> expectedMap, String
            assertReason) {
        if (!expectedMap.isEmpty()) {
            assertThat(assertReason, root,
                    xPath(arrayPath + "/value/struct").hasNodes(hasItem(allOf(
                            expectedMap.entrySet().stream()
                                    .map(e -> hasXPath("member[name='" + e.getKey() + "']/value",
                                            expectedValueMatcher(e.getValue())))
                                    .collect(toList())
                    )))
            );
        }
    }

    private static Matcher<String> expectedValueMatcher(Object expectedValue) {
        return (expectedValue instanceof Matcher) ? (Matcher<String>) expectedValue :
                is(Objects.toString(expectedValue));
    }

    public void mockWholeBalance() {
        mockWholeBalance(null);
    }

    public void resetRequests() {
        balanceMock.resetRequests();
    }

    public void resetAll() {
        balanceMock.resetAll();
    }

    WireMockServer balanceMock() {
        return balanceMock;
    }

    public void mockWholeBalance(Map<ResponseVariable, Object> variables) {
        stream(BalanceXMLRPCMethod.values()).forEach(mtd -> mockBalanceXMLRPCMethod(mtd, variables));
    }

    public void mockBalanceXMLRPCMethod(BalanceXMLRPCMethod method,
                                        BalanceResponseVariant variant) {
        mockBalanceXMLRPCMethod(method, variant, null);
    }

    public void mockBalanceXMLRPCMethod(BalanceXMLRPCMethod method,
                                        BalanceResponseVariant variant,
                                        Map<ResponseVariable, Object> variables) {
        try {
            String retBody = getRetBody(method, variant, variables);
            balanceMock.stubFor(createForXMLRPCMethod(method).willReturn(aResponse().withBody(retBody)));
        } catch (IOException io) {
            throw propagate(io);
        }
    }

    public void mockBalanceXMLRPCMethod(BalanceXMLRPCMethod method,
                                        Map<ResponseVariable, Object> variables) {
        mockBalanceXMLRPCMethod(method, null, variables);
    }

    public enum BalanceXMLRPCMethod {
        FindClient("Balance2"),
        UpdatePayment("Balance2"),
        CreateUserClientAssociation("Balance2"),
        CreateClient("Balance2"),
        GetClientPersons("Balance2"),
        GetPerson("Balance2"),
        CreateOffer("Balance2"),
        GetClientContracts("Balance2"),
        CreatePerson("Balance2"),
        GetContractInfoByTerminal("Balance2");

        private final String apiType;

        BalanceXMLRPCMethod(String apiType) {
            this.apiType = apiType;
        }

        public String fullName() {
            return apiType + "." + name();
        }
    }

    public enum BalanceResponseVariant {
        FAIL,
        FOR_RETURN,
        NO_MATCH,
        WITH_ORDER_ID,
        IS_NOT_ACTIVE,
        IS_ACTIVE_WITH_ORDER_ID,
        WITH_PURPOSE
    }
}
