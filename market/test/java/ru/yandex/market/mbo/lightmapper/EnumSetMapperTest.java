package ru.yandex.market.mbo.lightmapper;

import java.util.EnumSet;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.mbo.lightmapper.config.BaseDbTestClass;

import static ru.yandex.market.mbo.lightmapper.CompositeGenericMapper.PRIMARY_KEY;

class EnumSetMapperTest extends BaseDbTestClass {
    @BeforeEach
    public void setUp() {
        jdbcTemplate.update("create type etype as enum('FIRST', 'SECOND');");
        jdbcTemplate.update("create table test(key text primary key, enums etype[]);");
    }

    @AfterEach
    public void tearDown() {
        jdbcTemplate.update("drop table if exists test;");
        jdbcTemplate.update("drop type if exists etype;");
    }

    @Test
    public void mapperWorks() {
        Repo repo = new Repo(namedJdbcTemplate, transactionTemplate);

        repo.insert(new Entity("key1", EnumSet.of(EnumTest.FIRST)));
        Assertions.assertEquals(repo.findById("key1").enums, EnumSet.of(EnumTest.FIRST));

        repo.update(new Entity("key1", EnumSet.of(EnumTest.SECOND)));
        Assertions.assertEquals(repo.findById("key1").enums, EnumSet.of(EnumTest.SECOND));

        repo.update(new Entity("key1", EnumSet.noneOf(EnumTest.class)));
        Assertions.assertEquals(repo.findById("key1").enums, EnumSet.noneOf(EnumTest.class));

        repo.delete(Lists.newArrayList("key1"));
    }

    private enum EnumTest {
        FIRST, SECOND
    }

    private static class Entity {
        private String key;
        private EnumSet<EnumTest> enums;

        Entity() {

        }

        Entity(String key, EnumSet<EnumTest> enums) {
            this.key = key;
            this.enums = enums;
        }

        String getKey() {
            return key;
        }

        void setKey(String key) {
            this.key = key;
        }

        EnumSet<EnumTest> getEnums() {
            return enums;
        }

        void setEnums(EnumSet<EnumTest> enums) {
            this.enums = enums;
        }

        static CompositeMapper<Entity> createMapping() {
            return MapperBuilder.start(Entity::new)
                    .map("key", Entity::getKey, Entity::setKey)
                    .mark(PRIMARY_KEY)
                    .map("enums", Entity::getEnums, Entity::setEnums, new EnumSetMapper<>(EnumTest.class, "etype"))
                    .build();
        }
    }

    private static class Repo extends GenericMapperRepositoryImpl<Entity, String> {
        Repo(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
            super(Entity.createMapping(), jdbcTemplate, transactionTemplate, "test");
        }
    }
}
