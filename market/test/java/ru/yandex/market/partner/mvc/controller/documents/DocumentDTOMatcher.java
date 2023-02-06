package ru.yandex.market.partner.mvc.controller.documents;

import java.time.LocalDate;

import org.hamcrest.Matcher;

import ru.yandex.market.mbi.util.MbiMatchers;

public class DocumentDTOMatcher {

    public static Matcher<DocumentDTO> hasDocId(long docId) {
        return MbiMatchers.<DocumentDTO>newAllOfBuilder()
                .add(DocumentDTO::getDocId, docId, "docId")
                .build();
    }

    public static Matcher<DocumentDTO> hasDocNumber(String docNumber) {
        return MbiMatchers.<DocumentDTO>newAllOfBuilder()
                .add(DocumentDTO::getDocNumber, docNumber, "docNumber")
                .build();
    }

    public static Matcher<DocumentDTO> hasDocType(DocumentTypeDTO docType) {
        return MbiMatchers.<DocumentDTO>newAllOfBuilder()
                .add(DocumentDTO::getDocType, docType, "docType")
                .build();
    }

    public static Matcher<DocumentDTO> hasDocDate(LocalDate docDate) {
        return MbiMatchers.<DocumentDTO>newAllOfBuilder()
                .add(DocumentDTO::getDocDate, docDate, "docDate")
                .build();
    }

    public static Matcher<DocumentDTO> hasContractExternalId(String contractExternalId) {
        return MbiMatchers.<DocumentDTO>newAllOfBuilder()
                .add(DocumentDTO::getContractExternalId, contractExternalId, "contractExternalId")
                .build();
    }

    public static Matcher<DocumentDTO> hasContractType(PartnerContractTypeDTO contractType) {
        return MbiMatchers.<DocumentDTO>newAllOfBuilder()
                .add(DocumentDTO::getContractType, contractType, "contractType")
                .build();
    }
}
