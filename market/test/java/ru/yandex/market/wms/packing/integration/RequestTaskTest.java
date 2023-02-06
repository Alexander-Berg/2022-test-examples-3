package ru.yandex.market.wms.packing.integration;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.MockTaskConsumer;
import ru.yandex.market.wms.packing.enums.ItemStatus;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;

public class RequestTaskTest extends PackingIntegrationTest {

    private static final String USER = "TESTUSER";

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/ok.xml")
    void ok() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        assertions.assertThat(consumer.getTask()).isNotNull();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/ok_adjusted.xml")
    void okAdjusted() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        assertions.assertThat(consumer.getTask()).isNotNull();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/ok_not_adjusted.xml")
    void okNotAdjusted() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        assertions.assertThat(consumer.getTask()).isNotNull();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/failed_no_pickdetail.xml")
    void failedNoPickdetail() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        assertions.assertThat(consumer.getTask()).isNull();
        assertions.assertThat(consumer.getTaskAssignmentError()).isNotBlank();
    }

    /*
       Заказ с 2 деталями, 1 отсортирована, другая пока еще нет
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/partial_sorted.xml")
    void partialSortedTest() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        assertions.assertThat(consumer.getTask()).isNull();
    }

    /*
       Заказ с 2 деталями, 1 отсортирована, другая NONSORT
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/exclude_nonsort_pickdetails.xml")
    void excludeNonSortTest() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        assertions.assertThat(consumer.getTask()).isNotNull();
    }

    /*
        Заказ с 2 деталями, 1 отсортирована, у другой нет PickDetail
     */
    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/removed_one_pickdetail.xml")
    void excludeNonSortNotPickedTest() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        assertions.assertThat(consumer.getTask()).isNull();
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/nonsort_cancelled_order.xml")
    void cancelledFullOrderTest() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.NONSORT_TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        var task = consumer.getTask();
        assertions.assertThat(task).isNotNull();
        var items = task.getOrderTasks().get(0).getItems();
        assertions.assertThat(items.size()).isEqualTo(2);
        for (var item : items) {
            assertions.assertThat(item.getItemStatus()).isEqualTo(ItemStatus.CANCELLED);
        }
    }

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    @DatabaseSetup(value = "/db/base_setup.xml", type = INSERT)
    @DatabaseSetup(value = "/db/integration/request_task/nonsort_cancelled_order_detail.xml")
    void cancelledOrderDetailTest() {
        MockTaskConsumer consumer = new MockTaskConsumer(LocationsRov.NONSORT_TABLE_1, USER);
        manager.register(consumer);
        manager.requestTask(USER, false);
        var task = consumer.getTask();
        assertions.assertThat(task).isNotNull();
        var items = task.getOrderTasks().get(0).getItems();
        assertions.assertThat(items.size()).isEqualTo(2);

        var sku1item = items.stream().filter(a -> a.getSku().getSku().equals("SKU101")).findFirst().get();
        var sku2item = items.stream().filter(a -> a.getSku().getSku().equals("SKU102")).findFirst().get();
        assertions.assertThat(sku1item.getItemStatus()).isEqualTo(ItemStatus.OK);
        assertions.assertThat(sku2item.getItemStatus()).isEqualTo(ItemStatus.CANCELLED);
    }
}
