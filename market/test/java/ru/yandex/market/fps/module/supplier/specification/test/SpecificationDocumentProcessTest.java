package ru.yandex.market.fps.module.supplier.specification.test;


import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.crm.util.ResourceHelpers;
import ru.yandex.market.fps.module.axapta.AxaptaClient;
import ru.yandex.market.fps.module.axapta.ChangeSpecificationStatusResponse;
import ru.yandex.market.fps.module.axapta.CreateSpecificationDocumentResponse;
import ru.yandex.market.fps.module.supplier.specification.Specification;
import ru.yandex.market.fps.module.supplier.specification.SpecificationDocument;
import ru.yandex.market.fps.module.supplier.specification.SpecificationService;
import ru.yandex.market.fps.module.supplier.specification.SupplierAgreement;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.logic.def.Attachment;
import ru.yandex.market.jmf.time.Now;
import ru.yandex.market.jmf.trigger.TriggerServiceException;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@Transactional
@SpringJUnitConfig(InternalModuleSupplierSpecificationTestConfiguration.class)
public class SpecificationDocumentProcessTest {

    private final BcpService bcpService;
    private final SupplierTestUtils supplierTestUtils;
    private final AxaptaClient axaptaClient;
    private final ObjectSerializeService objectSerializeService;
    private final SpecificationService specificationService;


    public SpecificationDocumentProcessTest(BcpService bcpService,
                                            SupplierTestUtils supplierTestUtils,
                                            AxaptaClient axaptaClient,
                                            ObjectSerializeService objectSerializeService,
                                            SpecificationService specificationService) {
        this.bcpService = bcpService;
        this.supplierTestUtils = supplierTestUtils;
        this.axaptaClient = axaptaClient;
        this.objectSerializeService = objectSerializeService;
        this.specificationService = specificationService;
    }

    @Test
    public void sendError() {
        Supplier1p supplier1 = supplierTestUtils.createSupplier();

        Mockito.when(axaptaClient.createSpecificationDocument(any()))
                .thenReturn(new CreateSpecificationDocumentResponse(false, null, "error"));

        assertThrows(
                TriggerServiceException.class,
                () -> bcpService.create(SpecificationDocument.FQN, getSpecificationDocumentProperties(supplier1))
        );
    }

    @Test
    public void validationError() {
        Supplier1p supplier1 = supplierTestUtils.createSupplier();

        Mockito.when(axaptaClient.createSpecificationDocument(any()))
                .thenReturn(new CreateSpecificationDocumentResponse(true, UUID.randomUUID().toString(), null));

        Mockito.when(axaptaClient.changeSpecificationStatus(any()))
                .thenReturn(new ChangeSpecificationStatusResponse(true, UUID.randomUUID().toString(), null));

        bcpService.create(SupplierAgreement.FQN, getAgreementProperties(supplier1));

        SpecificationDocument document = bcpService.create(SpecificationDocument.FQN,
                getSpecificationDocumentProperties(supplier1));

        JsonNode validationError = getJsonNodeByPath("/test/fps/module/supplier/specification/json/validationError" +
                ".json");
        bcpService.edit(document, Maps.of(SpecificationDocument.AX_RAW_DATA, validationError));
        Assertions.assertEquals(SpecificationDocument.Statuses.VALIDATION_ERROR, document.getStatus());
        Assertions.assertFalse(document.getErrors().isEmpty());
    }

    @Test
    public void sendToAxaptaAllProcess() throws IOException {
        Supplier1p supplier1 = supplierTestUtils.createSupplier();

        Mockito.when(axaptaClient.createSpecificationDocument(any()))
                .thenReturn(new CreateSpecificationDocumentResponse(true, UUID.randomUUID().toString(), null));

        Mockito.when(axaptaClient.changeSpecificationStatus(any()))
                .thenReturn(new ChangeSpecificationStatusResponse(true, UUID.randomUUID().toString(), null));

        doReturn(createAttachment()).when(specificationService).createAttachment(any());

        bcpService.create(SupplierAgreement.FQN, getAgreementProperties(supplier1));

        SpecificationDocument document = bcpService.create(SpecificationDocument.FQN,
                getSpecificationDocumentProperties(supplier1));

        Assertions.assertEquals(SpecificationDocument.Statuses.SEND_TO_AXAPTA, document.getStatus());

        JsonNode formed = getJsonNodeByPath("/test/fps/module/supplier/specification/json/formed.json");
        bcpService.edit(document, Maps.of(SpecificationDocument.AX_RAW_DATA, formed));
        Assertions.assertEquals(SpecificationDocument.Statuses.FORMED, document.getStatus());

        JsonNode needApprove = getJsonNodeByPath("/test/fps/module/supplier/specification/json/needApprove.json");
        bcpService.edit(document, Maps.of(SpecificationDocument.AX_RAW_DATA, needApprove));
        Assertions.assertEquals(SpecificationDocument.Statuses.NEED_APPROVE, document.getStatus());

        sign(document);
        Assertions.assertEquals(SpecificationDocument.Statuses.PARTIALLY_APPROVED, document.getStatus());

        JsonNode needApprove2 = getJsonNodeByPath("/test/fps/module/supplier/specification/json/needApprove2.json");
        bcpService.edit(document, Maps.of(SpecificationDocument.AX_RAW_DATA, needApprove2));
        Assertions.assertEquals(SpecificationDocument.Statuses.NEED_APPROVE, document.getStatus());

        sign(document);
        Assertions.assertEquals(SpecificationDocument.Statuses.COMPLETED, document.getStatus());
    }

    @Test
    public void invalidUrl() {
        Supplier1p supplier1 = supplierTestUtils.createSupplier();

        Mockito.when(axaptaClient.createSpecificationDocument(any()))
                .thenReturn(new CreateSpecificationDocumentResponse(true, UUID.randomUUID().toString(), null));

        bcpService.create(SupplierAgreement.FQN, getAgreementProperties(supplier1));

        SpecificationDocument document = bcpService.create(SpecificationDocument.FQN,
                getSpecificationDocumentProperties(supplier1));

        JsonNode invalidUrl = getJsonNodeByPath("/test/fps/module/supplier/specification/json/invalidUrl.json");
        assertThrows(
                UndeclaredThrowableException.class,
                () -> bcpService.edit(document, Maps.of(SpecificationDocument.AX_RAW_DATA, invalidUrl))
        );

        Assertions.assertEquals(SpecificationDocument.Statuses.SEND_TO_AXAPTA, document.getStatus());
    }

    private void sign(SpecificationDocument document) {
        List<Specification> specifications = document.getSpecifications()
                .stream()
                .filter(x -> x.getStatus().equals(Specification.Statuses.SIGNED_BY_US))
                .toList();


        for (Specification specification : specifications) {
            bcpService.edit(specification, Maps.of(Specification.STATUS, Specification.Statuses.SIGNED_BY_VENDOR));
        }
    }

    private Map<String, Object> getAgreementProperties(Supplier1p supplier1p) {
        return Maps.of(
                SupplierAgreement.TITLE, Randoms.string(),
                SupplierAgreement.SUPPLIER_1P, supplier1p,
                SupplierAgreement.AX_RS_ID, UUID.randomUUID().toString(),
                SupplierAgreement.AGREEMENT_DATE, Now.localDate(),
                SupplierAgreement.FROM_DATE, Now.localDate(),
                SupplierAgreement.CODE, "ДП-000005670"
        );
    }

    private Map<String, Object> getSpecificationDocumentProperties(Supplier1p supplier1p) {
        return Maps.of(
                SpecificationDocument.TITLE, Randoms.string(),
                SpecificationDocument.SUPPLIER_1P, supplier1p,
                SpecificationDocument.DOCUMENT, createAttachment()
        );
    }

    private Attachment createAttachment() {
        return bcpService.create(Attachment.FQN_DEFAULT, Maps.of(
                Attachment.NAME, Randoms.string(),
                Attachment.CONTENT_TYPE, Randoms.string(),
                Attachment.URL, Randoms.string()
        ));
    }

    private JsonNode getJsonNodeByPath(String path) {
        String stringConfig = CrmStrings.valueOf(ResourceHelpers.getResource(path));
        return objectSerializeService.deserialize(
                CrmStrings.getBytes(stringConfig),
                JsonNode.class
        );
    }
}
