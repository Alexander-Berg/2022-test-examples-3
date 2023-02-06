package ru.yandex.market.crm.platform.mappers;

import java.util.List;

import org.junit.Test;

import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.CrmInfo;
import ru.yandex.market.crm.platform.commons.SendingType;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author apershukov
 */
public class EmailSendingMapperTest {

    private final EmailSendingMapper mapper = new EmailSendingMapper();

    @Test
    public void testParseActionEmail() {
        String line = "tskv\t" +
                "type=4\t" +
                "sendingId=beru_burn_2507\t" +
                "variantId=beru_burn_2507_a\t" +
                "segmentId=beru_burn_2507_a\t" +
                "stepId=send_emails_1056601a\t" +
                "account=beru\tcontrol=false\t" +
                "email=Heavenly-Arch@yandex.ru\t" +
                "originalEmail=Heavenly-Arch@yandex.ru\t" +
                "campaignId=112122\t" +
                "senderAccount=beru\t" +
                "subject=subject\t" +
                "timestamp=1564144238627\n";

        List<Email> results = parse(line);

        assertEquals(1, results.size());

        Email email = results.get(0);
        assertEquals(SendingType.ACTION, email.getSendingType());

        CrmInfo crmInfo = email.getCrmInfo();
        assertEquals("beru_burn_2507", crmInfo.getSendingId());
        assertEquals("beru_burn_2507_a", crmInfo.getVariantId());
        assertEquals("send_emails_1056601a", crmInfo.getStepId());
    }

    @Test
    public void testParsePeriodicEmail() {
        String line = "tskv\t" +
                "type=5\t" +
                "sendingId=beru_burn_2507\t" +
                "variantId=beru_burn_2507_a\t" +
                "segmentId=beru_burn_2507_a\t" +
                "account=beru\tcontrol=false\t" +
                "email=Heavenly-Arch@yandex.ru\t" +
                "originalEmail=Heavenly-Arch@yandex.ru\t" +
                "campaignId=112122\t" +
                "senderAccount=beru\t" +
                "subject=subject\t" +
                "versionId=beru_burn_2507:2\t" +
                "iteration=5\t" +
                "messageId=iddqd\t" +
                "timestamp=1564144238627\n";

        List<Email> results = parse(line);

        assertEquals(1, results.size());

        Email email = results.get(0);
        assertEquals(SendingType.PERIODIC_SENDING, email.getSendingType());
        assertEquals("iddqd", email.getMessageId());

        CrmInfo crmInfo = email.getCrmInfo();
        assertEquals("beru_burn_2507", crmInfo.getSendingId());
        assertEquals("beru_burn_2507_a", crmInfo.getVariantId());
        assertEquals("beru_burn_2507:2", crmInfo.getVersionId());
        assertEquals(5, crmInfo.getIteration());
    }

    @Test
    public void testParseMarketMailerEmail() {
        String line = createMarketMailerLine(123, 3, "market", "some_id", 1, "SOME_NAME");

        List<Email> results = parse(line);

        assertEquals(1, results.size());

        Email email = results.get(0);
        assertSame(SendingType.TRANSACTION, email.getSendingType());
        assertEquals("market", email.getSenderInfo().getAccount());
        assertEquals(Uids.create(UidType.EMAIL, "heavenly-arch@yandex.ru"), email.getUid());
        assertEquals(Uids.create(UidType.EMAIL, "Heavenly-Arch@yandex.ru"), email.getOriginalUid());
        assertEquals(123, email.getSenderInfo().getCampaignId());
        assertEquals("market", email.getSenderInfo().getAccount());
        assertEquals("some_id", email.getMessageId());
        assertEquals(1564144238, email.getTimestamp());
        assertEquals(1, email.getCrmInfo().getNotificationId());
        assertEquals("SOME_NAME", email.getCrmInfo().getNotificationName());
        assertEquals("123#some_id", email.getFactId());
        assertSame(Email.DeliveryStatus.UPLOADED, email.getDeliveryStatus());
        assertSame(Email.EventType.SENDING, email.getEventType());
    }

    @Test
    public void testSkipMarketMailerEmailIfNullCampaignId() {
        String line = createMarketMailerLine(null, 3, "market", "some_id", 1, "SOME_NAME");
        List<Email> results = parse(line);
        assertSame(0, results.size());
    }

    @Test
    public void testSkipMarketMailerEmailIfNullType() {
        String line = createMarketMailerLine(123, null, "market", "some_id", 1, "SOME_NAME");
        List<Email> results = parse(line);
        assertSame(0, results.size());
    }

    @Test
    public void testSkipMarketMailerEmailIfNullAccount() {
        String line = createMarketMailerLine(123, 3, null, "some_id", 1, "SOME_NAME");
        List<Email> results = parse(line);
        assertSame(0, results.size());
    }

    @Test
    public void testSkipMarketMailerEmailIfNullMessageId() {
        String line = createMarketMailerLine(123, 3, "market", null, 1, "SOME_NAME");
        List<Email> results = parse(line);
        assertSame(0, results.size());
    }

    @Test
    public void testSkipMarketMailerEmailIfNullEventId() {
        String line = createMarketMailerLine(123, 3, "market", "some_id", null, "SOME_NAME");
        List<Email> results = parse(line);
        assertSame(0, results.size());
    }

    @Test
    public void testSkipMarketMailerEmailIfNullEventName() {
        String line = createMarketMailerLine(123, 3, "market", "some_id", 1, null);
        List<Email> results = parse(line);
        assertSame(0, results.size());
    }

    private String createMarketMailerLine(Integer campaignId,
                                          Integer type,
                                          String account,
                                          String messageId,
                                          Integer notificationId,
                                          String notificationName) {
        return "tskv\t" +
                (type != null ? "type=" + type + "\t" : "") +
                (account != null ? "account=" + account + "\t" : "") +
                "control=false\t" +
                "globalControl=false\t" +
                "email=Heavenly-Arch@yandex.ru\t" +
                "originalEmail=Heavenly-Arch@yandex.ru\t" +
                (campaignId != null ? "campaignId=" + campaignId + "\t" : "") +
                "senderAccount=market\t" +
                (messageId != null ? "messageId=" + messageId + "\t" : "") +
                "timestamp=1564144238\t" +
                (notificationId != null ? "notificationId=" + notificationId + "\t" : "") +
                (notificationName != null ? "notificationName=" + notificationName : "");
    }

    private List<Email> parse(String line) {
        return mapper.apply(line.getBytes());
    }
}
