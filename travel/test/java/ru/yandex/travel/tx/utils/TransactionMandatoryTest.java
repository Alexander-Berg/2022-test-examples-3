package ru.yandex.travel.tx.utils;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class TransactionMandatoryTest {

    public static class TestService {
        @TransactionMandatory
        public int mustBeCallInTransaction() {
            return 42; // the answer
        }
    }

    @Autowired
    private TestService testService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void testExceptionThrownWhenCalledOutsideTransaction() {
        Assertions.assertThatCode(() -> {
            testService.mustBeCallInTransaction();
        }).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    public void testExceptionNotThrownWhenCallWithinTransaction() {
        Assertions.assertThatCode(() -> {
                    transactionTemplate.execute(ignored -> {
                        testService.mustBeCallInTransaction();
                        return null;
                    });
                }
        ).doesNotThrowAnyException();
    }


    @TestConfiguration
    public static class Configuration {
        @Bean
        TestService testService() {
            return new TestService();
        }
    }

}
