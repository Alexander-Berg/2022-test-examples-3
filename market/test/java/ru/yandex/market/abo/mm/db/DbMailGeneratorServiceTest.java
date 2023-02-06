package ru.yandex.market.abo.mm.db;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author frenki
 * created on 09.08.2017.
 */
public class DbMailGeneratorServiceTest extends EmptyTest {
    private static final int USER_ID = 111;

    @Autowired
    private DbMailGeneratorService dbMailGeneratorService;

    @Test
    public void testSaveGenIfNeed() {
        final long startTime = System.currentTimeMillis();
        dbMailGeneratorService.saveGeneratorIfNeed(USER_ID);
        System.out.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
    }
}
