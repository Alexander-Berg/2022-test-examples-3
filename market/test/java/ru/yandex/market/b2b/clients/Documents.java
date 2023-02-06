package ru.yandex.market.b2b.clients;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;

import ru.yandex.mj.generated.server.model.DocumentDto;
import ru.yandex.mj.generated.server.model.DocumentResponseDto;
import ru.yandex.mj.generated.server.model.DocumentType;
import ru.yandex.mj.generated.server.model.ForwardDestination;
import ru.yandex.mj.generated.server.model.GenerationStatusType;

public class Documents {

    public static void assertEquals(DocumentDto document, DocumentDto d) {
        Assertions.assertEquals(document.getOrder(), d.getOrder());
        Assertions.assertEquals(document.getType(), d.getType());
        Assertions.assertEquals(document.getNumber(), d.getNumber());
        if (document.getDate() != d.getDate()) {
            Assertions.assertTrue(document.getDate().truncatedTo(ChronoUnit.MILLIS)
                    .isEqual(d.getDate().truncatedTo(ChronoUnit.MILLIS)));
        }
        Assertions.assertEquals(document.getUrl(), d.getUrl());
        Assertions.assertEquals(document.getForward(), d.getForward());
        Assertions.assertEquals(document.getMeta(), d.getMeta());
        // NOT_NEED == null
        if(document.getGenerationStatus() == null) {
            Assertions.assertTrue(d.getGenerationStatus() == null || d.getGenerationStatus() == GenerationStatusType.NOT_NEED);
        } else if (d.getGenerationStatus() == null) {
            Assertions.assertTrue(document.getGenerationStatus() == null || document.getGenerationStatus() == GenerationStatusType.NOT_NEED);
        } else {
            Assertions.assertEquals(document.getGenerationStatus(), d.getGenerationStatus());
        }
        Assertions.assertEquals(document.getSellerId(), d.getSellerId());
    }

    public static void assertEquals(DocumentDto expect, DocumentResponseDto actual) {
        Assertions.assertEquals(expect.getNumber(), actual.getNumber());
        Assertions.assertEquals(expect.getOrder(), actual.getOrder());
        Assertions.assertEquals(expect.getType(), actual.getType());
        Assertions.assertEquals(expect.getUrl(), actual.getUrl());
        if (expect.getDate() != actual.getDate()) {
            Assertions.assertTrue(expect.getDate().truncatedTo(ChronoUnit.MILLIS)
                    .isEqual(actual.getDate().truncatedTo(ChronoUnit.MILLIS)));
        }
    }

    public static void assertContains(DocumentDto expected, List<DocumentDto> documents) {
        for (DocumentDto dto : documents) {
            if (dto.getOrder().equals(expected.getOrder()) && dto.getNumber().equals(expected.getNumber())
                    && dto.getType().equals(expected.getType())) {
                assertEquals(expected, dto);
            }
        }
    }

    public static DocumentDto random() {
        return random(false);
    }

    public static DocumentDto random(boolean withForwardAndMeta) {
        DocumentDto document = new DocumentDto();
        document.setOrder(Randoms.bigDecimal());
        document.setType(Randoms.enumConstant(DocumentType.class));
        document.setNumber(Randoms.string());
        document.setDate(Randoms.offsetDateTime());
        document.setUrl(Randoms.string());
        document.setGenerationStatus(null); // NOT_NEED
        document.setSellerId(Randoms.bigDecimal());

        if (withForwardAndMeta) {
            document.setForward(Randoms.enumConstant(ForwardDestination.class));
            document.setMeta(Map.of("document", Map.of(
                    "key1", "value1",
                    "key2", "value2",
                    "document_id", "100",
                    "document_source", "AXAPTA",
                    "document_type", "INV"
            )));
        }

        return document;
    }

    public static DocumentDto clone(DocumentDto doc) {
        DocumentDto document = new DocumentDto();
        document.setOrder(doc.getOrder());
        document.setType(doc.getType());
        document.setNumber(doc.getNumber());
        document.setDate(doc.getDate());
        document.setUrl(doc.getUrl());
        document.setGenerationStatus(doc.getGenerationStatus());
        document.setForward(doc.getForward());
        document.setMeta(doc.getMeta());
        document.setSellerId(doc.getSellerId());

        return document;
    }
}
