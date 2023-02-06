package ru.yandex.direct.balance.client;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.http.entity.ContentType;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.parser.XmlRpcRequestParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import ru.yandex.direct.balance.client.exception.BalanceClientException;
import ru.yandex.direct.balance.client.model.request.CreateClientRequest;
import ru.yandex.direct.balance.client.model.request.GetFirmCountryCurrencyRequest;
import ru.yandex.direct.balance.client.model.response.BalanceBankDescription;
import ru.yandex.direct.balance.client.model.response.ClientNdsItem;
import ru.yandex.direct.balance.client.model.response.ClientPassportInfo;
import ru.yandex.direct.balance.client.model.response.DirectDiscountItem;
import ru.yandex.direct.balance.client.model.response.FirmCountryCurrencyItem;
import ru.yandex.direct.balance.client.model.response.LinkedClientsItem;
import ru.yandex.direct.test.utils.MockedHttpWebServerExtention;

import static java.util.stream.Collectors.toList;
import static org.apache.xmlrpc.util.SAXParsers.newXMLReader;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.balance.client.BalanceXmlRpcClient.getXmlRpcClientConfig;

class BalanceClientTest {
    private static final String HTTP_PATH = "/xmlrpc";
    private static final Integer SERVICE_ID = 7;

    @RegisterExtension
    static MockedHttpWebServerExtention server = new MockedHttpWebServerExtention(ContentType.APPLICATION_XML);

    private BalanceClient client;
    private XmlRpcClientConfigImpl config;

    @BeforeEach
    void setUp() throws MalformedURLException {
        BalanceXmlRpcClientConfig config = new BalanceXmlRpcClientConfig(new URL(server.getServerURL() + HTTP_PATH));

        this.config = getXmlRpcClientConfig(config);
        client = new BalanceClient(new BalanceXmlRpcClient(config), null);
    }

    @Test
    void testError404() {
        assertThatCode(() -> {
            server.addResponse("/wrongpath", "");
            client.getLinkedClients(List.of(0));
        }).isInstanceOf(BalanceClientException.class);
    }

    @Test
    void testGetLinkedClients() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.GetLinkedClients"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><string>GROUP_ID\tCLIENT_ID\tLINK_TYPE\tBRAND_CLIENT_ID\n"
                        + "261567\t1234\t7\t123456\n"
                        + "258910\t6543\t7\t654321\n"
                        + "178338\t987\t7\t98765\n"
                        + "</string></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");

        ArrayList<Integer> linkTypes = new ArrayList();
        linkTypes.add(7);
        List<LinkedClientsItem> directBrands = client.getLinkedClients(linkTypes);
        LinkedClientsItem firstBrand = new LinkedClientsItem().withClientId(1234L).withBrandClientId(123456L);
        LinkedClientsItem secondBrand = new LinkedClientsItem().withClientId(6543L).withBrandClientId(654321L);
        LinkedClientsItem thirdBrand = new LinkedClientsItem().withClientId(987L).withBrandClientId(98765L);

        assertThat("получаем верный список брендов", directBrands, containsInAnyOrder(Arrays.asList(
                beanDiffer(firstBrand),
                beanDiffer(secondBrand),
                beanDiffer(thirdBrand))));
        checkRequest("Balance2.GetLinkedClients", Collections.singletonMap("LinkTypes", new Object[]{7}));
    }

    @Test
    void testCreateClient() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.CreateClient"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><array><data>\n"
                        + "<value><int>0</int></value>\n"
                        + "<value><string>SUCCESS</string></value>\n"
                        + "<value><int>12345678</int></value>\n"
                        + "</data></array></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        Long operatorUid = 1L;
        CreateClientRequest request = new CreateClientRequest().withOperatorUid(operatorUid).withAgencyId(12345L);
        Long clientId = client.createClient(request);
        assertThat("Получили верный id клиента", clientId, equalTo(12345678L));

        checkRequest("Balance2.CreateClient",
                Arrays.asList(operatorUid.toString(), Collections.singletonMap("AGENCY_ID", request.getAgencyId())));
    }

    @Test
    void testGetClientNds() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.GetClientNDS"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><string>CLIENT_ID\tDT\tNDS_PCT\tNSP_PCT\n"
                        + "12345678\t2003-01-01\t20\t5\n"
                        + "12345678\t2004-01-01\t18\t0\n"
                        + "</string></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");

        int clientId = 12345678;
        List<ClientNdsItem> clientNdsItems = client.getClientNds(SERVICE_ID, clientId);
        ClientNdsItem firstNds = new ClientNdsItem()
                .withClientId(clientId)
                .withDt(LocalDate.parse("2003-01-01"))
                .withNdsPct(new BigDecimal(20));
        ClientNdsItem secondNds = new ClientNdsItem()
                .withClientId(clientId)
                .withDt(LocalDate.parse("2004-01-01"))
                .withNdsPct(new BigDecimal(18));

        assertThat("получаем верный список НДС", clientNdsItems, containsInAnyOrder(Arrays.asList(
                beanDiffer(firstNds),
                beanDiffer(secondNds))));

        checkRequest("Balance2.GetClientNDS",
                ImmutableMap.<String, Object>builder()
                        .put("Mod", Integer.MAX_VALUE)
                        .put("Rem", clientId)
                        .put("ServiceID", SERVICE_ID)
                        .build());
    }

    @Test
    void testGetClientNdsEmpty() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.GetClientNDS"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><string>CLIENT_ID\tDT\tNDS_PCT\tNSP_PCT\n"
                        + "</string></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");

        int clientId = 12345678;
        List<ClientNdsItem> clientNdsItems = client.getClientNds(SERVICE_ID, clientId);
        assertThat("Получили пустой список элементов", clientNdsItems, hasSize(0));

        checkRequest("Balance2.GetClientNDS",
                ImmutableMap.<String, Object>builder()
                        .put("Mod", Integer.MAX_VALUE)
                        .put("Rem", clientId)
                        .put("ServiceID", SERVICE_ID)
                        .build());
    }

    @Test
    void testGetDirectDiscount() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.GetDirectDiscount"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><string>CLIENT_ID\tSTART_DT\tEND_DT\tDISCOUNT\tUPDATE_DT\n"
                        + "12345678\t2003-01-01\t2003-01-30\t34\t2003-02-01T00:00:01"
                        + "</string></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        int mod = 12345678;
        int rem = 0;
        List<DirectDiscountItem> clientNdsItems = client.getDirectDiscount(mod, rem);
        DirectDiscountItem item = new DirectDiscountItem()
                .withClientId(12345678)
                .withStartDt(LocalDate.parse("2003-01-01"))
                .withEndDt(LocalDate.parse("2003-01-30"))
                .withDiscount(new BigDecimal(34))
                .withUpdateDt(LocalDateTime.parse("2003-02-01T00:00:01"));
        assertThat("Получили корректный список элементов", clientNdsItems, contains(beanDiffer(item)));

        checkRequest("Balance2.GetDirectDiscount",
                ImmutableMap.<String, Object>builder()
                        .put("Mod", mod)
                        .put("Rem", rem)
                        .build());
    }

    @Test
    void testGetFirmCountryCurrency() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.GetFirmCountryCurrency"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><array><data>\n"
                        + "<value><int>0</int></value>\n"
                        + "<value><string>SUCCESS</string></value>\n"
                        + "<value><array><data>\n"
                        + "<value><struct>\n"
                        + "<member>\n"
                        + "<name>region_id</name>\n"
                        + "<value><int>225</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>resident</name>\n"
                        + "<value><int>1</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>agency</name>\n"
                        + "<value><int>0</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>region_name_en</name>\n"
                        + "<value><string>Russia</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>currency</name>\n"
                        + "<value><string>RUB</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>convert_type_modify</name>\n"
                        + "<value><int>1</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>firm_id</name>\n"
                        + "<value><int>1</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>region_name</name>\n"
                        + "<value><string>Россия</string></value>\n"
                        + "</member>\n"
                        + "</struct></value>\n"
                        + "</data></array></value>\n"
                        + "</data></array></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        Long clientId = 12345678L;
        GetFirmCountryCurrencyRequest request = new GetFirmCountryCurrencyRequest()
                .withClientId(clientId)
                .withServiceId(SERVICE_ID);
        List<FirmCountryCurrencyItem> currencyItems = client.getFirmCountryCurrency(request);
        FirmCountryCurrencyItem item = new FirmCountryCurrencyItem()
                .withConvertTypeModify(true)
                .withCurrency("RUB")
                .withFirmId("1")
                .withRegionId(225)
                .withRegionName("Россия")
                .withRegionNameEn("Russia")
                .withResident(true);

        assertThat("Получили корректный список элементов", currencyItems, contains(beanDiffer(item)));

        checkRequest("Balance2.GetFirmCountryCurrency",
                ImmutableMap.<String, Object>builder()
                        .put("currency_filter", false)
                        .put("service_filter", false)
                        .put("client_id", clientId.toString())
                        .put("service_id", SERVICE_ID)
                        .build());
    }

    @Test
    void testGetBank() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.GetBank"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><struct>\n"
                        + "<member>\n"
                        + "<name>name</name>\n"
                        + "<value><string>SBERBANK</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>bicint</name>\n"
                        + "<value><string>SABRRUMMXXX</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>country</name>\n"
                        + "<value><string>RUSSIA</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>zipcode</name>\n"
                        + "<value><string>117997 MOSCOW</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>place</name>\n"
                        + "<value><string>MOSCOW</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>address</name>\n"
                        + "<value><string>19, VAVILOV UL.</string></value>\n"
                        + "</member>\n"
                        + "</struct></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");

        String swift = "SABRRUMM";
        BalanceBankDescription bank = client.getBank(swift);

        BalanceBankDescription expected = new BalanceBankDescription()
                .withName("SBERBANK")
                .withBicint("SABRRUMMXXX")
                .withCountry("RUSSIA")
                .withZipcode("117997 MOSCOW")
                .withPlace("MOSCOW")
                .withAddress("19, VAVILOV UL.");
        assertThat("Получили ожидаемые значения", bank, beanDiffer(expected));

        checkRequest("Balance2.GetBank", Collections.singletonMap("Swift", swift));
    }

    @Test
    void testEditPassport() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.EditPassport"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><struct>\n"
                        + "<member>\n"
                        + "<name>IsMain</name>\n"
                        + "<value><int>1</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Login</name>\n"
                        + "<value><string>pupkin</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Name</name>\n"
                        + "<value><string>Pupkin Vasily</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Uid</name>\n"
                        + "<value><int>123456</int></value>\n"
                        + "</member>\n"
                        + "</struct></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");

        Long uid = 123456L;
        Long opUid = 123456L;
        ClientPassportInfo passportInfo = client.editPassport(opUid, uid, new ClientPassportInfo().withIsMain(1));
        ClientPassportInfo expected = new ClientPassportInfo()
                .withIsMain(1)
                .withLogin("pupkin")
                .withName("Pupkin Vasily")
                .withUid(uid);
        assertThat("Получили ожидаемые значения", passportInfo, beanDiffer(expected));

        checkRequest("Balance2.EditPassport",
                Arrays.asList(opUid.toString(), uid.toString(), Collections.singletonMap("IsMain", 1)));
    }

    @Test
    void testRemoveUserClientAssociationError() {
        server.addResponse(HTTP_PATH,
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><array><data>\n"
                        + "<value><int>4006</int></value>\n"
                        + "<value><string>Passport pupkin (122) not linked to THIS client 12345</string></value>\n"
                        + "</data></array></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        assertThatCode(() -> client.createUserClientAssociation(122L, 12345L, 123456L))
                .isInstanceOf(BalanceClientException.class);
    }

    @Test
    void testRemoveUserClientAssociationSuccess() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.RemoveUserClientAssociation"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><array><data>\n"
                        + "<value><int>0</int></value>\n"
                        + "<value><string>SUCCESS</string></value>\n"
                        + "</data></array></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        int opUid = 123;
        int clientId = 12345;
        int repUid = 123456;
        client.removeUserClientAssociation((long) opUid, (long) clientId, (long) repUid);

        List<Object> expectedRequest = Stream.of(opUid, clientId, repUid)
                .map(Objects::toString)
                .collect(toList());
        checkRequest("Balance2.RemoveUserClientAssociation", expectedRequest);
    }

    @Test
    void testCreateUserClientAssociationError() {
        server.addResponse(HTTP_PATH,
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><array><data>\n"
                        + "<value><int>4006</int></value>\n"
                        + "<value><string>Passport pupkin (124) is already linked to THIS client " +
                        "12345</string></value>\n"
                        + "</data></array></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        assertThatCode(() -> client.createUserClientAssociation(123L, 12345L, 123456L))
                .isInstanceOf(BalanceClientException.class);
    }

    @Test
    void testCreateUserClientAssociationSuccess() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.CreateUserClientAssociation"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><array><data>\n"
                        + "<value><int>0</int></value>\n"
                        + "<value><string>SUCCESS</string></value>\n"
                        + "</data></array></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        long opUid = 123;
        long clientId = 12345;
        long repUid = 123456;
        client.createUserClientAssociation(opUid, clientId, repUid);

        List<Object> expectedRequest = Stream.of(opUid, clientId, repUid)
                .map(Objects::toString)
                .collect(toList());
        checkRequest("Balance2.CreateUserClientAssociation", expectedRequest);
    }

    @Test
    void testGetClientRepresentativePassports() {
        server.addResponse(HTTP_PATH,
                new Checker("Balance2.ListClientPassports"),
                "<?xml version='1.0'?>\n"
                        + "<methodResponse>\n"
                        + "<params>\n"
                        + "<param>\n"
                        + "<value><array><data>\n"
                        + "<value><struct>\n"
                        + "<member>\n"
                        + "<name>IsMain</name>\n"
                        + "<value><int>1</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Login</name>\n"
                        + "<value><string>pupkin</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Name</name>\n"
                        + "<value><string>Pupkin Vasily</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>ClientId</name>\n"
                        + "<value><int>123456</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Uid</name>\n"
                        + "<value><int>1234568</int></value>\n"
                        + "</member>\n"
                        + "</struct></value>\n"
                        + "<value><struct>\n"
                        + "<member>\n"
                        + "<name>IsMain</name>\n"
                        + "<value><int>0</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Login</name>\n"
                        + "<value><string>kinpup</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Name</name>\n"
                        + "<value><string>Василий Кинпуп</string></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>ClientId</name>\n"
                        + "<value><int>123456</int></value>\n"
                        + "</member>\n"
                        + "<member>\n"
                        + "<name>Uid</name>\n"
                        + "<value><int>1234567</int></value>\n"
                        + "</member>\n"
                        + "</struct></value>\n"
                        + "</data></array></value>\n"
                        + "</param>\n"
                        + "</params>\n"
                        + "</methodResponse>");
        Long opUid = 1234568L;
        Long clientId = 123456L;
        List<ClientPassportInfo> passports = client.getClientRepresentativePassports(opUid, clientId);
        ClientPassportInfo expectedFirst = new ClientPassportInfo()
                .withIsMain(1)
                .withLogin("pupkin")
                .withName("Pupkin Vasily")
                .withClientId(clientId)
                .withUid(1234568L);
        ClientPassportInfo expectedSecond = new ClientPassportInfo()
                .withIsMain(0)
                .withLogin("kinpup")
                .withName("Василий Кинпуп")
                .withClientId(clientId)
                .withUid(1234567L);
        assertThat("Получили ожидаемые значения", passports,
                contains(Arrays.asList(beanDiffer(expectedFirst), beanDiffer(expectedSecond))));

        checkRequest("Balance2.ListClientPassports", Arrays.asList(opUid.toString(), clientId.toString()));
    }

    @AfterEach
    void tearDown() {
        server.clear();
    }

    private void checkRequest(String methodName, Map<String, Object> expectedRequest) {
        checkRequest(methodName, Collections.singletonList(expectedRequest));
    }

    private void checkRequest(String methodName, List<Object> expectedRequest) {
        String requestBody = server.getRequest(HTTP_PATH);
        XmlRpcRequestParser parser = new XmlRpcRequestParser(config, new TypeFactoryImpl(null));

        try {
            XMLReader reader = newXMLReader();
            reader.setContentHandler(parser);
            reader.parse(new InputSource(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw new AssertionError("Expected that request will be parsed correctly");
        }

        assertThat("Имя вызванного метода совпало с ожидаемым", parser.getMethodName(), equalTo(methodName));
        assertThat("Тело запроса совпало с ожидаемым", parser.getParams(), beanDiffer(expectedRequest));
    }

    private class Checker implements Predicate<String> {
        private final String methodName;

        private Checker(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public boolean test(String body) {
            return body.matches(".*<methodName>\\s*" + methodName + "\\s*</methodName>.*");
        }
    }
}
