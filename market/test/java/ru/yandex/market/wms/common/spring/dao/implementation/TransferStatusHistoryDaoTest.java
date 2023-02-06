package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.TransferStatusHistory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferStatusHistoryDaoTest extends IntegrationTest {

    @Autowired
    private TransferStatusHistoryDao transferStatusHistoryDao;

    @Test
    @DatabaseSetup("/db/dao/transferstatushistory/before.xml")
    void findTransferStatusHistory() {
        List<TransferStatusHistory> transferStatusHistory =
                transferStatusHistoryDao.findTransferStatusHistory("0000000001");
        assertNotNull(transferStatusHistory);
        assertEquals(3, transferStatusHistory.size());

        List<TransferStatusHistory> notFound =
                transferStatusHistoryDao.findTransferStatusHistory("0000000003");
        assertNotNull(notFound);
        assertTrue(notFound.isEmpty());
    }
}
