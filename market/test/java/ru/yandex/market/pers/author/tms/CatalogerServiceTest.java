package ru.yandex.market.pers.author.tms;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.cataloger.model.CatalogerResponseWrapper;
import ru.yandex.market.cataloger.model.NavigationNode;
import ru.yandex.market.pers.author.PersAuthorTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author grigor-vlad
 * 18.08.2021
 */
public class CatalogerServiceTest extends PersAuthorTest {

    @Autowired
    private CatalogerClient catalogerClient;
    @Autowired
    private CatalogerService catalogerService;

    @Test
    public void testReadWithSkips() {
        CatalogerResponseWrapper response = catalogerClient.getNavigationTreeFromDepartment().orElse(null);
        List<NavigationNode> allNodes = catalogerService.getFilteredNavigationNodes(response, Collections.emptySet());
        List<NavigationNode> filteredNodes = catalogerService.getFilteredNavigationNodes(response, Set.of("54437"));

        assertEquals(190, allNodes.size());
        assertEquals(151, filteredNodes.size());
    }

}
