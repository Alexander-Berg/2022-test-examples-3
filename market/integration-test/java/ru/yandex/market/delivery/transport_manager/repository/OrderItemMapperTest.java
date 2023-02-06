package ru.yandex.market.delivery.transport_manager.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderItem;
import ru.yandex.market.delivery.transport_manager.domain.enums.CargoType;
import ru.yandex.market.delivery.transport_manager.domain.enums.VatType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.OrderItemMapper;

public class OrderItemMapperTest extends AbstractContextualTest {

    private OrderItem orderItem;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @BeforeEach
    @SneakyThrows
    public void setup() {
        orderItem = new OrderItem()
            .setOrderId(1L)
            .setName("Пластиковые пакетики")
            .setArticle("HL362")
            .setVendorId(30001L)
            .setBoxCount(2)
            .setCount(5)
            .setCategoryName("Упаковка")
            .setWidth(100)
            .setHeight(200)
            .setLength(300)
            .setWeightGross(new BigDecimal("2.3500"))
            .setPrice(new BigDecimal("147.55"))
            .setRemovableIfAbsent(false)
            .setVatType(VatType.NO_VAT)
            .setCargoTypes(List.of(CargoType.CHEMICALS, CargoType.HOUSEHOLD_CHEMICALS))
            .setInstances(List.of(Map.of("cis", "12345")));
    }

    @Test
    @DatabaseSetup("/repository/order/order.xml")
    @ExpectedDatabase(
        value = "/repository/order_item/order_item.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void persist() {
        orderItemMapper.persist(orderItem);
    }

    @Test
    @DatabaseSetup("/repository/order/order.xml")
    @DatabaseSetup("/repository/order_item/order_item.xml")
    void getById() {
        OrderItem foundOrderItem = orderItemMapper.getById(1L);
        assertThatModelEquals(orderItem, foundOrderItem);
    }

}
