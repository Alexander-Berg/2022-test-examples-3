package ru.yandex.crm.tests.delivery.stageTwo;

        import org.junit.Assert;
        import org.junit.Test;
        import ru.yandex.core.MailProvider;
        import ru.yandex.crm.tests.support.SupportTicketStorage;
        import ru.yandex.crm.tests.support.TicketRow;

        import java.io.IOException;
        import java.sql.SQLException;

/**
 * Created by agroroza on 17.03.2016.
 */
public class ModDirectCategoryTests extends MailProvider {
    public ModDirectCategoryTests() throws IOException {
    }

    @Test
    public void ModMSKAgeCheck() throws SQLException {
        String ThreadNumMail = getStageMessageId("ModMSKAge");

        TicketRow ticketModMSKAge = SupportTicketStorage.getTicketByMessageId(ThreadNumMail);
        Assert.assertNotNull(ticketModMSKAge);
        Assert.assertEquals(1132, ticketModMSKAge.categoryId);
        Assert.assertEquals(1001, ticketModMSKAge.queueId);
    }
    @Test
    public void ModMSKDissentCheck() throws SQLException {
        String ThreadNumMail = getStageMessageId("ModMSKDissent");

        TicketRow ticketModMSKDissent = SupportTicketStorage.getTicketByMessageId(ThreadNumMail);
        Assert.assertNotNull(ticketModMSKDissent);
        Assert.assertEquals(1133, ticketModMSKDissent.categoryId);
        Assert.assertEquals(2000, ticketModMSKDissent.queueId);
    }

    @Test
    public void ModMSKDocCheck() throws SQLException {
        String ThreadNumMail = getStageMessageId("ModMSKDoc");

        TicketRow ticketModMSKDoc = SupportTicketStorage.getTicketByMessageId(ThreadNumMail);
        Assert.assertNotNull(ticketModMSKDoc);
        Assert.assertEquals(1135, ticketModMSKDoc.categoryId);
        Assert.assertEquals(2100, ticketModMSKDoc.queueId);
    }

    @Test
    public void ModMSKOtherCheck() throws SQLException {
        String ThreadNumMail = getStageMessageId("ModMSKOther");

        TicketRow ticketModMSKOther = SupportTicketStorage.getTicketByMessageId(ThreadNumMail);
        Assert.assertNotNull(ticketModMSKOther);
        Assert.assertEquals(1156, ticketModMSKOther.categoryId);
        Assert.assertEquals(1001, ticketModMSKOther.queueId);
    }

}