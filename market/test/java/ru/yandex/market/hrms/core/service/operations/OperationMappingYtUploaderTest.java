package ru.yandex.market.hrms.core.service.operations;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.operation.OperationMappingYtView;
import ru.yandex.market.hrms.core.domain.operation.repo.OperationMappingYtViewRepo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

@DbUnitDataSet(before = "OperationMappingYtUploaderTest.before.csv")
public class OperationMappingYtUploaderTest extends AbstractCoreTest {

    @Autowired
    private OperationMappingYtViewRepo operationMappingYtViewRepo;

    @Test
    public void shouldReturnOperationsCorrectly() {
        List<OperationMappingYtView> data = operationMappingYtViewRepo.load();

        assertThat(data.get(0), allOf(
                hasProperty("operation", is("операция1")),
                hasProperty("operationGroup", is("Тестовая группа"))
        ));

        assertThat(data.get(1), allOf(
                hasProperty("operation", is("операция2")),
                hasProperty("operationGroup", is("Тестовая группа"))
        ));

        assertThat(data.get(2), allOf(
                hasProperty("operation", is("операция3")),
                hasProperty("operationGroup", is("Тестовая группа2"))
        ));
    }
}
