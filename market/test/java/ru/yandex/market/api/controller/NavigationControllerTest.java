package ru.yandex.market.api.controller;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.NavigationNodeField;
import ru.yandex.market.api.domain.catalog.NavigationCategoryV1;
import ru.yandex.market.api.domain.catalog.NavigationTreeResponse;
import ru.yandex.market.api.domain.v2.AbstractResult;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.util.httpclient.clients.CatalogerTestClient;

import javax.inject.Inject;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

/**
 * Created by tesseract on 18.04.17.
 */
public class NavigationControllerTest extends BaseTest {

    @Inject
    NavigationController controller;
    @Inject
    CatalogerTestClient catalogerTestClient;

    /**
     * Проверяем правильность преобразования узла типа LINK с datasourceType="LIST"
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3471">MARKETAPI-3471: Разворачивать узлы нав. дерева типа LINK в ручке v2/navigation/tree</a>
     */
    @Test
    public void getTreeV2_LinkList() {
        // вызов системы
        NavigationTreeResponse result = getRootWithAllChildren();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertEquals(AbstractResult.Status.OK, result.getStatus());
        Assert.assertNotNull(result.getContext());
        NavigationCategoryV1 node = getNode(result, 61808);

        Assert.assertEquals("Мобильные телефоны", node.getName());
        Assert.assertEquals("Мобильные телефоны", node.getFullName());
        Assert.assertEquals("GURU_RECIPE", node.getType());
        Assert.assertNotNull(node.getDatasource());
        Assert.assertEquals("LIST", node.getDatasource().getType());
        Assert.assertEquals(Integer.valueOf(91491), node.getDatasource().getHid());
        Assert.assertEquals(61808, node.getDatasource().getNid());
        Assert.assertNotNull(node.getDatasource().getOrder());
        Assert.assertEquals(UniversalModelSort.SortField.DISCOUNT, node.getDatasource().getOrder().getSort());
        Assert.assertEquals(SortOrder.DESC, node.getDatasource().getOrder().getHow());
    }

    /**
     * Проверяем правильность преобразования узла типа LINK с datasourceType="CLUSTERLIST"
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3471">MARKETAPI-3471: Разворачивать узлы нав. дерева типа LINK в ручке v2/navigation/tree</a>
     */
    @Test
    public void getTreeV2_LinkClusterlist() {
        // вызов системы
        NavigationTreeResponse result = getRootWithAllChildren();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertEquals(AbstractResult.Status.OK, result.getStatus());
        Assert.assertNotNull(result.getContext());
        NavigationCategoryV1 node = getNode(result, 65392);

        Assert.assertEquals("Одежда для малышей", node.getName());
        Assert.assertEquals("Одежда для малышей", node.getFullName());
        Assert.assertEquals("GURU_RECIPE", node.getType());
        Assert.assertNotNull(node.getDatasource());
        Assert.assertEquals("LIST", node.getDatasource().getType());
        Assert.assertEquals(Integer.valueOf(7812011), node.getDatasource().getHid());
        Assert.assertEquals(65392, node.getDatasource().getNid());
        Assert.assertNull(node.getDatasource().getOrder());
    }

    /**
     * Проверяем правильность преобразования узла типа LINK с datasourceType="CATALOG"
     *
     * @see <a href="https://st.yandex-team.ru/MARKETAPI-3471">MARKETAPI-3471: Разворачивать узлы нав. дерева типа LINK в ручке v2/navigation/tree</a>
     */
    @Test
    public void getTreeV2_LinkExtends_LinkCatalog() {
        // вызов системы
        NavigationTreeResponse result = getRootWithAllChildren();
        // проверка утверждений
        Assert.assertNotNull(result);
        Assert.assertEquals(AbstractResult.Status.OK, result.getStatus());
        Assert.assertNotNull(result.getContext());
        NavigationCategoryV1 node = getNode(result, 66076);

        Assert.assertEquals("Название должно взяться из исходного узла", "Детская площадка", node.getName());
        Assert.assertEquals("Название должно взяться из исходного узла", "Детская площадка", node.getFullName());
        Assert.assertEquals("После преобразования узла тип должен измениться на тип категории назначения", "CATEGORY", node.getType());
        Assert.assertNotNull(node.getDatasource());
        Assert.assertEquals("После преобразования узла datasource должен измениться на datasource категории назначения","CATALOG", node.getDatasource().getType());
        Assert.assertEquals("После преобразования узла datasource должен измениться на datasource категории назначения", Integer.valueOf(13491298), node.getDatasource().getHid());
        Assert.assertEquals("После преобразования узла datasource должен измениться на datasource категории назначения",70088, node.getDatasource().getNid());
        Assert.assertNull(node.getDatasource().getOrder());
    }

    private NavigationCategoryV1 getNode(NavigationTreeResponse result, int id) {
        Deque<NavigationCategoryV1> queue = new ArrayDeque<>();
        queue.add(result.getCategory());

        while (!queue.isEmpty()) {
            NavigationCategoryV1 node = queue.poll();
            if (id == node.getId()) {
                return node;
            }
            queue.addAll(node.getChildren());
        }

        throw new RuntimeException("Not found: " + id);
    }

    private NavigationTreeResponse getRootWithAllChildren() {
        // настройка системы
        catalogerTestClient.getTree(0, 10, 213, "cataloger_navigationTree_root.xml");
        catalogerTestClient.getTree(70088, 7, 213, "cataloger_navigationTree_70088.xml");
        catalogerTestClient.getTree(68795, 8, 213, "cataloger_navigationTree_68795.xml");
        catalogerTestClient.getTree(66431, 7, 213, "cataloger_navigationTree_66431.xml");
        catalogerTestClient.getTree(66131, 7, 213, "cataloger_navigationTree_66131.xml");
        catalogerTestClient.getTree(66193, 7, 213, "cataloger_navigationTree_66193.xml");
        catalogerTestClient.getTree(66201, 7, 213, "cataloger_navigationTree_66201.xml");
        catalogerTestClient.getTree(66147, 7, 213, "cataloger_navigationTree_66147.xml");
        catalogerTestClient.getTree(66457, 7, 213, "cataloger_navigationTree_66457.xml");
        catalogerTestClient.getTree(54705, 7, 213, "cataloger_navigationTree_54705.xml");
        catalogerTestClient.getTree(54683, 7, 213, "cataloger_navigationTree_54683.xml");
        catalogerTestClient.getTree(60774, 8, 213, "cataloger_navigationTree_60774.xml");
        // вызов системы
        return controller.getTreeV2(0, 10,
            NavigationController.Visibility.ALL, EnumSet.allOf(NavigationNodeField.class), null).waitResult().getBody();
    }
}
