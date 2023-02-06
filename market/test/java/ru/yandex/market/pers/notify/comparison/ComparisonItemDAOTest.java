package ru.yandex.market.pers.notify.comparison;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItem;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.Uid;
import ru.yandex.market.pers.notify.model.Uuid;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         22.11.16
 */
public class ComparisonItemDAOTest extends ComparisonServiceTest {
    @Autowired
    @Qualifier("comparisonItemDAO")
    private void setComparisonService(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    @Test
    public void testExportProductIds() throws Exception {
        Identity identity1 = new Uuid("23ejk4hr9d8y192");
        long id1 = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity1);
        Identity identity2 = new Uid(325445L);
        long id2 = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity2);
        List<ComparisonItem> items1 = Arrays.asList(
            new ComparisonItem(id1, 12398123L, "adksjfhisaf893323"),
            new ComparisonItem(id1, 12398123L, "123"),
            new ComparisonItem(id1, 99L, "woi32ur8232")
        );
        List<ComparisonItem> items2 = Arrays.asList(
            new ComparisonItem(id2, 12398123L, "123"),
            new ComparisonItem(id2, 99L, "fh546gfh4")
        );
        assertTrue(comparisonService.saveItems(identity1, items1));
        assertTrue(comparisonService.saveItems(identity2, items2));
        ComparisonItemDAO dao = (ComparisonItemDAO) comparisonService;
        try (StringWriter sw = new StringWriter()) {
            dao.exportProductIds(new PrintWriter(sw), (s) -> true);
            Set<String> productIds = Arrays.stream(sw.toString().split("\n"))
                .collect(Collectors.toSet());
            Set<String> actualProductIds = new HashSet<>(
                Arrays.asList("adksjfhisaf893323", "123", "woi32ur8232", "fh546gfh4")
            );
            assertEquals(actualProductIds, productIds);
        }
    }
}
