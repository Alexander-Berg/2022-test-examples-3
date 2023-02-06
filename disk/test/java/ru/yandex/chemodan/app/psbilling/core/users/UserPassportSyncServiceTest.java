package ru.yandex.chemodan.app.psbilling.core.users;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.yt.YtSelectService;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class UserPassportSyncServiceTest extends AbstractPsBillingCoreTest {
    @Autowired
    private UserPassportSyncService userPassportSyncService;
    @Autowired
    private UserServiceManager userServiceManager;
    @Autowired
    private YtSelectService ytSelectService;

    private Tuple2<UserProductEntity, UserProductPriceEntity> userProduct1;
    private Tuple2<UserProductEntity, UserProductPriceEntity> userProduct2;
    private Tuple2<UserProductEntity, UserProductPriceEntity> inappUserProduct;

    @Test
    public void syncDeletedInPassportUsers() {
        // Пользователь с одной активной услугой
        PassportUid uid1 = PassportUid.cons(111);
        createUserService(userProduct1, uid1, true);

        // Пользователь с одной неактивной услугой
        PassportUid uid2 = PassportUid.cons(112);
        createUserService(userProduct1, uid2, true);
        createUserService(userProduct2, uid2, false);

        // Пользователь с двумя неактивными услугами
        PassportUid uid3 = PassportUid.cons(113);
        createUserService(userProduct1, uid3, false);
        createUserService(userProduct2, uid3, false);

        // Пользователь которого не будет в выгрузке
        PassportUid uid4 = PassportUid.cons(114);
        UserServiceEntity userService4 = createUserService(userProduct1, uid4, true);

        PassportUid uid5 = PassportUid.cons(115);
        createUserService(inappUserProduct, uid5, false);

        // Пользователя не существует у нас
        PassportUid uid6 = PassportUid.cons(116);

        Mockito.when(ytSelectService.getDeletedInPassportUids(Mockito.any())).thenReturn(Cf.list(uid1, uid2, uid3, uid5, uid6));

        userPassportSyncService.syncDeletedInPassportUsers();
        bazingaTaskManagerStub.executeTasks();

        checkUserService(uid1);
        checkUserService(uid2);
        checkUserService(uid3);
        checkUserService(uid5);

        Assert.sizeIs(1, userServiceManager.findEnabled(uid4.toString(), Option.empty()).filter(UserService::getAutoProlongEnabled));
        Assert.equals(userService4.getId(), userServiceManager.findEnabled(uid4.toString(), Option.empty()).first().getId());
    }

    private void checkUserService(PassportUid uid) {
        ListF<UserService> userServices = userServiceManager.find(uid.toString(), Option.empty());
        for (UserService userService: userServices) {
            Assert.isFalse(userService.getAutoProlongEnabled());
            Assert.equals(userService.getTarget(), Target.DISABLED);
        }
    }

    public UserServiceEntity createUserService(
            Tuple2<UserProductEntity, UserProductPriceEntity> userProduct,
            PassportUid uid,
            boolean autoProlong
    ) {
        UserServiceEntity userService = psBillingUsersFactory.createUserService(
                userProduct.get1().getId(),
                x -> x.uid(uid.toString()).autoProlongEnabled(Option.of(autoProlong))
        );

        Order order = psBillingOrdersFactory.createOrUpdateOrder(
                uid,
                userProduct.get2().getId(),
                UUID.randomUUID().toString(),
                o -> o.status(Option.of(OrderStatus.PAID))
        );
        orderDao.onSuccessfulOrderPurchase(order.getId(), Option.of(userService.getId()), 1);

        return userService;
    }

    @Before
    public void setup() {
        UserProductEntity userProduct1 = psBillingProductsFactory.createUserProduct();
        UserProductEntity userProduct2 = psBillingProductsFactory.createUserProduct();
        UserProductPriceEntity userProductPriceEntity1 = psBillingProductsFactory.createUserProductPrices(userProduct1, CustomPeriodUnit.ONE_DAY);
        UserProductPriceEntity userProductPriceEntity2 = psBillingProductsFactory.createUserProductPrices(userProduct2, CustomPeriodUnit.ONE_DAY);

        UserProductEntity inappUserProduct = psBillingProductsFactory.createUserProduct(x -> x.billingType(BillingType.INAPP_APPLE));
        UserProductPriceEntity inappUserProductPrice = psBillingProductsFactory.createUserProductPrices(inappUserProduct, CustomPeriodUnit.ONE_DAY);

        this.userProduct1 = Tuple2.tuple(userProduct1, userProductPriceEntity1);
        this.userProduct2 = Tuple2.tuple(userProduct2, userProductPriceEntity2);
        this.inappUserProduct = Tuple2.tuple(inappUserProduct, inappUserProductPrice);
    }
}
