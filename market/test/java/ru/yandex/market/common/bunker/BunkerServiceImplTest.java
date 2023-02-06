package ru.yandex.market.common.bunker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.market.common.bunker.loader.BunkerLoader;
import ru.yandex.market.common.bunker.loader.HttpBunkerLoader;
import ru.yandex.market.common.bunker.model.BunkerNode;
import ru.yandex.market.common.bunker.model.NodeInfo;
import ru.yandex.market.common.bunker.model.route.RouteContent;
import ru.yandex.market.common.bunker.model.route.RoutesDataField;
import ru.yandex.market.common.bunker.model.route.RoutesNodeContent;
import ru.yandex.market.common.test.matcher.HttpGetMatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * Тесты для {@link BunkerService}.
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class BunkerServiceImplTest {
    private final String bunkerUrl = "http://bunker-test.yandex.net";

    private BunkerService bunkerService;
    private BunkerService bunkerServiceWithNotMockLoader;

    @Mock
    private BunkerLoader bunkerLoader;

    @Mock
    private HttpClient httpClient;

    private HttpResponseFactory factory;

    @BeforeEach
    void setUp() {
        bunkerServiceWithNotMockLoader = new BunkerServiceImpl(new HttpBunkerLoader(bunkerUrl, httpClient));
        bunkerService = new BunkerServiceImpl(bunkerLoader);
        factory = new DefaultHttpResponseFactory();
    }

    /**
     * Тест проверяет, что сервис возвращает корректные данные для ноды routes.
     */
    @Test
    void getRoutesNodeTest() throws IOException {
        when(bunkerLoader.getNodeStream("/market-partner/routes", BunkerService.Version.LATEST)).
                thenReturn(this.getClass().getResourceAsStream("get_node_for_routes_test.json"));
        BunkerNode<?> actualNode = bunkerService.getNodeContent("/market-partner/routes",
                BunkerService.Version.LATEST, RoutesNodeContent.class);
        BunkerNode<?> expectedNode = new BunkerNode<>(Arrays.asList(
                new RouteContent("market-partner:file:agency-about-report:get",
                        "/api/agency/about/report", new RoutesDataField("GET")),
                new RouteContent("market-partner:html:auction:get",
                        "/auction(/)", new RoutesDataField("GET"))
        ));
        MatcherAssert.assertThat(actualNode, Matchers.equalTo(expectedNode));
    }

    /**
     * Тест проверяет, что при запросе удаленной ноды, возвращается 410 код и выбрасывается исключение.
     * Также проверяется, что проставляется дефолтное значение версии, когда пользователь не указывает его явно.
     */
    @Test
    void getRoutesNodeTestForDeletedNode() throws Exception {
        HttpResponse pingResponse = factory.newHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null);
        pingResponse.setEntity(new StringEntity("pong"));
        HttpResponse routesResponse = factory.newHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_GONE, null);
        routesResponse.setEntity(new StringEntity("Requested version of node is deleted"));
        when(httpClient.execute(argThat(new HttpGetMatcher(bunkerUrl + "/ping", "GET")))).
                thenReturn(pingResponse);
        when(httpClient.execute(argThat(new HttpGetMatcher(bunkerUrl + "/v1/cat?node=%2Ffill&version=stable", "GET")))).
                thenReturn(routesResponse);
        Throwable thrown = assertThrows(RuntimeException.class, () -> bunkerServiceWithNotMockLoader.
                getNodeContent("/fill", null, RoutesNodeContent.class));
        assertEquals("HTTP 410 Requested version of node is deleted", thrown.getCause().getMessage());
    }

    /**
     * Тест проверяет, что при запросе несущетсвующей ноды возвращается 404 код и выбрасывается исключение.
     */
    @Test
    void getRoutesNodeTestForNonexistentNode() throws Exception {
        HttpResponse pingResponse = factory.newHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null);
        pingResponse.setEntity(new StringEntity("pong"));
        HttpResponse routesResponse = factory.newHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, null);
        routesResponse.setEntity(new StringEntity("Node does not exist"));
        when(httpClient.execute(argThat(new HttpGetMatcher(bunkerUrl + "/ping", "GET")))).
                thenReturn(pingResponse);
        when(httpClient.execute(argThat(new HttpGetMatcher(bunkerUrl + "/v1/cat?node=%2Ffill&version=stable", "GET")))).
                thenReturn(routesResponse);
        Throwable thrown = assertThrows(RuntimeException.class, () -> bunkerServiceWithNotMockLoader.
                getNodeContent("/fill", null, RoutesNodeContent.class));
        assertEquals("HTTP 404 Node does not exist", thrown.getCause().getMessage());
    }

    /**
     * Тест проверяет, что сервис игнорирует поле condition при получении данных для ноды routes.
     */
    @Test
    void getRoutesNodeWithIgnoreConditionsField() throws IOException {
        when(bunkerLoader.getNodeStream("/market-partner/routes", BunkerService.Version.LATEST)).
                thenReturn(this.getClass().getResourceAsStream(
                        "get_node_for_routes_ignore_conditions_test.json"));
        BunkerNode<?> actualNode = bunkerService.getNodeContent("/market-partner/routes",
                BunkerService.Version.LATEST, RoutesNodeContent.class);
        BunkerNode<?> expectedNode = new BunkerNode<>(Collections.singletonList(
                new RouteContent("market-partner:file:agency-about-report:get",
                        "/api/agency/about/report", new RoutesDataField("GET"))
        ));
        MatcherAssert.assertThat(actualNode, Matchers.equalTo(expectedNode));
    }

    /**
     * Тест проверяет, что при запросе списка дочерних нод возвращаются корректные данные.
     */
    @Test
    void getListOfChildNodesTest() throws Exception {
        when(bunkerLoader.getListOfChildNodesStream("/market-partner/banners", BunkerService.Version.LATEST)).
                thenReturn(this.getClass().getResourceAsStream("get_list_of_child_bunker_service_test.json"));
        List<NodeInfo> actualListOfChildNodes = bunkerService.getListOfChildNodes("/market-partner/banners",
                BunkerService.Version.LATEST);
        List<NodeInfo> expectedListOfChildNodes = new ArrayList<>();
        NodeInfo nodeInfo = new NodeInfo(".schema",
                "/market-partner/banners/.schema", 3, false,
                "application/schema+json; charset=utf-8; schema=\"bunker:/.schema/base#\"");
        nodeInfo.setSaveDate("2016-11-01T15:40:03.000Z");
        expectedListOfChildNodes.add(nodeInfo);
        MatcherAssert.assertThat(actualListOfChildNodes, Matchers.equalTo(expectedListOfChildNodes));
    }

    /**
     * Тест проверяет, что при запросе списка дочерних нод для ноды без дочерних нод возвращается пустой список.
     */
    @Test
    void getListOfChildNodesForNodeWithoutChild() throws Exception {
        when(bunkerLoader.getListOfChildNodesStream("/market-partner/banners", BunkerService.Version.LATEST)).
                thenReturn(new ByteArrayInputStream("[]".getBytes()));
        List<NodeInfo> actualListOfChildNodes = bunkerService.getListOfChildNodes("/market-partner/banners",
                BunkerService.Version.LATEST);
        List<NodeInfo> expectedListOfChildNodes = new ArrayList<>();
        MatcherAssert.assertThat(actualListOfChildNodes, Matchers.equalTo(expectedListOfChildNodes));
    }

    /**
     * Тест проверяет, что при запросе списка дочерних нод для несущетсвующей ноды
     * возвращается 404 код и выбрасывается исключение.
     */
    @Test
    void getListOfChildNodesForNonexistentNode() throws Exception {
        HttpResponse pingResponse = factory.newHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null);
        pingResponse.setEntity(new StringEntity("pong"));
        HttpResponse childNodesResponse = factory.newHttpResponse(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, null);
        childNodesResponse.setEntity(new StringEntity("Node does not exist"));
        when(httpClient.execute(argThat(new HttpGetMatcher(bunkerUrl + "/ping", "GET")))).
                thenReturn(pingResponse);
        when(httpClient.execute(argThat(new HttpGetMatcher(bunkerUrl + "/v1/ls?node=%2Ffill&version=stable", "GET")))).
                thenReturn(childNodesResponse);
        Throwable thrown = assertThrows(RuntimeException.class, () -> bunkerServiceWithNotMockLoader.
                getListOfChildNodes("/fill", null));
        assertEquals("HTTP 404 Node does not exist", thrown.getCause().getMessage());
    }

    /**
     * Тест проверяет, что при рекурсивном запросе списка всех дочерних нод возвращаются корректные данные.
     */
    @Test
    void getListsChildrenNodesRecursively() throws Exception {
        when(bunkerLoader.getListsChildrenNodesRecursivelyStream("/market-partner/i18n",
                BunkerService.Version.LATEST)).thenReturn(this.getClass().
                getResourceAsStream("get_lists_children_nodes_bunker_service_test.json"));
        List<NodeInfo> actualListOfChildNodes = bunkerService.getListsChildrenNodesRecursively("/market-partner/i18n",
                BunkerService.Version.LATEST);
        List<NodeInfo> expectedListOfChildNodes = new ArrayList<>();
        expectedListOfChildNodes.add(new NodeInfo("i18n", "/market-partner/i18n", 5,
                false, null));
        expectedListOfChildNodes.add(new NodeInfo("b2b-annex", "/market-partner/i18n/b2b-annex", 6,
                false, null));
        expectedListOfChildNodes.add(new NodeInfo("partner", "/market-partner/i18n/partner", 2,
                false, null));
        MatcherAssert.assertThat(actualListOfChildNodes, Matchers.equalTo(expectedListOfChildNodes));
    }
}
