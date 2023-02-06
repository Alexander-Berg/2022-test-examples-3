package ru.yandex.direct.intapi.entity.statistic;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersStatByIntervalRequest;
import ru.yandex.direct.intapi.entity.statistic.service.StatisticValidationService;
import ru.yandex.direct.validation.defect.ids.DateDefectIds;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.Path;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersStatByIntervalRequest.END_DATE;
import static ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersStatByIntervalRequest.ORDER_IDS;
import static ru.yandex.direct.intapi.entity.statistic.model.order.GetOrdersStatByIntervalRequest.START_DATE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class StatisticValidationServiceGetOrdersStatByIntervalTest {

    private final StatisticValidationService statisticValidationService;

    @Parameterized.Parameter
    public List<Long> orderIds;

    @Parameterized.Parameter(1)
    public LocalDate startDate;

    @Parameterized.Parameter(2)
    public LocalDate endDate;

    @Parameterized.Parameter(3)
    public Boolean hasErrors;

    @Parameterized.Parameter(4)
    public Path path;

    @Parameterized.Parameter(5)
    public DefectId defectId;

    public StatisticValidationServiceGetOrdersStatByIntervalTest() {
        this.statisticValidationService = new StatisticValidationService(null);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {
                        null, LocalDate.of(2018, 12, 31), LocalDate.of(2019, 1, 1),
                        true, path(field(ORDER_IDS)), DefectIds.CANNOT_BE_NULL
                },
                {
                        Collections.singletonList(null), LocalDate.of(2018, 12, 31), LocalDate.of(2019, 1, 1),
                        true, path(field(ORDER_IDS), index(0)), DefectIds.CANNOT_BE_NULL
                },
                {
                        Collections.singletonList(0L), LocalDate.of(2018, 12, 31), LocalDate.of(2019, 1, 1),
                        true, path(field(ORDER_IDS), index(0)), DefectIds.MUST_BE_VALID_ID
                },
                {
                        Collections.emptyList(), null, LocalDate.of(2019, 1, 1),
                        true, path(field(START_DATE)), DefectIds.CANNOT_BE_NULL
                },
                {
                        Collections.emptyList(), LocalDate.of(2019, 1, 1), null,
                        true, path(field(END_DATE)), DefectIds.CANNOT_BE_NULL
                },
                {
                        Collections.emptyList(), LocalDate.of(2000, 1, 1), LocalDate.of(2019, 1, 1),
                        true, path(field(START_DATE)), DateDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN
                },
                {
                        Collections.emptyList(), LocalDate.of(2019, 1, 1), LocalDate.of(2000, 1, 1),
                        true, path(field(END_DATE)), DateDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN
                },
                {
                        Collections.emptyList(), LocalDate.of(2019, 7, 10), LocalDate.of(2019, 7, 1),
                        true, emptyPath(), DefectIds.INCONSISTENT_STATE
                },
                {
                        Collections.singletonList(1L), LocalDate.of(2019, 7, 1), LocalDate.of(2019, 7, 10),
                        false, null, null
                },
                {
                        Collections.singletonList(1L), LocalDate.of(2019, 7, 1), LocalDate.of(2019, 7, 1),
                        false, null, null
                },
                {
                        Collections.emptyList(), LocalDate.of(2019, 7, 1), LocalDate.of(2019, 7, 10),
                        false, null, null
                },
                {
                        Arrays.asList(1L, 2L), LocalDate.of(2019, 7, 1), LocalDate.of(2019, 7, 10),
                        false, null, null
                }
        });
    }

    @Test
    public void test() {
        var request = new GetOrdersStatByIntervalRequest(orderIds, startDate, endDate);
        var validationResult = statisticValidationService.validate(request);
        if (hasErrors) {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(path, defectId)));
        } else {
            assertThat(validationResult, hasNoErrors());
        }
    }
}

