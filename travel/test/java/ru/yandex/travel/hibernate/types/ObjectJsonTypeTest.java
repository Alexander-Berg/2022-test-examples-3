package ru.yandex.travel.hibernate.types;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.javamoney.moneta.Money;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectJsonTypeTest extends BaseCustomTypeTest {

    @Override
    protected List<Class> getAnnotatedClasses() {
        return Arrays.asList(TestEntity.class);
    }

    @Test
    public void testCorrectWorkOfType() {
        TestObject testObject = new TestObject();
        testObject.setField1("value1");
        testObject.setField2(2L);

        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();

            Map<Long, Money> testMap = Map.of(1L, Money.of(BigDecimal.valueOf(2.0), "RUB"));

            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setContent(testObject);
            entity.setMap(testMap);
            session.persist(entity);
            assertThat(entity.getId()).isNotNull();
            session.flush();
            session.clear();

            entity = session.get(TestEntity.class, entity.getId());

            assertThat(entity.getContent()).isEqualTo(testObject);
            assertThat(entity.getMap()).isEqualTo(testMap);
        } finally {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception e) {
                }
            }
            if (session != null) {
                session.close();
            }
        }
    }

    @Entity(name = "test_entity")
    @TypeDef(
            name = "json-object",
            typeClass = ObjectJsonType.class
    )
    @Data
    public static class TestEntity {

        @Id
        private Long id;

        @Column(name = "object_data")
        @Type(type = "json-object")
        private TestObject content;

        @Column(name = "generic_data")
        @Type(type = "json-object")
        private Map<Long, Money> map;
    }


    @Data
    private static final class TestObject {
        private String field1;
        private Long field2;
    }
}
