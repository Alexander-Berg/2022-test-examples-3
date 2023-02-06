package ru.yandex.travel.acceptance.orders.invoice.trust;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.entities.Account;
import ru.yandex.travel.orders.entities.FiscalItemType;
import ru.yandex.travel.orders.entities.InvoiceItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.entities.VatType;
import ru.yandex.travel.orders.repository.AccountRepository;
import ru.yandex.travel.orders.repository.TrustInvoiceRepository;
import ru.yandex.travel.orders.services.payments.PaymentProfile;
import ru.yandex.travel.orders.workflow.invoice.proto.TPaymentCreated;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.WorkflowMessageSender;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.repository.WorkflowRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestsRunner {

    private final TrustInvoiceRepository trustInvoiceRepository;

    private final AccountRepository accountRepository;

    private final TransactionTemplate transactionTemplate;

    private final WorkflowMessageSender workflowMessageSender;

    private final WorkflowRepository workflowRepository;

    private final CheckerRegistry checkerRegistry;

    private final TrustPayer trustPayer;

    public boolean checkHoldClearRefund() {
        UUID checkerWorkflowId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        AtomicReference<InvoiceInfo> invoiceInfoRef = new AtomicReference<>();
        AtomicReference<TrustInvoiceCheckerBase> checkerRef = new AtomicReference<>();
        transactionTemplate.execute(ignored -> {
                    checkerRef.set(createTrustInvoiceHoldClearRefundCheckerHandler(checkerWorkflowId, invoiceId));
                    invoiceInfoRef.set(Preconditions.checkNotNull(createInvoice(invoiceId, checkerWorkflowId)));
                    return null;
                }
        );
        checkerRegistry.registerChecker(checkerWorkflowId, checkerRef.get());
        transactionTemplate.execute(ignored -> {
            workflowMessageSender.scheduleEvent(invoiceInfoRef.get().getInvoiceWorkflowId(),
                    TPaymentCreated.newBuilder().build());
            return null;
        });
        return checkerRef.get().waitTestResult(Duration.ofMinutes(10));
    }


    public TrustInvoiceHoldClearRefundCheckerHandler createTrustInvoiceHoldClearRefundCheckerHandler(UUID checkerWorkflowId,
                                                                                                     UUID invoiceId) {
        createNewCheckerWorkflow(checkerWorkflowId, invoiceId);
        return new TrustInvoiceHoldClearRefundCheckerHandler(trustPayer);
    }

    public boolean checkHoldUnhold() {
        UUID checkerWorkflowId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        AtomicReference<InvoiceInfo> invoiceInfoRef = new AtomicReference<>();
        AtomicReference<TrustInvoiceCheckerBase> checkerRef = new AtomicReference<>();

        transactionTemplate.execute(ignored -> {
            checkerRef.set(createTrustInvoiceHoldUnholdCheckerHandler(checkerWorkflowId, invoiceId));
            invoiceInfoRef.set(Preconditions.checkNotNull(createInvoice(invoiceId, checkerWorkflowId)));
            return null;
        });

        checkerRegistry.registerChecker(checkerWorkflowId, checkerRef.get());

        transactionTemplate.execute(ignored -> {
            workflowMessageSender.scheduleEvent(invoiceInfoRef.get().getInvoiceWorkflowId(),
                    TPaymentCreated.newBuilder().build());
            return null;
        });
        return checkerRef.get().waitTestResult(Duration.ofMinutes(10));
    }

    public TrustInvoiceHoldUnholdCheckerHandler createTrustInvoiceHoldUnholdCheckerHandler(UUID checkerWorkflowId,
                                                                                           UUID invoiceId) {
        createNewCheckerWorkflow(checkerWorkflowId, invoiceId);
        return new TrustInvoiceHoldUnholdCheckerHandler(trustPayer);
    }

    private InvoiceInfo createInvoice(UUID invoiceId, UUID checkerWorkflowId) {
        Account account = Account.createAccount(ProtoCurrencyUnit.RUB);
        account = accountRepository.save(account);

        TrustInvoice trustInvoice = new TrustInvoice();
        trustInvoice.setId(invoiceId);
        trustInvoice.setPaymentProfile(PaymentProfile.HOTEL);
        trustInvoice.setAccount(account);
        trustInvoice.setExpirationDate(Instant.now().plus(Duration.ofDays(1)));
        trustInvoice.setClientEmail("test@yandex-team.ru");
        trustInvoice.setClientPhone("+79111111111");
        trustInvoice.setSource("desktop/form");
        trustInvoice.setReturnPath("https://travel.yandex.ru");

        InvoiceItem invoiceItem = new InvoiceItem();
        invoiceItem.setPrice(BigDecimal.valueOf(1000));
        invoiceItem.setFiscalItemId(1L);
        invoiceItem.setFiscalItemType(FiscalItemType.EXPEDIA_HOTEL); //TODO (mbobrov): parameterize it
        invoiceItem.setFiscalNds(VatType.VAT_NONE);
        invoiceItem.setFiscalTitle("Accommodation purchase");

        trustInvoice.addInvoiceItem(invoiceItem);
        trustInvoice.setOrderWorkflowId(checkerWorkflowId);
        trustInvoice = trustInvoiceRepository.save(trustInvoice);
        Workflow workflow = Workflow.createWorkflowForEntity(trustInvoice, checkerWorkflowId);
        workflow = workflowRepository.saveAndFlush(workflow);
        return new InvoiceInfo(trustInvoice.getId(), workflow.getId());
    }

    private void createNewCheckerWorkflow(UUID workflowId, UUID invoiceId) {
        Workflow checker = new Workflow();
        checker.setEntityType(TestWorkflows.CHECKER.getEntityType());
        checker.setId(workflowId);
        checker.setState(EWorkflowState.WS_RUNNING);
        checker.setEntityId(invoiceId);
        workflowRepository.saveAndFlush(checker);
    }

    @Data
    @AllArgsConstructor
    private static final class InvoiceInfo {
        private UUID invoiceId;
        private UUID invoiceWorkflowId;
    }

}
