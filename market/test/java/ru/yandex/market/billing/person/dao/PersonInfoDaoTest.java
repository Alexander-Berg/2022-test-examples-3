package ru.yandex.market.billing.person.dao;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.person.model.PersonInfo;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.billing.person.model.CustomerSubType.INDIVIDUAL;

@ParametersAreNonnullByDefault
class PersonInfoDaoTest extends FunctionalTest {

    @Autowired
    private PersonInfoDao personInfoDao;

    @Test
    @DbUnitDataSet(before = "PersonInfoDao.getPersonInfosWithActiveContractByIds.csv")
    void getPersonInfo() {
        var personsInfo = personInfoDao.getPersonInfosWithActiveContractByIds(List.of(1L, 3L, 4L, 5L, 6L));

        assertThat(personsInfo)
                .usingRecursiveFieldByFieldElementComparator(
                        RecursiveComparisonConfiguration.builder().withIgnoredFields("updatedAt").build()
                )
                .containsExactlyInAnyOrder(
                        PersonInfo.builder().setPersonId(3L).setCustomerType(INDIVIDUAL).build()
                );
    }
}
