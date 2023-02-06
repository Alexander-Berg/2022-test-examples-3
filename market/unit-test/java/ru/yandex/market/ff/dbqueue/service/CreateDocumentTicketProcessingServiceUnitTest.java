package ru.yandex.market.ff.dbqueue.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateDocumentTicketProcessingServiceUnitTest {

    @Test
    void replaceAllNonalphanumericCharsForRostov() {
        String value = CreateDocumentTicketProcessingService
                .replaceAllNonalphanumericChars("Яндекс.Маркет (Ростов-на-Дону)", "_");

        assertEquals("Яндекс_Маркет_Ростов_на_Дону", value);
    }
}
