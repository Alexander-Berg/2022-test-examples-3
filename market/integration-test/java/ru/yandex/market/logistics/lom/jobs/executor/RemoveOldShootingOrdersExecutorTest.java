package ru.yandex.market.logistics.lom.jobs.executor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.persistence.Entity;
import javax.transaction.Transactional;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.reflections.ReflectionUtils.withModifier;
import static org.reflections.ReflectionUtils.withName;
import static org.reflections.ReflectionUtils.withParametersCount;
import static org.reflections.ReflectionUtils.withPrefix;
import static org.reflections.ReflectionUtils.withReturnType;

@ParametersAreNonnullByDefault
@DisplayName("Удаление старых стрельбовых заказов")
class RemoveOldShootingOrdersExecutorTest extends AbstractContextualTest {

    /**
     * Некоторые данные могут быть незаполненны, и это соответствует бизнес-логике.
     */
    private static final Map<String, Set<String>> METHODS_CAN_BE_NULLABLE = ImmutableMap.<String, Set<String>>builder()
        .put("StorageUnit", Set.of("getParent", "getChildren"))
        .put("WaybillSegment", Set.of("getReturnWaybillSegment"))
        .put("WaybillSegmentStatusHistory", Set.of("getTrackerStatus"))
        .put("BillingEntity", Set.of("getOrder"))
        .build();

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private RemoveOldShootingOrdersExecutor executor;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-06-01T23:00:00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @SneakyThrows
    @Transactional
    @DisplayName("Все поля заказа в тесте и все вложенные поля заполнены")
    @DatabaseSetup("/jobs/executor/removeOldShootingOrders/before/setup.xml")
    void allOrderFieldsAreFilled() {
        Order order = orderRepository.getById(1L);
        softly.assertThat(order).isNotNull();

        Map<String, Set<Long>> checkedEntities = new HashMap<>();
        assertAllFieldsAreNonnull(order, order.getClass(), checkedEntities);
        softly.assertThat(checkedEntities).hasSize(30);
    }

    @SneakyThrows
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> void assertAllFieldsAreNonnull(
        T entity,
        Class<?> entityClass,
        Map<String, Set<Long>> checkedEntities
    ) {
        if (Collection.class.isAssignableFrom(entityClass)) {
            softly.assertThat(((Collection) entity))
                .as("Empty collection somewhere") //type erasure
                .isNotEmpty();
            ((Collection) entity).forEach(el -> assertAllFieldsAreNonnull(el, el.getClass(), checkedEntities));
            return;
        }

        if (!isEntityCorrectForChecking(entity, entityClass, checkedEntities)) {
            return;
        }

        Set<Method> getters = ReflectionUtils.getAllMethods(
            entityClass,
            withModifier(Modifier.PUBLIC),
            withPrefix("get"),
            withParametersCount(0)
        );
        for (Method getter : getters) {
            var fieldValue = getter.invoke(entity);
            boolean methodCanReturnNull = METHODS_CAN_BE_NULLABLE.getOrDefault(entityClass.getSimpleName(), Set.of())
                .contains(getter.getName());
            boolean isNullFieldValueOrEmptyCollection = fieldValue == null
                || (Collection.class.isAssignableFrom(getter.getReturnType()) && ((Collection) fieldValue).isEmpty());
            if (methodCanReturnNull && isNullFieldValueOrEmptyCollection) {
                continue;
            }

            softly.assertThat(fieldValue)
                .as(entityClass.getSimpleName() + " method value " + getter.getName() + " is null")
                .isNotNull();
            assertAllFieldsAreNonnull(fieldValue, getter.getReturnType(), checkedEntities);
        }
    }

    private <T> boolean isEntityCorrectForChecking(
        T entity,
        Class<?> entityClass,
        Map<String, Set<Long>> checkedEntities
    ) {
        Objects.requireNonNull(entity);
        if (entityClass.isAssignableFrom(Collection.class)) {
            return true;
        }
        if (entityClass.isPrimitive() || entityClass.getAnnotation(Entity.class) == null) {
            return false;
        }

        String className = entityClass.getSimpleName();
        Long id = getEntityId(entity, entityClass);
        Set<Long> savedIds = checkedEntities.get(className);

        if (savedIds != null) {
            if (savedIds.contains(id)) {
                return false;
            }

            savedIds.add(id);
            return true;
        }

        savedIds = new HashSet<>();
        savedIds.add(id);
        checkedEntities.put(className, savedIds);
        return true;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private <T> long getEntityId(
        T entity,
        Class<?> entityClass
    ) {
        Method getId = ReflectionUtils.getAllMethods(
                entityClass,
                withModifier(Modifier.PUBLIC),
                withParametersCount(0),
                withName("getId"),
                withReturnType(Long.class)
            )
            .stream()
            .filter(
                method ->
                    method.getDeclaringClass().equals(entityClass)
                        || method.getDeclaringClass().isAssignableFrom(entityClass)
            )
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No getId() method found for " + entityClass.getSimpleName()));

        return Objects.requireNonNull((Long) getId.invoke(entity));
    }

    @Test
    @JpaQueriesCount(68)
    @DisplayName("Успешное удаление")
    @SuppressWarnings("ConstantConditions")
    @DatabaseSetup("/jobs/executor/removeOldShootingOrders/before/setup.xml")
    @ExpectedDatabase(
        value = "/jobs/executor/removeOldShootingOrders/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successRemoval() {
        executor.doJob(null);
        ordersRemovalLogged(2);
    }

    @Test
    @JpaQueriesCount(15)
    @SuppressWarnings("ConstantConditions")
    @DisplayName("Заказ со всеми заполненными полями не подходит для удаления")
    @DatabaseSetup("/jobs/executor/removeOldShootingOrders/before/setup.xml")
    @DatabaseSetup(
        value = "/jobs/executor/removeOldShootingOrders/before/1_created_after_removal_deadline.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/jobs/executor/removeOldShootingOrders/after/only_1_left.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void fullInfoOrderNotRemoved() {
        executor.doJob(null);
        ordersRemovalLogged(1);
    }

    @Test
    @JpaQueriesCount(68)
    @DisplayName("Удаляются только старые заказы")
    @SuppressWarnings("ConstantConditions")
    @DatabaseSetup("/jobs/executor/removeOldShootingOrders/before/setup.xml")
    @DatabaseSetup(
        value = "/jobs/executor/removeOldShootingOrders/before/new_shooting_order.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/jobs/executor/removeOldShootingOrders/after/only_new_order_left.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void onlyOldRemoved() {
        executor.doJob(null);
        ordersRemovalLogged(2);
    }

    @Test
    @JpaQueriesCount(1)
    @DisplayName("Нет заказов для удаления")
    @SuppressWarnings("ConstantConditions")
    @DatabaseSetup(
        value = "/jobs/executor/removeOldShootingOrders/before/new_shooting_order.xml",
        type = DatabaseOperation.CLEAN_INSERT
    )
    @ExpectedDatabase(
        value = "/jobs/executor/removeOldShootingOrders/before/new_shooting_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noOrdersToRemove() {
        executor.doJob(null);
        ordersRemovalLogged(0);
    }

    private void ordersRemovalLogged(int amount) {
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t"
                + "format=plain\t"
                + "payload=Removed " + amount + " orders\t"
                + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                + "tags=ORDERS_REMOVAL\t"
                + "extra_keys=amount\t"
                + "extra_values=" + amount
        );
    }
}
