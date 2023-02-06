package ru.yandex.market.checkout.checkouter.storage.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.hamcrest.Matchers;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryUpdateValidationResult;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.delivery.DeliveryUpdateActions;
import ru.yandex.market.checkout.checkouter.returns.DeliveryCompensationType;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnHistory;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnItemType;
import ru.yandex.market.checkout.checkouter.returns.ReturnNotFoundException;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.checkouter.storage.DeliveryWritingDao;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.returns.ReturnReasonType.DAMAGE_DELIVERY;

/**
 * @author sergeykoles
 * Created on: 16.02.18
 */
public class ReturnsDaoTest extends AbstractWebTestBase {

    private static final String OFFER_ANOTHER = "2";
    @Autowired
    private ReturnsDao returnsDao;
    @Autowired
    private ReturnHistoryDao returnHistoryDao;
    @Autowired
    private ReturnItemDao returnItemDao;
    @Autowired
    private ReturnDeliveryDao returnDeliveryDao;
    @Autowired
    private DeliveryWritingDao deliveryWritingDao;

    /**
     * Заказ с двумя айтемами
     */
    private Order order;
    /**
     * два айтема заказа
     */
    private OrderItem[] items;

    @Test
    public void insertAndReadReturn() {
        Return re = createReturnInDb(order);
        assertThat(re.getId(), allOf(notNullValue(), not(equalTo(0L))));
        Optional<Return> readRe = returnsDao.findReturnById(re.getId());
        assertTrue(readRe.isPresent(), "Return not found");
        assertThat(readRe.get(), equalTo(re));
        transactionTemplate.execute(
                txStatus -> {
                    returnsDao.updateReturnStatus(
                            re.getId(),
                            ReturnStatus.REFUNDED,
                            LocalDateTime.now(getClock()),
                            null,
                            "test"
                    );
                    return null;
                }
        );
    }

    @Test
    public void insertAndReadReturnWithHistory() {
        final Return re = createReturnInDb(order);
        assertThat(re.getId(), allOf(notNullValue(), not(equalTo(0L))));
        Optional<Return> readRe = returnsDao.findReturnById(re.getId());
        assertTrue(readRe.isPresent(), "Return not found");

        ReturnHistory entity = createReturnHistory(readRe.get());
        createReturnHistoryInDb(entity, 1L);

        List<ReturnHistory> histories = returnHistoryDao.getReturnHistoryByReturnId(readRe.get().getId());
        assertThat(histories.size(), equalTo(1));

        ReturnHistory entity2 = createReturnHistory(readRe.get());
        createReturnHistoryInDb(entity2, 2L);

        histories = returnHistoryDao.getReturnHistoryByReturnId(readRe.get().getId());
        assertThat(histories.size(), equalTo(2));

        assertThat(histories.get(1).getPrevReturn(), notNullValue());
    }

    @Nonnull
    private ReturnHistory createReturnHistory(Return readRe) {
        Instant now = Instant.now();
        ReturnHistory entity = new ReturnHistory();
        entity.setId(readRe.getId());
        entity.setOrderId(readRe.getOrderId());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setStatusUpdatedAt(now);
        entity.setStatus(readRe.getStatus());
        entity.setUserCompensationSum(BigDecimal.ZERO);
        return entity;
    }

    @Test
    public void insertAndReadReturnWithItem() {
        assertThat(order.getItems(), hasSize(greaterThan(0)));
        Return re = createReturnInDb(order);
        List<ReturnItem> readItems = returnItemDao.findReturnItemsByReturnId(re.getId());

        assertThat(readItems, hasSize(order.getItems().size()));
        readItems.forEach(returnItem -> assertNotNull(order.getItem(returnItem.getItemId())));
    }

    @Test
    public void updateReturnWithItem() {
        Return re = createReturnInDb(order);
        long initialReturnItemid = returnItemDao.findReturnItemsByReturnId(re.getId())
                .stream().findFirst().orElseThrow().getId();

        //первая позиция (оператор АБО поменял причину возврата)
        ReturnItem itemFromAbo = ReturnProvider.generateReturnItem(re, items[0]);
        itemFromAbo.setId(initialReturnItemid);
        itemFromAbo.setReasonType(DAMAGE_DELIVERY);
        assertEquals(itemFromAbo.getItemId(), itemFromAbo.getItemId());
        //Оператор АБО добавил компенсацию за доставку
        ReturnItem deliveryItemFromAbo = ReturnItem.initReturnOrderDeliveryItem(order.getInternalDeliveryId());

        Return updated = returnsDao.findReturnById(re.getId()).orElseThrow();
        transactionTemplate.execute((txStatus) -> {
            returnItemDao.updateReturnItemsOrInsertNew(updated, List.of(itemFromAbo, deliveryItemFromAbo));
            return null;
        });
        var returnItemsInDb = returnItemDao.findReturnItemsByReturnId(re.getId());
        assertEquals(3, returnItemsInDb.size());
        ReturnItem updatedItem = returnItemsInDb.stream()
                .filter(r -> r.getId().equals(initialReturnItemid))
                .findFirst().orElseThrow();
        assertEquals(DAMAGE_DELIVERY, updatedItem.getReasonType());
        assertTrue(returnItemsInDb.stream().anyMatch(item -> item.getType() == ReturnItemType.ORDER_DELIVERY));
    }

    @Test
    public void updateReturnWithItemDuplicateDeliveryBugFix() {
        Return returnTemplate = ReturnProvider.generateReturn(order);
        ReturnItem returnItem = ReturnProvider.generateReturnItem(returnTemplate, items[0]);
        returnItem.setPicturesUrls(null);
        ReturnItem deliveryItem = ReturnItem.initReturnOrderDeliveryItem(order.getInternalDeliveryId());
        returnTemplate.setItems(List.of(returnItem, deliveryItem));
        Return ret = createReturnInDb(returnTemplate);
        List<ReturnItem> insertedReturnItems = ret.getItems();

        ReturnItem oldItemFromDb = insertedReturnItems.stream()
                .filter(item -> item.getType() == ReturnItemType.ORDER_ITEM)
                .findAny()
                .orElseThrow();
        //Меняем что угодно в айтеме + передаем строку доставки без указания айдишника.
        oldItemFromDb.setReasonType(ReturnReasonType.DO_NOT_FIT);
        assertEquals(2, insertedReturnItems.size());
        transactionTemplate.execute((txStatus) -> {
            returnItemDao.updateReturnItemsOrInsertNew(
                    ret, List.of(oldItemFromDb, ReturnItem.initReturnOrderDeliveryItem(order.getInternalDeliveryId())));
            return null;
        });
        List<ReturnItem> retItemsAfterUpdate = returnItemDao.findReturnItemsByReturn(ret);
        assertEquals(2, retItemsAfterUpdate.size());
    }

    @Test
    public void returnItemConstraintTest() {
        Return ret = createReturnInDb(order);
        ReturnItem returnItem = ReturnProvider.generateReturnItem(ret, items[0]);
        ReturnItem deliveryItem = ReturnProvider.generateReturnItem(ret, items[0]);
        deliveryItem.setDeliveryService(true);
        ret.setItems(List.of(returnItem, deliveryItem));
        ret.getItems().forEach(item -> {
            item.setDeliveryServiceId(null);
            item.setItemId(null);
        });
        Assertions.assertThrows(DataAccessException.class, () -> transactionTemplate.execute((txStatus) -> {
            returnItemDao.insertReturnItems(ret);
            return null;
        }));
    }

    @Test
    public void insertAndReadMultipleItems() {
        Pair<Return, List<ReturnItem>> retWithItems = createRetWithRetItems(order);

        Map<Long, ReturnItem> readItemsById = returnItemDao.findReturnItemsByReturn(retWithItems.getFirst()).stream()
                .collect(Collectors.toMap(ReturnItem::getId, Function.identity()));

        // for each item
        retWithItems.getSecond().forEach(i -> {
            assertThat(i.getId(), notNullValue());
            ReturnItem readItem = readItemsById.get(i.getId());
            assertThat(readItem, notNullValue());
            assertThat(readItem, equalTo(i));
        });
    }

    @Test
    public void insertCompensationEnum() {
        Return returnToSave = ReturnProvider.generateReturn(order);
        returnToSave.setDeliveryCompensationType(DeliveryCompensationType.YANDEX_PLUS);
        Return re = createReturnInDb(returnToSave);
        assertThat(re.getId(), allOf(notNullValue(), not(equalTo(0L))));
        Optional<Return> readRe = returnsDao.findReturnById(re.getId());
        assertTrue(readRe.isPresent(), "Return not found");
        assertThat(readRe.get(), equalTo(re));
        assertThat(readRe.get().getDeliveryCompensationType(), equalTo(DeliveryCompensationType.YANDEX_PLUS));
        transactionTemplate.execute(
                txStatus -> {
                    returnsDao.updateReturnStatus(
                            re.getId(),
                            ReturnStatus.REFUNDED,
                            LocalDateTime.now(getClock()),
                            null,
                            "test"
                    );
                    return null;
                }
        );
    }

    private Pair<Return, List<ReturnItem>> createRetWithRetItems(Order order) {
        Return re = createReturnInDb(order);
        List<ReturnItem> returnItems = order.getItems().stream()
                .map(i -> ReturnProvider.generateReturnItem(re, i))
                .collect(Collectors.toList());
        transactionTemplate.execute((txStatus) -> {
            Return ret = new Return();
            ret.setId(re.getId());
            ret.setOrderId(order.getId());
            ret.setItems(returnItems);
            returnItemDao.insertReturnItems(ret);
            return null;
        });

        return new Pair<>(returnsDao.findReturnById(
                re.getId()).orElseThrow(() -> new ReturnNotFoundException(re.getId())),
                returnItemDao.findReturnItemsByReturn(re)
        );
    }

    @Test
    public void findReturnsAndItemsByOrderId() {
        // возврат на общий заказ
        Pair<Return, List<ReturnItem>> retCommon = createRetWithRetItems(order);

        // дополнительный заказ и два возврата на него
        Order additionalOrder = orderCreateHelper.createOrder(new Parameters());
        Pair<Return, List<ReturnItem>> ret1 = createRetWithRetItems(additionalOrder);
        Pair<Return, List<ReturnItem>> ret2 = createRetWithRetItems(additionalOrder);

        List<Return> expectedRets = Arrays.asList(ret1.getFirst(), ret2.getFirst());

        List<ReturnItem> expectedRetItems = new ArrayList<>(ret1.getSecond());
        expectedRetItems.addAll(ret2.getSecond());

        // нужно проверить, что при запросе по второму заказу не прилетают элементы общего,
        // а прилетает ровно 2 возврата и 2 айтема, причём именно этого заказа
        List<Return> returnByOrderId = returnsDao.findReturns(additionalOrder.getId(), null, null);
        assertThat(returnByOrderId, hasSize(expectedRets.size()));
        assertThat(returnByOrderId, Matchers.hasItems(expectedRets.toArray(new Return[0])));

        List<ReturnItem> returnEntityByOrderId = returnItemDao.findReturnItemsByOrderId(additionalOrder.getId(),
                null);
        assertThat(returnEntityByOrderId, Matchers.hasItems(expectedRetItems.toArray(new ReturnItem[0])));
        assertThat(expectedRetItems, Matchers.hasItems(returnEntityByOrderId.toArray(new ReturnItem[0])));
    }

    @Test
    public void returnEntityAndItemPaginationTest() {
        // сделаем штук 10 возвратов
        for (int i = 0; i < 10; i++) {
            createRetWithRetItems(order);
        }

        // теперь надо проверить, что страница 2 отличается от страницы 3
        // при запросе страницами размером 3

        Pager page2 = Pager.atPage(2, 3);
        Pager page3 = Pager.atPage(3, 3);

        List<Return> page2Returns = returnsDao.findReturns(order.getId(), null, page2);
        List<Return> page3Returns = returnsDao.findReturns(order.getId(), null, page3);

        Set<Long> page2ReturnIds = page2Returns.stream()
                .map(Return::getId)
                .collect(Collectors.toSet());
        Set<Long> page3ReturnIds = page3Returns.stream()
                .map(Return::getId)
                .collect(Collectors.toSet());


        // проверяем размеры страниц
        assertThat(page2Returns, hasSize(3));
        assertThat(page3Returns, hasSize(3));

        // проверяем, что элементы не пересекаются
        page3Returns.forEach(i -> assertThat(page2ReturnIds, not(hasItem(i.getId()))));
        page2Returns.forEach(i -> assertThat(page3ReturnIds, not(hasItem(i.getId()))));

        // подгрузим айтемы и проверим, что только для указанных возвратов подгрузились
        List<ReturnItem> page2ReturnItems = returnItemDao.findReturnItemsByOrderId(
                order.getId(),
                page2Returns.stream()
                        .map(Return::getId)
                        .collect(Collectors.toList())
        );

        assertThat(page2ReturnIds, hasItems(
                page2ReturnItems.stream()
                        .map(ReturnItem::getReturnId)
                        .distinct()
                        .toArray(Long[]::new)
        ));

    }

    /**
     * Создаём в базе возврат на наш заказ.
     * Заодно проверяем что возвращаемый ID равен тому, который прописали в новой сущности
     *
     * @param order заказ, к которому прицепить возврат
     * @return новый возврат
     */
    private Return createReturnInDb(Order order) {
        return createReturnInDb(ReturnProvider.generateReturn(order));
    }

    private Return createReturnInDb(Return returnToSave) {
        Long id = transactionTemplate.execute((txStatus) -> {
            long returnId = returnsDao.insertReturn(returnToSave);
            returnItemDao.insertReturnItems(returnToSave);
            if (returnToSave.getDelivery() != null) {
                returnToSave.getDelivery().setReturnId(returnId);
                long returnDeliveryId = returnDeliveryDao.insertReturnDelivery(returnToSave.getDelivery(),
                        returnToSave.getOrderId());
                returnsDao.setReturnDeliveryId(returnToSave.getId(), returnDeliveryId);
            }
            return returnId;
        });
        if (id == null) {
            throw new RuntimeException("Return not saved");
        }
        Return savedRet = returnsDao.findReturnById(id).orElseThrow(() -> new RuntimeException("Return not found"));
        assertThat(savedRet.getId(), equalTo(id));
        return savedRet;
    }

    private void createReturnHistoryInDb(ReturnHistory re, long historyId) {
        transactionTemplate.execute((txStatus) -> returnHistoryDao.insertReturnHistory(re, historyId, null));
    }


    /**
     * Поскольку у нас есть жёсткие ссылки в БД, нам надо заиметь некоторые сущности
     */
    @BeforeEach
    @SuppressWarnings("checkstyle:HiddenField")
    public void setUpSuite() {
        Parameters parameters = new Parameters();
        Collection<OrderItem> items = new ArrayList<>(parameters.getOrder().getItems());
        items.add(OrderItemProvider.buildOrderItem(OFFER_ANOTHER));
        parameters.getOrder().setItems(items);
        order = orderCreateHelper.createOrder(parameters);
        this.items = new ArrayList<>(order.getItems()).toArray(new OrderItem[2]);
        generateOrderDelivery();

    }

    private void generateOrderDelivery() {
        Delivery delivery = DeliveryProvider.getShopDelivery();
        DeliveryUpdateActions actions = new DeliveryUpdateActions();
        actions.setDeliveryUpdateValidationResult(new DeliveryUpdateValidationResult<>(false, Set.of()));
        transactionTemplate.execute(tx -> {
            long deliveryId = deliveryWritingDao.updateOrderDelivery(delivery, order, actions);
            delivery.setDeliveryOptionId(String.valueOf(deliveryId));
            order.setDelivery(delivery);
            order.setInternalDeliveryId(deliveryId);
            return null;
        });
    }
}
