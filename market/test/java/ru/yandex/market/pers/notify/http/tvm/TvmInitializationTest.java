package ru.yandex.market.pers.notify.http.tvm;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.passport.tvmauth.exception.NonRetriableException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestTvmClientConfig.class)
@TestPropertySource(properties = {
    "pers.notify.tvm.id=2011932", // dev
    "pers.notify.tvm.secret=FAKE_SECRET",
    "pers.notify.sberlogAppId=2011276",
    "pers.notify.passportAppId=224"
})
@ActiveProfiles("production")
class TvmInitializationTest {

    @Autowired
    private BeanFactory beanFactory;

    @Test
    @Disabled
    void testInit() {
        // Проверяем, что клиент нормально создаётся, и нативные либы подгружаются
        try {
            final TvmClient tvmClient = beanFactory.getBean(TvmClient.class);// упадет здесь!
            fail("tvmClient shouldn't be created");
        } catch (Exception e) {
            if (e instanceof BeanCreationException) {
                Throwable cause = ((BeanCreationException) e).getRootCause();
                final String expected =
                    "Signature is bad: common reason is bad tvm_secret or tvm_id\\/tvm_secret mismatch.";
                // Доступа к паролю (TVM секрету) нет, поэтому без ошибок контекст поднять не получится
                System.out.println(e);
                assertTrue(cause instanceof NonRetriableException && cause.getMessage().contains(expected));
            } else {
                throw e;
            }
        }
    }
}
