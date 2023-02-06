package ru.yandex.market.crm.campaign.http.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.crm.campaign.domain.pluggabletable.PluggableTable;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.domain.sending.EmailPlainSending;
import ru.yandex.market.crm.campaign.domain.sending.TestEmail;
import ru.yandex.market.crm.campaign.domain.sending.TestEmailsGroup;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingConf;
import ru.yandex.market.crm.campaign.domain.sending.conf.EmailSendingVariantConf;
import ru.yandex.market.crm.campaign.dto.sending.TestSendEmailPromoRequest;
import ru.yandex.market.crm.campaign.services.segments.SegmentService;
import ru.yandex.market.crm.campaign.test.AbstractControllerLargeTest;
import ru.yandex.market.crm.campaign.test.utils.BlockTemplateTestHelper;
import ru.yandex.market.crm.campaign.test.utils.EmailSendingTestHelper;
import ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper;
import ru.yandex.market.crm.core.domain.messages.PluggedTable;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.domain.sending.conf.BannerBlockConf;
import ru.yandex.market.crm.core.domain.yasender.Recipient;
import ru.yandex.market.crm.core.domain.yasender.YaSenderCampaign;
import ru.yandex.market.crm.core.test.utils.SubscriptionTypes;
import ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper;
import ru.yandex.market.crm.core.test.utils.UserTestHelper.IdRelation;
import ru.yandex.market.crm.core.test.utils.YaSenderHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.yt.paths.YtFolders;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserData;
import ru.yandex.market.crm.mapreduce.domain.subscriptions.CampaignUserRow;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderData;
import ru.yandex.market.crm.mapreduce.domain.yasender.YaSenderDataRow;
import ru.yandex.market.crm.platform.models.Subscription;
import ru.yandex.market.crm.yt.client.YtClient;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.campaign.test.utils.EmailSendingConfigUtils.variant;
import static ru.yandex.market.crm.campaign.test.utils.PluggableTablesTestHelper.pluggedTableRow;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.emailsFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.subscriptionFilter;
import static ru.yandex.market.crm.core.test.utils.SubscriptionsTestHelper.subscription;

/**
 * @author apershukov
 */
public class EmailSendingGenerationTest2 extends AbstractControllerLargeTest {

    private static class EmailData {
        final String email;
        final String cryptaId;

        EmailData(String email, String cryptaId) {
            this.email = email;
            this.cryptaId = cryptaId;
        }
    }

    @NotNull
    private static List<EmailData> generate1000EmailsOf250Users() {
        return IntStream.rangeClosed(1, 250)
                .mapToObj(i -> "crypta-id-" + i)
                .flatMap(cryptaId -> IntStream.rangeClosed(1, 4)
                        .mapToObj(i -> new EmailData(cryptaId + "-" + i + "-@yandex.ru", cryptaId))
                )
                .sorted(Comparator.comparing(x -> x.email))
                .collect(Collectors.toList());
    }

    private static IdRelation emailCryptaIdRelation(String email, String cryptaId) {
        return new IdRelation(DigestUtils.md5Hex(email), cryptaId);
    }

    @Inject
    private SubscriptionsTestHelper subscriptionsTestHelper;

    @Inject
    private YtClient ytClient;

    @Inject
    private UserTestHelper userTestHelper;

    @Inject
    private BlockTemplateTestHelper blockTemplateTestHelper;

    @Inject
    private EmailSendingTestHelper emailSendingTestHelper;

    @Inject
    private YtFolders ytFolders;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private JsonDeserializer jsonDeserializer;

    @Inject
    private PluggableTablesTestHelper pluggableTablesTestHelper;

    @Inject
    private SegmentService segmentService;

    @Inject
    private YaSenderHelper yaSenderHelper;

    @Inject
    private JsonSerializer jsonSerializer;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareSubscriptionFactsTable();
        ytSchemaTestHelper.prepareCryptaMatchingTable(UserTestHelper.EMAIL_MD5, UserTestHelper.CRYPTA_ID);
        ytSchemaTestHelper.prepareGlobalControlSplitsTable();
        ytSchemaTestHelper.prepareEmailsGeoInfo();
        ytSchemaTestHelper.prepareUserTables();
    }

    /**
     * Email'ы связанные с одним и тем же crypta id попадают в один вариант
     */
    @Test
    public void testDistributeVariantsByCryptaId() throws Exception {
        Subscription[] subscriptions = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> subscription("user_" + i + "@yandex.ru"))
                .toArray(Subscription[]::new);

        subscriptionsTestHelper.saveSubscriptions(subscriptions);

        userTestHelper.saveLinks(
                UserTestHelper.EMAIL_MD5,
                UserTestHelper.CRYPTA_ID,
                Stream.of(subscriptions)
                        .map(x -> x.getUid().getStringValue())
                        .map(email -> emailCryptaIdRelation(email, "crypta_id"))
                        .toArray(IdRelation[]::new)
        );

        String templateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf bannerBlock = blockTemplateTestHelper.prepareCreativeBlock();

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(
                segment(
                        subscriptionFilter(SubscriptionTypes.ADVERTISING)
                ),
                LinkingMode.ALL,
                variant("variant_a", 50, templateId, bannerBlock),
                variant("variant_b", 50, templateId, bannerBlock)
        );

        List<CampaignUserRow> resultRows = generate(sending);

        String commonVariant = null;

        for (CampaignUserRow row : resultRows) {
            CampaignUserData data = jsonDeserializer.readObject(CampaignUserData.class, row.getData());
            String variantId = data.getUserInfo().getVariant();

            Assertions.assertTrue(
                    commonVariant == null || commonVariant.equals(variantId),
                    "Emails are is different variants");

            commonVariant = variantId;
        }
    }

    /**
     * В случае если в сегмент попали адреса которых нем ни в одном глобальном сплите и при
     * этом для них не нашлось crypta-id,
     * определение глобального сплита этих адресов происходит рандомно для каждого адреса
     * в процессе сборки рассылки.
     */
    @Test
    public void testGlobalSplittingUnknownEmailsWithoutCryptaId() throws Exception {
        var emails = IntStream.rangeClosed(1, 1000)
                .mapToObj(i -> "user-" + i + "@yandex.ru")
                .sorted()
                .collect(Collectors.toList());

        subscriptionsTestHelper.saveSubscriptions(
                emails.stream()
                        .map(SubscriptionsTestHelper::subscription)
                        .toArray(Subscription[]::new)
        );

        var sending = prepareSendingWithGlobalControl();

        var sendingEmails = generate(sending).stream()
                .map(CampaignUserRow::getEmail)
                .collect(Collectors.toSet());

        MatcherAssert.assertThat(
                sendingEmails.size(),
                allOf(
                        greaterThanOrEqualTo(800),
                        lessThanOrEqualTo(970)
                )
        );
    }

    /**
     * В случае если в рассылке используется подключаемая таблица в которой в качестве идентификатора
     * используется email при поиске значений переменных адреса в разных яндексовых доменах, имеющие одинаковым
     * префикс считаются за один.
     * <p>
     * К примеру, строка с адресом user@ya.ru будет приматчена к письму на адрес user@yandex.ru.
     */
    @Test
    public void testMatchUserVariablesInDifferentYandexDomains() throws Exception {
        String prefix = "user";

        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.EMAIL,
                pluggedTableRow(prefix + "@ya.ru", "100500")
        );

        String email = prefix + "@yandex.ru";

        subscriptionsTestHelper.saveSubscriptions(
                subscription(email)
        );

        Segment segment = segmentService.addSegment(segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        ));

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock(
                "{{data.u_vars.table.saved_money}}"
        );
        EmailSendingVariantConf variant = variant("variant_a", 100, messageTemplateId, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(Collections.singletonList(variant));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), "table")));

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(config);

        generate(sending);

        YPath path = ytFolders.getCampaignPath(sending.getId()).child("senderdata");

        List<YaSenderDataRow> senderRows = ytClient.read(path, YaSenderDataRow.class);

        Assertions.assertEquals(1, senderRows.size());

        YaSenderDataRow row = senderRows.get(0);
        Assertions.assertEquals(email, row.getEmail());
        YaSenderData data = jsonDeserializer.readObject(YaSenderData.class, row.getJsonData());

        Map<String, Map<String, YTreeNode>> expectedVars = Map.of(
                "table", Map.of("saved_money", YTree.stringNode("100500"))
        );

        Assertions.assertEquals(expectedVars, data.getUVars());
    }

    /**
     * При тестовой отправке рассылки с параметрами данные в рассылятор должны корректно прокидываться
     */
    @Test
    public void testGenerateAndTestSendPromo() throws Exception {
        String email = "email.111@yandex.ru";
        String toEmail = "email.1@yandex.ru";

        PluggableTable pluggableTable = pluggableTablesTestHelper.preparePluggableTable(UidType.EMAIL,
                pluggedTableRow(email, "100")
        );
        subscriptionsTestHelper.saveSubscriptions(
                subscription(email)
        );
        Segment segment = segmentService.addSegment(segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        ));

        String messageTemplateId = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock(
                "{{data.u_vars.table.saved_money}}"
        );
        EmailSendingVariantConf variant = variant("variant_a", 100, messageTemplateId, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(Collections.singletonList(variant));
        config.setPluggedTables(Collections.singletonList(new PluggedTable(pluggableTable.getId(), "table")));

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(config);

        generate(sending);

        TestEmailsGroup group = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(List.of(
                        new TestEmail(toEmail, true)
                ));

        String campaignSlug = "campaign_slug";
        mockMvc.perform(post(
                "/api/sendings/email/{id}/send-test",
                sending.getId()
        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(
                        new TestSendEmailPromoRequest()
                                .setEmailGroups(List.of(group))
                )))
                .andDo(print())
                .andExpect(status().isOk());

        BlockingQueue<YaSenderCampaign> campaignQueue = new ArrayBlockingQueue<>(1);
        yaSenderHelper.onSendOrCreatePromo(campaignSlug, campaignQueue::put);

        BlockingQueue<List<Recipient>> recipientsQueue = new ArrayBlockingQueue<>(1);
        yaSenderHelper.onTestSendPromo(campaignSlug, x -> {
            try {
                recipientsQueue.put(x);
            } catch (InterruptedException ex) {
                fail(ex);
            }
        });

        YaSenderCampaign campaign = campaignQueue.poll(1, TimeUnit.MINUTES);
        Assertions.assertNotNull(campaign, "No campaign was received");

        List<Recipient> recipients = recipientsQueue.poll(3, TimeUnit.MINUTES);
        Assertions.assertNotNull(recipients, "No recipients was received");
        assertEquals(1, recipients.size());

        Recipient recipient = recipients.get(0);
        assertEquals(toEmail, recipient.getEmail());

        Map<String, Map<String, YTreeNode>> expected = Map.of(
                "table", Map.of("saved_money", YTree.stringNode("100"))
        );

        YaSenderData data = jsonDeserializer.readObject(
                YaSenderData.class,
                jsonSerializer.writeObjectAsString(recipient.getParams().get("data"))
        );
        Assertions.assertEquals(expected, data.getUVars());
    }

    /**
     * На шаге подготовки таблиц с информацией о пользователях, во время проверки подписки email'ы должны быть
     * нормализованы.
     */
    @Test
    public void testNormalizeEmailsOnPrepareEmailsStep() throws Exception {
        String email1 = "email1@narod.ru";
        String normalizedEmail1 = "email1@yandex.ru";
        String normalizedEmail2 = "email2@yandex.ru";

        subscriptionsTestHelper.saveSubscriptions(
                subscription(email1)
        );

        Segment segment = segmentService.addSegment(segment(
                emailsFilter(normalizedEmail1, normalizedEmail2)
        ));

        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock();
        EmailPlainSending sending = emailSendingTestHelper.prepareSending(segment, LinkingMode.NONE, creative);

        List<CampaignUserRow> rows = generate(sending);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals(normalizedEmail1, rows.get(0).getEmail());
    }

    /**
     * Если при отправке тестовых писем указано 2 получателя, причём каждый должен получить 2 письма,
     * а также в рассылке 2 варианта, то каждый email получит 2 письма, при этом письма будут из разных вариантов
     */
    @Test
    public void testSendTestTwoMessageForTwoEmailsFromTwoVariants() throws Exception {
        String toEmail1 = "email.1@yandex.ru";
        String toEmail2 = "email.2@yandex.ru";

        Subscription[] subscriptions = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> "user-" + i + "@yandex.ru")
                .map(SubscriptionsTestHelper::subscription)
                .toArray(Subscription[]::new);
        subscriptionsTestHelper.saveSubscriptions(subscriptions);

        Segment segment = segmentService.addSegment(segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        ));

        String messageTemplateId1 = blockTemplateTestHelper.prepareMessageTemplate();
        String messageTemplateId2 = blockTemplateTestHelper.prepareMessageTemplate();
        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock();
        EmailSendingVariantConf variant1 = variant("variant_a", 50, messageTemplateId1, creative);
        EmailSendingVariantConf variant2 = variant("variant_b", 50, messageTemplateId2, creative);

        EmailSendingConf config = new EmailSendingConf();
        config.setSubscriptionType(2L);
        config.setTarget(new TargetAudience(LinkingMode.NONE, segment.getId()));
        config.setVariants(List.of(variant1, variant2));

        EmailPlainSending sending = emailSendingTestHelper.prepareSending(config);

        generate(sending);

        TestEmailsGroup group = new TestEmailsGroup()
                .setId("id-1")
                .setName("Group 1")
                .setItems(List.of(
                        new TestEmail(toEmail1, true),
                        new TestEmail(toEmail2, true)
                ));

        String campaignSlug = "campaign_slug";
        mockMvc.perform(post(
                "/api/sendings/email/{id}/send-test",
                sending.getId()
        )
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonSerializer.writeObjectAsBytes(
                        new TestSendEmailPromoRequest()
                                .setEmailGroups(List.of(group))
                                .setMessagesCount(2)
                )))
                .andDo(print())
                .andExpect(status().isOk());

        BlockingQueue<YaSenderCampaign> campaignQueue = new ArrayBlockingQueue<>(1);
        yaSenderHelper.onSendOrCreatePromo(campaignSlug, campaignQueue::put);

        BlockingQueue<List<Recipient>> recipientsQueue = new ArrayBlockingQueue<>(1);
        yaSenderHelper.onTestSendPromo(campaignSlug, recipients -> {
            try {
                recipientsQueue.put(recipients);
            } catch (InterruptedException ex) {
                fail(ex);
            }
        });

        YaSenderCampaign campaign = campaignQueue.poll(1, TimeUnit.MINUTES);
        Assertions.assertNotNull(campaign, "No campaign was received");

        List<Recipient> recipients = recipientsQueue.poll(3, TimeUnit.MINUTES);
        Assertions.assertNotNull(recipients, "No recipients was received");
        assertEquals(4, recipients.size());

        Map<String, List<YaSenderData>> messagesToEmails = new HashMap<>();
        recipients.forEach(recipient -> {
            messagesToEmails.putIfAbsent(recipient.getEmail(), new ArrayList<>());

            YaSenderData data = jsonDeserializer.readObject(
                    YaSenderData.class,
                    jsonSerializer.writeObjectAsString(recipient.getParams().get("data"))
            );
            messagesToEmails.get(recipient.getEmail()).add(data);
        });

        assertEquals(Set.of(toEmail1, toEmail2), messagesToEmails.keySet());

        messagesToEmails.values().forEach(messages -> {
            assertEquals(2, messages.size());

            Set<String> variants = messages.stream().map(YaSenderData::getVariantId).collect(Collectors.toSet());
            assertEquals(Set.of(variant1.getId(), variant2.getId()), variants);
        });
    }

    @NotNull
    private EmailPlainSending prepareSendingWithGlobalControl() {
        Segment segment = segment(
                subscriptionFilter(SubscriptionTypes.ADVERTISING)
        );

        BannerBlockConf creative = blockTemplateTestHelper.prepareCreativeBlock();
        EmailPlainSending sending = emailSendingTestHelper.prepareSending(segment, LinkingMode.NONE, creative);

        sending.getConfig().setGlobalControlEnabled(true);
        emailSendingTestHelper.updateSending(sending);
        return sending;
    }

    private List<CampaignUserRow> generate(EmailPlainSending sending) throws Exception {
        mockMvc.perform(post("/api/sendings/email/{id}/generate", sending.getId()))
                .andExpect(status().isOk())
                .andDo(print());

        emailSendingTestHelper.waitGenerated(sending.getId());

        YPath resultPath = ytFolders.getCampaignPath(sending.getId()).child("campaign");
        return ytClient.read(resultPath, CampaignUserRow.class);
    }
}
