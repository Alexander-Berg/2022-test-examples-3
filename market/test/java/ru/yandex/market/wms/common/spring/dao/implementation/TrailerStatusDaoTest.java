package ru.yandex.market.wms.common.spring.dao.implementation;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.TrailerStatus;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TrailerStatusDaoTest extends IntegrationTest {

    @Autowired
    private TrailerStatusDao trailerStatusDao;

    @Test
    @DatabaseSetup("/db/dao/trailer-status/before.xml")
    void findAllTransfersByKeyIn() {
        Optional<TrailerStatus> closingTrailerStatusByReceiptKey =
                trailerStatusDao.getClosingTrailerStatusByReceiptKey("0000000005");
        assertTrue(closingTrailerStatusByReceiptKey.isPresent());
    }
}
