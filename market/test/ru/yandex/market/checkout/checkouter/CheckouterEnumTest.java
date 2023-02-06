package ru.yandex.market.checkout.checkouter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;

import ru.yandex.market.checkout.checkouter.delivery.AvailableDeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.changes.ChangesStage;
import ru.yandex.market.checkout.checkouter.swagger.Direction;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.checkouter.tasks.v2.AbstractTask;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskRunType;
import ru.yandex.market.checkout.checkouter.tasks.v2.TaskStageType;

public class CheckouterEnumTest {

    private static final Collection<Class> IGNORED_ENUMS = new HashSet<>() {{
        add(ru.yandex.market.checkout.checkouter.balance.BasketStatus.class);
        add(ru.yandex.market.checkout.checkouter.balance.model.BalanceStatus.class);
        add(ru.yandex.market.checkout.checkouter.balance.model.NotificationMode.class);
        add(ru.yandex.market.checkout.checkouter.cart.CartChange.class);
        add(ru.yandex.market.checkout.checkouter.cart.ItemChange.class);
        add(ru.yandex.market.checkout.checkouter.event.HistorySortingField.class);
        add(ru.yandex.market.checkout.checkouter.log.cart.CartLoggingEvent.class);
        add(ru.yandex.market.checkout.checkouter.log.cart.LogType.class);
        add(ru.yandex.market.checkout.checkouter.order.MoveOrderStatus.class);
        add(ru.yandex.market.checkout.checkouter.order.OrderSortingField.class);
        add(ru.yandex.market.checkout.checkouter.pay.balance.PaymentFormType.class);
        add(ru.yandex.market.checkout.checkouter.pay.PaymentURLActionType.class);
        add(ru.yandex.market.checkout.checkouter.order.item.MissingItemsStrategyType.class);
        add(ru.yandex.market.checkout.checkouter.order.OrderEditRequest.EditRequestType.class);
        add(OrderItemInstance.InstanceType.class);
        add(Direction.class);
        add(TaskRunType.class);
        add(TaskStageType.class);
        add(AbstractTask.TaskMetricType.class);
        add(ChangesStage.class);
        add(Partition.PartitionType.class);
        add(AvailableDeliveryType.class);
    }};

    public static Stream<Arguments> parameterizedTestData() {
        Reflections reflections = new Reflections("ru.yandex.market.checkout.checkouter");
        Set<Class<? extends Enum>> enums = reflections.getSubTypesOf(Enum.class);
        enums.removeAll(IGNORED_ENUMS);
        // приватные enum'ы не надо. пока только для вложенных.
        enums.removeIf(clz -> Modifier.isPrivate(clz.getModifiers()) && clz.isMemberClass());
        enums.removeIf(Class::isAnonymousClass);
        return enums.stream().map(Arguments::of);
    }

    @Tag("auto")
    @DisplayName("Все enum должны содержать UNKNOWN значение")
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void testUnknownEnums(Class<? extends Enum> enumClass) {
        //Проверяем что в их значениях есть UNKNOWN(-1)
        Assertions.assertTrue(Arrays.stream(enumClass.getEnumConstants())
                .anyMatch(this::hasDefaultValue), "Enum " + enumClass.getName()
                + " should have default value annotated with JsonEnumDefaultValue");
    }

    private boolean hasDefaultValue(Enum v) {
        try {
            Annotation[] annotations = v.getClass().getField(v.name()).getAnnotations();
            return Arrays.stream(annotations).anyMatch(a -> a.annotationType().equals(JsonEnumDefaultValue.class));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
