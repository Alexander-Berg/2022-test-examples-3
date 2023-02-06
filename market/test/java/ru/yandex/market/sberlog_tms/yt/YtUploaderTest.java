package ru.yandex.market.sberlog_tms.yt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.market.sberlog_tms.SberlogtmsConfig;
import ru.yandex.market.sberlog_tms.dao.model.UserInfoModel;

import java.util.Collections;
import java.util.List;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 29.10.19
 */
@Disabled
@SpringJUnitConfig(SberlogtmsConfig.class)
public class YtUploaderTest {
    @Value("${sberlogtms.scheduled.uploadusertoyt.duration.minutes}")
    private long durationMinutes; // на сколько минут брать транзакцию

    @Autowired
    private YtUploader ytUploader;

    private List<UserInfoModel> userInfoModelList;


    @BeforeEach
    public void YtUploaderTestInitial() {
        this.userInfoModelList = Collections.singletonList(
                new UserInfoModel("2190550858953009286",
                        "300003",
                        "Василий",
                        "Пупкин",
                        "Васильевич",
                        1,
                        "2009-12-30",
                        new String[]{"81233213344", "+79095612567"},
                        new String[]{"user@yandex.ru", "user@ya.ru"},
                        new String[][]{{"[\"home\",\"Россия г. Москва ул. Строителей 1, подъезд 1, кв. 1\",]"}},
                        0));
    }

    @Test
    void uploadAllSberlogUserAndCheckTransactionCheckPathCommit() {
        String path = "uploadAllSberlogUserTest";

        Transaction transaction = ytUploader.getTransaction(durationMinutes);
        ytUploader.uploadAllSberlogUser(transaction, path, userInfoModelList);
        ytUploader.commit(transaction);

        Transaction transactionForCheckPathAfter = ytUploader.getTransaction(durationMinutes);
        Assertions.assertTrue(ytUploader.checkPath(transactionForCheckPathAfter, path));
        ytUploader.commit(transactionForCheckPathAfter);
    }


    @Test
    void moveAndRemovePath() {
        String path = "moveTest";
        String pathOld = "moveTest.old";

        Transaction transaction = ytUploader.getTransaction(durationMinutes);
        ytUploader.uploadAllSberlogUser(transaction, path, userInfoModelList);
        ytUploader.commit(transaction);

        Transaction transactionForMove = ytUploader.getTransaction(durationMinutes);
        ytUploader.move(transactionForMove, path, pathOld);
        ytUploader.commit(transactionForMove);

        Transaction transactionForCheckPathAfter = ytUploader.getTransaction(durationMinutes);
        Assertions.assertTrue(ytUploader.checkPath(transactionForCheckPathAfter, pathOld));
        ytUploader.commit(transactionForCheckPathAfter);

        Transaction transactionForRemove = ytUploader.getTransaction(durationMinutes);
        ytUploader.removePath(transactionForRemove, pathOld);
        ytUploader.commit(transactionForRemove);

        Transaction transactionForCheckPathAfterRemove = ytUploader.getTransaction(durationMinutes);
        Assertions.assertFalse(ytUploader.checkPath(transactionForCheckPathAfterRemove, pathOld));
        ytUploader.commit(transactionForCheckPathAfterRemove);
    }
}
