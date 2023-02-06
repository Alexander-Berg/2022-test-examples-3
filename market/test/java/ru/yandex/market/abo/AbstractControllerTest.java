package ru.yandex.market.abo;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author artemmz
 * @date 22.03.18.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:mvc-test-bean.xml")
@ActiveProfiles("development")
public abstract class AbstractControllerTest {

    static {
        System.setProperty("host.name", "development");
    }
}
