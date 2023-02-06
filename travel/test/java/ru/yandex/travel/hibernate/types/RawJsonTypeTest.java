package ru.yandex.travel.hibernate.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RawJsonTypeTest extends BaseCustomTypeTest {

    @Override
    protected List<Class> getAnnotatedClasses() {
        return Arrays.asList(TestEntity.class);
    }

    @Test
    public void testCorrectWorkOfType() {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode testData = objectMapper.createObjectNode();

        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            TestEntity entity = new TestEntity();
            entity.setId(1L);
            entity.setData(testData);
            session.persist(entity);
            assertThat(entity.getId()).isNotNull();
            session.flush();
            session.clear();

            entity = session.get(TestEntity.class, entity.getId());

            assertThat(entity.getData()).isEqualTo(testData);
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
            name = "raw-json",
            typeClass = RawJsonTypes.Jsonb.class
    )
    @Data
    public static class TestEntity {

        @Id
        private Long id;

        @Column(name = "data")
        @Type(type = "raw-json")
        private JsonNode data;
    }


}
