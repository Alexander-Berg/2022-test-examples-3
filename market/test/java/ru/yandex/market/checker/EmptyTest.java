package ru.yandex.market.checker;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author kukabara
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:test-bean.xml")
@Transactional
public abstract class EmptyTest {

    static {
        System.setProperty("environment", "development");
    }

}
