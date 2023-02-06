package ru.yandex.market.checkout.checkouter.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.controllers.oms.OrderItemsController;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.archive.ArchiveStorageFutureFactory;
import ru.yandex.market.checkout.checkouter.order.item.StorageOrderItemService;
import ru.yandex.market.checkout.checkouter.storage.archive.repository.OrderArchivingDao;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.ItemInfo;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Anastasiya Emelianova / orphie@ / 7/5/21
 */
public class OrderItemsControllerTest extends AbstractArchiveWebTestBase {

    @Autowired
    OrderItemsController controller;

    @Autowired
    private OrderArchivingDao orderArchiveDao;

    @Autowired
    private StorageOrderItemService orderItemService;

    @Autowired
    private ArchiveStorageFutureFactory archiveStorageFutureFactory;

    @Test
    void getActualOrdersItemsList() throws Exception {
        Parameters parameters1 = BlueParametersProvider.defaultBlueOrderParameters();
        parameters1.setupFulfillment(new ItemInfo.Fulfilment(FulfilmentProvider.FF_SHOP_ID,
                FulfilmentProvider.OTHER_TEST_SKU, FulfilmentProvider.OTHER_TEST_SHOP_SKU));
        Order order1 = orderCreateHelper.createOrder(parameters1);
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.DELIVERED);

        OrderItem aI = OrderItemProvider.getAnotherWarehouseOrderItem();
        aI.setMsku(FulfilmentProvider.ANOTHER_TEST_MSKU);
        aI.setSku(FulfilmentProvider.ANOTHER_TEST_SKU);
        Parameters parameters2 = BlueParametersProvider.defaultBlueOrderParametersWithItems(aI);
        parameters2.getBuyer().setUid(order1.getBuyer().getUid());
        Order order2 = orderCreateHelper.createOrder(parameters2);
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERED);

        long uid = order1.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, order1.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(uid))
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getActualOrdersItemsWithUid() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setUid(222L);
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        long uid = order.getBuyer().getUid();

        OrderItem oItem = order.getItems().stream().findFirst().get();
        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                        .param(CheckouterClientParams.CLIENT_ID, order.getUserClientInfo().getId().toString())
                        .param(CheckouterClientParams.NO_AUTH, order.isNoAuth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelId").value(oItem.getModelId()))
                .andExpect(jsonPath("$[0].wareMd5").exists())
                .andExpect(jsonPath("$[0].categoryId").value(oItem.getCategoryId()))
                .andExpect(jsonPath("$[0].offerName").value(oItem.getOfferName()))
                .andExpect(jsonPath("$[0].description").value(oItem.getDescription()))
                .andExpect(jsonPath("$", hasSize(1)));

    }

    @Test
    void getActualOrdersItemsWithMuid() throws Exception {
        orderItemService.setArchiveStorageFutureFactory(archiveStorageFutureFactory);
        Parameters parameters1 = BlueParametersProvider.defaultBlueOrderParameters();
        parameters1.getBuyer().setMuid(123L);
        Order order = orderCreateHelper.createOrder(parameters1);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);

        long muid = order.getBuyer().getMuid();
        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", muid)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.CLIENT_ID, order.getUserClientInfo().getId().toString())
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getOrdersItemsWithWrongStatus() throws Exception {
        Parameters parameters2 = BlueParametersProvider.defaultBlueOrderParameters();
        parameters2.getBuyer().setUid(12L);
        Order order = orderCreateHelper.createOrder(parameters2);
        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_NOT_PAID);

        long uid2 = order.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid2)
                        .param(CheckouterClientParams.CLIENT_ROLE, order.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, order.getUserClientInfo().getId().toString())
                        .param(CheckouterClientParams.NO_AUTH, order.isNoAuth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getOrdersItemsWithoutNoAuth() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        long uid = order.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, order.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, order.getUserClientInfo().getId().toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getOrdersItemsWithIncorrectUserId() throws Exception {
        long uid = 5L;

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.USER.name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(uid))
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getTheSameItemInMultipleOrders() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setUid(111L);

        Order order1 = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order1, OrderStatus.DELIVERED);

        Order order2 = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order2, OrderStatus.DELIVERED);
        orderStatusHelper.updateOrderStatus(order2.getId(), OrderStatus.DELIVERED,
                OrderSubstatus.DELIVERED_USER_RECEIVED);

        long uid = order1.getBuyer().getUid();
        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, order1.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, order1.getUserClientInfo().getId().toString())
                        .param(CheckouterClientParams.NO_AUTH, order1.isNoAuth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getOnlyArchivedOrderItemFromActualBase() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getBuyer().setUid(111L);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        archiveOrder(order);

        long uid = order.getBuyer().getUid();
        OrderItem oItem = order.getItems().stream().findFirst().get();

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, order.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, order.getUserClientInfo().getId().toString())
                        .param(CheckouterClientParams.NO_AUTH, order.isNoAuth().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].modelId").value(oItem.getModelId()))
                .andExpect(jsonPath("$[0].wareMd5").exists())
                .andExpect(jsonPath("$[0].categoryId").value(oItem.getCategoryId()))
                .andExpect(jsonPath("$[0].offerName").value(oItem.getOfferName()))
                .andExpect(jsonPath("$[0].description").value(oItem.getDescription()))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getActualAndArchivedOrderItems() throws Exception {
        orderItemService.setArchiveStorageFutureFactory(archiveStorageFutureFactory);
        Parameters parametersArchived = BlueParametersProvider.defaultBlueOrderParameters();
        parametersArchived.setupFulfillment(new ItemInfo.Fulfilment(FulfilmentProvider.FF_SHOP_ID,
                FulfilmentProvider.OTHER_TEST_SKU, FulfilmentProvider.OTHER_TEST_SHOP_SKU));
        parametersArchived.getBuyer().setUid(111L);
        Order archivedOrder = orderCreateHelper.createOrder(parametersArchived);
        orderStatusHelper.proceedOrderToStatus(archivedOrder, OrderStatus.DELIVERED);

        archiveOrder(archivedOrder);
        moveArchivedOrders();

        Parameters parametersActual =
                BlueParametersProvider.defaultBlueOrderParametersWithItems(
                        OrderItemProvider.getAnotherWarehouseOrderItem());
        parametersActual.getBuyer().setUid(111L);
        Order actualOrder = orderCreateHelper.createOrder(parametersActual);
        orderStatusHelper.proceedOrderToStatus(actualOrder, OrderStatus.DELIVERED);

        long uid = archivedOrder.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, archivedOrder.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(uid))
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getItemsWithSameSKU() throws Exception {
        Parameters parametersArchived = BlueParametersProvider.defaultBlueOrderParameters();
        parametersArchived.getBuyer().setUid(111L);
        parametersArchived.setupFulfillment(new ItemInfo.Fulfilment(FulfilmentProvider.FF_SHOP_ID,
                FulfilmentProvider.OTHER_TEST_SKU, FulfilmentProvider.OTHER_TEST_SHOP_SKU));
        Order archivedOrder = orderCreateHelper.createOrder(parametersArchived);
        orderStatusHelper.proceedOrderToStatus(archivedOrder, OrderStatus.DELIVERED);
        archiveOrder(archivedOrder);

        Parameters parametersArchived1 =
                BlueParametersProvider.defaultBlueOrderParametersWithItems(
                        OrderItemProvider.getAnotherWarehouseOrderItem());
        parametersArchived1.getBuyer().setUid(111L);
        parametersArchived1.setupFulfillment(new ItemInfo.Fulfilment(FulfilmentProvider.FF_SHOP_ID,
                FulfilmentProvider.OTHER_TEST_SKU, FulfilmentProvider.OTHER_TEST_SHOP_SKU));
        Order archivedOrder1 = orderCreateHelper.createOrder(parametersArchived1);
        orderStatusHelper.proceedOrderToStatus(archivedOrder1, OrderStatus.DELIVERED);
        archiveOrder(archivedOrder1);

        long uid = archivedOrder.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, archivedOrder.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(uid))
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void getArchivedOrderItemsFromTwoDatabases() throws Exception {
        orderItemService.setArchiveStorageFutureFactory(archiveStorageFutureFactory);
        Parameters parametersArchived = BlueParametersProvider.defaultBlueOrderParameters();
        parametersArchived.getBuyer().setUid(111L);
        parametersArchived.setupFulfillment(new ItemInfo.Fulfilment(FulfilmentProvider.FF_SHOP_ID,
                FulfilmentProvider.OTHER_TEST_SKU, FulfilmentProvider.OTHER_TEST_SHOP_SKU));
        var item = parametersArchived.getItems().iterator().next();
        Order archivedOrder = orderCreateHelper.createOrder(parametersArchived);
        orderStatusHelper.proceedOrderToStatus(archivedOrder, OrderStatus.DELIVERED);
        archiveOrder(archivedOrder);
        moveArchivedOrders();

        Parameters parametersArchived1 =
                BlueParametersProvider.defaultBlueOrderParametersWithItems(
                        OrderItemProvider.getAnotherWarehouseOrderItem());
        parametersArchived1.getBuyer().setUid(111L);
        Order archivedOrder1 = orderCreateHelper.createOrder(parametersArchived1);
        orderStatusHelper.proceedOrderToStatus(archivedOrder1, OrderStatus.DELIVERED);
        archiveOrder(archivedOrder1);

        long uid = archivedOrder1.getBuyer().getUid();

        mockMvc.perform(
                get("/orders/all-items/by-uid/{userId}", uid)
                        .param(CheckouterClientParams.CLIENT_ROLE, archivedOrder.getUserClientInfo().getRole().name())
                        .param(CheckouterClientParams.CLIENT_ID, String.valueOf(uid))
                        .param(CheckouterClientParams.NO_AUTH, String.valueOf(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    protected Order archiveOrder(Order order) {
        transactionTemplate.execute(ts -> {
            order.setArchived(true);
            orderArchiveDao.updateArchived(order);
            return null;
        });
        return order;
    }
}
