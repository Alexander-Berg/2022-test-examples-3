package ru.yandex.market.hrms.core.service.environment;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.environment.repo.EnvironmentKey;

class EnvironmentServiceTest extends AbstractCoreTest {

    @Autowired
    private EnvironmentService environmentService;

    @DbUnitDataSet(before = "EnviromentServiceTest.before.csv")
    @CsvSource(value = {
            "CALENDAR_FOR_NEXT_MONTH,,true",
            "CALENDAR_FOR_NEXT_MONTH,2,true",
            "CALENDAR_FOR_NEXT_MONTH,1,false",
            "CALENDAR_FOR_NEXT_MONTH,3,false"
    })
    @ParameterizedTest
    void getBooleanOrDefault(EnvironmentKey environmentKey, Long domainId, Boolean expected) {
        Boolean result = environmentService.getBooleanOpt(environmentKey, domainId).orElse(null);
        Assertions.assertEquals(expected, result);
    }

}
