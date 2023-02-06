package ru.yandex.market.hrms.core.service.wms;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.employee.repo.EmployeeEntity;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffEntity;
import ru.yandex.market.hrms.core.domain.wms.LoginGenerator;

public class WmsLoginGeneratorTest extends AbstractCoreTest {

    @Autowired
    private LoginGenerator loginGenerator;

    @Test
    public void employeeTest() {
        var employee = EmployeeEntity.builder()
                .name("Петров Иван Васильевич")
                .staffLogin("mercurius")
                .birthday(LocalDate.parse("1990-01-10"))
                .build();
        var candidates = loginGenerator.generate("dev", employee);

        Assertions.assertEquals(List.of(
                "dev-mercurius",
                "dev-mercur0190",
                "dev-ivavapetro",
                "dev-ivavapetr2",
                "dev-ivavapetr3",
                "dev-ivavapetr4",
                "dev-ivavapetr5",
                "dev-ivavapetr6",
                "dev-ivavapetr7"
        ), candidates);
    }

    @Test
    public void outstaffTest() {
        var out = OutstaffEntity.builder()
                .lastName("Петров")
                .firstName("Иван")
                .midName("Васильевич")
                .build();
        var candidates = loginGenerator.generate("dev-ya", out);

        Assertions.assertEquals(List.of(
                "dev-ya-petrivv",
                "dev-ya-petriv2",
                "dev-ya-petriv3",
                "dev-ya-petriv4",
                "dev-ya-petriv5",
                "dev-ya-petriv6",
                "dev-ya-petriv7"
        ), candidates);
    }
}
