package ru.yandex.market.core.application.meta;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.application.PartnerApplicationStatus;

import static org.mockito.Mockito.doReturn;

public class PartnerApplicationDAOTest extends FunctionalTest {

    @Autowired
    private PartnerApplicationDAO partnerApplicationDAO;

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(before = "partnerApplicationDaoTest.before.csv",
            after = "partnerApplicationDaoTest.typeNotChanged.after.csv")
    void testTypeNotChanged() {
        doReturn(Instant.parse("2022-07-05T01:01:01Z")).when(clock).instant();
        partnerApplicationDAO.save(List.of(partnerApplicationDAO.findById(1)));
    }

    @Test
    @DbUnitDataSet(before = "partnerApplicationDaoTest.getAplStatus.before.csv")
    void checkGetPartnersApplications() {
        Map<Long, PartnerApplicationStatus> partnersLastApplicationStatus =
                partnerApplicationDAO.getPartnersLastApplicationStatus(List.of(500L, 700L));
        Assertions.assertEquals(2, partnersLastApplicationStatus.size());
        Assertions.assertEquals(PartnerApplicationStatus.NEW, partnersLastApplicationStatus.get(500L));
        Assertions.assertEquals(PartnerApplicationStatus.INIT, partnersLastApplicationStatus.get(700L));
    }
}
