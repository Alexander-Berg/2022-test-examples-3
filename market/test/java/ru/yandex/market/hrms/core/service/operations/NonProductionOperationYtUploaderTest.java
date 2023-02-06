package ru.yandex.market.hrms.core.service.operations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.operation.repo.NonProductionOperationYtViewRepo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

@DbUnitDataSet(before = "NonProductionOperationYtUploaderTest.before.csv")
public class NonProductionOperationYtUploaderTest extends AbstractCoreTest {

    @Autowired
    private NonProductionOperationYtViewRepo nonProductionOperationYtViewRepo;

    @Test
    public void shouldReturnOperationsCorrectly() {
        var data = nonProductionOperationYtViewRepo.load();
        assertThat(data.get(0), allOf(
                hasProperty("day", is("2021-07-19")),
                hasProperty("hours", is(11.0)),
                hasProperty("operationGroup", is("Прочие работы")),
                hasProperty("shiftType", is("день")),
                hasProperty("wmsLogin", is("sof-gjmrd"))
        ));
        assertThat(data.get(1), allOf(
                hasProperty("day", is("2021-07-20")),
                hasProperty("hours", is(11.0)),
                hasProperty("operationGroup", is("Прочие работы")),
                hasProperty("shiftType", is("день")),
                hasProperty("wmsLogin", is("sof-timur-shakurov"))
        ));
    }
}
