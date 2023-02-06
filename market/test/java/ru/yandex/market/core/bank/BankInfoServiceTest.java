package ru.yandex.market.core.bank;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.bank.model.BankInfo;

class BankInfoServiceTest extends FunctionalTest {

    @Autowired
    private BankInfoService bankInfoService;

    @Test
    @DisplayName("Банк поменял БИК")
    @DbUnitDataSet(
            before = "csv/BankInfoService.bank_changed_bic.before.csv",
            after = "csv/BankInfoService.bank_changed_bic.after.csv"
    )
    void testBankChangedBic() {
        final BankInfo BankInfo = new BankInfo(
                null,
                "049999999",
                "bank",
                "place",
                false,
                false);
        doUpdate(BankInfo);
    }

    @Test
    @DisplayName("Информация из ручки о закрытых банках не учитывается")
    @DbUnitDataSet(
            before = "csv/BankInfoService.archived_info_is_ignored.before.csv",
            after = "csv/BankInfoService.archived_info_is_ignored.after.csv"
    )
    void testArchivedInfoIsIgnored() {
        final BankInfo BankInfo1 = new BankInfo(
                null,
                "041111111",
                "bank1",
                "place1",
                false,
                true);
        final BankInfo BankInfo2 = new BankInfo(
                null,
                "042222222",
                "bank2",
                "place2",
                true,
                true);

        // информация по активному банку актуальна
        final BankInfo BankInfo3 = new BankInfo(
                null, "042345678",
                "bank1",
                "place1",
                false,
                false);

        doUpdate(BankInfo1, BankInfo2, BankInfo3);
    }

    @Test
    @DisplayName("Активный банк архивируется, если по нему не пришла информация")
    @DbUnitDataSet(
            before = "csv/BankInfoService.info_archived_if_not_received.before.csv",
            after = "csv/BankInfoService.info_archived_if_not_received.after.csv"
    )
    void testInfoArchivedIfNotReceived() {
        final BankInfo BankInfo1 = new BankInfo(
                null,
                "041234567",
                "bank1",
                "place1",
                false,
                false);

        doUpdate(BankInfo1);
    }

    @Test
    @DisplayName("Добавляется новая запись, если пришел новый активный банк")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.inserted_if_new_active.before.csv",
            after = "csv/BankInfoDao.inserted_if_new_active.after.csv"
    )
    void testInsertedIfNewActive() {
        final BankInfo BankInfo1 = new BankInfo(null, "041234567", "bank1", "place1", false, false);

        doUpdate(BankInfo1);
    }

    @Test
    @DisplayName("Обновляется существующая запись, если пришел существующий активный банк")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.inserted_if_present_active.before.csv",
            after = "csv/BankInfoDao.inserted_if_present_active.after.csv"
    )
    void testUpdatedIfPresentActive() {
        final BankInfo BankInfo2 = new BankInfo(
                null,
                "042345678",
                "new_bank2",
                "new_place2",
                false,
                false);
        doUpdate(BankInfo2);
    }

    @Test
    @DisplayName("Все возможные случаи сразу - архивация, обновление, удаление")
    @DbUnitDataSet(
            before = "csv/BankInfoService.all_possible_cases.before.csv",
            after = "csv/BankInfoService.all_possible_cases.after.csv"
    )
    void testAllPossibleCases() {
        final BankInfo BankInfo2 = new BankInfo(
                null,
                "042222222",
                "new_name2",
                "new_place2",
                false,
                false);
        final BankInfo BankInfo3 = new BankInfo(
                null,
                "043333333",
                "new_name3",
                "new_place3",
                false,
                false);
        doUpdate(BankInfo2, BankInfo3);
    }

    private void doUpdate(final BankInfo... bankInfos) {
        bankInfoService.updateBankInfos(Arrays.asList(bankInfos));
    }
}
