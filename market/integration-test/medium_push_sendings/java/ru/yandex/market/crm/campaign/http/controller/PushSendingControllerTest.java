package ru.yandex.market.crm.campaign.http.controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.crm.campaign.domain.promo.entities.TestIdsGroup;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushSendingFactInfo;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactStatus;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactTransportState;
import ru.yandex.market.crm.campaign.domain.sending.SendingFactType;
import ru.yandex.market.crm.campaign.domain.sending.SendingStage;
import ru.yandex.market.crm.campaign.domain.sending.TestDevicesGroup;
import ru.yandex.market.crm.campaign.domain.sending.TestPushDevice;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.workflow.StageStatus;
import ru.yandex.market.crm.campaign.dto.sending.PushPlainSendingDto;
import ru.yandex.market.crm.campaign.dto.sending.SchedulePushGenerationRequest;
import ru.yandex.market.crm.campaign.dto.sending.SchedulePushGenerationRequest.UploadSettings;
import ru.yandex.market.crm.campaign.dto.sending.facts.PushSendingFactInfoDto;
import ru.yandex.market.crm.campaign.http.response.ErrorResponse;
import ru.yandex.market.crm.campaign.services.converter.sending.PushSendingConverter;
import ru.yandex.market.crm.campaign.services.security.ObjectPermissions;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.services.sending.PushSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.facts.PushSendingFactInfoDAO;
import ru.yandex.market.crm.campaign.services.sql.TestDevicesDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumWithoutYtTest;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType;
import ru.yandex.market.crm.core.test.utils.MobileAppsTestHelper;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.http.security.BlackboxProfile;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.push.ActionType;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.tsum.event.EventId;

import static java.util.Comparator.comparing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.util.CrmCollections.nullToEmpty;

/**
 * @author apershukov
 */
public class PushSendingControllerTest extends AbstractControllerMediumWithoutYtTest {

    private static PushSendingVariantConf variant() {
        PushSendingVariantConf variantConf = new PushSendingVariantConf()
                .setId("variant_a")
                .setPercent(100);
        AndroidPushConf pushConf = new AndroidPushConf();
        pushConf.setTitle("Test push title");
        pushConf.setText("Test push text");
        pushConf.setActionType(ActionType.URL);
        pushConf.setAction("https://market.yandex.ru/product/111");
        variantConf.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));

        return variantConf;
    }

    private static PushSendingConf config(PushSendingVariantConf... variants) {
        var config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setTarget(new TargetAudience(LinkingMode.NONE, "segment_id"));
        config.setVariants(Arrays.asList(variants));
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());
        return config;
    }

    @Inject
    private PushSendingDAO sendingDAO;
    @Inject
    private TestDevicesDAO devicesDao;
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private JsonDeserializer jsonDeserializer;
    @Inject
    private PushSendingTestHelper pushSendingTestHelper;
    @Inject
    private PushSendingConverter pushSendingConverter;
    @Inject
    private UsersRolesDao usersRolesDao;
    @Inject
    private MobileAppsTestHelper mobileAppsTestHelper;
    @Inject
    private PushSendingFactInfoDAO sendingFactInfoDAO;

    private PushPlainSending sending;

    private static void assertGroup(TestDevicesGroup expected, TestDevicesGroup actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());

        List<TestPushDevice> expectedItems = nullToEmpty(expected.getItems());
        List<TestPushDevice> actualItems = nullToEmpty(actual.getItems());

        assertEquals(expectedItems.size(), actualItems.size());

        expectedItems.sort(comparing(TestPushDevice::getIdValue));
        actualItems.sort(comparing(TestPushDevice::getIdValue));

        for (int i = 0; i < expectedItems.size(); ++i) {
            TestPushDevice expectedItem = expectedItems.get(i);
            TestPushDevice actualItem = actualItems.get(i);

            assertEquals(expectedItem.getIdType(), actualItem.getIdType());
            assertEquals(expectedItem.getIdValue(), actualItem.getIdValue());
            assertEquals(expectedItem.getName(), actualItem.getName());
        }
    }

    @BeforeEach
    public void setUp() {
        sending = prepareBuiltSending();

        httpEnvironment.when(HttpRequest.post("https://tsum-api.market.yandex.net:4203/events/addEvent"))
                .then(
                        ResponseBuilder.newBuilder()
                                .body(jsonSerializer.writeObjectAsString(
                                        EventId.newBuilder()
                                                .setId(UUID.randomUUID().toString())
                                                .build()
                                ))
                                .build()
                );
    }

    @Test
    public void testSaveNewGroup() throws Exception {
        TestDevicesGroup group = new TestDevicesGroup()
                .setId("id")
                .setName("Group")
                .setItems(Collections.singletonList(
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd", "username")
                ));

        sendTest(group);

        List<TestDevicesGroup> groups = devicesDao.getAll();
        assertEquals(1, groups.size());

        assertGroup(group, groups.get(0));
    }

    @Test
    public void testDeleteDevices() throws Exception {
        TestDevicesGroup group1 = new TestDevicesGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Collections.singletonList(
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd-1", "username")
                ));

        TestDevicesGroup group2 = new TestDevicesGroup()
                .setId("id-2")
                .setName("Group 2")
                .setItems(Arrays.asList(
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd-1", "username"),
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd-2", "username"),
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd-3", "username")
                ));

        devicesDao.saveGroups(Arrays.asList(group1, group2));

        group2.setItems(Collections.singletonList(
                new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd-3", "username")
        ));

        sendTest(group1, group2);

        List<TestDevicesGroup> groups = devicesDao.getAll();
        assertEquals(2, groups.size());

        groups.sort(comparing(TestIdsGroup::getId));

        assertGroup(group1, groups.get(0));
        assertGroup(group2, groups.get(1));
    }

    @Test
    public void testEditDevice() throws Exception {
        TestDevicesGroup group1 = new TestDevicesGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(Collections.singletonList(
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd", "username")
                ));

        TestDevicesGroup group2 = new TestDevicesGroup()
                .setId("id-2")
                .setName("Group 2")
                .setItems(Collections.singletonList(
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd", "username")
                ));

        devicesDao.saveGroups(Arrays.asList(group1, group2));

        group2.setItems(Collections.singletonList(
                new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd", "login")
        ));

        sendTest(group1, group2);

        List<TestDevicesGroup> groups = devicesDao.getAll();
        assertEquals(2, groups.size());

        groups.sort(comparing(TestIdsGroup::getId));

        assertGroup(group1, groups.get(0));
        assertGroup(group2, groups.get(1));
    }

    @Test
    public void testAddDeviceToEmptyGroup() throws Exception {
        TestDevicesGroup group = new TestDevicesGroup()
                .setId("id-1")
                .setName("Group");

        devicesDao.saveGroups(Collections.singletonList(group));

        group.setItems(Collections.singletonList(
                new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd", "login")
        ));

        sendTest(group);

        List<TestDevicesGroup> groups = devicesDao.getAll();
        assertEquals(1, groups.size());
        assertGroup(group, groups.get(0));
    }

    @Test
    public void testGetGroups() throws Exception {
        TestDevicesGroup group = new TestDevicesGroup()
                .setId("id")
                .setName("Group")
                .setItems(Collections.singletonList(
                        new TestPushDevice(DeviceIdType.GOOGLE_AID, "iddqd", "login")
                ));

        devicesDao.saveGroups(Collections.singletonList(group));

        MvcResult result = mockMvc.perform(get("/api/sendings/push/test-devices"))
                .andExpect(status().isOk())
                .andDo(print())
                .andReturn();

        List<TestDevicesGroup> groups = jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );

        assertEquals(1, groups.size());
        assertGroup(group, groups.get(0));
    }

    @Test
    public void testEditGroupName() throws Exception {
        TestDevicesGroup group = new TestDevicesGroup()
                .setId("id")
                .setName("Group");

        devicesDao.saveGroups(Collections.singletonList(group));

        group.setName("New Group Name");

        sendTest(group);

        List<TestDevicesGroup> groups = devicesDao.getAll();
        assertEquals(1, groups.size());
        assertGroup(group, groups.get(0));
    }

    @Test
    public void testCopyPushSendingChangeUtmCampaignMiddle() throws Exception {
        var action = "yamarket://test?referrer=appmetrica_tracking_id%3D1035348116234076871%26utm_source" +
                "%3Dpush_andr%26reattribution%3D1%26utm_campaign%3Dtest_utm_campaign123%26clid%3D621%26utm_referrer" +
                "%3D621%26utm_medium%3Dmassmail";

        String expectedAction =
                """
                        yamarket://test?referrer=\
                        appmetrica_tracking_id%%3D1035348116234076871%%26\
                        utm_source%%3Dpush_andr%%26\
                        reattribution%%3D1%%26\
                        utm_campaign%%3D%s%%26\
                        clid%%3D621%%26\
                        utm_referrer%%3D621%%26\
                        utm_medium%%3Dmassmail\
                        """;
        testCopyPushSendingChangeUtmCampaign(action, expectedAction);
    }

    @Test
    public void testCopyPushSendingChangeUtmCampaignEnd() throws Exception {

        var action = "yamarket://test?referrer=appmetrica_tracking_id%3D1035348116234076871%26utm_source" +
                "%3Dpush_andr%26reattribution%3D1%26clid%3D621%26utm_referrer" +
                "%3D621%26utm_medium%3Dmassmail%26utm_campaign%3Dtest_utm_campaign123";

        String expectedAction =
                """
                        yamarket://test?referrer=\
                        appmetrica_tracking_id%%3D1035348116234076871%%26\
                        utm_source%%3Dpush_andr%%26\
                        reattribution%%3D1%%26\
                        clid%%3D621%%26\
                        utm_referrer%%3D621%%26\
                        utm_medium%%3Dmassmail%%26\
                        utm_campaign%%3D%s\
                        """;
        testCopyPushSendingChangeUtmCampaign(action, expectedAction);
    }

    /**
     * Пользователь с ролью "Агент" может обновлять любые промо-рассылки
     */
    @Test
    public void testUserWithAgentRoleIsNotAllowedToUpdateNotAgentSendings() throws Exception {
        BlackboxProfile agent = SecurityUtils.profile("agent_profile");

        usersRolesDao.addRole(agent.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT));

        Set<String> roles = Set.of(Roles.AGENT, Roles.OPERATOR, Roles.ADMIN);
        for (var role : roles) {
            BlackboxProfile someUser = SecurityUtils.profile(role + "_profile");
            usersRolesDao.addRole(someUser.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, role));

            PushPlainSending sending = prepareBuiltSending(someUser, variant());

            PushPlainSendingDto dto = pushSendingConverter.convert(sending);

            SecurityUtils.setAuthentication(agent);

            mockMvc
                    .perform(post("/api/sendings/push//update/{id}", sending.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonSerializer.writeObjectAsBytes(dto)))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    /**
     * Пользователь с ролью "Агент" может формировать рассылки
     */
    @Test
    public void testUserWithAgentRoleIsAllowedToGenerateSendings() throws Exception {
        var agent = SecurityUtils.profile("agent_profile");
        usersRolesDao.addRole(agent.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT));

        var sending = prepareBuiltSending(agent, variant());

        var dto = pushSendingConverter.convert(sending);

        SecurityUtils.setAuthentication(agent);

        mockMvc.perform(post("/api/sendings/push/{id}/generate", sending.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(dto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    /**
     * Пользователь с ролью "Агент" не может удалять любые промо рассылки
     */
    @Test
    public void testUserWithAgentRoleIsNotAllowedToDeleteAnySendings() throws Exception {
        BlackboxProfile userAgent = SecurityUtils.profile("userAgent_profile");
        BlackboxProfile someOperator = SecurityUtils.profile("operator_profile");
        BlackboxProfile someAgent = SecurityUtils.profile("agent_profile");
        usersRolesDao.addRole(userAgent.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT));
        usersRolesDao.addRole(someAgent.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT));
        usersRolesDao.addRole(someOperator.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));

        var variant = variant();
        PushPlainSending sending1 = prepareBuiltSending(someOperator, variant);
        PushPlainSending sending2 = prepareBuiltSending(someAgent, variant);

        SecurityUtils.setAuthentication(userAgent);

        for (PushPlainSending sending : Set.of(sending1, sending2)) {
            MockHttpServletResponse response = mockMvc
                    .perform(delete("/api/sendings/push/{id}", sending.getId()))
                    .andDo(print())
                    .andExpect(status().isForbidden())
                    .andReturn().getResponse();

            ErrorResponse errorResponse = jsonDeserializer.readObject(
                    ErrorResponse.class,
                    response.getContentAsString()
            );

            assertEquals(
                    "Access denied for object. Required permission: " + ObjectPermissions.DELETE,
                    errorResponse.getMessage()
            );
        }
    }

    /**
     * Пользователь с ролью "Оператор" может удалять промо рассылки, созданные пользователями с ролью "Агент"
     */
    @Test
    public void testUserWithOperatorRoleIsAllowedToUpdateAgentSendings() throws Exception {
        BlackboxProfile operator = SecurityUtils.profile("operator_profile");
        BlackboxProfile agent = SecurityUtils.profile("agent_profile");
        usersRolesDao.addRole(operator.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));
        usersRolesDao.addRole(agent.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT));

        PushPlainSending sending = prepareBuiltSending(agent, variant());

        SecurityUtils.setAuthentication(operator);

        PushPlainSendingDto dto = pushSendingConverter.convert(sending);

        mockMvc
                .perform(post("/api/sendings/push/update/{id}", sending.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsBytes(dto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    /**
     * Если в конфигурации рассылки указано приложение, поддерживающее раздельные подписки,
     * конфигурацию в которой не указан тип подписки невозможно сохранить
     */
    @Test
    void testSendingForAppWithSubscriptionCannotBeSavedWithoutSubsType() throws Exception {
        var sending = prepareSending();

        var newConfig = new PushSendingConf();
        newConfig.setApplication(MobileApplication.MARKET_APP);
        newConfig.setVariants(List.of(variant()));

        var dto = new PushPlainSendingDto();
        dto.setId(sending.getId());
        dto.setName(sending.getName());
        dto.setConfig(newConfig);

        requestUpdate(dto)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testSendingWithoutSubscriptionCanBeSavedWithoutSubsType() throws Exception {
        var appId = "test_app";
        mobileAppsTestHelper.insertApplication(appId, 111, YPath.cypressRoot(), List.of());

        var newConfig = new PushSendingConf();
        newConfig.setApplication(appId);
        newConfig.setVariants(List.of(variant()));

        var dto = new PushPlainSendingDto();
        dto.setId(sending.getId());
        dto.setName(sending.getName());
        dto.setConfig(newConfig);

        requestUpdate(dto)
                .andExpect(status().isOk());
    }

    /**
     * Если в конфигурации рассылки указано приложение, поддерживающее раздельные подписки,
     * конфигурация в которой задан тип подписки успешно сохраняется.
     */
    @Test
    void testSendingForAppWithSubscriptionCanBeSavedWithSubsType() throws Exception {
        var sending = prepareSending();

        var newConfig = new PushSendingConf();
        newConfig.setApplication(MobileApplication.MARKET_APP);
        newConfig.setVariants(List.of(variant()));
        newConfig.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());

        var dto = new PushPlainSendingDto();
        dto.setId(sending.getId());
        dto.setName(sending.getName());
        dto.setConfig(newConfig);

        requestUpdate(dto)
                .andExpect(status().isOk());
    }

    /**
     * Если рассылка была собрана более суток назад, в выдаче ручки GET /api/sendings/push/{}
     * устанавливается флаг sendingDataIsStale
     */
    @Test
    void testStaleDataFlagIsSetForSendingsGeneratedMoreThanADayAgo() throws Exception {
        var generationTime = LocalDateTime.now().minusDays(2);
        var sending = prepareBuiltSending(generationTime, config());

        var dto = requestSending(sending.getId());

        assertTrue(dto.sendingDataIsStale());
    }

    /**
     * Если рассылка была собрана менее суток назад флаг sendingDataIsStale в выдаче ручки
     * GET /api/sendings/push/{} не устанавливается
     */
    @Test
    void testStaleDataFlagIsNotSetForSendingGeneratedLessThanADayAgo() throws Exception {
        var generationTime = LocalDateTime.now().minusHours(8);
        var sending = prepareBuiltSending(generationTime, config());

        var dto = requestSending(sending.getId());

        assertFalse(dto.sendingDataIsStale());
    }

    /**
     * Если сборка рассылки была приостановлена более суток назад, в выдаче ручки GET /api/sendings/push/{}
     * устанавливается флаг sendingDataIsStale
     */
    @Test
    void testStaleDataFlagIsSetForSendingWhichGenerationPausedMoreThanADayAgo() throws Exception {
        var sending = prepareSending(config());

        sending.setStageAndStatus(SendingStage.GENERATE, StageStatus.SUSPENDED);
        sending.setGenerationTime(LocalDateTime.now().minusDays(2));
        sendingDAO.updateSendingStates(sending.getId(), sending);

        var dto = requestSending(sending.getId());

        assertTrue(dto.sendingDataIsStale());
    }

    /**
     * Если сборка рассылки упала более суток назад, в выдаче ручки GET /api/sendings/push/{}
     * устанавливается флаг sendingDataIsStale
     */
    @Test
    void testStaleDataFlagIsSetForSendingWhichGenerationFailedMoreThanADayAgo() throws Exception {
        var sending = prepareSending(config());

        sending.setStageAndStatus(SendingStage.GENERATE, StageStatus.ERROR);
        sending.setGenerationTime(LocalDateTime.now().minusDays(2));
        sendingDAO.updateSendingStates(sending.getId(), sending);

        var dto = requestSending(sending.getId());

        assertTrue(dto.sendingDataIsStale());
    }

    /**
     * Если сборка завершилась более суток назад, после чего началась выгрузка, которая была приостановлена
     * в выдаче ручки GET /api/sendings/push/{} устанавливается флаг sendingDataIsStale
     */
    @Test
    void testStaleDataFlagIsSetForSendingWhichGenerationCompletedMoreThanADayAgo() throws Exception {
        var sending = prepareSending(config());

        sending.setStageAndStatus(SendingStage.UPLOAD, StageStatus.SUSPENDED);
        sending.setGenerationTime(LocalDateTime.now().minusDays(2));
        sendingDAO.updateSendingStates(sending.getId(), sending);

        var dto = requestSending(sending.getId());

        assertTrue(dto.sendingDataIsStale());
    }

    /**
     * Если рассылка была выгружена, флаг sendingDataIsStale в выдаче ручки GET /api/sendings/push/{}
     * не устанавливается вне зависимости от того когда была завешена сборка
     */
    @Test
    void testStaleDataFlagIsNotSetForSendingWhichHasBeenUploaded() throws Exception {
        var sending = prepareSending(config());

        sending.setStageAndStatus(SendingStage.UPLOAD, StageStatus.FINISHED);
        sending.setGenerationTime(LocalDateTime.now().minusDays(21));
        sendingDAO.updateSendingStates(sending.getId(), sending);

        var dto = requestSending(sending.getId());

        assertFalse(dto.sendingDataIsStale());
    }

    /**
     * В случае если рассылка была собрана более суток назад в факте её боевой отправки не возвращается
     * ссылка на таблицу с данными в YT. Это делается для того чтобы не давать пользователю ссылку, которая
     * почти наверняка будет указывать на таблицу которая больше не существует.
     */
    @Test
    void testDoNotReturnTableLinksForSendingGeneratedMoreThanADayAgo() throws Exception {
        var generationTime = LocalDateTime.now().minusDays(2);

        var sending = prepareSending(config());
        sending.setGenerationTime(generationTime);
        sending.setStageAndStatus(SendingStage.UPLOAD, StageStatus.FINISHED);

        var sendingId = sending.getId();
        sendingDAO.updateSendingStates(sendingId, sending);

        var sendingFact = new PushSendingFactInfo();
        sendingFact.setId(sendingId);
        sendingFact.setSendingId(sendingId);
        sendingFact.setUploadTime(LocalDateTime.now().minusDays(1));
        sendingFact.setType(SendingFactType.FINAL);
        sendingFact.setSendingState(SendingFactTransportState.FINISHED);
        sendingFact.setStatus(SendingFactStatus.FINISHED);

        sendingFactInfoDAO.save(sendingFact);

        var dto = requestSending(sendingId);

        var facts = dto.getSendingFacts();
        assertThat(facts, hasSize(1));

        var fact = (PushSendingFactInfoDto) facts.get(0);
        assertNull(fact.getSendingDataUrl());
    }

    /**
     * При вызове ручки POST /api/sendings/push/{}/schedule_generation с последующей отправкой в
     * конфигурации рассылки обновляются настройки выгрузки.
     */
    @Test
    void testScheduleGeneration() throws Exception {
        var sending = prepareSending();

        var uploadSettings = new UploadSettings();
        uploadSettings.setFinishLimit(LocalTime.of(18, 0));
        uploadSettings.setEnableMaxRate(true);

        var startTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusDays(1);

        var dto = requestGenerationScheduling(sending, startTime, true, uploadSettings);
        var config = dto.getConfig();

        assertAll(
                () -> assertEquals(startTime, dto.getScheduleTime()),
                () -> assertTrue(dto.isSendAfterGeneration()),
                () -> assertTrue(config.isEnableMaxRate()),
                () -> assertEquals(uploadSettings.getFinishLimit(), config.getFinishLimit())
        );
    }

    /**
     * При вызове ручки POST /api/sending/push/{}/schedule_generation пользователем с ролью AGENT
     * и sendAfterGeneration=true
     * 1. Рассылка переводится в состояние CONFIRM
     * 2. Время переданное в качестве startTime устанавливается в качестве значения поля scheduled_time
     */
    @Test
    void testScheduleGenerationWithSendingByAgentAuthor() throws Exception {
        var agent = prepareProfile("agent", Roles.AGENT);
        SecurityUtils.setAuthentication(agent);
        var sending = prepareSending();

        var uploadSettings = new UploadSettings();
        uploadSettings.setFinishLimit(LocalTime.of(18, 0));
        uploadSettings.setEnableMaxRate(true);

        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);

        var dto = requestGenerationScheduling(sending, scheduleTime, true, uploadSettings);

        assertAll(
                () -> assertEquals(scheduleTime, dto.getScheduleTime()),
                () -> assertEquals(SendingStage.CONFIRM, dto.getStage()),
                () -> assertNull(dto.getStageStatus()),
                () -> assertTrue(dto.isSendAfterGeneration())
        );
    }

    /**
     * При вызове ручки POST /api/sending/push/{}/schedule_generation пользователем с ролью AGENT
     * и sendAfterGeneration=false
     * 1. Рассылка переводится в состояние GENERATE (FINISHED)
     * 2. Время переданное в качестве startTime устанавливается в качестве значения поля scheduled_time
     */
    @Test
    void testScheduleGenerationWithoutSendingByAgentAuthor() throws Exception {
        var agent = prepareProfile("agent", Roles.AGENT);
        SecurityUtils.setAuthentication(agent);
        var sending = prepareSending();

        var uploadSettings = new UploadSettings();
        uploadSettings.setFinishLimit(LocalTime.of(18, 0));
        uploadSettings.setEnableMaxRate(true);

        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);

        var dto = requestGenerationScheduling(sending, scheduleTime, false, uploadSettings);

        assertAll(
                () -> assertEquals(scheduleTime, dto.getScheduleTime()),
                () -> assertEquals(SendingStage.GENERATE, dto.getStage()),
                () -> assertEquals(StageStatus.SCHEDULED, dto.getStageStatus()),
                () -> assertFalse(dto.isSendAfterGeneration())
        );
    }

    /**
     * При вызове ручки POST /api/sending/push/{}/schedule_generation/confirm пользователем с ролью AGENT,
     * являющимся автором рассылки, в ответ возвращается 403
     */
    @Test
    void test403OnConfirmSchedulingByAgentAuthor() throws Exception {
        var agent = prepareProfile("agent", Roles.AGENT);
        SecurityUtils.setAuthentication(agent);
        var sending = prepareSending();

        var sendingId = sending.getId();
        sendingDAO.updateSchedulingFields(sendingId, LocalDateTime.now().plusDays(7), SendingStage.CONFIRM,
                null,false);

        makeConfirmSchedulingRequest(sending)
                .andExpect(status().isForbidden());
    }

    @Test
    void testConfirmScheduling() throws Exception {
        var sending = prepareSending();

        var sendingId = sending.getId();
        var scheduleTime = LocalDateTime.now().plusDays(7).truncatedTo(ChronoUnit.SECONDS);
        sendingDAO.updateSchedulingFields(sendingId, scheduleTime, SendingStage.CONFIRM, null,false);

        var dto = requestConfirmScheduling(sending);

        assertAll(
                () -> assertEquals(SendingStage.GENERATE, dto.getStage()),
                () -> assertEquals(StageStatus.SCHEDULED, dto.getStageStatus()),
                () -> assertEquals(scheduleTime, dto.getScheduleTime())
        );
    }

    @Nonnull
    private PushPlainSendingDto requestGenerationScheduling(PushPlainSending sending,
                                                            LocalDateTime scheduleTime,
                                                            boolean sendAfterGeneration,
                                                            UploadSettings uploadSettings) throws Exception {
        var response = makeScheduleRequest(sending, scheduleTime, sendAfterGeneration, uploadSettings)
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return jsonDeserializer.readObject(PushPlainSendingDto.class, response.getContentAsString());
    }

    private ResultActions makeScheduleRequest(PushPlainSending sending,
                                              LocalDateTime scheduleTime,
                                              boolean sendAfterGeneration,
                                              UploadSettings uploadSettings) throws Exception {
        var requestBody = new SchedulePushGenerationRequest();
        requestBody.setStartTime(scheduleTime);
        requestBody.setSendAfterGeneration(sendAfterGeneration);
        requestBody.setUploadSettings(uploadSettings);

        var request = post("/api/sendings/push/{id}/schedule_generation", sending.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(requestBody));

        return mockMvc.perform(request)
                .andDo(print());
    }

    private ResultActions makeConfirmSchedulingRequest(PushPlainSending sending) throws Exception {
        var request = post(
                "/api/sendings/push/{id}/schedule_generation/confirm", sending.getId()
        );

        return mockMvc.perform(request)
                .andDo(print());
    }

    @Nonnull
    private PushPlainSendingDto requestConfirmScheduling(PushPlainSending sending) throws Exception {
        var response = makeConfirmSchedulingRequest(sending)
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return jsonDeserializer.readObject(PushPlainSendingDto.class, response.getContentAsString());
    }

    private BlackboxProfile prepareProfile(String login, String role) {
        var profile = SecurityUtils.profile(login);
        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, role));
        return profile;
    }

    private PushPlainSendingDto requestCopy(PushPlainSending sending) throws Exception {
        var oldId = sending.getId();
        sending.setId(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) + "_" + UUID.randomUUID());
        return jsonDeserializer.readObject(
                PushPlainSendingDto.class,
                mockMvc.perform(post("/api/sendings/push/{id}/copy", oldId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsString(pushSendingConverter.convert(sending))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        );
    }

    private void sendTest(TestDevicesGroup... groups) throws Exception {
        mockMvc.perform(post("/api/sendings/push/{id}/send-test", sending.getId())
                .param("version", "3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(Arrays.asList(groups))))
                .andDo(print())
                .andExpect(status().isOk());
    }

    private ResultActions requestUpdate(PushPlainSendingDto dto) throws Exception {
        return mockMvc.perform(post("/api/sendings/push/update/{id}", dto.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(dto)))
                .andDo(print());
    }

    private PushPlainSendingDto requestSending(String sendingId) throws Exception {
        var response =  mockMvc.perform(get("/api/sendings/push/{id}", sendingId))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        return jsonDeserializer.readObject(PushPlainSendingDto.class, response.getContentAsString());
    }

    @Nonnull
    private PushPlainSending prepareBuiltSending(PushSendingVariantConf... variants) {
        return prepareBuiltSending(config(variants));
    }

    @Nonnull
    private PushPlainSending prepareBuiltSending(BlackboxProfile profile, PushSendingVariantConf... variants) {
        SecurityUtils.setAuthentication(profile);
        return prepareBuiltSending(variants);
    }

    @Nonnull
    private PushPlainSending prepareBuiltSending(LocalDateTime generationTime, PushSendingConf config) {
        var sending = prepareSending(config);

        sending.setStageAndStatus(SendingStage.GENERATE, StageStatus.FINISHED);
        sending.setGenerationTime(generationTime);
        sendingDAO.updateSendingStates(sending.getId(), sending);
        return sending;
    }

    @Nonnull
    private PushPlainSending prepareBuiltSending(PushSendingConf config) {
        return prepareBuiltSending(LocalDateTime.now(), config);
    }

    @Nonnull
    private PushPlainSending prepareSending(PushSendingConf config) {
        return pushSendingTestHelper.prepareSending(config);
    }

    @Nonnull
    private PushPlainSending prepareSending() {
        var config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        return prepareSending(config);
    }

    private void testCopyPushSendingChangeUtmCampaign(String action, String expectedAction) throws Exception {
        PushSendingVariantConf variant = PushSendingTestHelper.variant(
                "variant_a",
                100,
                "test_title",
                action
        );
        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setVariants(Collections.singletonList(variant));
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());

        PushPlainSending sending = pushSendingTestHelper.prepareSending(config);

        var copy = requestCopy(sending);

        assertEquals(1, copy.getConfig().getVariants().size());

        PushSendingVariantConf variantCopy = copy.getConfig().getVariants().get(0);
        for (var platformConfig : variantCopy.getPushConfigs().values()) {
            assertEquals(String.format(expectedAction, variantCopy.getId()), platformConfig.getAction());
        }
    }
}
