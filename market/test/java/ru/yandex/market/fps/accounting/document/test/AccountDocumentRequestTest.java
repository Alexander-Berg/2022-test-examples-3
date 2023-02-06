package ru.yandex.market.fps.accounting.document.test;

import java.util.Map;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.accounting.document.AccountingDocument;
import ru.yandex.market.fps.accounting.document.AccountingDocumentType;
import ru.yandex.market.fps.accounting.document.AccountingDocumentsRequest;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.UniqueAttributeValidationException;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.logic.def.AttachmentsService;
import ru.yandex.market.jmf.logic.wf.HasWorkflow;
import ru.yandex.market.jmf.utils.Maps;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(InternalModuleFpsAccountingDocumentTestConfiguration.class)
@Transactional
public class AccountDocumentRequestTest {
    private final BcpService bcpService;
    private final AttachmentsService attachmentsService;
    private final SupplierTestUtils supplierTestUtils;

    public AccountDocumentRequestTest(BcpService bcpService, AttachmentsService attachmentsService,
                                      SupplierTestUtils supplierTestUtils) {
        this.bcpService = bcpService;
        this.attachmentsService = attachmentsService;
        this.supplierTestUtils = supplierTestUtils;
    }

    @Test
    public void requestWithActiveAccountDocuments() {
        Supplier1p supplier = createSupplier1p(1L);
        createAccountingDocument(AccountingDocumentType.FINANCIAL, false);
        AccountingDocumentsRequest request = createRequest(supplier);

        Assertions.assertEquals(1, request.getDocuments().size());
        Assertions.assertEquals(AccountingDocumentsRequest.Statuses.REGISTERED, request.getStatus());
    }

    @Test
    public void requestWithArchivedAccountDocuments() {
        Supplier1p supplier = createSupplier1p(1L);

        createAccountingDocument(AccountingDocumentType.FINANCIAL, false);
        createAccountingDocument(AccountingDocumentType.FOUND, false);
        createAccountingDocument(AccountingDocumentType.SAMPLE, false);
        createAccountingDocument(AccountingDocumentType.SEPARATED_SUBDIVISION, false);
        createAccountingDocument(AccountingDocumentType.FINANCIAL, true);
        createAccountingDocument(AccountingDocumentType.FOUND, true);
        createAccountingDocument(AccountingDocumentType.SAMPLE, true);
        createAccountingDocument(AccountingDocumentType.SEPARATED_SUBDIVISION, true);

        AccountingDocumentsRequest request = createRequest(supplier);

        Assertions.assertEquals(4, request.getDocuments().size());
    }

    @Test
    public void twoAccountingDocumentRequests() {
        Supplier1p supplier1 = createSupplier1p(1L);
        Supplier1p supplier2 = createSupplier1p(2L);

        createAccountingDocument(AccountingDocumentType.FINANCIAL, false);
        createAccountingDocument(AccountingDocumentType.FINANCIAL, true);

        AccountingDocumentsRequest request1 = createRequest(supplier1);
        AccountingDocumentsRequest request2 = createRequest(supplier2);

        Assertions.assertEquals(1, request1.getDocuments().size());
        Assertions.assertEquals(1, request2.getDocuments().size());
    }

    @Test
    public void twoAccountingDocumentRequestsFromOneSupplier() {
        Supplier1p supplier = createSupplier1p(1L);

        createAccountingDocument(AccountingDocumentType.FINANCIAL, false);
        createAccountingDocument(AccountingDocumentType.FINANCIAL, true);

        createRequest(supplier);
        assertThrows(UniqueAttributeValidationException.class, () -> {
            createRequest(supplier);
        });
    }


    private AccountingDocumentsRequest createRequest(Supplier1p supplier) {
        Map<String, Object> properties = Maps.of(
                AccountingDocumentsRequest.TITLE, Randoms.string(),
                AccountingDocumentsRequest.PARTNER, supplier
        );

        return bcpService.create(AccountingDocumentsRequest.FQN, properties);
    }

    private AccountingDocument createAccountingDocument(String type, boolean isArchived) {
        Attachment attachment = attachmentsService.createDetached(
                Randoms.string(),
                Randoms.string(),
                Randoms.string(),
                500L);

        Map<String, Object> properties = new java.util.HashMap<>(Map.of(
                AccountingDocument.CODE, Randoms.string(),
                AccountingDocument.TITLE, Randoms.string(),
                AccountingDocument.TYPE, type,
                AccountingDocument.DOCUMENT, attachment
        ));

        if (isArchived) {
            properties.put(AccountingDocument.STATUS, HasWorkflow.ARCHIVED);
        }

        return bcpService.create(AccountingDocument.FQN, properties);
    }

    private Supplier1p createSupplier1p(Long mbiPartnerId) {
        return supplierTestUtils.createSupplier(Map.of(
                Supplier1p.MBI_PARTNER_ID, mbiPartnerId,
                Supplier1p.CLIENT_EMAIL, Randoms.email()
        ));
    }
}
