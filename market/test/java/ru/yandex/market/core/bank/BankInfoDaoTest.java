package ru.yandex.market.core.bank;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.bank.model.BankInfo;

class BankInfoDaoTest extends FunctionalTest {

    @Autowired
    BankInfoDao bankInfoDao;

    @Test
    @DisplayName("Архивируются только запрошенные")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.archived_requested_only.before.csv",
            after = "csv/BankInfoDao.archived_requested_only.after.csv"
    )
    void testArchivedRequestedOnly() {
        doArchive("042345678", "043456789");
    }

    @Test
    void testArchiveWithNoBics() {
        Assertions.assertDoesNotThrow(() -> doArchive());
    }

    @Test
    @DisplayName("Уже есть заархивированный банк с тем же БИК")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.same_bic_already_archived.before.csv",
            after = "csv/BankInfoDao.same_bic_already_archived.after.csv"
    )
    void testSameBicAlreadyArchived() {
        doArchive("041234567");
    }

    @Test
    @DisplayName("ID не учитывается при добавлении новой записи")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.insert_id_is_ignored.before.csv",
            after = "csv/BankInfoDao.insert_id_is_ignored.after.csv"
    )
    void testInsertIdIsIgnored() {
        BankInfo bankInfo = new BankInfo(-150, "041111111", "name", "place", false, false);
        doInsert(bankInfo);
    }

    @Test
    @DisplayName("В базе могут лежать одинаковые заархивированные банки")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.insert_same_archived_bic_allowed.before.csv",
            after = "csv/BankInfoDao.insert_same_archived_bic_allowed.after.csv"
    )
    void testInsertSameArchivedBicAllowed() {
        insertSameRecords(true);
    }

    @Disabled("Unique index by expression is inconvertible to H2")
    @DisplayName("В базе не могут лежать действующие банки с одинаковым БИК")
    @Test
    @DbUnitDataSet(
            before = "csv/BankInfoDao.insert_same_active_bic_forbidden.before.csv",
            after = "csv/BankInfoDao.insert_same_active_bic_forbidden.after.csv"
    )
    void testInsertSameActiveBicForbidden() {
        insertSameRecords(false);
    }

    private void insertSameRecords(final boolean isArchived) {
        BankInfo bankInfo = new BankInfo(-1, "041111111", "name", "place", false, isArchived);
        doInsert(bankInfo, bankInfo);
    }

    @Test
    @DisplayName("Обновление затрагивает только активные банки")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.only_active_info_updated.before.csv",
            after = "csv/BankInfoDao.only_active_info_updated.after.csv"
    )
    void testOnlyActiveInfoUpdated() {
        BankInfo bankInfo = new BankInfo(-1, "041234567", "new_bank3", "place3", true, false);
        doUpdate(bankInfo);
    }

    @Test
    @DisplayName("Обновление нескольких банков")
    @DbUnitDataSet(
            before = "csv/BankInfoDao.batch_update.before.csv",
            after = "csv/BankInfoDao.batch_update.after.csv"
    )
    void testBatchUpdate() {
        BankInfo firstBankInfo = new BankInfo(-1, "041234567", "new_bank1", "new_place1", false, false);
        BankInfo thirdBankInfo = new BankInfo(-1, "043456789", "new_bank3", "new_place3", false, false);
        doUpdate(firstBankInfo, thirdBankInfo);
    }

    @Test
    @DisplayName("Список активных БИКов возвращается корректно")
    @DbUnitDataSet(before = "csv/BankInfoDao.get_active_bics.csv")
    void testGetActiveBics() {
        Set<String> activeBics = bankInfoDao.getActiveBanksBics();
        Set<String> expectedBics = Sets.newHashSet("041234567", "042345678");

        Assertions.assertEquals(expectedBics, activeBics);
    }

    @Test
    @DisplayName("Список активных банков возвращается корректно")
    @DbUnitDataSet(before = "csv/BankInfoDao.get_active_infos.csv")
    void testGetActiveInfos() {
        BankInfo bankInfo1 = new BankInfo(1001, "041234567", "bank1", "place1", false, false);
        BankInfo bankInfo2 = new BankInfo(1002, "042345678", "bank2", "place2", true, false);

        Set<BankInfo> activeBanks = new HashSet<>(bankInfoDao.getActiveBanksInfos());
        Set<BankInfo> expectedBanks = Sets.newHashSet(bankInfo1, bankInfo2);

        Assertions.assertEquals(expectedBanks, activeBanks);
    }

    private void doArchive(final String... bics) {
        bankInfoDao.archiveBanks(Arrays.asList(bics));
    }

    private void doInsert(final BankInfo... banks) {
        bankInfoDao.insertInfos(Arrays.asList(banks));
    }

    private void doUpdate(final BankInfo... banks) {
        bankInfoDao.updateInfos(Arrays.asList(banks));
    }
}
