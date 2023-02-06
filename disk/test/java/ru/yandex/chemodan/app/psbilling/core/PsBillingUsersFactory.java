package ru.yandex.chemodan.app.psbilling.core;

import java.util.UUID;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.dao.features.UserServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductTemplateFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.features.UserServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.inside.passport.PassportUid;

@RequiredArgsConstructor
public class PsBillingUsersFactory {
    public static final String DEFAULT_UID = "123213123";
    public static final PassportUid UID = PassportUid.cons(Long.parseLong(DEFAULT_UID));

    @Autowired
    private UserServiceFeatureDao userServiceFeatureDao;
    @Autowired
    private UserServiceDao userServiceDao;
    @Autowired
    private ProductTemplateFeatureDao productTemplateFeatureDao;
    @Autowired
    private PsBillingProductsFactory productsFactory;
    @Autowired
    private PsBillingOrdersFactory ordersFactory;
    @Autowired
    private UserProductPeriodDao periodDao;
    @Autowired
    private UserProductPricesDao pricesDao;
    @Autowired
    private OrderDao orderDao;

    public UserServiceFeature createUserServiceFeature(FeatureEntity feature,
                                                       Function<ProductFeatureDao.InsertData.InsertDataBuilder,
                                                               ProductFeatureDao.InsertData.InsertDataBuilder> featureCustomizer) {
        return createUserServiceFeature(feature, featureCustomizer, Function.identity());
    }

    public UserServiceFeature createUserServiceFeature(
            FeatureEntity feature,
            Function<ProductFeatureDao.InsertData.InsertDataBuilder, ProductFeatureDao.InsertData.InsertDataBuilder> featureCustomizer,
            Function<UserServiceDao.InsertData.InsertDataBuilder, UserServiceDao.InsertData.InsertDataBuilder> userServiceCustomizer
    ) {
        UserProductEntity product = productsFactory.createUserProduct();
        ProductFeatureEntity productFeature = productsFactory.createProductFeature(
                product.getId(), feature, featureCustomizer
        );
        return createUserServiceFeature(productFeature, userServiceCustomizer);
    }

    public UserServiceFeature createUserServiceFeature(ProductFeatureEntity productFeature) {
        return createUserServiceFeature(productFeature, Function.identity());
    }

    public UserServiceFeature createUserServiceFeature(
            ProductFeatureEntity productFeature,
            Function<UserServiceDao.InsertData.InsertDataBuilder, UserServiceDao.InsertData.InsertDataBuilder> userServiceCustomizer) {
        UserServiceEntity userService = createUserService(productFeature.getUserProductId(), userServiceCustomizer);

        return userServiceFeatureDao.insert(UserServiceFeatureDao.InsertData.builder()
                .productFeatureId(productFeature.getId())
                .productTemplateFeatureId(productTemplateFeatureDao.findByProductFeatureId(productFeature.getId())
                        .map(AbstractEntity::getId).orElse((UUID) null))
                .userServiceId(userService.getId())
                .uid(userService.getUid())
                .build(), Target.ENABLED);
    }

    public UserServiceEntity createUserService(PassportUid uid, UserProductPrice userProductPrice) {
        return createUserService(userProductPrice.getPeriod().getUserProduct().getId(),
                x -> x.uid(uid.toString()).userProductPriceId(Option.of(userProductPrice.getId())));
    }

    public UserServiceEntity createUserService(UserProductEntity userProduct) {
        return createUserService(userProduct.getId(), Function.identity());
    }

    public UserServiceEntity createUserService(Target target) {
        return createUserService(productsFactory.createUserProduct().getId(), Function.identity(), target);
    }

    public UserServiceEntity createUserServiceWithOrder() {
        return createUserServiceWithOrder(Target.ENABLED);
    }

    public UserServiceEntity createUserServiceWithOrder(Target target) {
        return createUserServiceWithOrder(target, UID);
    }

    public UserServiceEntity createUserServiceWithOrder(Target target, PassportUid uid) {
        Order order = ordersFactory.createOrder(uid, OrderStatus.PAID);
        UserServiceEntity userService = createUserService(target, order);
        orderDao.onSuccessfulOrderPurchase(order.getId(), Option.of(userService.getId()), 1);
        return userService;
    }

    public UserServiceEntity createUserServiceWithOrder(Target target, OrderStatus orderStatus){
        Order order = ordersFactory.createOrder(UID, orderStatus);
        UserServiceEntity userService = createUserService(target, order);
        orderDao.onSuccessfulOrderPurchase(order.getId(), Option.of(userService.getId()), 1);
        return userService;
    }

    public UserServiceEntity createUserService(Target target, Order order) {
        UUID userProductId = periodDao.findById(
                pricesDao.findById(order.getUserProductPriceId()).getUserProductPeriodId())
                .getUserProductId();
        return createUserService(userProductId, builder -> {
            builder.paidByOrderId(Option.of(order.getId()))
                    .uid(order.getUid());
            return builder;
        }, target);
    }


    public UserServiceEntity createUserService(
            UUID userProductId,
            Function<UserServiceDao.InsertData.InsertDataBuilder, UserServiceDao.InsertData.InsertDataBuilder> customizer) {
        return createUserService(userProductId, customizer, Target.ENABLED);
    }

    public UserServiceEntity createUserService(
            UUID userProductId,
            Function<UserServiceDao.InsertData.InsertDataBuilder, UserServiceDao.InsertData.InsertDataBuilder> customizer, Target target) {
        UserServiceDao.InsertData.InsertDataBuilder defultBuilder = UserServiceDao.InsertData.builder()
                .nextCheckDate(Option.empty())
                .uid(DEFAULT_UID)
                .userProductId(userProductId);
        defultBuilder = customizer.apply(defultBuilder);
        return userServiceDao.insert(defultBuilder.build(), target);
    }


}
