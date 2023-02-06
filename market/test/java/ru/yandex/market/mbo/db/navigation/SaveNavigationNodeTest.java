package ru.yandex.market.mbo.db.navigation;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleNavigationNode;
import ru.yandex.market.mbo.gwt.models.navigation.SimpleTag;
import ru.yandex.market.mbo.gwt.models.tovartree.OutputType;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SaveNavigationNodeTest {

    private static final long UID = 228L;

    private NavigationTreeService navigationTreeService;

    @Before
    public void setUp() throws Exception {
        navigationTreeService = new NavigationTreeServiceMock();
    }

    @Test
    public void shouldSucceedWithSaveCategoryNodeToNoTovarNode() throws Exception {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.CATEGORY);
        node.setHid(100L); // нет такой товарной категории

        navigationTreeService.saveNavigationNode(UID, node);
    }

    @Test
    public void shouldSucceedWithSaveGurulightRecipeNodeToGuruTovarNode() throws Exception {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.GURULIGHT_RECIPE);
        node.setHid(2L);

        navigationTreeService.saveNavigationNode(UID, node);
    }

    @Test
    public void shouldSucceedWithSaveGurulightRecipeNodeToGurulightNode() throws Exception {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.GURULIGHT_RECIPE);
        node.setHid(3L);

        navigationTreeService.saveNavigationNode(UID, node);
    }

    @Test
    public void shouldSucceedWithSaveGurulightRecipeNodeToMixedNode() throws Exception {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.GURULIGHT_RECIPE);
        node.setHid(4L);

        navigationTreeService.saveNavigationNode(UID, node);
    }

    @Test
    public void shouldSucceedWithSaveGurulightRecipeNodeToSimpleNode() throws Exception {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.GURULIGHT_RECIPE);
        node.setHid(5L);

        navigationTreeService.saveNavigationNode(UID, node);
    }

    @Test
    public void shouldSucceedWithSaveGurulightRecipeNodeToUnderfinedNode() throws Exception {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.GURULIGHT_RECIPE);
        node.setHid(6L);

        navigationTreeService.saveNavigationNode(UID, node);
    }

    @Test
    public void shouldSucceedWithSaveGurulightRecipeNodeToVisualNode() throws Exception {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.GURULIGHT_RECIPE);
        node.setHid(7L);

        navigationTreeService.saveNavigationNode(UID, node);
    }

    @Test
    public void shouldSuccedWithSaveCategoryNodeToAnyTovarNode() throws Exception {
        for (long i = 0; i < OutputType.values().length; i++) {
            NavigationNode node = SimpleNavigationNode.newValue();
            node.setType(NavigationNode.Type.CATEGORY);
            node.setHid(i + 2);

            navigationTreeService.saveNavigationNode(UID, node);
        }
    }

    @Test
    public void shouldSuccedWithSaveGenericNodeToAnyTovarNode() throws Exception {
        for (long i = 0; i < OutputType.values().length; i++) {
            NavigationNode node = SimpleNavigationNode.newValue();
            node.setType(NavigationNode.Type.GENERIC);
            node.setHid(i + 2);

            navigationTreeService.saveNavigationNode(UID, node);
        }
    }

    @Test
    public void shouldSuccedWithSaveUrlNodeToAnyTovarNode() throws Exception {
        for (long i = 0; i < OutputType.values().length; i++) {
            NavigationNode node = SimpleNavigationNode.newValue();
            node.setType(NavigationNode.Type.URL);
            node.setHid(i + 2);

            navigationTreeService.saveNavigationNode(UID, node);
        }
    }

    @Test
    public void shouldSucceedSaveNodeWithCategoryTag() {
        NavigationNode node = SimpleNavigationNode.newValue();
        node.setType(NavigationNode.Type.CATEGORY);
        node.setTagList(Collections.singletonList(new SimpleTag()));

        navigationTreeService.saveNavigationNode(UID, node);
    }
}
