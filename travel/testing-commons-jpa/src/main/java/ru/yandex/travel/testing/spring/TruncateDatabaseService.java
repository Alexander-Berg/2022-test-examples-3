package ru.yandex.travel.testing.spring;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.Metamodel;
import org.hibernate.Session;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.transaction.support.TransactionTemplate;

public class TruncateDatabaseService {

    private EntityManager entityManager;
    private TransactionTemplate transactionTemplate;

    public TruncateDatabaseService(TransactionTemplate transactionTemplate, EntityManager entityManager) {
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    public void truncate() {
        transactionTemplate.execute(notUsed -> {
            List<String> tableNames = new ArrayList<>();
            Session session = entityManager.unwrap(Session.class);
            Metamodel metamodel = session.getSessionFactory().getMetamodel();
            for (ManagedType mt : metamodel.getManagedTypes()) {
                Table annotation = AnnotationUtils.findAnnotation(mt.getJavaType(), Table.class);
                if (annotation != null) {
                    tableNames.add(annotation.name());
                }
            }

            entityManager.flush();
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            tableNames.forEach(tableName -> entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate());
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            return null;
        });
    }
}
