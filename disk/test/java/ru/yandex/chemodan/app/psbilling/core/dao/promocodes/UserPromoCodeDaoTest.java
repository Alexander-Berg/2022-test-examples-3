package ru.yandex.chemodan.app.psbilling.core.dao.promocodes;

import java.util.UUID;

import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.UserPromoDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.PromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promocodes.UserPromoCodeEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoApplicationArea;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.PromoTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.promos.UserPromoEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class UserPromoCodeDaoTest extends AbstractPsBillingCoreTest {

    @Autowired
    PromoTemplateDao promoTemplateDao;
    @Autowired
    UserPromoDao userPromoDao;
    @Autowired
    PromoService promoService;
    @Autowired
    UserPromoCodeDao userPromoCodeDao;

    PromoTemplateEntity testPromo;
    UserPromoEntity userPromo;
    UserProductEntity testProduct;
    UserProductPriceEntity price;
    ProductLineEntity line;
    PromoCodeEntity promoPromoCode;
    PromoCodeEntity productPromoCode;
    Order order;

    @Before
    public void setup(){
        testProduct = psBillingProductsFactory.createUserProduct(x -> x);
        price = psBillingProductsFactory.createUserProductPrices(testProduct, CustomPeriodUnit.ONE_MONTH);
        line = psBillingProductsFactory.createProductLine("xxx", x->x, testProduct);

        testPromo = psBillingPromoFactory.createPromo(x->x.applicationArea(PromoApplicationArea.PER_USER));
        promoTemplateDao.bindProductLines(testPromo.getId(), line.getId());
        promoService.activatePromoForUser(uid, testPromo.getId());
        userPromo = userPromoDao.findUserPromo(uid, testPromo.getId()).get();

        promoPromoCode = psBillingPromoFactory.createPromoCodePromo(UUID.randomUUID().toString(), testPromo.getId(), x->x);
        productPromoCode = psBillingPromoFactory.createPromoCodeProductPrice(UUID.randomUUID().toString(), price.getId(), x->x);

        order = psBillingOrdersFactory.createOrder(uid, OrderStatus.INIT);
    }

    @Test
    public void testCreateAndRead(){
        Instant frozenNow = DateUtils.freezeTime();
        UserPromoCodeDao.InsertData data = UserPromoCodeDao.InsertData.builder()
                .code(promoPromoCode.getCode())
                .uid(uid)
                .userPromoId(Option.of(userPromo.getId()))
                .build();
        userPromoCodeDao.create(data);

        UserPromoCodeEntity userPromoCodeEntity = userPromoCodeDao.findByCodeAndUid(promoPromoCode.getCode(), uid).orElseThrow();
        Assert.equals(userPromoCodeEntity.getCode(), data.getCode());
        Assert.equals(userPromoCodeEntity.getUid(), data.getUid());
        Assert.equals(userPromoCodeEntity.getCreatedAt(), frozenNow);
        Assert.equals(userPromoCodeEntity.getUserPromoId(), data.getUserPromoId());
        Assert.equals(userPromoCodeEntity.getOrderId(), Option.empty());

        data = UserPromoCodeDao.InsertData.builder()
                .code(productPromoCode.getCode())
                .uid(uid)
                .orderId(Option.of(order.getId()))
                .build();
        userPromoCodeDao.create(data);

        userPromoCodeEntity = userPromoCodeDao.findByCodeAndUid(productPromoCode.getCode(), uid).orElseThrow();
        Assert.equals(userPromoCodeEntity.getCode(), data.getCode());
        Assert.equals(userPromoCodeEntity.getUid(), data.getUid());
        Assert.equals(userPromoCodeEntity.getCreatedAt(), frozenNow);
        Assert.equals(userPromoCodeEntity.getOrderId(), data.getOrderId());
        Assert.equals(userPromoCodeEntity.getUserPromoId(), Option.empty());
    }

    @Test
    public void testDefaults() {
        DateUtils.freezeTime();
        //don't specify activated when
        UserPromoCodeEntity code = userPromoCodeDao.create(UserPromoCodeDao.InsertData.builder()
                .code(productPromoCode.getCode())
                .uid(uid)
                .orderId(Option.of(order.getId()))
                .build()
        );
        Assert.equals(code.getCreatedAt(), Instant.now());
    }

    @Test
    public void testConstraints() {
        //both order id and promo id
        Assert.assertThrows(() -> {
            UserPromoCodeDao.InsertData data = UserPromoCodeDao.InsertData.builder()
                    .code(productPromoCode.getCode())
                    .uid(uid)
                    .orderId(Option.of(order.getId()))
                    .userPromoId(Option.of(userPromo.getId()))
                    .build();
            userPromoCodeDao.create(data);
        }, DataIntegrityViolationException.class);

        //neither order id nor promo id
        Assert.assertThrows(() -> {
            UserPromoCodeDao.InsertData data = UserPromoCodeDao.InsertData.builder()
                    .code(productPromoCode.getCode())
                    .uid(uid)
                    .build();
            userPromoCodeDao.create(data);
        }, DataIntegrityViolationException.class);

        //wrong order id
        Assert.assertThrows(() -> {
            UserPromoCodeDao.InsertData data = UserPromoCodeDao.InsertData.builder()
                    .code(productPromoCode.getCode())
                    .uid(uid)
                    .orderId(Option.of(UUID.randomUUID()))
                    .userPromoId(Option.of(userPromo.getId()))
                    .build();
            userPromoCodeDao.create(data);
        }, DataIntegrityViolationException.class);

        //wrong promo id
        Assert.assertThrows(() -> {
            UserPromoCodeDao.InsertData data = UserPromoCodeDao.InsertData.builder()
                    .code(productPromoCode.getCode())
                    .uid(uid)
                    .userPromoId(Option.of(UUID.randomUUID()))
                    .build();
            userPromoCodeDao.create(data);
        }, DataIntegrityViolationException.class);

    }
}
