package ru.yandex.market.tpl.billing.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.entity.Partner;
import ru.yandex.yadoc.FileResource;
import ru.yandex.yadoc.YaDocClient;
import ru.yandex.yadoc.client.model.DocumentMeta;
import ru.yandex.yadoc.client.model.DocumentsContractsRequest;
import ru.yandex.yadoc.client.model.DocumentsMeta;

import static org.mockito.Mockito.when;

public class YaDocServiceTest extends AbstractFunctionalTest {

    @Autowired
    YaDocService yaDocService;

    @Autowired
    YaDocClient yaDocClient;

    @Test
    void getDocumentByIdTest() {
        FileResource document = yaDocService.getYaDocumentById(1853095L);
        Assertions.assertThat(document).isEqualTo(null);
    }

    @Test
    @DbUnitDataSet(before = "/database/service/yadocservice/before/two_partners.csv")
    void getDocumentMetaData() {
        when(yaDocClient.getDocumentsMeta(getDocumentsRequest())).thenReturn(getDocumentMeta());
        FileResource fileResource = yaDocService
                .getDocument(LocalDate.parse("2021-01-01"), new Partner().setId(1L).setBalanceContractId(1932363L));
        Assertions.assertThat(fileResource).isEqualTo(null);
    }

    private DocumentsContractsRequest getDocumentsRequest() {
        DocumentsContractsRequest documentsRequest = new DocumentsContractsRequest();
        documentsRequest.setContractIds(List.of(1932363L));
        documentsRequest.setDateTo(LocalDate.parse("2021-01-31"));
        documentsRequest.setDateFrom(LocalDate.parse("2021-01-01"));
        return documentsRequest;
    }

    private DocumentsMeta getDocumentMeta() {
        DocumentMeta inv = new DocumentMeta();
        inv.setDocDate(OffsetDateTime.parse("2021-01-31T00:00:00+03:00"));
        inv.setDocType(DocumentMeta.DocTypeEnum.INV);
        inv.setDocNumber("139361782");
        inv.setDocId(139361782L);
        inv.contractId(1932363L);
        inv.partyId(11234549L);
        inv.isSentByEmail(true);
        inv.isReversed(false);

        DocumentMeta act = new DocumentMeta();
        act.setDocDate(OffsetDateTime.parse("2021-01-31T00:00:00+03:00"));
        act.setDocType(DocumentMeta.DocTypeEnum.PARTNER_ACT);
        act.setDocNumber("139361782");
        act.setDocId(1853095L);
        act.contractId(1932363L);
        act.partyId(11234549L);
        act.isSentByEmail(true);
        act.isReversed(false);

        DocumentsMeta documentsMeta = new DocumentsMeta();
        documentsMeta.addDocumentsItem(inv);
        documentsMeta.addDocumentsItem(act);

        return documentsMeta;
    }
}
