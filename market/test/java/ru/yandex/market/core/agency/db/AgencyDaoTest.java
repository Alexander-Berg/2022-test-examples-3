package ru.yandex.market.core.agency.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.agency.Agency;
import ru.yandex.market.core.agency.PassportUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author otedikova
 */
class AgencyDaoTest extends FunctionalTest {
    @Autowired
    private AgencyDao agencyDao;

    @Test
    @DisplayName("Обновление информации об агентстве")
    @DbUnitDataSet(
            before = "csv/AgencyDaoTest.update.before.csv",
            after = "csv/AgencyDaoTest.update.after.csv"
    )
    void updateAgency() {
        agencyDao.updateAgency(1, new Agency(1, "agency1-1", 1, "agency1@mail.com"));
        agencyDao.updateAgency(2, new Agency(2, "agency2", 21, "agency2@mail.com"));
        agencyDao.updateAgency(3, new Agency(3, "agency3", 3, "agency3-1@mail.com"));
        agencyDao.updateAgency(4, new Agency(4, "agency4", 4, "agency4@mail.com"));
    }

    @Test
    @DbUnitDataSet(before = "csv/AgencyDaoTest.getAgencyClients.before.csv")
    void getAgencyClients() {
        var agencyClients = agencyDao.getAgencyClients(1);
        assertThat(agencyClients).satisfiesExactlyInAnyOrder(
                ac -> {
                    assertThat(ac.getId()).isEqualTo(101L);
                    assertThat(ac.getCampaigns()).containsOnly(100201L);
                    assertThat(ac.getUsers()).containsOnly(
                            new PassportUser(100501L, null),
                            new PassportUser(100502L, null),
                            new PassportUser(100503L, null)
                    );
                },
                ac -> {
                    assertThat(ac.getId()).isEqualTo(102L);
                    assertThat(ac.getCampaigns()).containsOnly(100202L);
                    assertThat(ac.getUsers()).isEmpty();
                }
        );
    }
}
