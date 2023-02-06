package ru.yandex.market.pvz.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import ru.yandex.market.pvz.core.test.ResetCachesAfterBeforeEachExtension;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.TYPE)

@ExtendWith(ResetCachesAfterBeforeEachExtension.class)
@PvzIntTest
@TransactionlessEmbeddedDbTest
@AutoConfigureMockMvc(secure = false)
@ImportAutoConfiguration({ValidationAutoConfiguration.class})
public @interface WebLayerTest {
}
