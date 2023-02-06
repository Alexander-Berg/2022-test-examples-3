package ru.yandex.mail.cerberus.asyncdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.val;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.yandex.mail.micronaut.common.Pageable;
import ru.yandex.mail.cerberus.asyncdb.Entity.TypeSafeId;
import ru.yandex.mail.cerberus.asyncdb.EntityRepository.Keys;
import ru.yandex.mail.cerberus.asyncdb.EntityRepository.LongKey;
import ru.yandex.mail.cerberus.asyncdb.EntityRepository.TestBean;
import ru.yandex.mail.cerberus.asyncdb.EntityRepository.UUIDKey;
import ru.yandex.mail.pglocal.Database;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.mail.micronaut.common.CerberusUtils.mapToList;

@ExtendWith(PgLocalExtension.class)
class RepositoryTest {
    private static final String MIGRATIONS = "migrations";
    private static final NestedData NESTED = new NestedData("nestedStr", 101L);
    private static final List<Entity> ENTITIES = List.of(
        new Entity(
            new TypeSafeId(100L),
            "one",
            OptionalInt.of(1),
            Optional.of(new Entity.Data(1, "1")),
            Optional.of(new Entity.Data(11, "b1")),
            Optional.empty(),
            NESTED
        ),
        new Entity(
            new TypeSafeId(101L),
            "two",
            OptionalInt.empty(),
            Optional.of(new Entity.Data(2, "2")),
            Optional.empty(),
            Optional.empty(),
            null
        ),
        new Entity(
            new TypeSafeId(102L),
            "three",
            OptionalInt.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            NESTED
        )
    );

    private static final List<EntityWithCompositeKey> ENTITIES_WITH_COMPOSITE_KEY = List.of(
        new EntityWithCompositeKey(100L, "type", "data1"),
        new EntityWithCompositeKey(101L, "type", "data2"),
        new EntityWithCompositeKey(102L, "type", "data3")
    );

    private static final String SIMPLE_CASE = "simple entity case";
    private static final String COMPOSITE_KEY_CASE = "entity with composite key case";

    private static final ObjectMapper objectMapper = initObjectMapper();
    private static EntityRepository repository;
    private static EntityWithCompositeKeyRepository compositeKeyRepository;
    private static List<Entity> entities;
    private static List<EntityWithCompositeKey> entitiesWithCompositeKey;

    private static ObjectMapper initObjectMapper() {
        return new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule())
            .registerModule(new JavaTimeModule());
    }

    private static RepositoryFactory initRepositoryFactory(Database database) {
        return new RepositoryFactory(database.getSimpleDataSource(), objectMapper, Optional.empty(), Optional.empty());
    }

    @BeforeAll
    static void init(@InitDb(migration = MIGRATIONS, name = "repository_test") Database database) {
        val factory = initRepositoryFactory(database);
        repository = factory.createRepository(EntityRepository.class);
        compositeKeyRepository = factory.createRepository(EntityWithCompositeKeyRepository.class);

        entities = repository.insertAll(ENTITIES);
        entitiesWithCompositeKey = compositeKeyRepository.insertAll(ENTITIES_WITH_COMPOSITE_KEY);
    }

    @SuppressWarnings("unchecked")
    private static <ID, E> ID idOf(E entity) {
        if (entity instanceof Entity) {
            val simpleEntity = (Entity) entity;
            return (ID) simpleEntity.getId();
        } else if (entity instanceof EntityWithCompositeKey) {
            val entityWithCompositeKey = (EntityWithCompositeKey) entity;
            return (ID) new CompositeKey(entityWithCompositeKey.getId(), entityWithCompositeKey.getType());
        } else {
            throw new IllegalArgumentException("Unexpected entity type");
        }
    }

    private static Stream<Arguments> predefinedParameters() {
        return Stream.of(
            Arguments.of(
                repository,
                entities.get(0),
                entities.get(1),
                entities.get(2),
                (LongFunction<TypeSafeId>) TypeSafeId::new,
                SIMPLE_CASE
            ),
            Arguments.of(
                compositeKeyRepository,
                entitiesWithCompositeKey.get(0),
                entitiesWithCompositeKey.get(1),
                entitiesWithCompositeKey.get(2),
                (LongFunction<CompositeKey>) id -> new CompositeKey(id, "type"),
                COMPOSITE_KEY_CASE
            )
        );
    }

    @MethodSource("predefinedParameters")
    @ParameterizedTest(name = "{5}")
    @DisplayName("Verify that built-in repository 'find*' methods returns correct values")
    <ID, E> void findTest(CrudRepository<ID, E> repository, E one, E two, E three, LongFunction<ID> idFactory, String name) {
        assertThat(repository.find(idOf(one)))
            .contains(one);

        assertThat(repository.findAll(List.of(idOf(one), idOf(two), idOf(three))))
            .containsExactlyInAnyOrder(one, two, three);

        assertThat(repository.findAll(List.of(idOf(one), idOf(two), idOf(three))))
            .containsExactlyInAnyOrder(one, two, three);

        assertThat(repository.find(idFactory.apply(100500L)))
            .isEmpty();

        assertThat(repository.findAll(List.of(idFactory.apply(100500L), idFactory.apply(-100500L))))
            .isEmpty();

        final ID expectedExistingId = idOf(one);
        assertThat(repository.findExistingIds(List.of(idOf(one), idFactory.apply(100500L))))
            .containsExactlyInAnyOrder(expectedExistingId);

        assertThat(repository.findMissingIds(List.of(idOf(one), idFactory.apply(100500L))))
            .containsExactlyInAnyOrder(idFactory.apply(100500L));
    }

    @MethodSource("predefinedParameters")
    @ParameterizedTest(name = "{5}")
    @DisplayName("Verify that built-in repository 'exists*' methods returns correct values")
    <ID, E> void existsTest(CrudRepository<ID, E> repository, E one, E two, E three, LongFunction<ID> idFactory, String name) {
        assertThat(repository.exists(idOf(one)))
            .isTrue();

        assertThat(repository.existsAny(List.of(idOf(one), idOf(two), idOf(three))))
            .isTrue();

        assertThat(repository.exists(idFactory.apply(100500L)))
            .isFalse();

        assertThat(repository.existsAny(List.of(idFactory.apply(100500L), idFactory.apply(-100500L))))
            .isFalse();

        assertThat(repository.existsAny(List.of(idOf(one), idFactory.apply(-100500L))))
            .isTrue();
    }

    private static Stream<Arguments> repositoryParameters() {
        return Stream.of(
            Arguments.of(repository, SIMPLE_CASE),
            Arguments.of(compositeKeyRepository, COMPOSITE_KEY_CASE)
        );
    }

    @MethodSource("repositoryParameters")
    @ParameterizedTest(name = "{1}")
    @DisplayName("Verify that built-in repository count method returns correct value")
    <ID, E> void countTest(CrudRepository<ID, E> repository, String name) {
        assertThat(repository.count())
            .isEqualTo(3L);
    }

    private static Stream<Arguments> initialParameters() {
        return Stream.of(
            Arguments.of(EntityRepository.class, ENTITIES, SIMPLE_CASE),
            Arguments.of(EntityWithCompositeKeyRepository.class, ENTITIES_WITH_COMPOSITE_KEY, COMPOSITE_KEY_CASE)
        );
    }

    @MethodSource("initialParameters")
    @ParameterizedTest(name = "{2}")
    @DisplayName("Verify that built-in repository 'delete*' methods correctly remove records")
    <ID, E, R extends CrudRepository<ID, E>> void deleteTest(Class<R> repositoryClass, List<E> entitiesData, String name,
                                                             @InitDb(migration = MIGRATIONS, name = "repository_delete_test") Database database) {
        val factory = initRepositoryFactory(database);
        val deleteRepo = factory.createRepository(repositoryClass);

        val first = deleteRepo.insert(entitiesData.get(0));
        val second = deleteRepo.insert(entitiesData.get(1));
        val third = deleteRepo.insert(entitiesData.get(2));

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(third).isNotNull();

        assertThat(deleteRepo.delete(idOf(second)))
            .isTrue();
        assertThat(deleteRepo.find(idOf(second)))
            .isEmpty();
        assertThat(deleteRepo.count())
            .isEqualTo(2);

        val deletedIds = deleteRepo.deleteAll(List.of(idOf(first), idOf(third)));
        assertThat(deletedIds)
            .containsExactlyInAnyOrder(idOf(first), idOf(third));
        assertThat(deleteRepo.find(idOf(first)))
            .isEmpty();
        assertThat(deleteRepo.find(idOf(third)))
            .isEmpty();
        assertThat(deleteRepo.count())
            .isEqualTo(0);
    }

    @MethodSource("initialParameters")
    @ParameterizedTest(name = "{2}")
    @DisplayName("Verify that built-in repository 'get*Chunk' methods returns correct values")
    <ID, E, R extends CrudRepository<ID, E>> void pagesReadTest(Class<R> repositoryClass, List<E> entitiesData, String name,
                                                                @InitDb(migration = MIGRATIONS, name = "chunks_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(repositoryClass);

        val entities = repo.insertAll(entitiesData);

        assertThat(entities)
            .allSatisfy(entity -> assertThat(entity).isNotNull());

        val firstPage = repo.findPage(Pageable.first(2), RepositoryTest::idOf);
        assertThat(firstPage.hasNext())
            .isTrue();
        assertThat(firstPage.getElements())
            .containsExactly(entities.get(0), entities.get(1));
        assertThat(firstPage.getNextPageId())
            .isNotEmpty();

        val lastPage = repo.findPage(new Pageable<>(firstPage.getNextPageId(), 2), RepositoryTest::idOf);
        assertThat(lastPage.hasNext())
            .isFalse();
        assertThat(lastPage.getElements())
            .containsExactly(entities.get(2));
        assertThat(lastPage.getNextPageId())
            .isEmpty();
    }

    private static Stream<Arguments> chunksParameters() {
        return Stream.of(
            Arguments.of(
                EntityRepository.class,
                List.of(
                    new Entity(new TypeSafeId(1L), "1", OptionalInt.of(10), Optional.empty(), Optional.empty(), Optional.empty() , NESTED),
                    new Entity(new TypeSafeId(2L), "2", OptionalInt.of(20), Optional.empty(), Optional.empty(), Optional.empty(), NESTED),
                    new Entity(new TypeSafeId(3L), "3", OptionalInt.of(30), Optional.empty(), Optional.empty(), Optional.empty(), null),
                    new Entity(new TypeSafeId(4L), "4", OptionalInt.of(40), Optional.empty(), Optional.empty(), Optional.empty(), null),
                    new Entity(new TypeSafeId(5L), "5", OptionalInt.of(50), Optional.empty(), Optional.empty(), Optional.empty(), NESTED)
                ),
                Condition.of("age > :age").bind("age", 25),
                SIMPLE_CASE
            ),
            Arguments.of(
                EntityWithCompositeKeyRepository.class,
                List.of(
                    new EntityWithCompositeKey(1L, "type", "data1"),
                    new EntityWithCompositeKey(2L, "type", "data2"),
                    new EntityWithCompositeKey(3L, "type2", "data3"),
                    new EntityWithCompositeKey(4L, "type2", "data4"),
                    new EntityWithCompositeKey(5L, "type2", "data5")
                ),
                Condition.of("type = :type").bind("type", "type2"),
                COMPOSITE_KEY_CASE
            )
        );
    }

    @MethodSource("chunksParameters")
    @ParameterizedTest(name = "{3}")
    @DisplayName("Verify that built-in repository 'get*Chunk' methods returns correct values when condition is applied")
    <ID, E, R extends CrudRepository<ID, E>> void conditionalChunksReadTest(Class<R> repositoryClass, List<E> entitiesData,
                                                                            Condition condition, String name,
                                                                            @InitDb(migration = MIGRATIONS, name = "conditional_chunks_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(repositoryClass);

        val entities = repo.insertAll(entitiesData);

        assertThat(entities)
            .allSatisfy(entity -> assertThat(entity).isNotNull());

        val firstPage = repo.findPage(Pageable.first(2), RepositoryTest::idOf, condition);
        assertThat(firstPage.hasNext())
            .isTrue();
        assertThat(firstPage.getElements())
            .containsExactly(entities.get(2), entities.get(3));
        assertThat(firstPage.getNextPageId())
            .isNotEmpty();

        val lastPageResult = repo.findPage(new Pageable<>(firstPage.getNextPageId(), 2), RepositoryTest::idOf, condition);
        assertThat(lastPageResult.hasNext())
            .isFalse();
        assertThat(lastPageResult.getElements())
            .containsExactly(entities.get(4));
        assertThat(lastPageResult.getNextPageId())
            .isEmpty();
    }

    private static Stream<Arguments> updateParameters() {
        return Stream.of(
            Arguments.of(
                EntityRepository.class,
                ENTITIES,
                (Function<Entity, Entity>) entity -> entity.withAge(OptionalInt.of(42)).withName("changed"),
                SIMPLE_CASE
            ),
            Arguments.of(
                EntityWithCompositeKeyRepository.class,
                ENTITIES_WITH_COMPOSITE_KEY,
                (Function<EntityWithCompositeKey, EntityWithCompositeKey>) entity -> entity.withData("changed"),
                COMPOSITE_KEY_CASE
            )
        );
    }

    @MethodSource("updateParameters")
    @ParameterizedTest(name = "{3}")
    @DisplayName("Verify that built-in repository 'update' method applies changes and returns previous value")
    <ID, E, R extends CrudRepository<ID, E>> void updateTest(Class<R> repositoryClass, List<E> entitiesData, Function<E, E> updater, String name,
                                                             @InitDb(migration = MIGRATIONS, name = "update_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(repositoryClass);

        val entity = repo.insert(entitiesData.get(0));
        val changed = updater.apply(entity);
        val previous = repo.update(changed);

        assertThat(repo.find(idOf(entity)))
            .contains(changed);
        assertThat(previous)
            .contains(entity);
    }

    @MethodSource("updateParameters")
    @ParameterizedTest(name = "{3}")
    @DisplayName("Verify that built-in repository 'updateAll' method applies changes and returns previous values")
    <ID, E, R extends CrudRepository<ID, E>> void updateAllTest(Class<R> repositoryClass, List<E> entitiesData,
                                                                Function<E, E> updater, String name,
                                                                @InitDb(migration = MIGRATIONS, name = "update_all_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(repositoryClass);

        val entities = repo.insertAll(entitiesData);
        @SuppressWarnings("unchecked")
        val ids = StreamEx.of(entities)
            .map(entity -> (ID) idOf(entity))
            .toImmutableList();
        val changedEntities = StreamEx.of(entities)
            .map(updater)
            .toImmutableList();
        val previousEntities = repo.updateAll(changedEntities);

        assertThat(repo.findAll(ids))
            .containsExactlyInAnyOrderElementsOf(changedEntities);
        assertThat(previousEntities)
            .containsExactlyInAnyOrderElementsOf(entities);
    }

    @Test
    @DisplayName("Verify that the one-to-many result set could be mapped onto OneToMany")
    void oneToManyTest(@InitDb(migration = MIGRATIONS, name = "one_to_many_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(EntityRepository.class);

        val entities = repo.insertAll(ENTITIES);
        val expectedEntitiesNames = Set.of("one", "two");
        val expectedEntities = StreamEx.of(entities)
            .filter(entity -> expectedEntitiesNames.contains(entity.getName()))
            .toImmutableList();

        val numbers = List.of(1L, 2L, 3L);
        val oneToManyMap = repo.selectOneToMany(numbers, expectedEntitiesNames).getMapping();
        assertThat(oneToManyMap)
            .hasSize(3)
            .containsOnlyKeys(numbers)
            .allSatisfy((num, values) -> {
                assertThat(values)
                    .containsExactlyInAnyOrder(expectedEntities.toArray(Entity[]::new));
            });
    }

    @Test
    @DisplayName("Verify that @BindBeanValues correctly bind bean arguments")
    void bindBeanValuesTest(@InitDb(migration = MIGRATIONS, name = "bind_bean_values_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(EntityRepository.class);

        val result = repo.decayBeans(List.of(
            new TestBean(0L, "zero"),
            new TestBean(42L, "non-zero")
        ));

        assertThat(result)
            .containsExactly(
                entry(0L, "zero"),
                entry(42L, "non-zero")
            );
    }

    @Test
    @DisplayName("Verify that wrapper types can be stored and retrieved from database")
    void wrapperTypesTest(@InitDb(migration = MIGRATIONS, name = "wrapper_types_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(EntityRepository.class);

        val uuid = UUID.randomUUID();
        val keys = repo.insertKeys(new LongKey(1L), new UUIDKey(uuid));
        assertThat(keys)
            .isEqualTo(new Keys(new LongKey(1L), new UUIDKey(uuid)));
    }

    private static Stream<Arguments> serialParameters() {
        return Stream.of(
            Arguments.of(
                EntityRepository.class,
                mapToList(ENTITIES, e -> e.withId(null)),
                SIMPLE_CASE
            ),
            Arguments.of(
                EntityWithCompositeKeyRepository.class,
                mapToList(ENTITIES_WITH_COMPOSITE_KEY, e -> e.withId(null)),
                COMPOSITE_KEY_CASE
            )
        );
    }

    @MethodSource("serialParameters")
    @ParameterizedTest(name = "{2}")
    @DisplayName("Verify that built-in repository 'insert*' methods uses sequence for the serial columns bounded with null")
    <ID, E, R extends CrudRepository<ID, E>> void serialTest(Class<R> repositoryClass, List<E> entitiesData, String name,
                                                             @InitDb(migration = MIGRATIONS, name = "update_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(repositoryClass);

        val entities = StreamEx.of(entitiesData)
            .headTail((head, tail) -> {
                return StreamEx.of(repo.insert(head))
                    .append(repo.insertAll(tail.toImmutableList()));
            })
            .toImmutableList();

        assertThat(repo.findAll())
            .containsExactlyInAnyOrderElementsOf(entities);
    }

    @Test
    void genericJsonBTest(@InitDb(migration = MIGRATIONS, name = "generic_jsonb_test") Database database) {
        val factory = initRepositoryFactory(database);
        val repo = factory.createRepository(EntityRepository.class);

        val entity = new Entity(
            new TypeSafeId(42L),
            "name",
            OptionalInt.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("{\"field\": \"some text\"}"),
            new NestedData("str", 0L)
        );

        repo.insert(entity);
        assertThat(repo.count())
            .isEqualTo(1);

        val text = repo.findGenericJsonBinaryDataText("'field'", entity.getId().getValue());
        assertThat(text)
            .isEqualTo("some text");
    }
}
