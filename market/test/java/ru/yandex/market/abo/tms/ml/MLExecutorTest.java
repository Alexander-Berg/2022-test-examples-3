package ru.yandex.market.abo.tms.ml;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author imelnikov
 */
public class MLExecutorTest extends EmptyTest {

    @Autowired
    private MLExecutor executor;

    @Test
    public void run() {
        executor.processShop(1L, 720L);
    }
}
