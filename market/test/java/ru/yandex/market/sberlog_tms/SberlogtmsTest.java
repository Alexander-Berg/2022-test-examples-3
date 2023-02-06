package ru.yandex.market.sberlog_tms;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 22/05/2018
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
public class SberlogtmsTest {

    @Test
    @DisplayName("just test as is test")
    public void test() {
        Assertions.assertTrue(true);
    }

}
