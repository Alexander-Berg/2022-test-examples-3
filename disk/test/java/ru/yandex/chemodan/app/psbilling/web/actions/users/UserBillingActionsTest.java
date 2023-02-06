package ru.yandex.chemodan.app.psbilling.web.actions.users;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingOrdersFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderType;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.texts.TankerTranslation;
import ru.yandex.chemodan.app.psbilling.web.BaseWebTest;
import ru.yandex.chemodan.app.psbilling.web.model.OrderTypeApi;
import ru.yandex.chemodan.app.psbilling.web.model.ServiceStatusFilter;
import ru.yandex.chemodan.app.psbilling.web.model.UserServicePojo;
import ru.yandex.chemodan.app.psbilling.web.model.UserServicesPojo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.PassportUidOrZero;

public class UserBillingActionsTest extends BaseWebTest {
    @Autowired
    private UserBillingActions actions;
    @Autowired
    private PsBillingUsersFactory usersFactory;

    @Autowired
    PsBillingOrdersFactory ordersFactory;
    @Autowired
    PsBillingProductsFactory productsFactory;
    @Autowired
    UserBillingActionsHelper userBillingActionsHelper;
    private PassportUid uid = PsBillingUsersFactory.UID;


    @Test
    public void findDisabledWithEmpty() {
        usersFactory.createUserServiceWithOrder(Target.DISABLED);
        testImpl(Option.empty(), Cf.list());
    }

    @Test
    public void findEnabledWithEmpty() {
        UUID userServiceId = usersFactory.createUserServiceWithOrder(Target.ENABLED).getId();
        testImpl(Option.empty(), Cf.arrayList(userServiceId));
    }

    @Test
    public void findDisabledWithDisabled() {
        UUID userServiceId = usersFactory.createUserServiceWithOrder(Target.DISABLED).getId();
        testImpl(Option.of(ServiceStatusFilter.DISABLED.value()), Cf.arrayList(userServiceId));
    }

    @Test
    public void findHoldWithHold() {
        UserServiceEntity userService = usersFactory.createUserServiceWithOrder(Target.DISABLED);
        orderDao.holdOrder(userService.getLastPaymentOrderId().get());
        testImpl(Option.of(ServiceStatusFilter.ON_HOLD.value()), Cf.arrayList(userService.getId()));
    }

    @Test
    public void findHoldWithEnabled() {
        UserServiceEntity userService = usersFactory.createUserServiceWithOrder(Target.DISABLED);
        orderDao.holdOrder(userService.getLastPaymentOrderId().get());
        testImpl(Option.of(ServiceStatusFilter.ENABLED.value()), Cf.arrayList());
    }

    @Test
    public void findHoldWithDisabled() {
        UserServiceEntity userService = usersFactory.createUserServiceWithOrder(Target.DISABLED);
        orderDao.holdOrder(userService.getLastPaymentOrderId().get());
        testImpl(Option.of(ServiceStatusFilter.DISABLED.value()), Cf.arrayList(userService.getId()));
    }

    @Test
    public void onlyOneServicePerOrder() {
        UserServiceEntity userService1 = usersFactory.createUserServiceWithOrder(Target.DISABLED);
        UserServiceEntity userService2 = usersFactory.createUserService(Target.DISABLED,
                orderDao.findByIdO(userService1.getLastPaymentOrderId().get()).get());

        testImpl(Option.of(ServiceStatusFilter.DISABLED.value()), Cf.arrayList(userService1.getId()));
    }

    @Test
    public void returnEnabledServiceWithoutOrder() {
        UserServiceEntity userService = usersFactory.createUserService(Target.ENABLED);
        testImpl(Option.of(ServiceStatusFilter.ENABLED.value()), Cf.arrayList(userService.getId()));
    }
    @Test
    public void doNotReturnDisabledServiceWithoutOrder() {
        usersFactory.createUserService(Target.DISABLED);
        testImpl(Option.of(ServiceStatusFilter.DISABLED.value()), Cf.arrayList());
    }

    public void testImpl(Option<String> statusString, List<UUID> exptected) {

        UserServicesPojo userServices = actions.getUserServices(PassportUidOrZero.fromUid(uid),
                Option.empty(), "", statusString);
        List<String> actual = userServices.getItems().stream()
                .map(UserServicePojo::getServiceId)
                .sorted()
                .collect(Collectors.toList());
        Collections.sort(exptected);
        Assert.assertEquals(exptected.stream().map(UUID::toString).collect(Collectors.toList()), actual);
    }

    @Test
    public void testOrderTypeOfDifferentOrders(){
        UserProductPriceEntity userProductPrice = productsFactory.createUserProductPrice();

        Order promocodeOrder = ordersFactory.createOrUpdateOrder(uid, userProductPrice.getId(), UUID.randomUUID().toString(), x -> x.type(OrderType.PROMOCODE_ORDER));
        UserServiceEntity promocodeService = usersFactory.createUserService(Target.ENABLED, promocodeOrder);
        orderDao.onSuccessfulOrderPurchase(promocodeOrder.getId(), Option.of(promocodeService.getId()), 1); //update service in order

        Order paidOrder = ordersFactory.createOrUpdateOrder(uid, userProductPrice.getId(), UUID.randomUUID().toString(), x -> x.type(OrderType.SUBSCRIPTION));
        UserServiceEntity paidService = usersFactory.createUserService(Target.ENABLED, paidOrder);
        orderDao.onSuccessfulOrderPurchase(paidOrder.getId(), Option.of(paidService.getId()), 1);

        Order orderNoService = ordersFactory.createOrUpdateOrder(uid, userProductPrice.getId(), UUID.randomUUID().toString(), x -> x.type(OrderType.PROMOCODE_ORDER));
        UserServiceEntity userServiceOrderNoService = usersFactory.createUserService(Target.ENABLED, orderNoService);

        UserProductPrice upp = userProductManager.findPrice(userProductPrice.getId());
        UserServiceEntity noOrderService = usersFactory.createUserService(upp.getPeriod().getUserProductId(), x->x.uid(uid.toString()), Target.ENABLED);

        Order wrongServiceOrder = ordersFactory.createOrUpdateOrder(uid, userProductPrice.getId(), UUID.randomUUID().toString(), x -> x.type(OrderType.PROMOCODE_ORDER));
        UserServiceEntity wrongServiceService = usersFactory.createUserService(Target.ENABLED, wrongServiceOrder);
        UserServiceEntity wrongServiceWrongService = usersFactory.createUserService(Target.ENABLED, wrongServiceOrder);
        orderDao.onSuccessfulOrderPurchase(wrongServiceOrder.getId(), Option.of(wrongServiceService.getId()), 1); //update service in order
        orderDao.onSuccessfulOrderPurchase(wrongServiceOrder.getId(), Option.of(wrongServiceWrongService.getId()), 1); //update service in order


        UserServicesPojo result = userBillingActionsHelper.getUserServices(
                PassportUidOrZero.fromUid(uid.getUid()),
                Option.empty(),
                TankerTranslation.DEFAULT_LANGUAGE,
                Cf.list()
        );

        assertOrderType(result, promocodeService.getId(), OrderTypeApi.PROMO_CODE);
        assertOrderType(result, paidService.getId(), OrderTypeApi.UNSPECIFIED);
        assertOrderType(result, noOrderService.getId(), OrderTypeApi.UNSPECIFIED);
        //has order with no service -> won't fetch the service
        Assert.assertEquals(Option.empty(), serviceById(result, userServiceOrderNoService.getId()));
        //has order with wrong service -> won't fetch the service
        Assert.assertEquals(Option.empty(), serviceById(result, wrongServiceService.getId()));
    }

    private Option<UserServicePojo> serviceById(UserServicesPojo services, UUID id){
        for(UserServicePojo service: services.getItems()) {
            if(!id.toString().equals(service.getServiceId())){
                continue;
            }
            return Option.of(service);
        }
        return Option.empty();
    }

    private void assertOrderType(UserServicesPojo services, UUID id, OrderTypeApi expectedType) {
        Assert.assertEquals(expectedType, serviceById(services, id).map(UserServicePojo::getOrderType).getOrNull());
    }
}
