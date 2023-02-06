package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.checkouter.order.JsonInstance.toNode;
import static ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil.CIS_NODE_NAME;
import static ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil.CIS_OPTIONAL_CARGOTYPE_CODE;
import static ru.yandex.market.checkout.checkouter.util.OrderItemInstancesUtil.CIS_REQUIRED_CARGOTYPE_CODE;

@ExtendWith(MockitoExtension.class)
public class OrderItemInstancesUtilTest {

    private final String cis1 = "c#1";
    private final String cis2 = "c#2";
    private final String uit1 = "u#1";
    private final String orderBal1 = "ob#1";
    private final String orderBal2 = "ob#2";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseCises() throws IOException {
        String instancesStr = "[{\"cis\": \"" + cis1 + "\"}, {\"cis\": \"" + cis2 + "\"}]\n";
        ArrayNode instances = (ArrayNode) mapper.readTree(instancesStr);
        List<String> cises = OrderItemInstancesUtil.parseCises(instances);
        assertThat(cises, hasSize(2));
        assertThat(cises, equalTo(Arrays.asList(cis1, cis2)));

        instancesStr = "[{\"sn\": \"" + cis1 + "\"}, {\"cis\": \"" + cis2 + "\"}]\n";
        instances = (ArrayNode) mapper.readTree(instancesStr);
        cises = OrderItemInstancesUtil.parseCises(instances);
        assertThat(cises, hasSize(1));
        assertThat(cises, equalTo(Collections.singletonList(cis2)));

        instancesStr = "[{\"sn\": \"" + cis1 + "\"}, {\"sn\": \"" + cis2 + "\"}]\n";
        instances = (ArrayNode) mapper.readTree(instancesStr);
        cises = OrderItemInstancesUtil.parseCises(instances);
        assertThat(cises, empty());

        instancesStr = "[]\n";
        instances = (ArrayNode) mapper.readTree(instancesStr);
        cises = OrderItemInstancesUtil.parseCises(instances);
        assertThat(cises, empty());

        instancesStr = "";
        instances = (ArrayNode) mapper.readTree(instancesStr);
        cises = OrderItemInstancesUtil.parseCises(instances);
        assertThat(cises, empty());

        cises = OrderItemInstancesUtil.parseCises(null);
        assertThat(cises, empty());

        instancesStr = "[{\"sn\": \"" + "123" + "\", \"cis\": \"" + cis1 + "\", \"cis\": \"" + cis2 + "\"}, " +
                "{\"imei\": \"" + cis2 + "\"}]\n";
        instances = (ArrayNode) mapper.readTree(instancesStr);
        cises = OrderItemInstancesUtil.parseCises(instances);
        assertThat(cises, hasSize(1));
        assertThat(cises, equalTo(Collections.singletonList(cis2)));
    }

    @Test
    public void putCises() throws IOException {
        ArrayNode instances = null;
        List<OrderItemInstance> cises = new ArrayList<>();
        cises.add(new OrderItemInstance(cis1));
        cises.add(new OrderItemInstance(cis2));
        ArrayNode arrayNode = OrderItemInstancesUtil.putCises(instances, cises);
        assertThat(arrayNode.toString(), equalTo("[{\"cis\":\"" + cis1 + "\"},{\"cis\":\"" + cis2 + "\"}]"));

        instances = (ArrayNode) mapper.readTree("[{\"cis\":\"" + "123" + "\"},{\"cis\":\"" + "123" + "\"}]");
        arrayNode = OrderItemInstancesUtil.putCises(instances, cises);
        assertThat(arrayNode.toString(), equalTo("[{\"cis\":\"" + cis1 + "\"},{\"cis\":\"" + cis2 + "\"}]"));

        instances = (ArrayNode) mapper.readTree("[{\"balanceOrderId\":\"123\",\"sn\":\"abc\"},"
                + "{\"balanceOrderId\": \"123\"}]");
        arrayNode = OrderItemInstancesUtil.putCises(instances, cises);
        assertThat(arrayNode.toString(), equalTo("[{\"cis\":\"" + cis1 + "\",\"balanceOrderId\":\"123\"," +
                "\"sn\":\"abc\"},{\"cis\":\"" + cis2 + "\",\"balanceOrderId\":\"123\"}]"));
    }

    @Test
    @DisplayName("Вставка идентификаторов товаров. Новых столько же, как было, но в новых есть дубли")
    public void putCisesEqualsCaseWithDuplicate() throws IOException {
        ArrayNode instanceNode = (ArrayNode) mapper.readTree(toNode(
                new JsonInstance("c1", "b1"),
                new JsonInstance("c2", "b2")));

        ArrayNode result = OrderItemInstancesUtil.putCises(instanceNode,
                List.of(new OrderItemInstance(cis1), new OrderItemInstance(cis1)));
        assertThat(result.toString(), equalTo(toNode(
                new JsonInstance(cis1, "b1"))));
    }

    @Test
    @DisplayName("Вставка идентификаторов товаров. Новых столько же, как было")
    public void putCisesEqualsCase() throws IOException {
        ArrayNode instanceNode = (ArrayNode) mapper.readTree(toNode(
                new JsonInstance("c1", "b1"),
                new JsonInstance("c2", "b2")));

        ArrayNode result = OrderItemInstancesUtil.putNodes(instanceNode, CIS_NODE_NAME, List.of(cis1, cis2));

        assertThat(result.toString(), equalTo(toNode(
                new JsonInstance(cis1, "b1"),
                new JsonInstance(cis2, "b2"))));
    }

    @Test
    @DisplayName("Вставка идентификаторов. Новых меньше чем было")
    public void putCisesGreaterCase() throws IOException {
        ArrayNode instanceNode = (ArrayNode) mapper.readTree(toNode(
                new JsonInstance("c1", "b1"),
                new JsonInstance("c2", "b2"),
                new JsonInstance("c3", "b3")));

        ArrayNode result = OrderItemInstancesUtil.putNodes(instanceNode, CIS_NODE_NAME, List.of(cis1, cis2));

        assertThat(result.toString(), equalTo(toNode(
                new JsonInstance(cis1, "b1"),
                new JsonInstance(cis2, "b2"))));
    }

    @Test
    @DisplayName("Вставка идентификаторов. Новых больше чем было")
    public void putCisesLowerCase() throws IOException {
        ArrayNode instanceNode = (ArrayNode) mapper.readTree(toNode(
                new JsonInstance("c1", "b1")));

        ArrayNode result = OrderItemInstancesUtil.putNodes(instanceNode, CIS_NODE_NAME, List.of(cis1, cis2));

        assertThat(result.toString(), equalTo(toNode(
                new JsonInstance(cis1, "b1"),
                new JsonInstance(cis2, null))));
    }

    @Test
    @DisplayName("Вставка идентификаторов. Новых значений нет")
    public void putCisesEmptyCase() throws IOException {
        ArrayNode instanceNode = (ArrayNode) mapper.readTree(toNode(
                new JsonInstance("c1", "b1")));

        ArrayNode result = OrderItemInstancesUtil.putNodes(instanceNode, CIS_NODE_NAME, List.of());

        assertThat(result.toString(), equalTo("[]"));
    }

    @Test
    @DisplayName("Определение обязательности КиЗ")
    public void defineOrderItemRequireCises() {
        OrderItem orderItem = Mockito.mock(OrderItem.class);
        Mockito.when(orderItem.getCargoTypes()).thenReturn(null);
        Assertions.assertFalse(OrderItemInstancesUtil.isOrderItemRequireCises(orderItem));

        Mockito.when(orderItem.getCargoTypes()).thenReturn(Set.of(CIS_OPTIONAL_CARGOTYPE_CODE));
        Assertions.assertFalse(OrderItemInstancesUtil.isOrderItemRequireCises(orderItem));

        Mockito.when(orderItem.getCargoTypes()).thenReturn(Set.of(CIS_REQUIRED_CARGOTYPE_CODE));
        Assertions.assertTrue(OrderItemInstancesUtil.isOrderItemRequireCises(orderItem));
    }

    @Test
    @DisplayName("Конвертация идентификаторов к ноде. Пустой список")
    public void convertToNodeWithEmptyInstances() {
        List<OrderItemInstance> instances = List.of();

        ArrayNode result = OrderItemInstancesUtil.convertToNode(instances);

        assertThat(result.toString(), equalTo(toNode()));
    }

    @Test
    @DisplayName("Конвертация идентификаторов к ноде. Один идентификатор")
    public void convertToNodeWithOneInstances() {
        List<OrderItemInstance> instances = List.of(
                new OrderItemInstance(cis1, null, orderBal1));

        ArrayNode result = OrderItemInstancesUtil.convertToNode(instances);

        assertThat(result.toString(), equalTo(toNode(
                new JsonInstance(cis1, null, orderBal1))));
    }

    @Test
    @DisplayName("Конвертация идентификаторов к ноде. Много идентификаторов")
    public void convertToNodeWithManyInstances() {
        List<OrderItemInstance> instances = List.of(
                new OrderItemInstance(cis1, uit1, null),
                new OrderItemInstance(cis2, null, orderBal2)
        );

        ArrayNode result = OrderItemInstancesUtil.convertToNode(instances);

        assertThat(result.toString(), equalTo(toNode(
                new JsonInstance(cis1, uit1, null),
                new JsonInstance(cis2, null, orderBal2))));
    }

    @Test
    @DisplayName("Проверить что парсинг информации о элементе заказа находит все поля")
    public void parseInstancesAllFields() throws Exception {
        ArrayNode jsonNode = (ArrayNode) mapper.readTree("[{" +
                "\"balanceOrderId\":\"balanceOrderId\"," +
                "\"cis\":\"cis\"," +
                "\"cisFull\":\"cisFull\"," +
                "\"UIT\":\"UIT\"," +
                "\"imei\":\"imei\"," +
                "\"sn\":\"sn\"" +
                "}]");

        OrderItemInstance instance = new OrderItemInstance();
        instance.setBalanceOrderId("balanceOrderId");
        instance.setCis("cis");
        instance.setCisFull("cisFull");
        instance.setUit("UIT");
        instance.setImei("imei");
        instance.setSn("sn");

        List<OrderItemInstance> instances = OrderItemInstancesUtil.parseInstances(jsonNode);

        assertThat(instances, equalTo(List.of(instance)));
    }

}
