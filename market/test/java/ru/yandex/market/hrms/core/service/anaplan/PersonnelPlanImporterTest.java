package ru.yandex.market.hrms.core.service.anaplan;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.plan.PersonnelPlanChytRepo;
import ru.yandex.market.hrms.core.domain.plan.PersonnelPlanJdbcRepo;
import ru.yandex.market.hrms.core.domain.plan.PersonnelType;
import ru.yandex.market.hrms.core.domain.plan.dto.PersonnelPlanDto;

public class PersonnelPlanImporterTest extends AbstractCoreTest {

    private static final String SELECT = "SELECT * FROM personnel_plan";

    private static final RowMapper<PersonnelPlanDto> ROW_MAPPER = (rs, rowNum) -> new PersonnelPlanDto(
            PersonnelType.valueOf(rs.getString("personnel_type")),
            rs.getDate("date").toLocalDate(),
            rs.getString("operation_group"),
            rs.getString("domain_name"),
            rs.getFloat("amount"),
            rs.getLong("domain_id"),
            rs.getBoolean("is_production"),
            rs.getBoolean("is_changed_in_past")
    );

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private PersonnelPlanJdbcRepo personnelPlanJdbcRepo;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(before = "PersonnelPlanImporterTest.ImportData.before.csv",
            after = "PersonnelPlanImporterTest.ImportData.after.csv")
    void importData() {
        mockClock(LocalDate.of(2021, 10, 14));

        PersonnelPlanChytRepo personnelPlanChytRepoMock = Mockito.mock(PersonnelPlanChytRepo.class);
        Mockito.when(personnelPlanChytRepoMock.importData()).thenReturn(getPersonnelPlanDtosFromYt());

        PersonnelPlanImporter personnelPlanImporter = new PersonnelPlanImporter(
                personnelPlanChytRepoMock,
                personnelPlanJdbcRepo,
                transactionTemplate,
                clock
        );

        personnelPlanImporter.importData();
    }

    List<PersonnelPlanDto> getPersonnelPlanDtosFromYt() {
        PersonnelPlanDto ppd1 = new PersonnelPlanDto(
                PersonnelType.STAFF,
                LocalDate.of(2021, 10, 12),
                "Приемка первичная",
                "Яндекс.Маркет (Софьино)",
                2.0f,
                null,
                null,
                null
        );
        PersonnelPlanDto ppd2 = new PersonnelPlanDto(
                PersonnelType.STAFF,
                LocalDate.of(2021, 10, 13),
                "Приемка первичная",
                "Яндекс.Маркет (Софьино)",
                2.0f,
                null,
                null,
                null
        );
        PersonnelPlanDto ppd3 = new PersonnelPlanDto(
                PersonnelType.STAFF,
                LocalDate.of(2021, 10, 14),
                "Приемка первичная",
                "Яндекс.Маркет (Софьино)",
                2.0f,
                null,
                null,
                null
        );
        PersonnelPlanDto ppd4 = new PersonnelPlanDto(
                PersonnelType.STAFF,
                LocalDate.of(2021, 10, 15),
                "Приемка первичная",
                "Яндекс.Маркет (Софьино)",
                2.0f,
                null,
                null,
                null
        );
        return List.of(ppd1, ppd2, ppd3, ppd4);
    }
}
