package ru.yandex.market.contact.sync;

import com.google.common.collect.Lists;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.EmailInfo;
import ru.yandex.market.core.passport.model.UserInfo;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.when;

/**
 * @author otedikova
 */
@DbUnitDataBaseConfig({@DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true")})
class SyncPassportContactsJobTest extends FunctionalTest {
    @Autowired
    @Qualifier("syncPassportContactsExecutor")
    private SyncPassportContactsJob syncPassportContactsJob;

    @Autowired
    @Qualifier("balanceService")
    private BalanceService balanceService;

    @Autowired
    private PassportService passportService;


    /**
     * Проверяет обновление контактов из паспорта, и что в shops_web.entity_history добавляются записи только
     * для изменившихся контактов.
     */
    @Test
    @DbUnitDataSet(before = "syncPassportContactsJob.contact.history.before.csv",
            after = "syncPassportContactsJob.contact.history.after.csv")
    void testContactHistoryChange() {
        when(balanceService.getClient(1)).thenReturn(new ClientInfo(1, ClientType.OAO));
        when(balanceService.getClient(2)).thenReturn(new ClientInfo(2, ClientType.OAO));

        //у контакта 1 появилось 2е имя и обновился логин
        mockPassportUserParams("Иванов Иван Иваныч", "petrov", 111);
        //у контакта 2 поменялась фамилия
        mockPassportUserParams("Фернандез Хуан Антонио", "hafernandez", 222);
        //у контакта 3 параметры не поменялись
        mockPassportUserParams("Семенов Семен", "ssemenov", 333);
        //у контакта 4 поменялись параметры, но логин пустой, все равно обновляем
        mockPassportUserParams("Михайлов Михон", "", 444);

        //у контакта 3 удаляется емэйл
        when(passportService.getEmails(111)).thenReturn(Lists.newArrayList(new EmailInfo("iivanov@yandex.ru", false)));
        when(passportService.getEmails(222)).thenReturn(Lists.newArrayList(new EmailInfo("hafernandez@yandex.ru", false)));
        when(passportService.getEmails(333)).thenReturn(Lists.newArrayList(new EmailInfo("ssemenov@yandex.ru", false)));
        when(passportService.getEmails(444)).thenReturn(Lists.newArrayList(new EmailInfo("mmichailov@mail.ru", false)));
        syncPassportContactsJob.doJob(null);
    }

    private void mockPassportUserParams(String fio, String login, long uid) {
        when(passportService.getUserInfo(uid)).thenReturn(new UserInfo(uid, fio, null, login));
    }

    @Test
    @DbUnitDataSet(before = "syncPassportContactsJob.contact.active.before.csv",
            after = "syncPassportContactsJob.contact.active.after.csv")
    void testEmailActiveChange() {
        when(balanceService.getClient(1)).thenReturn(new ClientInfo(1, ClientType.OAO));
        when(balanceService.getClient(2)).thenReturn(new ClientInfo(2, ClientType.OAO));


        mockPassportUserParams("Иванов Иван", "iivanov", 111);
        mockPassportUserParams("Фернандес Хуан Антонио", "hafernandez", 222);
        mockPassportUserParams("Семенов Семен", "ssemenov", 333);
        mockPassportUserParams("Михайлов Михаил", "mmichailov", 444);

        //у 1 контакта емэйл станет активным из за постфикса @yandex.ru
        when(passportService.getEmails(111)).thenReturn(Lists.newArrayList(new EmailInfo("iivanov@yandex.ru", false)));
        //у 2 контакта уже был активный емэйл так что этот емэйл не станет активным
        when(passportService.getEmails(222)).thenReturn(Lists.newArrayList(new EmailInfo("hafernandez@yandex.ru", true)));
        //у 3 контакта нет емэйла с постфиксом @yandex.ru и активных емэйлов, так что этот емэйл станет активным
        when(passportService.getEmails(333)).thenReturn(Lists.newArrayList(new EmailInfo("mmichailov@mail.ru", true)));
        //у 4 контакта нет активных емэйлов
        when(passportService.getEmails(444)).thenReturn(Lists.newArrayList(new EmailInfo("ssemenov@mail.ru", false)));
        syncPassportContactsJob.doJob(null);
    }
}
