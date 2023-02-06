package ru.yandex.market.crm.operatorwindow;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.crm.operatorwindow.utils.MailProcessingTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.module.comment.test.impl.CommentTestUtils;
import ru.yandex.market.jmf.module.def.test.impl.ModuleDefaultTestUtils;
import ru.yandex.market.jmf.module.mail.InMailMessage;
import ru.yandex.market.jmf.module.mail.impl.MailProcessingService;
import ru.yandex.market.jmf.module.mail.test.impl.MailMessageBuilderService;
import ru.yandex.market.jmf.module.mail.test.impl.MailTestUtils;
import ru.yandex.market.jmf.module.ticket.test.impl.TicketTestUtils;
import ru.yandex.market.jmf.script.ScriptContextVariablesService;
import ru.yandex.market.jmf.timings.test.impl.ServiceTimeTestUtils;
import ru.yandex.market.ocrm.module.checkouter.test.MockCheckouterAPI;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

public abstract class AbstractMailProcessingTest extends AbstractModuleOwTest {

    @Inject
    protected MailProcessingTestUtils mailProcessingTestUtils;
    @Inject
    protected MailTestUtils mailTestUtils;
    @Inject
    protected ServiceTimeTestUtils serviceTimeTestUtils;
    @Inject
    protected OrderTestUtils orderTestUtils;
    @Inject
    protected TicketTestUtils ticketTestUtils;
    @Inject
    protected DbService dbService;
    @Inject
    protected BcpService bcpService;
    @Inject
    protected ScriptContextVariablesService scriptContextVariablesService;
    @Inject
    protected CommentTestUtils commentTestUtils;
    @Inject
    protected ModuleDefaultTestUtils moduleDefaultTestUtils;
    @Inject
    protected MailMessageBuilderService mailMessageBuilderService;
    @Inject
    protected MailProcessingService mailProcessingService;
    @Inject
    protected MockCheckouterAPI mockCheckouterAPI;

    @BeforeEach
    public void setUp() {
        mockCheckouterAPI.clearMockGetOrderItems();
    }

    protected void processMessage(InMailMessage mailMessage) {
        mailProcessingService.processInMessage(mailMessage);
    }

    public MailMessageBuilderService.MailMessageBuilder getMailMessageBuilder(String connection) {
        return mailMessageBuilderService.getMailMessageBuilder(connection);
    }
}
