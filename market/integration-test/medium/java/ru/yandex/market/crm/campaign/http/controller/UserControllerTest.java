package ru.yandex.market.crm.campaign.http.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.domain.mobileapp.MetrikaMobileAppDto;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.http.response.ErrorResponse;
import ru.yandex.market.crm.campaign.services.user.CommunicationsMap;
import ru.yandex.market.crm.campaign.services.user.CrmInfoDto;
import ru.yandex.market.crm.campaign.services.user.EmailCrmDto;
import ru.yandex.market.crm.campaign.services.user.PushCrmDto;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.core.services.platform.PlatformUserIdType;
import ru.yandex.market.crm.core.test.utils.PlatformHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.util.PlatformUtils;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.DeviceType;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MetrikaMobileApp;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobileOS;
import ru.yandex.market.crm.mapreduce.domain.user.IdsGraph;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.User;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.models.Email;
import ru.yandex.market.crm.platform.models.Push;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.HttpResponse;
import ru.yandex.market.mcrm.http.ResponseMock;

import static java.util.Comparator.comparing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author apershukov
 */
public class UserControllerTest extends AbstractControllerMediumTest {
    private static final String EMAIL = "user@yandex.ru";
    private static final String LOGIN = "some.user";
    private static final Uid UID = Uid.asPuid(111L);

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private PlatformHelper platformHelper;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private EmailPeriodicSendingTestHelper periodicSendingTestHelper;

    /**
     * TODO Переделать после решения LILUCRM-1601
     */
    @Disabled
    @Test
    public void testGetUserMobileDevices() throws Exception {
        setUpBlackbox("bb_response.json");

        String uuid1 = "222";
        String uuid2 = "333";

        User user = new User()
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(UID)
                                .addNode(Uid.asUuid(uuid1))
                                .addNode(Uid.asUuid(uuid2))
                                .addNode(Uid.asUuid("444"))
                                .addEdge(0, 1)
                                .addEdge(0, 2)
                                .addEdge(0, 3)
                );

        addUser(user);

        MetrikaMobileApp app1 = new MetrikaMobileApp(uuid1)
                .setModel(
                        new MetrikaMobileApp.Model()
                                .setManufacturer("Samsung")
                                .setModel("Galaxy A5(2017)")
                )
                .setDeviceType(DeviceType.PHONE)
                .setOS(MobileOS.ANDROID)
                .setDeviceIdHash("hash-1");

        MetrikaMobileApp app2 = new MetrikaMobileApp(uuid2)
                .setDeviceType(DeviceType.TABLET)
                .setDeviceIdHash("hash-2");

        MetrikaMobileApp app3 = new MetrikaMobileApp("555")
                .setDeviceType(DeviceType.DESKTOP)
                .setDeviceIdHash("hash-3");

        addApps(app1, app2, app3);

        List<MetrikaMobileAppDto> devices = requestDevices();

        Assertions.assertEquals(2, devices.size());

        devices.sort(comparing(MetrikaMobileAppDto::getDeviceIdHash));

        Assertions.assertEquals(app1.getDeviceIdHash(), devices.get(0).getDeviceIdHash());
        Assertions.assertEquals("Samsung Galaxy A5(2017)", devices.get(0).getModel());
        Assertions.assertEquals(app1.getDeviceType(), devices.get(0).getDeviceType());
        Assertions.assertEquals(app1.getOS(), devices.get(0).getOs());

        Assertions.assertEquals(app2.getDeviceIdHash(), devices.get(1).getDeviceIdHash());
    }

    @Test
    public void testGetDevicesOfUserWithoutUuids() throws Exception {
        setUpBlackbox("bb_response.json");

        User user = new User()
                .setIdsGraph(
                        new IdsGraph()
                                .addNode(UID)
                );

        addUser(user);

        requestDevicesExpect404("Ни одного устройства на найдено");
    }

    @Test
    public void test404GetDevicesIfPuidIsUnknown() throws Exception {
        setUpBlackbox("bb_response.json");
        emptyUser(UID);

        requestDevicesExpect404("Ни одного устройства на найдено");
    }

    @Test
    public void test404IfInvalidLoginIsPassed() throws Exception {
        setUpBlackbox("bb_user-not-found.json");
        requestDevicesExpect404("Пользователя с логином '" + LOGIN + "' не существует");
    }

    /**
     * Проверка получения карты коммуникаций с письмом из регулярной рассылки
     */
    @Test
    public void testGetCommunicationMapWithPeriodicLetter() throws Exception {
        EmailPeriodicSending sending = periodicSendingTestHelper.prepareSending();

        ru.yandex.market.crm.platform.commons.Uid uid =
                Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL);

        CrmInfo crmInfo = CrmInfo.newBuilder()
                .setSendingId(sending.getKey())
                .setIteration(4)
                .setVersionId(sending.getId())
                .build();

        platformHelper.putFact(
                "Email",
                uid,
                Email.newBuilder()
                        .setUid(uid)
                        .setSendingType(SendingType.PERIODIC_SENDING)
                        .setCrmInfo(crmInfo)
                        .build()
        );

        yaSenderHelper.setGlobalUnsubscribeStatus(EMAIL);

        MvcResult result = mockMvc.perform(
                post("/api/users/communications-map")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(List.of(Uid.asEmail(EMAIL))))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        CommunicationsMap map = jsonDeserializer.readObject(
                CommunicationsMap.class,
                result.getResponse().getContentAsString()
        );

        List<EmailCrmDto> emails = map.getCrmEmails();
        Assertions.assertNotNull(emails);
        Assertions.assertEquals(1, emails.size());

        EmailCrmDto email = emails.get(0);
        Assertions.assertEquals(EMAIL, email.getEmail());
        Assertions.assertEquals(SendingType.PERIODIC_SENDING, email.getSendingType());

        CrmInfoDto crmInfoDto = email.getCrmInfo();
        Assertions.assertNotNull(crmInfoDto);
        Assertions.assertEquals(crmInfo.getSendingId(), crmInfoDto.getSendingId());
        Assertions.assertEquals(crmInfo.getVersionId(), crmInfoDto.getVersionId());
        Assertions.assertEquals(crmInfo.getIteration(), (int) crmInfoDto.getIteration());
        Assertions.assertEquals(sending.getName(), crmInfoDto.getSendingName());
    }

    /**
     * Проверка получения карты коммуникаций с email, отправленных из market-mailer
     */
    @Test
    public void testGetCommunicationMapWithEmailsFromMarketMailer() throws Exception {
        String triggerEmail = "trigger@example.com";
        String transactionEmail = "transaction@example.com";

        prepareEmailFact(triggerEmail, SendingType.TRIGGER, "TRIGGER_EMAIL", 10);
        prepareEmailFact(transactionEmail, SendingType.TRANSACTION, "TRANSACTION_EMAIL", 20);

        MvcResult result = mockMvc.perform(
                post("/api/users/communications-map")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(
                                List.of(
                                        Uid.asEmail(triggerEmail),
                                        Uid.asEmail(transactionEmail)
                                )
                        ))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        CommunicationsMap map = jsonDeserializer.readObject(
                CommunicationsMap.class,
                result.getResponse().getContentAsString()
        );

        List<EmailCrmDto> emails = map.getCrmEmails();
        Assertions.assertEquals(2, emails.size());

        Map<String, EmailCrmDto> emailsByUid = Map.of(
                emails.get(0).getEmail(), emails.get(0),
                emails.get(1).getEmail(), emails.get(1)
        );

        EmailCrmDto email0 = emailsByUid.get(triggerEmail);
        Assertions.assertNotNull(email0);
        Assertions.assertEquals(SendingType.TRIGGER, email0.getSendingType());
        Assertions.assertEquals("TRIGGER_EMAIL", email0.getCrmInfo().getNotificationName());
        Assertions.assertEquals(Integer.valueOf(10), email0.getCrmInfo().getNotificationId());

        EmailCrmDto email1 = emailsByUid.get(transactionEmail);
        Assertions.assertNotNull(email1);
        Assertions.assertEquals(SendingType.TRANSACTION, email1.getSendingType());
        Assertions.assertEquals("TRANSACTION_EMAIL", email1.getCrmInfo().getNotificationName());
        Assertions.assertEquals(Integer.valueOf(20), email1.getCrmInfo().getNotificationId());
    }

    /**
     * Проверка получения карты коммуникаций с push, отправленных из market-mailer
     */
    @Test
    public void testGetCommunicationMapWithPushesFromMarketMailer() throws Exception {
        String triggerUuid = "trigger_uuid";
        String transactionUuid = "transaction_uuid";

        preparePushFact(triggerUuid, SendingType.TRIGGER, "TRIGGER_PUSH", 30, "trigger_id");
        preparePushFact(transactionUuid, SendingType.TRANSACTION, "TRANSACTION_PUSH", 40, "transaction_id");

        MvcResult result = mockMvc.perform(
                post("/api/users/communications-map")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(
                                List.of(
                                        Uid.asUuid(triggerUuid),
                                        Uid.asUuid(transactionUuid)
                                )
                        ))
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        CommunicationsMap map = jsonDeserializer.readObject(
                CommunicationsMap.class,
                result.getResponse().getContentAsString()
        );

        List<PushCrmDto> pushes = map.getCrmPushes();
        Assertions.assertEquals(2, pushes.size());

        Map<String, PushCrmDto> pushesByUid = Map.of(
                pushes.get(0).getUuid(), pushes.get(0),
                pushes.get(1).getUuid(), pushes.get(1)
        );

        PushCrmDto push0 = pushesByUid.get(triggerUuid);
        Assertions.assertNotNull(push0);
        Assertions.assertEquals(SendingType.TRIGGER, push0.getSendingType());
        Assertions.assertEquals("TRIGGER_PUSH", push0.getCrmInfo().getNotificationName());
        Assertions.assertEquals(Integer.valueOf(30), push0.getCrmInfo().getNotificationId());
        Assertions.assertEquals("trigger_id", push0.getTransitId());

        PushCrmDto push1 = pushesByUid.get(transactionUuid);
        Assertions.assertNotNull(push1);
        Assertions.assertEquals(SendingType.TRANSACTION, push1.getSendingType());
        Assertions.assertEquals("TRANSACTION_PUSH", push1.getCrmInfo().getNotificationName());
        Assertions.assertEquals(Integer.valueOf(40), push1.getCrmInfo().getNotificationId());
        Assertions.assertEquals("transaction_id", push1.getTransitId());
    }

    private void prepareEmailFact(String email,
                                  SendingType sendingType,
                                  String notificationName,
                                  int notificationId) {
        ru.yandex.market.crm.platform.commons.Uid uid =
                Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, email);

        platformHelper.putFact(
                "Email",
                uid,
                Email.newBuilder()
                        .setUid(uid)
                        .setSendingType(sendingType)
                        .setCrmInfo(
                                CrmInfo.newBuilder()
                                        .setNotificationName(notificationName)
                                        .setNotificationId(notificationId)
                        )
                        .build()
        );

        yaSenderHelper.setGlobalUnsubscribeStatus(EMAIL);
    }

    private void preparePushFact(String uuid,
                                 SendingType sendingType,
                                 String notificationName,
                                 int notificationId,
                                 String transitId) {
        ru.yandex.market.crm.platform.commons.Uid uid =
                Uids.create(ru.yandex.market.crm.platform.commons.UidType.UUID, uuid);

        platformHelper.putFact(
                "Push",
                uid,
                Push.newBuilder()
                        .setUid(uid)
                        .setSendingType(sendingType)
                        .setCrmInfo(
                                CrmInfo.newBuilder()
                                        .setNotificationName(notificationName)
                                        .setNotificationId(notificationId)
                        )
                        .setMetrikaInfo(
                                Push.MetrikaInfo.newBuilder()
                                        .setTransitId(transitId)
                        )
                        .build()
        );
    }

    private void requestDevicesExpect404(String expectedMessage) throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/users/logins/{login}/mobile-devices", LOGIN)
        )
                .andDo(print())
                .andExpect(status().isNotFound())
                .andReturn();

        ErrorResponse response = jsonDeserializer.readObject(
                ErrorResponse.class,
                result.getResponse().getContentAsByteArray()
        );

        Assertions.assertEquals(expectedMessage, response.getMessage());
    }

    private List<MetrikaMobileAppDto> requestDevices() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/users/logins/{login}/mobile-devices", LOGIN)
        )
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsByteArray()
        );
    }

    private void emptyUser(Uid uid) {
        platformHelper.prepareUser(PlatformUserIdType.select(uid.getType()).get().getPlatformType(), uid.getValue(),
                null);
    }

    private void addUser(User user) {
        user.setId(UUID.randomUUID().toString());

        ru.yandex.market.crm.platform.api.User platformUser = PlatformUtils.convert(user);
        platformUser.getIdsGraph().getNodeList().forEach(node ->
                platformHelper.prepareUser(node.getType(), Uids.asStringValue(node), platformUser));
    }

    private void setUpBlackbox(String responsePath) throws IOException {
        httpEnvironment.when(
                HttpRequest.get("https://blackbox.yandex.net/blackbox?" +
                        "login=" + LOGIN + "&method=userinfo&emails=getdefault&format=json")
        ).then(
                new HttpResponse(
                        new ResponseMock(IOUtils.toByteArray(getClass().getResourceAsStream(responsePath)))
                )
        );
    }

    private void addApps(MetrikaMobileApp... apps) {
        // TODO Implement when reviving test
    }
}
