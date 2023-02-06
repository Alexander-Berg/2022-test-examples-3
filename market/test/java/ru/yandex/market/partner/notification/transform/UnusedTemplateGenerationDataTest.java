package ru.yandex.market.partner.notification.transform;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.partner.notification.transform.common.AbstractTemplateGenerationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnusedTemplateGenerationDataTest {
    private static final Logger log = LoggerFactory.getLogger(UnusedTemplateGenerationDataTest.class);

    private final Set<String> setOfXslResources = AbstractTemplateGenerationTest
            .getResourcesMap("liquibase/templates/xsl", "xsl").keySet();

    @Test
    @DisplayName("Тест, проверяющий есть ли неиспользуемые тестовые данные")
    void checkUnusedTestData() {
        Set<String> setOfTxtResources = AbstractTemplateGenerationTest
                .getResourcesMap("templates/telegram", "txt").keySet();

        Set<String> setOfXmlResources = AbstractTemplateGenerationTest
                .getResourcesMap("xml/data", "xml").keySet();

        int totalUnused = findUnusedIdsInSet(setOfTxtResources) + findUnusedIdsInSet(setOfXmlResources);

        assertEquals(0, totalUnused);
    }

    private int findUnusedIdsInSet(Set<String> templates) {
        int found = 0;

        for (String filename : templates) {
            String idPrefix = filename.split("_")[0];

            if (!setOfXslResources.contains(idPrefix)) {
                ++found;
                log.error("Found unused data: {}", filename);
            }
        }

        return found;
    }
}
