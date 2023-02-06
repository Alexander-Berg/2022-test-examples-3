package ru.yandex.market.billing.distribution.imports;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.distribution.imports.dao.HardcodedClidDao;
import ru.yandex.market.billing.distribution.share.model.DistributionPartner;
import ru.yandex.market.billing.distribution.share.model.DistributionPartnerSegment;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class HardcodedClidServiceTest extends FunctionalTest {

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    @DbUnitDataSet(before="HardcodedClidServiceTest.testUpdate.before.csv", after="HardcodedClidServiceTest.testUpdate.after.csv")
    public void testUpdate() {
        var hardcodedClidService = new HardcodedClidService(
                new HardcodedClidDao(namedParameterJdbcTemplate), transactionTemplate);
        hardcodedClidService.updateClids(List.of(
                clid(1000, DistributionPartnerSegment.CLOSER),
                clid(1001, DistributionPartnerSegment.MARKETING),
                clid(1, DistributionPartnerSegment.CLOSER),
                clid(2, null)
        ));
    }

    private static DistributionPartner clid(long clid, DistributionPartnerSegment segment) {
        return DistributionPartner.builder().setClid(clid).setPartnerSegment(segment).build();
    }

}