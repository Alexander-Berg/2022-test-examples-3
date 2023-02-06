package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransferDaoTest extends IntegrationTest {

    @Autowired
    private TransferDao transferDao;

    @Test
    @DatabaseSetup("/db/dao/transfer/before.xml")
    void findAllTransfersByKeyIn() {
        List<Transfer> transfers =
                transferDao.findAllTransfersByKeyIn(Arrays.asList("0000000001", "0000000002", "0000000003"));
        assertNotNull(transfers);
        assertEquals(2, transfers.size());
    }
}
