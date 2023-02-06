package ru.yandex.market.crm.campaign.http.controller;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.grouping.campaign.Campaign;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule;
import ru.yandex.market.crm.campaign.domain.periodic.Schedule.DateTimeInterval;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.PushPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.dto.actions.PeriodicActionDto;
import ru.yandex.market.crm.campaign.dto.actions.PlainActionDto;
import ru.yandex.market.crm.campaign.dto.campaign.AbstractPromoEntityDto;
import ru.yandex.market.crm.campaign.dto.sending.EmailPeriodicSendingDto;
import ru.yandex.market.crm.campaign.dto.sending.EmailPlainSendingDto;
import ru.yandex.market.crm.campaign.dto.sending.PushPeriodicSendingDto;
import ru.yandex.market.crm.campaign.dto.sending.PushPlainSendingDto;
import ru.yandex.market.crm.campaign.http.response.ErrorResponse;
import ru.yandex.market.crm.campaign.services.actions.PeriodicActionDAO;
import ru.yandex.market.crm.campaign.services.grouping.campaign.CampaignDAO;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.services.sending.EmailSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.dao.EmailPeriodicSendingDAO;
import ru.yandex.market.crm.campaign.services.sending.dao.PushPeriodicSendingDAO;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.SecurityUtils;
import ru.yandex.market.crm.dao.UsersRolesDao;
import ru.yandex.market.crm.domain.Account;
import ru.yandex.market.crm.domain.CompositeUserRole;
import ru.yandex.market.crm.http.security.BlackboxProfile;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.config;
import static ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.plusFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class CampaignControllerTest extends AbstractControllerMediumTest {

    private static EmailPlainSendingDto emailSending(String id) {
        EmailPlainSendingDto body = new EmailPlainSendingDto();
        body.setId(id);
        body.setName("Email Sending");
        return body;
    }

    private static void assertDefaultEmailConfig(EmailSendingConf config) {
        Assertions.assertNotNull(config);
        Assertions.assertNotNull(config.getVariants());
        Assertions.assertTrue(config.isGlobalControlEnabled());

        Map<String, Object> properties = config.getProperties();
        Assertions.assertNotNull(properties);
        Assertions.assertTrue(properties.containsKey("period"));
        Assertions.assertTrue(properties.containsKey("exclude_models_date_start"));
    }

    private static EmailPlainSendingDto emailSending() {
        return emailSending("test_email_sending");
    }

    @NotNull
    private static Schedule schedule() {
        return new Schedule()
                .setTime(LocalTime.of(10, 30))
                .setDaysOfWeek(Set.of(DayOfWeek.MONDAY))
                .setActiveInterval(
                        new DateTimeInterval()
                                .setBegin(LocalDateTime.now())
                                .setEnd(LocalDateTime.now().plusMonths(1))
                );
    }

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private EmailSendingTestHelper emailSendingTestHelper;

    @Inject
    private CampaignDAO campaignDAO;

    @Inject
    private UsersRolesDao usersRolesDao;

    @Inject
    private EmailPeriodicSendingDAO emailPeriodicSendingDAO;

    @Inject
    private EmailSendingDAO emailSendingDAO;

    @Inject
    private EmailPeriodicSendingTestHelper emailPeriodicSendingTestHelper;

    @Inject
    private PushPeriodicSendingTestHelper pushPeriodicSendingTestHelper;

    @Inject
    private PushPeriodicSendingDAO pushPeriodicSendingDAO;

    @Inject
    private SegmentService segmentService;

    @Inject
    private PushSendingTestHelper pushSendingTestHelper;

    @Inject
    private PeriodicActionDAO periodicActionDAO;

    @Test
    public void testAddCampaign() throws Exception {
        var campaigns = campaignDAO.listAll();
        var initialSize = campaigns.size();

        String campaignName = "testCampaign";
        requestCreateCampaign(campaignName).andExpect(status().isOk());
        campaigns = campaignDAO.listAll();

        Assertions.assertEquals(initialSize + 1, campaigns.size());
        Assertions.assertEquals(campaignName, campaigns.get(0).getName());
    }

    @Test
    public void testUpdateCampaign() throws Exception {
        var campaigns = campaignDAO.listAll();
        var initialSize = campaigns.size();

        String campaignName = "testCampaign";
        requestCreateCampaign(campaignName).andExpect(status().isOk());

        campaigns = campaignDAO.listAll();
        var campaign = campaigns.get(0);
        String newName = "newName";
        campaign.setName(newName);

        mockMvc.perform(put("/api/campaigns/{id}", campaign.getId())
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(campaign)))
                .andDo(print()).andExpect(status().isOk()).andReturn();

        campaigns = campaignDAO.listAll();

        Assertions.assertEquals(initialSize + 1, campaigns.size());
        Assertions.assertEquals(newName, campaigns.get(0).getName());
    }

    @Test
    public void testDeleteCampaign() throws Exception {
        var campaigns = campaignDAO.listAll();
        var initialSize = campaigns.size();

        String campaignName = "testCampaign";
        requestCreateCampaign(campaignName).andExpect(status().isOk());

        campaigns = campaignDAO.listAll();
        var campaign = campaigns.get(0);
        String newName = "newName";
        campaign.setName(newName);
        mockMvc.perform(delete("/api/campaigns/{id}", campaign.getId()))
                .andExpect(status().isOk());

        campaigns = campaignDAO.listAll();

        Assertions.assertEquals(initialSize, campaigns.size());
    }

    /**
     * Нельзя добавить акцию с id совпадающим с id существующей промо-рассылки любого типа
     */
    @Test
    public void test400OnAddActionWithSameIdAsEmailSending() throws Exception {
        EmailPlainSending emailSending = emailSendingTestHelper.saveSending();

        PlainActionDto body = new PlainActionDto();
        body.setName("Action");
        body.setId(emailSending.getId());

        requestActionAddAssertError(emailSending.getCampaignId(), body);
    }

    /**
     * Нельзя добавить рассылку с id совпадающим с id существующей акции
     */
    @Test
    public void test400OnAddEmailSendingWithSameIdAsAction() throws Exception {
        PlainAction action = actionTestHelper.prepareActionWithVariants("segment_id", LinkingMode.NONE);

        EmailPlainSendingDto emailSending = emailSending(action.getId());

        requestAddEmailSending(action.getCampaignId(), emailSending)
                .andExpect(status().isBadRequest());

    }

    /**
     * Пользователь с ролью "Наблюдатель" не может добавить email-рассылку
     */
    @Test
    public void testUserWithViewerRoleIsNotAllowedToAddEmailSending() throws Exception {
        BlackboxProfile profile = SecurityUtils.profile("viewer_profile");
        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.VIEWER));

        SecurityUtils.setAuthentication(profile);

        Campaign campaign = prepareCampaign();

        EmailPlainSendingDto sending = emailSending();

        MockHttpServletResponse response = requestAddEmailSending(campaign.getId(), sending)
                .andExpect(status().isForbidden())
                .andReturn().getResponse();

        ErrorResponse errorResponse = jsonDeserializer.readObject(
                ErrorResponse.class,
                response.getContentAsString()
        );

        Assertions.assertEquals(
                "Required permissions: 'CREATE_PROMOS'",
                errorResponse.getMessage()
        );
    }

    /**
     * Пользователь с ролью "Оператор" может создавать рассылки
     */
    @Test
    public void testUserWithRoleOperatorIsAllowedToAddEmailSending() throws Exception {
        BlackboxProfile profile = SecurityUtils.profile("operator_profile");

        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.OPERATOR));

        SecurityUtils.setAuthentication(profile);

        Campaign campaign = prepareCampaign();

        EmailPlainSendingDto sending = emailSending();

        requestAddEmailSending(campaign.getId(), sending)
                .andExpect(status().isOk());

        List<EmailPlainSending> sendings = emailSendingDAO.getSendingsOfCampaign(campaign.getId());
        Assertions.assertEquals(1, sendings.size());
        assertDefaultEmailConfig(sendings.get(0).getConfig());
    }

    /**
     * Периодическая email-рассылка добавляется в кампанию через вызов ручки
     * GET /api/campaigns/{id}/periodic_sendings/email
     * <p>
     * При этом для нее заполняется конфигурация по умолчанию
     */
    @Test
    public void testAddNewEmailPeriodicSending() throws Exception {
        Campaign campaign = prepareCampaign();

        EmailPeriodicSendingDto dto = new EmailPeriodicSendingDto();
        dto.setKey("test_sending");
        dto.setName("Test sending");
        dto.setSchedule(schedule());

        requestAddEmailPeriodicSending(campaign.getId(), dto)
                .andExpect(status().isOk());

        List<EmailPeriodicSending> sendings = emailPeriodicSendingDAO.getOfCampaign(campaign.getId());
        Assertions.assertEquals(1, sendings.size());

        EmailPeriodicSending sending = sendings.get(0);
        Assertions.assertEquals(dto.getKey(), sending.getKey());
        Assertions.assertEquals(1, sending.getVersion());
        Assertions.assertEquals(dto.getKey() + ":1", sending.getId());
        Assertions.assertEquals(dto.getName(), sending.getName());
        Assertions.assertEquals(0, sending.getIteration());
        Assertions.assertEquals(campaign.getId(), sending.getCampaignId());
        Assertions.assertNotNull(sending.getSchedule());

        EmailSendingConf config = sending.getConfig();
        assertDefaultEmailConfig(config);
    }

    /**
     * Нельзя добавить периодическую email-рассылку с пустым расписанием
     */
    @Test
    public void test400OnAddPeriodicSendingWithEmptySchedule() throws Exception {
        Campaign campaign = prepareCampaign();

        EmailPeriodicSendingDto dto = new EmailPeriodicSendingDto();
        dto.setId("test_sending");
        dto.setName("Test sending");
        dto.setSchedule(new Schedule());

        requestAddEmailPeriodicSending(campaign.getId(), dto)
                .andExpect(status().isBadRequest());
    }

    /**
     * В выдаче ручки GET /api/campaigns/{id}/periodic_sendings/email
     * присутствуют все периодические email-рассылки кампании с идентификатором id.
     */
    @Test
    public void testGetPeriodicEmailSendings() throws Exception {
        Campaign campaign1 = prepareCampaign();
        EmailPeriodicSending sending1 = emailPeriodicSendingTestHelper.prepareSending(campaign1);
        EmailPeriodicSending sending2 = emailPeriodicSendingTestHelper.prepareSending(campaign1);

        Campaign campaign2 = prepareCampaign();
        emailPeriodicSendingTestHelper.prepareSending(campaign2);

        MvcResult result = mockMvc.perform(get("/api/campaigns/{id}/periodic_sendings/email", campaign1.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        List<EmailPeriodicSendingDto> sendings = jsonDeserializer.readObject(
                new TypeReference<>() {
                },
                result.getResponse().getContentAsString()
        );

        List<String> sendingIds = sendings.stream()
                .map(AbstractPromoEntityDto::getId)
                .collect(Collectors.toList());

        Assertions.assertEquals(List.of(sending2.getId(), sending1.getId()), sendingIds);

        EmailPeriodicSendingDto dto = sendings.get(1);
        Assertions.assertNotNull(dto.getName());
        Assertions.assertNotNull(dto.getCreationTime());
        Assertions.assertNotNull(dto.getConfig());
        Assertions.assertNotNull(dto.getKey());
        Assertions.assertNotNull(dto.getVersion());
        Assertions.assertNotNull(dto.getSecurityPermissions());
    }

    /**
     * Нельзя добавить регулярную email-рассылку ключ которой совпадает с id обычной email-рассылки
     */
    @Test
    public void test400OnAddEmailPeriodicSendingWithKeySameAsEmailPlainSendingId() throws Exception {
        EmailPlainSending plainSending = emailSendingTestHelper.saveSending();

        EmailPeriodicSendingDto dto = new EmailPeriodicSendingDto();
        dto.setKey(plainSending.getId());
        dto.setName("Test sending");
        dto.setSchedule(schedule());

        requestAddEmailPeriodicSending(plainSending.getCampaignId(), dto)
                .andExpect(status().isBadRequest());
    }

    /**
     * Нельзя добавить обычную email-рассылу идентификатор которой совпадает с ключем регулярной email-рассылки
     */
    @Test
    public void test400OnAddEmailPlainSendingWithIdSameAsEmailPeriodicSendingKey() throws Exception {
        EmailPeriodicSending periodicSending = emailPeriodicSendingTestHelper.prepareSending();

        EmailPlainSendingDto emailSending = emailSending(periodicSending.getKey());

        requestAddEmailSending(periodicSending.getCampaignId(), emailSending)
                .andExpect(status().isBadRequest());
    }

    /**
     * Нельзя добавить акцию id которой совпадает с ключем регулярной email-рассылки
     */
    @Test
    public void test400OnAddActionWithSameIdAsEmailPeriodicSendingKey() throws Exception {
        EmailPeriodicSending periodicSending = emailPeriodicSendingTestHelper.prepareSending();

        PlainActionDto body = new PlainActionDto();
        body.setName("Action");
        body.setId(periodicSending.getKey());

        requestActionAddAssertError(periodicSending.getCampaignId(), body);
    }

    /**
     * Периодическая push-рассылка добавляется в кампанию через вызов ручки
     * GET /api/campaigns/{id}/periodic_sendings/push
     * <p>
     * При этом для нее заполняется конфигурация по умолчанию
     */
    @Test
    public void testAddNewPromoPeriodicSending() throws Exception {
        Campaign campaign = prepareCampaign();

        PushPeriodicSendingDto dto = new PushPeriodicSendingDto();
        dto.setKey("test_sending");
        dto.setName("Test sending");
        dto.setSchedule(schedule());

        requestAddPushPeriodicSending(campaign.getId(), dto)
                .andExpect(status().isOk());

        List<PushPeriodicSending> sendings = pushPeriodicSendingDAO.getOfCampaign(campaign.getId());
        Assertions.assertEquals(1, sendings.size());

        PushPeriodicSending sending = sendings.get(0);
        Assertions.assertEquals(dto.getKey(), sending.getKey());
        Assertions.assertEquals(1, sending.getVersion());
        Assertions.assertEquals(dto.getKey() + ":1", sending.getId());
        Assertions.assertEquals(dto.getName(), sending.getName());
        Assertions.assertEquals(0, sending.getIteration());
        Assertions.assertEquals(campaign.getId(), sending.getCampaignId());
        Assertions.assertNotNull(sending.getSchedule());

        PushSendingConf config = sending.getConfig();
        Assertions.assertNotNull(config);
        Assertions.assertTrue(config.isGlobalControlEnabled());

        Assertions.assertNull(sending.getSendJobName());
        Assertions.assertNull(sending.getEndNotificationJobName());
    }

    /**
     * Нельзя добавить регулярную push-рассылку с ключем, совпадающим с id существующей
     * обычной push-рассылки
     */
    @Test
    public void test400OnAddPushPeriodicSendingWithKeyMatchingWithPlainSendingId() throws Exception {
        Campaign campaign = prepareCampaign();

        Segment segment = segmentService.addSegment(segment(plusFilter()));
        PushPlainSending sending = pushSendingTestHelper.prepareSending(config(segment, variant()));

        PushPeriodicSendingDto dto = new PushPeriodicSendingDto();
        dto.setKey(sending.getId());
        dto.setName("Test sending");
        dto.setSchedule(schedule());

        requestAddPushPeriodicSending(campaign.getId(), dto)
                .andExpect(status().isBadRequest());
    }

    /**
     * Нельзя добавить обычную push-рассылку с id, совпадающим с ключем существующей
     * регулярной push-рассылки
     */
    @Test
    public void test400OnAddPushSendingWithIdMatchingPeriodicSendingKey() throws Exception {
        Campaign campaign = prepareCampaign();

        PushPeriodicSending sending = pushPeriodicSendingTestHelper.addSending();

        PushPlainSendingDto dto = new PushPlainSendingDto();
        dto.setId(sending.getKey());
        dto.setName("Test sending");

        mockMvc.perform(post("/api/campaigns/{id}/sendings/push", campaign.getId())
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAddPeriodicAction() throws Exception {
        long campaign = prepareCampaign().getId();

        PeriodicActionDto dto = new PeriodicActionDto();
        dto.setKey("test_action");
        dto.setName("Test Action");
        dto.setSchedule(schedule());

        requestAddPeriodicAction(campaign, dto)
                .andExpect(status().isOk());

        List<PeriodicAction> actions = periodicActionDAO.getOfCampaign(campaign);
        MatcherAssert.assertThat(actions, hasSize(1));

        PeriodicAction action = actions.get(0);
        Assertions.assertNotNull(action.getId());
        Assertions.assertEquals(dto.getKey(), action.getKey());
        Assertions.assertEquals(1, action.getVersion());
        Assertions.assertFalse(action.isExecuted());
        Assertions.assertTrue(action.isLastVersion());
        Assertions.assertFalse(action.isEnabled());
    }

    @Nonnull
    private ResultActions requestCreateCampaign(String campaignName) throws Exception {
        var campaign = new Campaign();
        campaign.setName(campaignName);
        return mockMvc.perform(post("/api/campaigns/")
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(campaign)))
                .andDo(print());
    }

    @Nonnull
    private ResultActions requestAddEmailPeriodicSending(long campaignId,
                                                         EmailPeriodicSendingDto dto) throws Exception {
        return mockMvc.perform(post("/api/campaigns/{id}/periodic_sendings/email", campaignId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print());
    }

    @Nonnull
    private ResultActions requestAddPushPeriodicSending(long campaignId,
                                                        PushPeriodicSendingDto dto) throws Exception {
        return mockMvc.perform(post("/api/campaigns/{id}/periodic_sendings/push", campaignId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print());
    }

    @Nonnull
    private ResultActions requestAddPeriodicAction(long campaignId, PeriodicActionDto dto) throws Exception {
        return mockMvc.perform(post("/api/campaigns/{id}/periodic_actions", campaignId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(dto)))
                .andDo(print());
    }

    private Campaign prepareCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Test campaign");
        return campaignDAO.insert(campaign);
    }

    private ResultActions requestAddEmailSending(long campaignId, EmailPlainSendingDto emailSending) throws Exception {
        return mockMvc.perform(post("/api/campaigns/{id}/sendings/email", campaignId)
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(emailSending)))
                .andDo(print());
    }

    private void requestActionAddAssertError(long campaignId, PlainActionDto body) throws Exception {
        mockMvc.perform(post("/api/campaigns/{id}/actions", campaignId)
                .contentType("application/json")
                .content(jsonSerializer.writeObjectAsString(body)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
