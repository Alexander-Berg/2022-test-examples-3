package ru.yandex.mail.cerberus.asyncdb;

import lombok.Value;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import ru.yandex.mail.cerberus.asyncdb.Entity.TypeSafeId;
import ru.yandex.mail.cerberus.asyncdb.annotations.BindBeanValues;
import ru.yandex.mail.cerberus.asyncdb.annotations.BindValues;
import ru.yandex.mail.cerberus.asyncdb.annotations.ConfigureCrudRepository;
import ru.yandex.mail.cerberus.asyncdb.util.OneToMany;
import ru.yandex.mail.micronaut.common.value.LongValueType;
import ru.yandex.mail.micronaut.common.value.ValueType;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ConfigureCrudRepository(table = "entities")
public interface EntityRepository extends CrudRepository<TypeSafeId, Entity> {
    @KeyColumn("number")
    @SqlQuery("SELECT * FROM (VALUES <numbers>) AS numbers (number)\n"
            + "NATURAL JOIN <table> tbl\n"
            + "WHERE tbl.name IN (<names>)")
    OneToMany<Long, Entity> selectOneToMany(@BindValues Iterable<Long> numbers, @BindList Set<String> names);

    @Value
    class TestBean {
        long index;
        String name;
    }

    @KeyColumn("index")
    @ValueColumn("name")
    @RegisterBeanMapper(TestBean.class)
    @SqlQuery("SELECT index, name FROM (VALUES <beans>) AS t (<beans_columns>)")
    Map<Long, String> decayBeans(@BindBeanValues Iterable<TestBean> beans);

    @Value
    class LongKey implements LongValueType {
        long value;
    }

    @Value
    class UUIDKey implements ValueType<UUID> {
        UUID value;
    }

    @Value
    class Keys {
        LongKey longKey;
        UUIDKey uuidKey;
    }

    @GetGeneratedKeys
    @RegisterConstructorMapper(Keys.class)
    @SqlUpdate("INSERT INTO keys (long_key, uuid_key)\n"
             + "VALUES (:longKey, :uuidKey)\n"
             + "RETURNING long_key, uuid_key")
    Keys insertKeys(LongKey longKey, UUIDKey uuidKey);

    @SqlQuery("SELECT CAST(generic_json_binary_data ->> <jsonColumn> AS TEXT) AS txt FROM <table>\n"
            + "WHERE id = :id")
    String findGenericJsonBinaryDataText(@Define String jsonColumn, long id);
}
