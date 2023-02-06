package ru.yandex.travel.hibernate.types;

import lombok.Data;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import ru.yandex.travel.test.fake.proto.TTestMethodReq;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtobufMessageTypeTest extends BaseCustomTypeTest {
    @Override
    protected List<Class> getAnnotatedClasses() {
        return Collections.singletonList(TestEntity.class);
    }

    @Test
    public void testCorrectWorkOfType() {
        TTestMethodReq testMessage = TTestMethodReq.newBuilder()
                .setTestValue("testValue")
                .build();

        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            TestEntity testEntity = new TestEntity();
            testEntity.setId(1L);
            testEntity.setProtobufMessage(testMessage);
            session.persist(testEntity);
            assertThat(testEntity.getId()).isNotNull();
            session.flush();
            session.clear();

            testEntity = session.get(TestEntity.class, testEntity.getId());

            assertThat(testEntity.getProtobufMessage()).isInstanceOf(TTestMethodReq.class);
            assertThat(((TTestMethodReq) testEntity.getProtobufMessage()).getTestValue()).isEqualTo("testValue");
            assertThat(testEntity.getProtobufMessage()).isEqualTo(testMessage);
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
            name = "protobuf-message",
            typeClass = ProtobufMessageType.class
    )
    @Data
    public static class TestEntity {

        @Id
        private Long id;

        @Type(type = "protobuf-message")
        @Columns(columns = {
                @Column(name = "class_name"), @Column(name = "data")
        })
        private Object protobufMessage;
    }


}
