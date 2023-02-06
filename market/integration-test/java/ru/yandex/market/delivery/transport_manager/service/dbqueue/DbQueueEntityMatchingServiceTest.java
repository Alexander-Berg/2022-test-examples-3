package ru.yandex.market.delivery.transport_manager.service.dbqueue;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueEntity;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueEntityIdType;
import ru.yandex.market.delivery.transport_manager.queue.base.QueueTaskDto;
import ru.yandex.market.delivery.transport_manager.queue.base.annotations.DbQueueEntityId;
import ru.yandex.market.delivery.transport_manager.queue.base.annotations.DbQueueEntityIds;
import ru.yandex.market.delivery.transport_manager.queue.base.annotations.DbQueueNoEntity;
import ru.yandex.market.delivery.transport_manager.queue.base.annotations.DbQueuePrefixedEntityId;
import ru.yandex.market.delivery.transport_manager.util.testdata.ObjectTestFieldValuesUtils;

public class DbQueueEntityMatchingServiceTest extends AbstractContextualTest {

    public static class SingleLong implements QueueTaskDto {
        @Getter
        @DbQueueEntityId(idType = DbQueueEntityIdType.TRANSPORTATION_UNIT_REQUEST_ID)
        private Long val = 5L;
    }

    public static class SinglePrimitive implements QueueTaskDto {
        @Getter
        @DbQueueEntityId
        private long val = 5;
    }

    public static class WrongType implements QueueTaskDto {
        @Getter
        @DbQueueEntityId
        private String val = "";
    }

    public static class LongList implements QueueTaskDto {
        @Getter
        @DbQueueEntityIds
        private List<Long> val = List.of(5L, 6L);
    }

    public static class WrongList implements QueueTaskDto {
        @Getter
        @DbQueueEntityIds
        private List<String> val = List.of("1");
    }

    @Data
    public static class SimplePrefixed implements QueueTaskDto {
        @DbQueuePrefixedEntityId
        private String entityId = "TM1";
    }

    public static class SimplePrefixedWrongType implements QueueTaskDto {
        @Getter
        @DbQueuePrefixedEntityId
        private Long entityId = 1L;
    }

    public static class CompositePrefixed implements QueueTaskDto {
        @Getter
        @DbQueuePrefixedEntityId(path = "idContainer.entityId")
        private EntityIdDto entityId = new EntityIdDto();

        @Data
        public static class EntityIdDto {
            private EntityIdContainer idContainer = new EntityIdContainer();
        }
        @Data
        public static class EntityIdContainer {
            private String entityId = "TM1";
        }
    }



    @Autowired
    private DbQueueEntityMatchingService entityMatchingService;

    private static Set<Class<? extends QueueTaskDto>> dbQueueDtoClasses;

    private static final Predicate<Field> CHECK_SINGLE_FIELD_RULE =
        field -> field.getAnnotation(DbQueueEntityId.class) != null &&
            DbQueueEntityMatchingService.SUPPORTED_FIELD_CLASSES.contains(field.getType());

    private static final Predicate<Field> CHECK_COLLECTION_RULE =
        field -> field.getAnnotation(DbQueueEntityIds.class) != null &&
            Collection.class.isAssignableFrom(field.getType()) &&
            DbQueueEntityMatchingService.SUPPORTED_FIELD_CLASSES.contains(
                (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]
            );
    private static final Predicate<Field> CHECK_PREFIXED_RULE =
        field -> field.getAnnotation(DbQueuePrefixedEntityId.class) != null && (
            String.class.isAssignableFrom(field.getType()) ||
                field.getType().getPackage().getName().startsWith("ru.yandex.market.delivery.transport_manager") ||
                field.getType().getPackage().getName().startsWith("ru.yandex.market.delivery.gruzin")
        );

    private static final Predicate<Class<? extends QueueTaskDto>> CHECK_RULE = c ->
        c.getAnnotation(DbQueueNoEntity.class) != null ||
            Arrays.stream(c.getDeclaredFields()).anyMatch(
                CHECK_SINGLE_FIELD_RULE
                    .or(CHECK_COLLECTION_RULE)
                    .or(CHECK_PREFIXED_RULE)
            );

    @BeforeAll
    static void init() {
        Reflections reflections = new Reflections("ru/yandex/market/delivery/transport_manager/queue");
        dbQueueDtoClasses = new HashSet<>(reflections.getSubTypesOf(QueueTaskDto.class));
    }

    @Test
    void testAllClassesAnnotated() {
        Set<Class<? extends QueueTaskDto>> annotatedClasses = dbQueueDtoClasses.stream()
            .filter(CHECK_RULE)
            .collect(Collectors.toSet());

        softly.assertThat(Sets.difference(dbQueueDtoClasses, annotatedClasses)).isEmpty();

    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dbQueueDtoClassesWithEntityIds")
    void testAllEntityRelatedClassesReturnSomething(Class<? extends QueueTaskDto> dtoClass) {
        QueueTaskDto instance = getInstance(dtoClass);
        softly.assertThat(entityMatchingService.getRelatedEntities(instance)).isNotEmpty();
    }

    @Test
    void testSingleLong() {
        QueueTaskDto queueTaskDto = new SingleLong();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities.size()).isEqualTo(1);

        DbQueueEntity expected =
            new DbQueueEntity()
                .setId(5L)
                .setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_REQUEST_ID);

        assertThatModelEquals(expected, entities.stream().findFirst().get());
    }

    @Test
    void testSinglePrimitive() {
        QueueTaskDto queueTaskDto = new SinglePrimitive();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities.size()).isEqualTo(1);

        DbQueueEntity expected =
            new DbQueueEntity()
                .setId(5L)
                .setIdType(DbQueueEntityIdType.TRANSPORTATION_ID);

        assertThatModelEquals(expected, entities.stream().findFirst().get());
    }

    @Test
    void testWrongType() {
        QueueTaskDto queueTaskDto = new WrongType();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities.size()).isEqualTo(0);
    }

    @Test
    void testCollection() {
        QueueTaskDto queueTaskDto = new LongList();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities.size()).isEqualTo(2);

        DbQueueEntity first =
            new DbQueueEntity()
                .setId(5L)
                .setIdType(DbQueueEntityIdType.TRANSPORTATION_ID);

        DbQueueEntity second =
            new DbQueueEntity()
                .setId(5L)
                .setIdType(DbQueueEntityIdType.TRANSPORTATION_ID);

        assertContainsExactlyInAnyOrder(new ArrayList<>(entities), first, second);
    }

    @Test
    void testWrongTypeCollection() {
        QueueTaskDto queueTaskDto = new WrongList();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities.size()).isEqualTo(0);
    }

    @Test
    void testPrefixed() {
        QueueTaskDto queueTaskDto = new SimplePrefixed();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities).containsExactlyInAnyOrder(
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID)
        );
    }

    @Test
    void testCompositePrefixed() {
        QueueTaskDto queueTaskDto = new CompositePrefixed();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities).containsExactlyInAnyOrder(
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID)
        );
    }

    @Test
    void testWrongTypePrefixed() {
        QueueTaskDto queueTaskDto = new SimplePrefixedWrongType();

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities.size()).isEqualTo(0);
    }

    @Test
    void testWrongPrefixe() {
        SimplePrefixed queueTaskDto = new SimplePrefixed();
        queueTaskDto.setEntityId("ABC1");

        Set<DbQueueEntity> entities = entityMatchingService.getRelatedEntities(queueTaskDto);
        softly.assertThat(entities.size()).isEqualTo(0);
    }

    private <T extends QueueTaskDto> T getInstance(Class<T> c) {
        T instance = ObjectTestFieldValuesUtils.createAndFillInstance(c, false);
        setEntityIds(instance);
        return instance;
    }

    private void setEntityIds(Object instance) {
        // В дефолтных данных заполнены строки, но заполнения TMU* - это более высокоуровневая операция - делаем тут
        Stream.of(instance.getClass().getDeclaredFields())
            .filter(CHECK_PREFIXED_RULE)
            .forEach(f -> setTmId(instance, f));
    }

    @SneakyThrows
    private void setTmId(Object instance, Field field) {
        field.setAccessible(true);

        String path = field.getAnnotation(DbQueuePrefixedEntityId.class).path();
        if (StringUtils.isBlank(path)) {
            field.set(instance, "TMU1");
            return;
        }
        String[] pathElements = path.split("\\.");

        Object o = instance;
        Field f = field;
        for (int i = 0; i < pathElements.length; i++) {
            o = f.get(o);
            f = o.getClass().getDeclaredField(pathElements[i]);
            f.setAccessible(true);
        }
        f.set(o, "TMU1");
    }

    static Stream<Arguments> dbQueueDtoClassesWithEntityIds() {
        return dbQueueDtoClasses.stream()
            .filter(c -> c.getAnnotation(DbQueueNoEntity.class) == null)
            .map(Arguments::of);
    }
}
