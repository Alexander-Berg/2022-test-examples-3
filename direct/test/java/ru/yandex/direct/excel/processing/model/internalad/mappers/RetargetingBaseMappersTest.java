package ru.yandex.direct.excel.processing.model.internalad.mappers;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.retargeting.Constants;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.core.entity.retargeting.model.RuleType;
import ru.yandex.direct.excel.processing.model.internalad.RetargetingConditionRepresentation;
import ru.yandex.direct.excel.processing.service.internalad.CryptaSegmentDictionariesService;
import ru.yandex.direct.excelmapper.ExcelMapper;
import ru.yandex.direct.excelmapper.MapperTestUtils;
import ru.yandex.direct.excelmapper.SheetRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class RetargetingBaseMappersTest {

    private static final Long CRYPTA_ID = defaultGoalByType(GoalType.FAMILY).getId();
    private static final Goal CRYPTA_GOAL =
            (Goal) new Goal()
                    .withId(CRYPTA_ID)
                    .withKeyword("777")
                    .withKeywordValue("88")
                    .withTime(Constants.CRYPTA_GOAL_TIME_VALUE);

    public static Object[][] testData() {
        Long goalId = defaultGoalByType(GoalType.GOAL).getId();
        Long audienceId = defaultGoalByType(GoalType.AUDIENCE).getId();
        Integer defaultTime = 3;

        return new Object[][]{
                {new Rule().withType(RuleType.NOT).withGoals(List.of((Goal) new Goal().withId(goalId).withTime(defaultTime)))},
                {new Rule().withType(RuleType.OR).withGoals(List.of((Goal) new Goal().withId(audienceId).withTime(Constants.AUDIENCE_TIME_VALUE)))},
                {new Rule().withType(RuleType.NOT).withGoals(List.of((Goal) new Goal().withId(audienceId).withTime(Constants.AUDIENCE_TIME_VALUE)))},
                {new Rule().withType(RuleType.OR).withGoals(List.of((Goal) new Goal().withId(CRYPTA_ID).withTime(CRYPTA_GOAL.getTime())))},
                {new Rule().withType(RuleType.NOT).withGoals(List.of((Goal) new Goal().withId(CRYPTA_ID).withTime(CRYPTA_GOAL.getTime())))},
        };
    }

    @Mock
    private CryptaSegmentDictionariesService cryptaSegmentDictionariesService;

    @Before
    public void initTestData() {
        initMocks(this);
        doReturn(CRYPTA_GOAL)
                .when(cryptaSegmentDictionariesService).getCryptaByGoalId(CRYPTA_ID);
        doReturn(CRYPTA_GOAL)
                .when(cryptaSegmentDictionariesService)
                .getGoalIdByKeywordAndType(CRYPTA_GOAL.getKeyword(), CRYPTA_GOAL.getKeywordValue());
    }

    @Test
    @TestCaseName("{method} [{index}]")
    @Parameters(method = "testData")
    public void exportAndImportTest(Rule rule) {
        var retargetingConditionRepresentation =
                new RetargetingConditionRepresentation(List.of(rule), cryptaSegmentDictionariesService);
        checkExportThenImport(retargetingConditionRepresentation);
    }

    private void checkExportThenImport(RetargetingConditionRepresentation retargetingConditionRepresentation) {
        ExcelMapper<RetargetingConditionRepresentation> mapper =
                RetargetingBaseMappers.getRetargetingMapper(cryptaSegmentDictionariesService);
        SheetRange sheetRange = MapperTestUtils.createEmptySheet();
        mapper.write(sheetRange, retargetingConditionRepresentation);
        SheetRange readSheetRange = MapperTestUtils.createStringSheetFromLists(
                MapperTestUtils.sheetToLists(sheetRange, mapper.getMeta().getWidth()));
        RetargetingConditionRepresentation actualValue = mapper.read(readSheetRange).getValue();
        assertThat(actualValue)
                .isEqualToComparingFieldByFieldRecursively(retargetingConditionRepresentation);
    }

}
