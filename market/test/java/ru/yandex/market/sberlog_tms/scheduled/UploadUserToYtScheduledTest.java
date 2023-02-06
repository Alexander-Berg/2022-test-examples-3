package ru.yandex.market.sberlog_tms.scheduled;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;
import ru.yandex.market.sberlog_tms.dao.SberlogDbDao;
import ru.yandex.market.sberlog_tms.lock.LockService;
import ru.yandex.market.sberlog_tms.yt.YtUploader;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 28.10.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
public class UploadUserToYtScheduledTest {
    @Value("${sberlogtms.scheduled.uploadusertoyt.duration.minutes}")
    private long durationMinutes; // на сколько минут брать транзакцию

    @Value("${sberlogtms.scheduled.uploadusertoyt.ttl}")
    private long ttlSeconds; // ттл в секундах на время последнего запуска

    @Autowired
    private LockService lockService;
    @Autowired
    private SberlogDbDao sberlogDbDao;
    @Autowired
    private YtUploader ytUploader;

    @BeforeEach
    public void UploadUserToYtScheduledInitial() throws InterruptedException {
        UploadUserToYtScheduled uploadUserToYtScheduled
                = new UploadUserToYtScheduled(lockService, sberlogDbDao, ytUploader);

        uploadUserToYtScheduled.setDurationMinutes(durationMinutes);
        uploadUserToYtScheduled.setTtlSeconds(ttlSeconds);

        // вызываем дважды, что бы создалась таблица .old
        uploadUserToYtScheduled.UploadUserToYt();
        Thread.sleep(ttlSeconds * 1000 * 2);
        uploadUserToYtScheduled.UploadUserToYt();
    }

    @Test
    @DisplayName("happy path: how we can get user from PG to YT")
    public void UploadUserToYt() {
        final String ypCypress = "sberlogtms_task";
        final String ypTable = "sberlog_all_user";

        Assertions.assertTrue(lockService.checkPath(ypTable));
        Assertions.assertTrue(lockService.checkPath(ypCypress));

        Assertions.assertTrue(lockService.checkPath(ypTable + ".old"));
    }

}
