package ru.yandex.market.fulfillment.wrap.marschroute.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fulfillment.wrap.marschroute.entity.OrderInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.functional.configuration.RepositoryTest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.DeliveryId;

import java.util.Optional;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class OrderInfoRepositoryTest extends RepositoryTest {

    @Autowired
    private OrderInfoRepository repository;

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_or_update_existing_result.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertOrUpdateExisting() {
        repository.insertOrUpdate("1", "ORDER456", 21);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_or_update_new_result.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertOrUpdateNew() {
        repository.insertOrUpdate("1", "NEWORDER", 21);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_if_not_exists_existing.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertIfNotExistsExisting() {
        repository.insertIfNotExists("1", "ORDER456", 21);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_if_not_exists_new.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertIfNotExistsNew() {
        repository.insertIfNotExists("1", "NEWORDER", 21);
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_or_update_existing_result.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertOrUpdateExistingObject() {
        repository.insertOrUpdate(new OrderInfo("1", "ORDER456", DeliveryId.POST_ONLINE_RF));
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_or_update_new_result.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertOrUpdateNewObject() {
        repository.insertOrUpdate(new OrderInfo("1", "NEWORDER", DeliveryId.POST_ONLINE_RF));
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_if_not_exists_existing.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertIfNotExistsExistingObject() {
        repository.insertIfNotExists(new OrderInfo("1", "ORDER456", DeliveryId.POST_ONLINE_RF));
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    @ExpectedDatabase(value = "classpath:repository/order_info/insert_if_not_exists_new.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void insertIfNotExistsNewObject() {
        repository.insertIfNotExists(new OrderInfo("1", "NEWORDER", DeliveryId.POST_ONLINE_RF));
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    void findByYandexIdWithExistingObject() {
        String yandexId = "2";
        Optional<OrderInfo> orderInfo = repository.findByYandexId(yandexId);

        softly.assertThat(orderInfo).isPresent();

        orderInfo.ifPresent(info -> {
            softly.assertThat(info.getYandexId()).isEqualTo(yandexId);
            softly.assertThat(info.getDeliveryId()).isEqualTo(1);
            softly.assertThat(info.getOrderId()).isEqualTo("ORDER456");
        });
    }

    @Test
    @DatabaseSetup(value = "classpath:repository/order_info/setup.xml")
    void findByYandexIdWithMissingObject() {
        Optional<OrderInfo> orderInfo = repository.findByYandexId("4");

        softly.assertThat(orderInfo).isEmpty();
    }
}
