package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.DatabaseSchema;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.TransferDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferDetailDaoTest extends IntegrationTest {

    @Autowired
    private TransferDetailDao transferDetailDao;

    @Test
    @DatabaseSetup("/db/dao/transferdetail/before.xml")
    void findTransferDetailsByTransferKey() {
        List<TransferDetail> detailsFounded =
                transferDetailDao.findTransferDetailsByTransferKey("0000000001");
        assertNotNull(detailsFounded);
        assertEquals(2, detailsFounded.size());
        List<TransferDetail> detailsNotFounded =
                transferDetailDao.findTransferDetailsByTransferKey("0000000003");
        assertNotNull(detailsNotFounded);
        assertTrue(detailsNotFounded.isEmpty());
    }

    @Test
    void getTotalQtyForHeldSuccessfulTransfersWhenThereIsNoHeldSuccessfulTransferDetails() {
        int qty = transferDetailDao.getTotalQtyForHeldSuccessfulTransfers();
        assertEquals(0, qty);
    }

    @Test
    @DatabaseSetup("/db/dao/transferdetail/before-getTotalQtyForHeldSuccessfulTransfers-1-td.xml")
    void getTotalQtyForHeldSuccessfulTransfersWhenThereIsOneTransferDetail() {
        int qty = transferDetailDao.getTotalQtyForHeldSuccessfulTransfers();
        assertEquals(1, qty);
    }

    @Test
    @DatabaseSetup("/db/dao/transferdetail/before-getTotalQtyForHeldSuccessfulTransfers-multiple-td.xml")
    void getTotalQtyForHeldSuccessfulTransfersWhenThereAreMultipleTransferDetails() {
        int qty = transferDetailDao.getTotalQtyForHeldSuccessfulTransfers();
        assertEquals(9, qty);
    }

    @Test
    @DatabaseSetup("/db/dao/transferdetail/before-deleteTransferDetailByLoc.xml")
    void deleteTransferDetailByLoc() {
        List<TransferDetail> transferDetailsByTransferKey =
                transferDetailDao.findTransferDetailsByTransferKey("0000000001");
        assertEquals(3, transferDetailsByTransferKey.size());
        transferDetailDao.deleteByLoc("KOROBKI1", DatabaseSchema.WMWHSE1);
        transferDetailsByTransferKey = transferDetailDao.findTransferDetailsByTransferKey("0000000001");
        assertEquals(1, transferDetailsByTransferKey.size());
    }
}
