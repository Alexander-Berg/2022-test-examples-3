package ru.yandex.market.crm.campaign.http.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.PushPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.SendingType;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.PushSendingConf;
import ru.yandex.market.crm.campaign.domain.utils.NamedEntity;
import ru.yandex.market.crm.campaign.dto.pluggabletable.PluggableTableDto;
import ru.yandex.market.crm.campaign.services.messages.MessageTemplatesService;
import ru.yandex.market.crm.campaign.services.pluggabletable.PluggableTableService;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PushTemplatesTestHelper;
import ru.yandex.market.crm.core.domain.PagedResult;
import ru.yandex.market.crm.core.domain.ReactTableRequest;
import ru.yandex.market.crm.core.domain.messages.AndroidPushConf;
import ru.yandex.market.crm.core.domain.messages.MessageTemplate;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.messages.PushMessageConf;
import ru.yandex.market.crm.core.domain.mobile.MobileApplication;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.mapreduce.domain.mobileapp.MobilePlatform;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class PluggableTablesControllerTest extends AbstractControllerMediumTest {

    private static final TableSchema SIMPLE_SCHEMA = new TableSchema.Builder()
            .setUniqueKeys(false)
            .addValue("id", ColumnValueType.STRING)
            .build();

    @Inject
    private EmailSendingTestHelper emailSendingTestHelper;

    @Inject
    private PushSendingTestHelper pushSendingTestHelper;

    @Inject
    private PushTemplatesTestHelper pushTemplatesTestHelper;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private PluggableTableService pluggableTableService;

    @Inject
    private MessageTemplatesService messageTemplatesService;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private EmailPeriodicSendingTestHelper emailPeriodicSendingTestHelper;

    @Inject
    private PushPeriodicSendingTestHelper pushPeriodicSendingTestHelper;

    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;

    /**
     * Таблица, которая нигде не используется, удаляется через ручку DELETE /api/segments/pluggableTables/{id}
     */
    @Test
    public void testDeletePluggableTable() throws Exception {
        PluggableTable table = preparePluggableTable();

        requestDelete(table)
                .andExpect(status().isOk());

        PagedResult<?> result = pluggableTableService.listTables(new ReactTableRequest());
        Assertions.assertEquals(0, result.getElements().size());
    }

    /**
     * Таблицу, используемую в email-рассылке, удалить невозможно
     */
    @Test
    public void test400OnDeleteTableUsedInEmailSending() throws Exception {
        PluggableTable table = preparePluggableTable();
        prepareEmailSending(table);

        requestDelete(table)
                .andExpect(status().isBadRequest());
    }

    /**
     * Таблицу, используемую в push-рассылке, удалить невозможно
     */
    @Test
    public void test400OnDeleteTableUsedInPushSending() throws Exception {
        PluggableTable table = preparePluggableTable();
        preparePushSending(table);

        requestDelete(table)
                .andExpect(status().isBadRequest());
    }

    /**
     * Таблицу, используемую в шаблоне push-сообщения, удалить невозможно
     */
    @Test
    public void test400OnDeleteTableUsedInPushMessageTemplate() throws Exception {
        PluggableTable table = preparePluggableTable();
        prepareAndroidPushMessageTemplate(table);

        requestDelete(table)
                .andExpect(status().isBadRequest());
    }

    /**
     * Таблицу, используемую в периодической email-рассылке, удалить невозможно
     */
    @Test
    public void test400OnDeleteTableUsedInEmailPeriodicSending() throws Exception {
        PluggableTable pluggableTable = preparePluggableTable();

        emailPeriodicSendingTestHelper.prepareSending(s ->
                s.getConfig().setPluggedTables(List.of(
                        new PluggedTable(pluggableTable.getId(), "table")
                ))
        );

        requestDelete(pluggableTable)
                .andExpect(status().isBadRequest());
    }

    /**
     * Если таблица исползуется в email-рассылке, эта рассылка указывается в
     * выдаче ручки GET /api/segments/pluggableTables/{id}
     */
    @Test
    public void testEmailSendingsUsingTableAreSpecifiedInIt() throws Exception {
        PluggableTable table = preparePluggableTable();
        EmailPlainSending sending = prepareEmailSending(table);

        PluggableTableDto dto = requestTable(table);

        Map<SendingType, List<NamedEntity>> sendings = dto.getSendings();
        Assertions.assertNotNull(sendings);
        Assertions.assertEquals(3, sendings.size());

        List<NamedEntity> emailSendings = sendings.get(SendingType.EMAIL);
        Assertions.assertNotNull(emailSendings);
        Assertions.assertEquals(1, emailSendings.size());
        Assertions.assertEquals(sending.getId(), emailSendings.get(0).getId());
        Assertions.assertEquals(sending.getName(), emailSendings.get(0).getName());

        List<NamedEntity> pushSendings = sendings.get(SendingType.PUSH);
        Assertions.assertNotNull(pushSendings);
        Assertions.assertTrue(pushSendings.isEmpty());

        List<NamedEntity> gncSendings = sendings.get(SendingType.GNC);
        Assertions.assertNotNull(gncSendings);
        Assertions.assertTrue(gncSendings.isEmpty());
    }

    /**
     * Если таблица исползуется в шаблоне push-сообщения, этот шаблон указывается в
     * выдаче ручки GET /api/segments/pluggableTables/{id}
     */
    @Test
    public void testMessageTemplateUsingTableIsSpecifiedInIt() throws Exception {
        PluggableTable table = preparePluggableTable();
        var template = prepareAndroidPushMessageTemplate(table);
        messageTemplatesService.publish(template.getId());

        // Create a new version
        AndroidPushConf pushConf = (AndroidPushConf) template.getConfig().getPushConfigs().get(MobilePlatform.ANDROID);
        pushConf.setBanner("http://yandex.ru/new_background.jpg");
        template = (MessageTemplate<PushMessageConf>) messageTemplatesService.update(template.getKey(), template);
        Assertions.assertEquals(2, template.getVersion());

        PluggableTableDto dto = requestTable(table);

        List<NamedEntity> templates = dto.getMessageTemplates();
        Assertions.assertNotNull(templates);
        Assertions.assertEquals(1, templates.size());

        Assertions.assertEquals(template.getId(), templates.get(0).getId());
        Assertions.assertEquals(template.getName(), templates.get(0).getName());
    }

    /**
     * Если таблица исползуется в периодической email-рассылке, эта рассылка указывается в
     * выдаче ручки GET /api/segments/pluggableTables/{id}
     */
    @Test
    public void testEmailPeriodicSendingUsingTableIsSpecifiedInIt() throws Exception {
        PluggableTable pluggableTable = preparePluggableTable();

        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending(s ->
                s.getConfig().setPluggedTables(List.of(
                        new PluggedTable(pluggableTable.getId(), "table")
                ))
        );

        PluggableTableDto dto = requestTable(pluggableTable);

        Map<SendingType, List<NamedEntity>> periodicSendings = dto.getPeriodicSendings();
        Assertions.assertNotNull(periodicSendings);

        List<NamedEntity> emailSendings = periodicSendings.get(SendingType.EMAIL);
        Assertions.assertNotNull(emailSendings);
        Assertions.assertEquals(1, emailSendings.size());

        Assertions.assertEquals(sending.getId(), emailSendings.get(0).getId());
        Assertions.assertEquals(sending.getName(), emailSendings.get(0).getName());
    }

    /**
     * В случае если в запросе к ручке GET /api/segments/pluggableTables не указаны
     * параметры в ответе возвращаются все существующие таблицы
     */
    @Test
    public void testAllTablesIsReturnedIfNoRequestParamsSpecified() throws Exception {
        PluggableTable table1 = preparePluggableTable();
        PluggableTable table2 = pluggableTablesTestHelper.preparePluggableTable(UidType.PUID, SIMPLE_SCHEMA);

        Set<Long> tables = requestTables();

        Assertions.assertEquals(Set.of(table1.getId(), table2.getId()), tables);
    }

    /**
     * В случае если в запросе к ручке GET /api/segments/pluggableTables указан параметр
     * with_payload=true в ответ попадают только таблицы у которых помимо колонки с id
     * пользователя присутствуют колонки, способные нести какую-либо полезную информацию.
     * <p>
     * Нужно для интерфейса настройки таблиц, подключаемых к рассылкам и шаблонам сообщений
     */
    @Test
    public void testIfWithPayloadFlagIsSpecifiedOnlyTablesWithAdditionalAreReturned() throws Exception {
        PluggableTable pluggableTable = preparePluggableTable();
        pluggableTablesTestHelper.preparePluggableTable(UidType.PUID, SIMPLE_SCHEMA);

        Set<Long> tables = requestTables(request -> request.param("with_payload", "true"));

        Assertions.assertEquals(Set.of(pluggableTable.getId()), tables);
    }

    /**
     * Если таблица исползуется в периодической push-рассылке, эта рассылка указывается в
     * выдаче ручки GET /api/segments/pluggableTables/{id}
     */
    @Test
    public void testIfTableIsUsedInPeriodicPushSendingItIsShownInResponse() throws Exception {
        PluggableTable pluggableTable = preparePluggableTable();

        PushSendingConf config = pushPeriodicSendingTestHelper.prepareConfig();
        config.setPluggedTables(List.of(
                new PluggedTable(pluggableTable.getId(), "table")
        ));
        PushPeriodicSending sending = pushPeriodicSendingTestHelper.prepareSending(config);

        PluggableTableDto dto = requestTable(pluggableTable);

        Map<SendingType, List<NamedEntity>> periodicSendings = dto.getPeriodicSendings();
        Assertions.assertNotNull(periodicSendings);

        List<NamedEntity> relatedSendings = periodicSendings.get(SendingType.PUSH);
        MatcherAssert.assertThat(relatedSendings, hasSize(1));

        NamedEntity relatedSending = relatedSendings.get(0);
        Assertions.assertEquals(sending.getId(), relatedSending.getId());
        Assertions.assertEquals(sending.getName(), relatedSending.getName());
    }

    /**
     * Таблицу, используемую в регулярных push-рассылках, удалить нельзя
     */
    @Test
    public void test400OnDeleteTableUsedInPeriodicPushSendings() throws Exception {
        PluggableTable pluggableTable = preparePluggableTable();

        PushSendingConf config = pushPeriodicSendingTestHelper.prepareConfig();
        config.setPluggedTables(List.of(
                new PluggedTable(pluggableTable.getId(), "table")
        ));
        pushPeriodicSendingTestHelper.prepareSending(config);

        requestDelete(pluggableTable)
                .andExpect(status().isBadRequest());
    }

    @NotNull
    private Set<Long> requestTables(Consumer<MockHttpServletRequestBuilder> customizer) throws Exception {
        MockHttpServletRequestBuilder builder = get("/api/segments/pluggableTables");
        customizer.accept(builder);

        MvcResult result = mockMvc.perform(builder)
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        return jsonDeserializer.readObject(
                new TypeReference<List<PluggableTableDto>>() {
                },
                result.getResponse().getContentAsString()
        ).stream()
                .map(PluggableTableDto::getId)
                .collect(Collectors.toSet());
    }

    private Set<Long> requestTables() throws Exception {
        return requestTables(v -> {
        });
    }

    @NotNull
    private PluggableTableDto requestTable(PluggableTable table) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/segments/pluggableTables/{id}", table.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        PluggableTableDto dto = jsonDeserializer.readObject(
                PluggableTableDto.class,
                result.getResponse().getContentAsString()
        );

        Assertions.assertNotNull(dto);
        return dto;
    }

    @NotNull
    private ResultActions requestDelete(PluggableTable table) throws Exception {
        return mockMvc.perform(delete("/api/segments/pluggableTables/{id}", table.getId()))
                .andDo(print());
    }

    private PluggableTable preparePluggableTable() {
        return pluggableTablesTestHelper.preparePluggableTable();
    }

    private EmailPlainSending prepareEmailSending(PluggableTable table) {
        Segment segment = prepareSegment();

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock();
        EmailSendingVariantConf variant = variant("variant_a", 100, messageTemplateId, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(Collections.singletonList(variant));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(table.getId(), "table")));

        return emailSendingTestHelper.prepareSending(config);
    }

    private Segment prepareSegment() {
        return segmentService.addSegment(segment(
                passportGender("m")
        ));
    }

    private void preparePushSending(PluggableTable table) {
        Segment segment = prepareSegment();

        PushSendingConf config = new PushSendingConf();
        config.setApplication(MobileApplication.MARKET_APP);
        config.setVariants(Collections.singletonList(PushSendingTestHelper.variant()));
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(table.getId(), "table")));
        config.setSubscriptionType(SubscriptionTypes.STORE_PUSH_GENERAL_ADVERTISING.getId());

        pushSendingTestHelper.prepareSending(config);
    }

    private MessageTemplate<PushMessageConf> prepareAndroidPushMessageTemplate(PluggableTable table) {
        PushMessageConf config = new PushMessageConf();
        AndroidPushConf pushConf = new AndroidPushConf();
        pushConf.setTitle("Test push title");
        pushConf.setText("Test push text");
        config.setPushConfigs(Map.of(pushConf.getPlatform(), pushConf));
        config.setPluggedTables(List.of(new PluggedTable(table.getId(), "table")));

        return pushTemplatesTestHelper.prepare(config);
    }
}
