package ru.yandex.market.logistics.lom.service.redis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Работа с redis")
class RedisServiceImplTest extends AbstractRedisTest {

    private static final String HASH_TABLE_NAME = "test-hash-table";

    @Autowired
    private RedisService redisService;

    @Test
    @DisplayName("Вызовов в redis не происходит, если список сущностей пустой")
    void noRedisCallingWhenNoEntities() {
        redisService.saveEntitiesByIds(
            HASH_TABLE_NAME,
            Map.of()
        );
    }

    @Test
    @DisplayName("Успешное сохранение сущностей")
    void saveEntitiesByKey() {
        redisService.saveEntitiesByIds(
            HASH_TABLE_NAME,
            entitiesMap()
        );

        verify(migrationJedis).hmset(HASH_TABLE_NAME, entitiesMap());
    }

    @Test
    @DisplayName("Получение количества ключей в хэш таблице")
    void keysAmount() {
        long keysAmount = 10;
        String tableName = "table-name";
        doReturn(keysAmount)
            .when(migrationJedis).hlen(tableName);

        softly.assertThat(redisService.getKeysAmount(tableName)).isEqualTo(keysAmount);

        verify(migrationJedis).hlen(tableName);
    }

    @Test
    @DisplayName("Получение сущности из хэш таблицы по идентификатору: сущность существует")
    void getEntityByIdEntityExists() {
        String tableName = "table-name";
        String id = "1";
        doReturn(entitiesMap().get(id))
            .when(clientJedis).hget(tableName, id);

        softly.assertThat(
            redisService.getFromHashTableById(
                tableName,
                TestEntity.class,
                id
            )
        )
            .isEqualTo(entities().get(0));

        verify(clientJedis).hget(tableName, id);
    }

    @SneakyThrows
    @Test
    @DisplayName("Получение сущности из хэш таблицы по идентификатору: сущность не существует")
    void getEntityByIdEntityNotExists() {
        String tableName = "table-name";
        String id = "123";
        doReturn(null).when(clientJedis).hget(tableName, id);

        softly.assertThat(
            redisService.getFromHashTableById(
                tableName,
                TestEntity.class,
                id
            )
        )
            .isNull();

        verify(clientJedis).hget(tableName, id);
    }

    @Test
    @DisplayName("Удаление ключа")
    void removeKey() {
        redisService.removeKey(HASH_TABLE_NAME);
        verify(migrationJedis).del(HASH_TABLE_NAME);
    }

    @Test
    @DisplayName("Добавление значения по ключу")
    void setValue() {
        redisService.setValue("key", "value");
        verify(migrationJedis).set("key", "value");
    }

    @Test
    @DisplayName("Получение значений происходит хэшировано")
    void getValueCacheable() {
        doReturn("value").when(clientJedis).get("key");

        for (int i = 0; i < 100; i++) {
            softly.assertThat(redisService.getCachedValue("key")).isEqualTo("value");
        }

        verify(clientJedis).get("key");
    }

    @Test
    @DisplayName("Получение значений не из кэша")
    void getValue() {
        doReturn("value").when(migrationJedis).get("key");

        for (int i = 0; i < 100; i++) {
            softly.assertThat(redisService.getValue("key")).isEqualTo("value");
        }

        verify(migrationJedis, times(100)).get("key");
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Получение значений по ключам")
    @MethodSource("getValuesSource")
    void getValuesByIds(String name, List<String> expectedValues, List<TestEntity> expectedEntities) {
        String tableName = "table";
        Set<String> keys = entities().stream().map(TestEntity::getId).collect(Collectors.toSet());
        doReturn(expectedValues).when(clientJedis).hmget(eq(tableName), anyString(), anyString());

        Set<TestEntity> actualEntities = redisService.getFromHashTableByIds(tableName, TestEntity.class, keys);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(clientJedis).hmget(eq(tableName), captor.capture(), captor.capture());
        softly.assertThat(captor.getAllValues()).containsExactlyInAnyOrderElementsOf(keys);
        softly.assertThat(actualEntities).containsExactlyInAnyOrderElementsOf(expectedEntities);
    }

    @Test
    @DisplayName("Получение значений по пустому списку ключей")
    void getValuesByIdsEmpty() {
        redisService.getFromHashTableByIds("table", TestEntity.class, Set.of());
    }

    @Nonnull
    private static Stream<Arguments> getValuesSource() {
        List<String> listWithSomeNulls = new ArrayList<>();
        listWithSomeNulls.add(entitiesMap().get("1"));
        listWithSomeNulls.add(null);

        List<String> listWithAllNulls = new ArrayList<>();
        listWithSomeNulls.add(null);
        listWithSomeNulls.add(null);

        return Stream.of(
            Arguments.of(
                "Все значения найдены",
                List.copyOf(entitiesMap().values()),
                entities()
            ),
            Arguments.of(
                "Некоторые значения не найдены",
                listWithSomeNulls,
                List.of(entities().get(0))
            ),
            Arguments.of(
                "Ни одного значения не найдено",
                listWithAllNulls,
                Collections.emptyList()
            )
        );
    }

    @Nonnull
    private static List<TestEntity> entities() {
        return List.of(
            new TestEntity("1", "name1"),
            new TestEntity("2", "name2")
        );
    }

    @Nonnull
    private static Map<String, String> entitiesMap() {
        return StreamEx.of(entities())
            .mapToEntry(
                TestEntity::getId,
                value -> String.format(
                    "{\"id\": \"%s\", \"name\": \"%s\"}",
                    value.getId(),
                    value.getName()
                )
            )
            .toMap();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class TestEntity implements Serializable {
        private String id;

        private String name;
    }
}
