package ru.yandex.market.robot.tasks.crawlers;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 03.02.12
 */
public class OffersImportTaskTest extends Assert {
    @Test
    public void testCleanTitleFromCategory() throws Exception {
        OffersImportTask task = new OffersImportTask();
        assertEquals(
            "Samsung C3011 Blue",
            cleanStringFromCategory(
                task,
                "Сотовый телефон Samsung C3011 Blue",
                "Samsung|Сотовые телефоны"
            )
        );

        assertEquals(
            "Кружка термо Hama Office Blue 111133",
            cleanStringFromCategory(
                task,
                "гаджеты автомобильные Кружка термо Hama Office Blue 111133",
                "Автотехника и аксессуары гаджеты автомобильные"
            )
        );
    }

    public String cleanStringFromCategory(OffersImportTask task, String title, String category) {
        List<OffersImportTask.Token> tokens = task.tokenize(title);
        for (String categoryName : category.split("[|]")) {
            task.cleanTitleFromCategory(tokens, categoryName);
        }
        return task.getStringForTokens(title, tokens);
    }
}
