package ru.yandex.travel.tx.utils;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.tx.utils.entities.TestObj;
import ru.yandex.travel.tx.utils.repository.TestObjRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SavepointHelperTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private TestObjRepository testObjRepository;

    @Autowired
    private SavepointHelper savepointHelper;

    @Test
    public void testEntitiesSavedAfterSavepoint() {
        UUID notSaved = UUID.randomUUID();
        UUID saved = UUID.randomUUID();
        transactionTemplate.execute(ignored -> {
            savepointHelper.execute(
                    () -> {
                        createAndSaveEntity(notSaved, "new");
                        return null;
                    }, notUsed -> true
            );
            createAndSaveEntity(saved, "new");
            return null;
        });

        AtomicBoolean existsNotSaved = new AtomicBoolean(false);
        AtomicBoolean existsSaved = new AtomicBoolean(false);

        transactionTemplate.execute(ignored -> {
            existsNotSaved.set(testObjRepository.existsById(notSaved));
            existsSaved.set(testObjRepository.existsById(saved));
            return null;
        });
        assertThat(existsNotSaved.get()).isFalse();
        assertThat(existsSaved.get()).isTrue();
    }


    private void createAndSaveEntity(UUID id, String state) {
        TestObj testObj = new TestObj();
        testObj.setId(id);
        testObj.setState(state);
        testObjRepository.saveAndFlush(testObj);
    }

    @TestConfiguration
    public static class Configuration {
        @Bean
        public SavepointHelper savepointHelper(JpaTransactionManager jpaTransactionManager,
                                               EntityManager entityManager) {
            return new SavepointHelper(jpaTransactionManager, entityManager);
        }
    }
}
