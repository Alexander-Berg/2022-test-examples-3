package ru.yandex.market.hrms.tms.manager.ispring.courses;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.ispring.IspringCourseMapper;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringAccountRepository;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringCourseRepo;
import ru.yandex.market.hrms.core.domain.ispring.repo.IspringCourseResultRepo;
import ru.yandex.market.hrms.core.service.environment.EnvironmentService;
import ru.yandex.market.hrms.core.service.ispring.ISpringService;
import ru.yandex.market.hrms.core.service.util.HrmsCollectionUtils;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.ispring.ISpringCoursesSyncManager;
import ru.yandex.market.ispring.ISpringClientMock;
import ru.yandex.market.ispring.pojo.ContentDto;
import ru.yandex.market.ispring.pojo.EnrollmentDto;
import ru.yandex.market.ispring.pojo.FinalStatusDto;

@DbUnitDataSet(schema = "public", before = "ISpringCoursesManagerTest.before.csv")
public class ISpringCoursesManagerTest extends AbstractTmsTest {

    @Autowired
    ISpringCoursesSyncManager coursesSyncManager;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private IspringCourseRepo ispringCourseRepo;

    @Autowired
    private IspringCourseResultRepo resultRepo;

    @Autowired
    private IspringCourseMapper ispringCourseMapper;

    @Autowired
    private IspringAccountRepository ispringAccountRepository;

    @Autowired
    private Clock clock;

    private final ISpringClientMock iSpringClient = Mockito.spy(new ISpringClientMock());

    @BeforeEach
    void setUp() {
        ISpringService iSpringService = new ISpringService(iSpringClient, environmentService, ispringAccountRepository);

        coursesSyncManager = new ISpringCoursesSyncManager(iSpringService, ispringCourseRepo,
                resultRepo, ispringCourseMapper, environmentService, clock);
    }


    @Test
    @DbUnitDataSet(
            after = "ISpringCoursesManagerTest.after.csv")
    void syncCoursesResultTest() {

        mockClock(LocalDate.of(2022, 3,1));
        iSpringClient.setCourses(getIspringCourses());
        iSpringClient.setEnrollments(getIspringEnrollments());
        iSpringClient.setFinalStatuses(getFinalResults());

        coursesSyncManager.importCourses();
        coursesSyncManager.importIspringEnrollments();
    }

    private List<ContentDto> getIspringCourses() {
        List<ContentDto> dtos = new ArrayList<>();
        dtos.add(new ContentDto("course-1",
                "Введение в профессию кладовщик сортировки на станции упаковки (ФФЦ Екатеринбург)", "Курс", "[обязательный] \"Отбор\" [карьерное развитие]"));
        dtos.add(new ContentDto("course-2",
                "Введение в профессию \"Кладовщик консолидации и отгрузки\" (Самара ФФЦ)", "Курс", "[необязательный] \"Отбор\""));
        dtos.add(new ContentDto("course-3", "Привычки высокой эффективности на работе", "Курс", "[обязательный]"));
        dtos.add(new ContentDto("course-4", "Введение в профессию кладовщик инвентаризации (Ростов)", "Курс", "\"Отбор\""));
        dtos.add(new ContentDto("course-5", "Введение в профессию кладовщик консолидации и отгрузки (Ростов)", "Курс", ""));
        dtos.add(new ContentDto("not-course-6", "Тестирование для кладовщиков", "Тест", null));
        return dtos;
    }

    private Map<String, List<EnrollmentDto>> getIspringEnrollments() {
        List<EnrollmentDto> dtos = new ArrayList<>();
        dtos.add(new EnrollmentDto("enrollment-1", "course-1", "user-1", "2022-01-10", null, null));
        dtos.add(new EnrollmentDto("enrollment-2", "course-2", "user-1", "2022-01-11", "2022-02-11", "2022-02-11"));
        dtos.add(new EnrollmentDto("enrollment-3", "course-2", "user-2", "2022-01-11", "2022-02-11", "2022-02-11"));
        dtos.add(new EnrollmentDto("enrollment-4", "course-3", "user-2", "2021-12-02", "2021-12-25", "2021-12-25"));
        dtos.add(new EnrollmentDto("enrollment-5", "course-4", "user-3", "2022-02-02", "2022-03-25", "2022-03-25"));
        dtos.add(new EnrollmentDto("enrollment-6", "course-5", "user-4", "2022-02-02", null, null));
        return HrmsCollectionUtils.groupBy(dtos, EnrollmentDto::getCourseId);
    }

    private Map<String, List<FinalStatusDto>> getFinalResults() {
        Map<String, List<FinalStatusDto>> dtos = new HashMap<>();
        dtos.put("course-1",
                List.of(new FinalStatusDto("user-1", "Завершен", "100",
                        "2022-02-01T12:00:00.00+00:00", "2022-02-01T12:05:00.00+00:00"))
        );

        dtos.put("course-2",
                List.of(
                        new FinalStatusDto("user-1", "Не начат", "0", null, null),
                        new FinalStatusDto("user-2", "В процессе", "45", null, "2022-01-30T14:08:00.00+00:00")
                ));

        dtos.put("course-3",
                List.of(new FinalStatusDto("user-2", "Не завершен", "0", null, null)));

        dtos.put("course-4",
                List.of(new FinalStatusDto("user-3", "Завершен", "88",
                        "2022-03-20T13:57:00.00+00:00", "2022-03-20T14:08:00.00+00:00")));

        dtos.put("course-5",
                List.of(new FinalStatusDto("user-4", "Не начат", "0", null, null),
                        new FinalStatusDto("user-6", "Не начат", "0",
                                "2021-11-20T13:57:00.00+00:00", "2021-11-20T15:57:00.00+00:00")));

        return dtos;
    }
}
