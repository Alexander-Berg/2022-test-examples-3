package ru.yandex.market.tpl.core.domain.order.code.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

import lombok.SneakyThrows;

import ru.yandex.market.tpl.core.domain.base.property.TplPropertyType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderProperty;
import ru.yandex.market.tpl.core.domain.order.property.TplOrderProperties;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.CallToRecipientTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.Task;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;

public final class ModelBuilderTestUtil {

    private ModelBuilderTestUtil() {

    }

    @SneakyThrows
    public static Order buildOrder(Long id) {
        Constructor<Order> declaredConstructor = Order.class.getDeclaredConstructor();
        declaredConstructor.trySetAccessible();
        Order order = declaredConstructor.newInstance();

        order.setId(id);

        return order;
    }

    @SneakyThrows
    public static Order buildOrderWithVerificationCode(String verificationCode) {
        Constructor<Order> declaredConstructor = Order.class.getDeclaredConstructor();
        declaredConstructor.trySetAccessible();
        Order order = declaredConstructor.newInstance();

        setVerificationCodeValue(order, verificationCode);

        return order;
    }

    @SneakyThrows
    public static Order buildB2bCustomersOrder(Long id, String verificationCode) {
        return buildB2bCustomersOrder(id, null, verificationCode);
    }

    @SneakyThrows
    public static Order buildB2bCustomersOrder(Long id, String yandexId, String verificationCode) {
        Constructor<Order> declaredConstructor = Order.class.getDeclaredConstructor();
        declaredConstructor.trySetAccessible();
        Order order = declaredConstructor.newInstance();

        order.setId(id);
        order.setExternalOrderId(yandexId);
        setVerificationCodeValue(order, verificationCode);
        setB2bCustomersType(order);

        return order;
    }

    private static void setVerificationCodeValue(Order order, String verificationCode) {
        String propertyName = TplOrderProperties.Names.VERIFICATION_CODE_BEFORE_HANDING.name();
        Map<String, OrderProperty> propertyMap = Map.of(
                propertyName,
                new OrderProperty(order, TplPropertyType.STRING, propertyName, verificationCode)
        );

        Map<String, OrderProperty> properties = order.getProperties();
        properties.putAll(propertyMap);

        order.setProperties(properties);
    }

    private static void setB2bCustomersType(Order order) {
        String propertyName = TplOrderProperties.Names.CUSTOMER_TYPE.name();
        Map<String, OrderProperty> propertyMap = Map.of(
                propertyName,
                new OrderProperty(
                        order, TplPropertyType.STRING, propertyName, TplOrderProperties.CustomerValues.B2B_CUSTOMER_TYPE.name()
                )
        );

        Map<String, OrderProperty> properties = order.getProperties();
        properties.putAll(propertyMap);

        order.setProperties(properties);
    }

    @SneakyThrows
    public static User buildUser(Long id) {
        User user = new User();

        setFieldValue(User.class, user, Long.class, "setId", id);

        return user;
    }

    @SneakyThrows
    public static OrderDeliveryTask buildOrderDeliveryTask(Long taskId, Long orderId, Long callTaskId) {
        OrderDeliveryTask orderDeliveryTask = new OrderDeliveryTask()
                .setOrderId(orderId)
                .setCallToRecipientTask(buildCallTask(callTaskId));

        setFieldValue(Task.class, orderDeliveryTask, Long.class, "setId", taskId);

        return orderDeliveryTask;
    }

    public static CallToRecipientTask buildCallTask(long id) {
        CallToRecipientTask callToRecipientTask = new CallToRecipientTask();

        setFieldValue(CallToRecipientTask.class, callToRecipientTask, Long.class, "setId", id);

        return callToRecipientTask;
    }

    @SneakyThrows
    public static UserShift buildUserShift(long userId) {
        Constructor<UserShift> declaredConstructor = UserShift.class.getDeclaredConstructor();
        declaredConstructor.trySetAccessible();
        UserShift userShift = declaredConstructor.newInstance();

        User user = buildUser(userId);

        setFieldValue(UserShift.class, userShift, User.class, "setUser", user);
        return userShift;
    }

    @SneakyThrows
    private static void setFieldValue(Class<?> modelClass,
                                      Object model,
                                      Class<?> fieldClazz,
                                      String methodName,
                                      Object fieldValue
    ) {
        Method method = modelClass.getDeclaredMethod(methodName, fieldClazz);
        method.trySetAccessible();
        method.invoke(model, fieldValue);
    }
}
