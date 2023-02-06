package ru.yandex.market.crm.campaign.http.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.preview.PreviewVariantRequest;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPeriodicSending;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerMediumTest;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailPeriodicSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.test.utils.PlatformHelper;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.EmailContext;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.json.serialization.JsonSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.passportGender;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class PreviewControllerTest extends AbstractControllerMediumTest {

    private static final String MAIL_HTML = "<div>Preview</div>";
    private static final String VARIANT = "variant_a";

    @Inject
    private EmailSendingTestHelper emailSendingTestHelper;
    @Inject
    private JsonSerializer jsonSerializer;
    @Inject
    private YaSenderHelper yaSenderHelper;
    @Inject
    private SegmentService segmentService;
    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;
    @Inject
    private EmailPeriodicSendingTestHelper emailPeriodicSendingTestHelper;
    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;
    @Inject
    private PlatformHelper platformHelper;

    /**
     * Ручка POST /api/sendings/email/variants/preview
     * <p>
     * Пользовательские переменные из подключаемых таблиц при отображении превью принимают
     * замоканные значения
     */
    @Test
    public void testPreviewEmailVariantWithUserVars() throws Exception {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable();

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock(
                "{{data.u_vars.table.saved_money}}"
        );

        EmailSendingVariantConf variant = variant("variant_a", 100, messageTemplateId, creative);
        List<PluggedTable> pluggedTables = List.of(new PluggedTable(pluggableTable.getId(), "table"));

        yaSenderHelper.expectPreview(MAIL_HTML, request -> {
            YaSenderData data = request.getData().getData();

            Map<String, Map<String, YTreeNode>> expected = Map.of(
                    "table", Map.of("saved_money", YTree.stringNode("saved_money"))
            );

            assertEquals(expected, data.getUVars());
        });

        PreviewVariantRequest request = new PreviewVariantRequest(pluggedTables, variant);

        mockMvc.perform(post("/api/sendings/email/variants/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    /**
     * Ручка GET /api/sendings/email/{id}/variants/{variant}/preview
     * <p>
     * Пользовательские переменные из подключаемых таблиц при отображении превью принимают
     * замоканные значения
     */
    @Test
    public void testPreviewSavedEmailVariantWithUserVars() throws Exception {
        EmailPlainSending sending = prepareSending();

        yaSenderHelper.expectPreview(MAIL_HTML, request -> {
            YaSenderData data = request.getData().getData();

            Map<String, Map<String, YTreeNode>> expected = Map.of(
                    "table", Map.of("saved_money", YTree.stringNode("saved_money"))
            );

            assertEquals(expected, data.getUVars());
        });

        mockMvc.perform(get("/api/sendings/email/{id}/variants/{variant}/preview", sending.getId(), VARIANT))
                .andDo(print())
                .andExpect(status().isOk());
    }

    /**
     * Тестирование предварительного просмотра варианта регулярной рассылки
     */
    @Test
    public void testPreviewEmailPeriodicSendingVariant() throws Exception {
        EmailPeriodicSending sending = emailPeriodicSendingTestHelper.prepareSending();
        EmailSendingVariantConf variant = sending.getConfig().getVariants().get(0);

        yaSenderHelper.expectPreview(MAIL_HTML);

        MvcResult result = mockMvc.perform(
                get(
                        "/api/periodic_sendings/email/{id}/variants/{variant}/preview",
                        sending.getId(),
                        variant.getId()
                )
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(MAIL_HTML, result.getResponse().getContentAsString());
    }

    /**
     * Случае если в Платформе хранится контекст отправленного сообщения для пары email-message_id в ответ на
     * запрос GET /api/sent_email возвращается html письма.
     */
    @Test
    void testGetEmailHtmlIfContextsIsKnown() throws Exception {
        var messageId = "test_message_id";
        var email = Uids.create(UidType.EMAIL, "user@yandex.ru");
        var campaignId = 123;
        var letterId = 321;

        var context = new YaSenderDataRow();
        context.setData(
                new YaSenderData()
                        .setVars(Map.of("var", YTree.stringNode("value")))
        );

        var fact = EmailContext.newBuilder()
                .setUid(email)
                .setMessageId(messageId)
                .setCampaignId(campaignId)
                .setLetterId(letterId)
                .setContext(jsonSerializer.writeObjectAsString(context))
                .build();

        platformHelper.putFact("EmailContext", email, fact);

        yaSenderHelper.onRenderRequest(campaignId, letterId, request -> {
            var data = request.getData();
            assertNotNull(data);

            var vars = data.getData().getVars();
            assertNotNull(vars);
            assertTrue(vars.containsKey("var"));
            return MAIL_HTML;
        });

        var result = requestLetterPreview(email.getStringValue(), messageId)
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(MAIL_HTML, result.getResponse().getContentAsString());
    }

    /**
     * Случае если в Платформе для пары email-message_id нет контекста отправленного сообщения в ответ на
     * запрос GET /api/sent_email возвращается 404 с текстом "Данные не найдены".
     */
    @Test
    void test404OnNoContextForRequestedMail() throws Exception {
        var result = requestLetterPreview("user@yandex.ru", "message_id")
                .andExpect(status().isNotFound())
                .andReturn();

        assertEquals("Данные не найдены", result.getResponse().getContentAsString());
    }

    @Nonnull
    private ResultActions requestLetterPreview(String email, String messageId) throws Exception {
        return mockMvc.perform(get("/api/sent_email")
                .param("email", email)
                .param("message_id", messageId))
                .andDo(print());
    }

    @Nonnull
    private EmailPlainSending prepareSending() {
        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable();

        Segment segment = segmentService.addSegment(segment(
                passportGender("m")
        ));

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock(
                "{{data.u_vars.table.saved_money}}"
        );
        EmailSendingVariantConf variant = variant(VARIANT, 100, messageTemplateId, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(Collections.singletonList(variant));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), "table")));

        return emailSendingTestHelper.prepareSending(config);
    }
}
