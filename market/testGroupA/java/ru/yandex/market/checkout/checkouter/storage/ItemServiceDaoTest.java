package ru.yandex.market.checkout.checkouter.storage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.storage.itemservice.ItemServiceDao;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemServiceDaoTest extends AbstractWebTestBase {

    @Autowired
    private ItemServiceDao itemServiceDao;
    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void testInsertItemService() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order savedOrder = orderCreateHelper.createOrder(parameters);

        final ItemService itemService = buildItemService();
        itemService.setOrderId(savedOrder.getId());
        transactionTemplate.execute(ts -> {
            itemServiceDao.insert(savedOrder.getId(), singletonMap(savedOrder.getItems().iterator().next().getId(),
                    singletonList(itemService)));
            return null;
        });

        List<ItemService> actual = itemServiceDao.findByIds(singletonList(itemService.getId()));
        assertThat(actual, hasSize(1));

        ItemService actualEntity = actual.get(0);
        checkItemServiceEntity(itemService, actualEntity);
    }

    @Test
    public void testLoadItemServices() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order savedOrder = orderCreateHelper.createOrder(parameters);

        // добавляем услугу отдельно от заказа
        final ItemService itemService = buildItemService();
        itemService.setOrderId(savedOrder.getId());
        transactionTemplate.execute(ts -> {
            itemServiceDao.insert(savedOrder.getId(), singletonMap(savedOrder.getItems().iterator().next().getId(),
                    singletonList(itemService)));
            return null;
        });

        //проверяем, что у OrderItem'a нет услуг и подгружаем услугу из БД
        OrderItem item = savedOrder.getItems().iterator().next();
        assertThat(item.getServices(), Matchers.empty());

        itemServiceDao.loadToOrderItems(singletonList(item));
        assertThat(item.getServices(), hasSize(1));
        checkItemService(itemService, item.getServices().iterator().next());
    }

    @Test
    public void testLoadAllItemServicesToOrders() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order savedOrder = orderCreateHelper.createOrder(parameters);

        // добавляем услугу отдельно от заказа
        final ItemService itemServiceEntity = buildItemService();
        itemServiceEntity.setOrderId(savedOrder.getId());
        transactionTemplate.execute(ts -> {
            itemServiceDao.insert(savedOrder.getId(), singletonMap(savedOrder.getItems().iterator().next().getId(),
                    singletonList(itemServiceEntity)));
            return null;
        });

        //проверяем, что у OrderItem'a нет услуг и подгружаем услугу из БД
        OrderItem item = savedOrder.getItems().iterator().next();
        assertThat(item.getServices(), Matchers.empty());

        itemServiceDao.loadToOrders(singletonList(savedOrder));
        assertThat(item.getServices(), hasSize(1));
        checkItemService(itemServiceEntity, item.getServices().iterator().next());
    }

    private void checkItemService(ItemService expected, ItemService actual) {
        assertEquals(expected.getServiceId(), actual.getServiceId());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getYaServiceId(), actual.getYaServiceId());
        assertEquals(expected.getPaymentType(), actual.getPaymentType());
        assertEquals(expected.getPrice().compareTo(actual.getPrice()), 0);
        assertEquals(expected.getPartnerId(), actual.getPartnerId());
    }

    private void checkItemServiceEntity(ItemService expected, ItemService actual) {
        assertEquals(expected.getOrderId(), actual.getOrderId());
        assertEquals(expected.getServiceId(), actual.getServiceId());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getYaServiceId(), actual.getYaServiceId());
        assertEquals(expected.getPaymentType(), actual.getPaymentType());
        assertEquals(expected.getPrice().compareTo(actual.getPrice()), 0);
        assertEquals(expected.getPartnerId(), actual.getPartnerId());
    }

    private ItemService buildItemService() {
        ItemService itemService = new ItemService();
        itemService.setServiceId(10L);
        itemService.setTitle("test_service_title");
        itemService.setDescription("test_service_description");
        itemService.setDate(Date.from(LocalDateTime.now().plusDays(2).atZone(getClock().getZone()).toInstant()));
        itemService.setStatus(ItemServiceStatus.NEW);
        itemService.setPrice(BigDecimal.valueOf(150L));
        itemService.setYaServiceId("321123321");
        itemService.setPaymentType(PaymentType.POSTPAID);
        itemService.setCount(1);
        itemService.setPartnerId("somePartnerId");
        return itemService;
    }

}
