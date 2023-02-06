package ru.yandex.market.b2b.clients.common;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.mj.generated.client.yadoc.model.TransformationStatus;
import ru.yandex.mj.generated.server.model.DocumentPrintStatus;

public class DocumentPrintStatusTest {

    @Test
    public void testValues() {
        List<String> transformationStatuses = Arrays.stream(TransformationStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());
        List<String> documentPrintStatuses = Arrays.stream(DocumentPrintStatus.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        Assertions.assertTrue(documentPrintStatuses.containsAll(transformationStatuses),
                "TransformationStatus должен быть подмножеством DocumentPrintStatus (если нет, то надо добавить)");
    }
}
