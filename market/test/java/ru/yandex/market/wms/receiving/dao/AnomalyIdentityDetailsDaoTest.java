package ru.yandex.market.wms.receiving.dao;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.dao.entity.AnomalyIdentityDetails;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class AnomalyIdentityDetailsDaoTest extends ReceivingIntegrationTest {

    @Autowired
    AnomalyIdentityDetailsDao anomalyIdentityDetailsDao;

    @Test
    @DatabaseSetup("/dao/anomaly-identity-details/before-insert.xml")
    @ExpectedDatabase(value = "/dao/anomaly-identity-details/after-insert.xml", assertionMode = NON_STRICT_UNORDERED)
    public void insertTest() {
        final AnomalyIdentityDetails anomalyIdentityDetails = AnomalyIdentityDetails.builder()
                .anomalyLotKey("1")
                .type("CIS")
                .identity("011004317234553921mbg:zCaRlU%c010#GS#1")
                .status(true)
                .item("123e4567-e89b-12d3-a456-426655440001")
                .build();

        anomalyIdentityDetailsDao.insertBatch(Collections.singletonList(anomalyIdentityDetails));
    }

    @Test
    @DatabaseSetup("/dao/anomaly-identity-details/before-find.xml")
    @ExpectedDatabase(value = "/dao/anomaly-identity-details/before-find.xml", assertionMode = NON_STRICT_UNORDERED)
    public void findByAnomalyLotTest() {
        final String anomalyLot = "1";
        final List<AnomalyIdentityDetails> result =
                anomalyIdentityDetailsDao.findByAnomalyLot(anomalyLot);

        final AnomalyIdentityDetails anomalyIdentityDetailsExpected = AnomalyIdentityDetails.builder()
                .anomalyLotKey("1")
                .type("CIS")
                .identity("011004317234553921mbg:zCaRlU%c010#GS#1")
                .status(true)
                .item("123e4567-e89b-12d3-a456-426655440001")
                .build();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(anomalyIdentityDetailsExpected, result.get(0));
    }
}
