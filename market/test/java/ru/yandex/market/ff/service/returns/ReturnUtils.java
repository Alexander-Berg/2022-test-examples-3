package ru.yandex.market.ff.service.returns;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.returns.ReturnItemDto;
import ru.yandex.market.ff.model.returns.ReturnUnitComplexKey;
import ru.yandex.market.ff.util.FileContentUtils;

public class ReturnUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private SoftAssertions assertions;


    @NotNull
    public static Order getOrder() throws Exception {
        OrderItem orderItem = new OrderItem();
        orderItem.setOfferItemKey(new OfferItemKey("0", 0L, "0"));
        orderItem.setOrderId(3960222L);
        orderItem.setId(5271467L);
        orderItem.setShopSku("10124");
        orderItem.setSupplierId(48000L);
        orderItem.setCount(1);
        HashSet<Integer> objects = new HashSet<>();
        objects.add(600);
        orderItem.setCargoTypes(objects);
        orderItem.setInstances(getArrayNode());

        OrderItem orderItem2 = new OrderItem();
        orderItem2.setOfferItemKey(new OfferItemKey("1", 1L, "1"));
        orderItem2.setOrderId(3960222L);
        orderItem2.setId(52714672L);
        orderItem2.setShopSku("10125");
        orderItem2.setSupplierId(48000L);
        orderItem2.setCount(1);
        orderItem2.setCargoTypes(objects);

        Order order = new Order();
        order.setId(3960222L);
        order.setItems(Arrays.asList(orderItem, orderItem2));
        return order;
    }

    private static ArrayNode getArrayNode() throws Exception {
        String json =
                FileContentUtils.getFileContent("service/checkouter-order-service/order_item_instances.json");
        return (ArrayNode) MAPPER.readTree(json);
    }

    @NotNull
    public static Return getReturn() {
        ReturnItem returnItem = new ReturnItem();
        returnItem.setItemId(5271467L);
        returnItem.setCount(1);
        returnItem.setReturnReason("broken");
        returnItem.setReasonType(ReturnReasonType.BAD_QUALITY);

        ReturnItem returnItem2 = new ReturnItem();
        returnItem2.setItemId(52714672L);
        returnItem2.setCount(1);
        returnItem2.setReasonType(ReturnReasonType.BAD_QUALITY);

        Return orderReturn = new Return();
        orderReturn.setId(1232131L);
        orderReturn.setOrderId(3960222L);
        orderReturn.setItems(Arrays.asList(returnItem, returnItem2));
        return orderReturn;
    }

    private void comparison(ReturnItemDto expected, List<ReturnItemDto> actual) {
        actual.forEach(item -> {
            assertions.assertThat(expected).isEqualTo(item);

            if (expected.getInstances() != null) {
                assertions.assertThat(expected.getInstances()).isEqualTo(item.getInstances());
            }
        });
    }

    private ReturnItemDto itemReturnResult() {

        ReturnItemDto returnItemDto = new ReturnItemDto(
                List.of(),
                ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                null,
                1);

        return returnItemDto;
    }

    private ReturnItemDto itemReturnWithCISResult() {
        RegistryUnitId registryUnitId = RegistryUnitId.of(
                RegistryUnitIdType.CIS,
                "010964018661011021mbg:zCaRlU%c08-cis1",
                RegistryUnitIdType.SERIAL_NUMBER,
                "32397437-item1-9324312-1");

        ReturnItemDto returnItemDto = new ReturnItemDto(
                List.of(registryUnitId),
                ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                "broken",
                1);

        return returnItemDto;
    }

    private ReturnUnitComplexKey createComplexKeyForOrderWithCIS() {
        return ReturnUnitComplexKey.of(
                "1232131",
                null,
                48000L,
                "10124",
                "3960222"
        );
    }

    private ReturnUnitComplexKey createComplexKey() {
        return ReturnUnitComplexKey.of(
                "1232131",
                null,
                48000L,
                "10125",
                "3960222"
        );
    }
}
