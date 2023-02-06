package ru.yandex.market.sberlog_tms.lock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 29.10.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
public class LockServiceTest {
    @Value("${sberlogtms.scheduled.uploadusertoyt.duration.minutes}")
    private long durationMinutes; // на сколько минут брать транзакцию

    @Autowired
    private LockService lockService;

    private final String description = "some text for example";

    @Test
    void lockUnlock() {
        String path = "locktest";

        Transaction transactionLock = lockService.lock(path, durationMinutes, description);
        Assertions.assertNotNull(transactionLock);

        Transaction secondTransactionLock = lockService.lock(path, durationMinutes, description);
        Assertions.assertNull(secondTransactionLock);

        lockService.unlock(transactionLock);

        Transaction thirdTransactionLock = lockService.lock(path, durationMinutes, description);
        Assertions.assertNotNull(thirdTransactionLock);
        lockService.unlock(thirdTransactionLock);
    }

    @Test
    void checkPath() {
        String path = "checkpathtest";

        Transaction transactionLock = lockService.lock(path, durationMinutes, description);
        Assertions.assertTrue(lockService.checkPath(path));
        lockService.unlock(transactionLock);
    }

    @Test
    void removePath() {
        String path = "checkremovetest";

        Transaction transactionLock = lockService.lock(path, durationMinutes, description);
        Assertions.assertTrue(lockService.checkPath(path));
        lockService.removePath(transactionLock, path);
        lockService.unlock(transactionLock);
        Assertions.assertFalse(lockService.checkPath(path));
    }

    @Test
    void getNodeInfo() {
        String path = "getnodeinfotest";

        Transaction transactionLock = lockService.lock(path, durationMinutes, description);
        lockService.unlock(transactionLock);

        Assertions.assertEquals(description, lockService.getNodeInfo(path));
    }
}
