package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.LocStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.ReceivedItemIdentity;
import ru.yandex.market.wms.common.spring.enums.TypeOfIdentity;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailIdentity;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

class ReceiptDetailIdentityDaoTest extends IntegrationTest {

    @Autowired
    private ReceiptDetailIdentityDao receiptDetailIdentityDao;

    @Test
    @DatabaseSetup("/db/dao/receipt-detail-identity/before-delete.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail-identity/after-delete.xml", assertionMode = NON_STRICT)
    void delete() {
        receiptDetailIdentityDao.deleteByReceiptKey("1");
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail-identity/before-find-by-receipt-and-type.xml")
    void findByReceiptDetailAndType() {
        final List<ReceiptDetailIdentity> receiptDetailIdentities =
                receiptDetailIdentityDao.findByReceiptAndType("0000000102", TypeOfIdentity.CIS);

        Assertions.assertEquals(2, receiptDetailIdentities.size());
        Assertions.assertEquals(TypeOfIdentity.CIS,
                receiptDetailIdentities.stream()
                        .map(rdi -> rdi.getPk().getType())
                        .distinct().findFirst().get());
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail-identity/before-find-by-receipt-and-uits.xml")
    void findReceivedByUits() {
        final List<ReceivedItemIdentity> receiptDetailIdentities =
                receiptDetailIdentityDao.findReceivedByUits(List.of("553304157937", "553304157929", "553304157928"),
                        "0000000102");

        assertions.assertThat(receiptDetailIdentities)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("addDate", "addWho", "editDate", "editWho")
                .containsAll(getExpected());
    }

    private List<ReceivedItemIdentity> getExpected() {
        return List.of(ReceivedItemIdentity.builder()
                        .uuid("ae4b6213-1242-11ec-9147-7bdc19bf1778")
                        .uit("553304157937")
                        .receivingLine("00001")
                        .locStatus(LocStatus.OK)
                        .identityType("CIS")
                        .identityValue("CIS123")
                        .orderId("234")
                        .returnReasonId("BAD_QUALITY")
                        .build(),
                ReceivedItemIdentity.builder()
                        .uuid("be4b6213-1242-11ec-9147-7bdc19bf1779")
                        .uit("553304157928")
                        .receivingLine("00002")
                        .locStatus(LocStatus.OK)
                        .orderId("234")
                        .returnReasonId("BAD_QUALITY")
                        .build(),
                ReceivedItemIdentity.builder()
                        .uuid("be4b6213-1242-11ec-9147-7bdc19bf1780")
                        .uit("553304157929")
                        .receivingLine("00002")
                        .locStatus(LocStatus.OK)
                        .orderId("234")
                        .build()
        );
    }
}
