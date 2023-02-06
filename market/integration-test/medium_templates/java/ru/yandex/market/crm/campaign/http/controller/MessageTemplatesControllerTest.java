package ru.yandex.market.crm.campaign.http.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.crm.campaign.domain.actions.PeriodicAction;
import ru.yandex.market.crm.campaign.domain.actions.PlainAction;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.utils.NamedEntity;
import ru.yandex.market.crm.campaign.dto.actions.PeriodicActionDto;
import ru.yandex.market.crm.campaign.dto.actions.PlainActionDto;
import ru.yandex.market.crm.campaign.dto.message.CopyTemplateRequest;
import ru.yandex.market.crm.campaign.dto.message.EmailMessageTemplateDto;
import ru.yandex.market.crm.campaign.dto.message.MessageTemplateDto;
import ru.yandex.market.crm.campaign.dto.message.PushMessageTemplateDto;
import ru.yandex.market.crm.campaign.http.response.ErrorResponse;
import ru.yandex.market.crm.campaign.services.converter.message.MessageTemplateConverter;
import ru.yandex.market.crm.campaign.services.messages.PluggableTablesInMessageTemplatesDAO;
import ru.yandex.market.crm.campaign.services.security.Roles;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.ActionTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailTemplatesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PeriodicActionsTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushTemplatesTestHelper;
import ru.yandex.market.crm.core.domain.messages.AbstractPushConf;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.EmailMessageConf;
import ru.yandex.market.crm.core.domain.messages.IosPushConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.templates.BlockTemplate;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.ActionTestHelper.sendEmails;

/**
 * @author apershukov
 */
public class MessageTemplatesControllerTest extends AbstractControllerMediumTest {

    @Inject
    private ActionTestHelper actionTestHelper;

    @Inject
    private EmailTemplatesTestHelper emailTemplatesTestHelper;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private JsonSerializer jsonSerializer;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private MessageTemplateConverter messageTemplateConverter;

    @Inject
    private PluggableTablesInMessageTemplatesDAO tablesInTemplatesDAO;

    @Inject
    private UsersRolesDao usersRolesDao;

    @Inject
    private PeriodicActionsTestHelper periodicActionsTestHelper;

    @Inject
    private PushTemplatesTestHelper pushTemplatesTestHelper;

    @Test
    public void testReturnLinkedActions() throws Exception {
        var template = emailTemplatesTestHelper.prepareEmailTemplate();

        PlainAction action = prepareAction(template.getId());

        EmailMessageTemplateDto response = requestTemplate(template);

        Assertions.assertFalse(response.canDelete());

        List<PlainActionDto> linkedActions = response.getLinkedPlainActions();
        Assertions.assertNotNull(linkedActions);
        Assertions.assertEquals(1, linkedActions.size());
        Assertions.assertEquals(action.getId(), linkedActions.get(0).getId());
        Assertions.assertEquals(action.getName(), linkedActions.get(0).getName());
    }

    @Test
    public void testCannotDeleteTemplateWithOlderVersionUsedInAction() throws Exception {
        var oldVersion = emailTemplatesTestHelper.prepareEmailTemplate();
        prepareAction(oldVersion.getId());

        var newVersion = emailTemplatesTestHelper.createNewVersion(oldVersion);

        EmailMessageTemplateDto response = requestTemplate(newVersion);

        Assertions.assertFalse(response.canDelete());
        Assertions.assertTrue(response.getLinkedPlainActions().isEmpty());
    }

    /**
     * В случае когда подключаемая таблица включается в шаблон при его редактировании
     * она отмечается как используемая в шаблоне
     */
    @Test
    public void testWhenTableIsPluggedOnEditItIsMarkedAsUsedByTemplate() throws Exception {
        var template = emailTemplatesTestHelper.prepareEmailTemplate();

        PluggableTable table = preparePluggableTable();
        template.getConfig()
                .setPluggedTables(List.of(new PluggedTable(table.getId(), "table")));

        var dto = makeUpdateRequest(template);

        List<NamedEntity> templates = tablesInTemplatesDAO.getTemplatesUsingTable(table.getId());
        Assertions.assertEquals(1, templates.size());
        Assertions.assertEquals(dto.getId(), templates.get(0).getId());
    }

    /**
     * В случае если подключенная таблица удаляется из шаблона при редактировании она
     * перестает отмечаться как используемая
     */
    @Test
    public void testWhenTableIsRemovedFromTemplateItIsNotMarkedAsUsed() throws Exception {
        PluggableTable table = preparePluggableTable();

        var template = prepareTemplateWithPluggedTable(table);

        template.getConfig().setPluggedTables(Collections.emptyList());

        makeUpdateRequest(template);

        Assertions.assertFalse(tablesInTemplatesDAO.isUsed(table.getId()), "Table is still used somewhere");
    }

    @Test
    public void testMessageTemplateCopy() throws Exception {
        PluggableTable table = preparePluggableTable();

        var template = prepareTemplateWithPluggedTable(table);

        var newName = "templateCopyNameTesting";
        var copy = makeCopyMessageTemplateRequest(template, newName, MessageTemplateDto.class);

        Assertions.assertNotEquals(copy.getId(), template.getId());
        Assertions.assertNotEquals(copy.getKey(), template.getKey());

        Assertions.assertEquals(copy.getName(), newName);
        Assertions.assertEquals(copy.getType(), template.getType());
        Assertions.assertEquals(copy.getVersion(), template.getVersion());

        EmailMessageConf copyConfig = ((EmailMessageTemplateDto) copy).getConfig();
        EmailMessageConf originalConfig = template.getConfig();
        checkEmailMessageConfIsEquals(copyConfig, originalConfig);
    }

    @Test
    public void testPushMessageTemplateCopyReplaceUtmCampaignMiddle() throws Exception {
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

        testCopyPushWithAction(action, expectedAction);
    }

    @Test
    public void testPushMessageTemplateCopyReplaceUtmCampaignEnd() throws Exception {
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
        testCopyPushWithAction(action, expectedAction);
    }

    @Test
    public void testWhenTemplateIsRemovedTablePreviouslyUsedInItIsUnmarkedAsUsed() throws Exception {
        PluggableTable table = preparePluggableTable();

        var template = prepareTemplateWithPluggedTable(table);

        mockMvc.perform(delete("/api/message-templates/by-key/{id}", template.getKey()))
                .andDo(print())
                .andExpect(status().isOk());

        Assertions.assertFalse(tablesInTemplatesDAO.isUsed(table.getId()), "Table is still used somewhere");
    }

    /**
     * Пользователь с ролью "Агент" не может активировать шаблоны сообщений
     */
    @Test
    public void testUserWithAgentRoleIsNotAllowedToActivateMessageTemplate() throws Exception {
        BlackboxProfile profile = SecurityUtils.profile("viewer_profile");
        usersRolesDao.addRole(profile.getUid(), new CompositeUserRole(Account.MARKET_ACCOUNT, Roles.AGENT));

        SecurityUtils.setAuthentication(profile);

        PluggableTable table = preparePluggableTable();
        var template = prepareTemplateWithPluggedTable(table);

        MockHttpServletResponse response = mockMvc
                .perform(post("/api/message-templates/by-id/{id}/activate", template.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSerializer.writeObjectAsBytes(List.of("1", "2"))))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andReturn().getResponse();

        ErrorResponse errorResponse = jsonDeserializer.readObject(
                ErrorResponse.class,
                response.getContentAsString()
        );

        Assertions.assertEquals(
                "Required permissions: 'ACTIVATE_MESSAGE_TEMPLATES'",
                errorResponse.getMessage()
        );
    }

    /**
     * Tсли шаблон используется в акции она присутствует в поле linkedPeriodicActions.
     * При этом флаг canDelete сбрасывается.
     */
    @Test
    public void testReturnLinkedPeriodicActions() throws Exception {
        var template = emailTemplatesTestHelper.prepareEmailTemplate();
        ActionStep sendStep = sendEmails(template.getId());
        PeriodicAction action = periodicActionsTestHelper.prepareAction(sendStep);

        EmailMessageTemplateDto response = requestTemplate(template);

        Assertions.assertFalse(response.canDelete());

        List<PeriodicActionDto> linkedActions = response.getLinkedPeriodicActions();
        MatcherAssert.assertThat(linkedActions, hasSize(1));

        PeriodicActionDto linkedAction = linkedActions.get(0);
        Assertions.assertEquals(action.getName(), linkedAction.getName());
        Assertions.assertEquals(action.getId(), linkedAction.getId());
        Assertions.assertEquals(action.getVersion(), linkedAction.getVersion());
    }

    private MessageTemplate<EmailMessageConf> prepareTemplateWithPluggedTable(PluggableTable table) {
        BannerBlockConf bannerBlock = emailTemplatesTestHelper.prepareBannerBlock();
        BlockTemplate messageTemplate = emailTemplatesTestHelper.prepareMessageTemplate();

        EmailMessageConf config = new EmailMessageConf();
        config.setTemplate(messageTemplate.getId());
        config.setBlocks(List.of(bannerBlock));
        config.setSubject("Email message template");
        config.setPluggedTables(List.of(new PluggedTable(table.getId(), "table")));

        return emailTemplatesTestHelper.prepareEmailTemplate(config);
    }

    private MessageTemplateDto<?> makeUpdateRequest(MessageTemplate<EmailMessageConf> template) throws Exception {
        MessageTemplateDto<?> dto = messageTemplateConverter.convert(template, true);

        MvcResult result = mockMvc.perform(post("/api/message-templates/by-key/{key}/update", template.getKey())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        dto = jsonDeserializer.readObject(
                EmailMessageTemplateDto.class,
                result.getResponse().getContentAsString()
        );
        return dto;
    }

    private EmailMessageTemplateDto requestTemplate(MessageTemplate<EmailMessageConf> template) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/message-templates/by-id/{id}", template.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        return jsonDeserializer.readObject(
                EmailMessageTemplateDto.class,
                result.getResponse().getContentAsByteArray()
        );
    }

    private PlainAction prepareAction(String tempateId) {
        return actionTestHelper.prepareAction(
                "unknown",
                LinkingMode.NONE,
                sendEmails(tempateId)
        );
    }

    private PluggableTable preparePluggableTable() {
        return pluggableTablesTestHelper.preparePluggableTable();
    }

    private <T extends MessageTemplateDto<?>> T makeCopyMessageTemplateRequest(
            MessageTemplate<?> messageTemplate,
            String newName,
            Class<T> clazz) throws Exception {

        var messageTemplateCopyDto = new CopyTemplateRequest();
        messageTemplateCopyDto.setId(messageTemplate.getId());
        messageTemplateCopyDto.setName(newName);

        MvcResult result = mockMvc.perform(post("/api/message-templates/copy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(messageTemplateCopyDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        return jsonDeserializer.readObject(
                clazz,
                result.getResponse().getContentAsString()
        );
    }

    private void testCopyPushWithAction(String action, String expectedAction) throws Exception {

        var conf1 = new PushMessageConf();
        AbstractPushConf confAndroid = new AndroidPushConf();
        confAndroid.setAction(action);

        AbstractPushConf confIos = new IosPushConf();
        confIos.setAction(action);

        conf1.setPushConfigs(Map.of(
                confAndroid.getPlatform(), confAndroid,
                confIos.getPlatform(), confIos
        ));
        var messageTemplate = pushTemplatesTestHelper.prepare(conf1);

        var newName = "templateCopyNameTesting";
        var copy = makeCopyMessageTemplateRequest(messageTemplate, newName, PushMessageTemplateDto.class);

        for (var platformConfig : copy.getConfig().getPushConfigs().values()) {
            Assertions.assertEquals(String.format(expectedAction, copy.getKey()), platformConfig.getAction());

        }
    }

    private void checkEmailMessageConfIsEquals(EmailMessageConf copy, EmailMessageConf original) {
        Assertions.assertEquals(copy.getSubject(), original.getSubject());
        Assertions.assertEquals(copy.getTemplate(), original.getTemplate());
        Assertions.assertEquals(copy.getPreheader(), original.getPreheader());
        Assertions.assertEquals(copy.getTitle(), original.getTitle());
        Assertions.assertEquals(copy.getBanners(), original.getBanners());
    }
}
