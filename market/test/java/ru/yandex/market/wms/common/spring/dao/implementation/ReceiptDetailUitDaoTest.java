package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetailUit;
import ru.yandex.market.wms.common.spring.pojo.ItemToReceive;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class ReceiptDetailUitDaoTest extends IntegrationTest {

    @Autowired
    private ReceiptDetailUitDao receiptDetailUitDao;

    @Test
    @DatabaseSetup("/db/dao/receipt-detail-uit/delete/before-delete.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail-uit/delete/after-delete.xml", assertionMode = NON_STRICT)
    void delete() {
        receiptDetailUitDao.deleteReceipt("1");
    }

    @Test
    @DisplayName("Проверка вставки нескольких уитов")
    @DatabaseSetup("/db/dao/receipt-detail-uit/insert-multiple/before-insert.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail-uit/insert-multiple/after-insert.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void insertMap() {
        receiptDetailUitDao.insert(Map.of(
                new ReceiptDetailKey("1", "2"), Set.of(
                        ItemToReceive.builder().uit("1").uuid("uuid1").build(),
                        ItemToReceive.builder().uit("2").uuid("uuid2").build()),
                new ReceiptDetailKey("1", "3"), Set.of(
                        ItemToReceive.builder().uit("3").uuid("uuid3").build(),
                        ItemToReceive.builder().uit("4").uuid("uuid4").build())), "TEST");
    }

    @Test
    @DisplayName("Проверка вставки одного уита")
    @DatabaseSetup("/db/dao/receipt-detail-uit/insert/before-insert.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail-uit/insert/after-insert.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void insertSingle() {
        receiptDetailUitDao.insert(new ReceiptDetailKey("1", "2"), "3", "4", "TEST");
    }

    @Test
    @DisplayName("Найти записи по ключу поставки")
    @DatabaseSetup("/db/dao/receipt-detail-uit/select/select.xml")
    void find() {
        List<ReceiptDetailUit> receiptDetailUits = receiptDetailUitDao.find("1");
        assertions.assertThat(receiptDetailUits).hasOnlyOneElementSatisfying(receiptDetailUit -> {
            assertions.assertThat(receiptDetailUit.getReceiptDetailKey().getReceiptKey()).isEqualTo("1");
            assertions.assertThat(receiptDetailUit.getReceiptDetailKey().getReceiptLineNumber()).isEqualTo("2");
            assertions.assertThat(receiptDetailUit.getUit()).isEqualTo("3");
            assertions.assertThat(receiptDetailUit.getUuid()).isEqualTo("4");
        });
    }
}
