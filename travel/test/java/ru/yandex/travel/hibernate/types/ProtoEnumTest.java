package ru.yandex.travel.hibernate.types;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import ru.yandex.travel.test.fake.proto.ETestEnum;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtoEnumTest extends BaseCustomTypeTest {

    @Test
    public void testEnumField() {
        TestEntityWithCustomType testEntity1 = new TestEntityWithCustomType();
        testEntity1.setId(0L);
        testEntity1.setData(ETestEnum.TE_Foo);

        TestEntityWithoutCustomType testEntity2 = new TestEntityWithoutCustomType();
        testEntity2.setId(0L);
        testEntity2.setData(ETestEnum.TE_Foo);

        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            session.persist(testEntity1);
            session.persist(testEntity2);
            session.flush();
            session.clear();
            // querying by the protobuf number - 4
            var count_with_type = session.createQuery("select count(te) from test_with_type te where te.data = 4").uniqueResult();
            // querying by the order in enum - 1
            var count_without_type = session.createQuery("select count(te) from test_without_type te where te.data = 1").uniqueResult();
            assertThat(count_with_type).isEqualTo(1L);
            assertThat(count_without_type).isEqualTo(1L);
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


    @Override
    protected List<Class> getAnnotatedClasses() {
        return Arrays.asList(TestEntityWithCustomType.class, TestEntityWithoutCustomType.class);
    }

    @TypeDef(
            name = "proto-enum",
            typeClass = ProtobufEnumType.class
    )
    @Data
    @Entity(name = "test_with_type")
    public static class TestEntityWithCustomType {

        @Id
        private Long id;

        @Column(name = "data")
        @Type(type = "proto-enum")
        private ETestEnum data;
    }

    @Data
    @Entity(name = "test_without_type")
    public static class TestEntityWithoutCustomType {

        @Id
        private Long id;

        @Column(name = "data")
        private ETestEnum data;
    }
}
