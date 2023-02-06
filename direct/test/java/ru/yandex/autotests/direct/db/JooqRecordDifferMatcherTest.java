package ru.yandex.autotests.direct.db;

import java.math.BigInteger;

import org.hamcrest.Description;
import org.hamcrest.StringDescription;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.autotests.direct.db.models.jooq.ppc.enums.ModResyncQueueObjectType;
import ru.yandex.autotests.direct.db.models.jooq.ppc.tables.records.ModResyncQueueRecord;
import ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher;
import ru.yandex.autotests.irt.testutils.RandomUtils;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static ru.yandex.autotests.direct.db.utils.JooqRecordDifferMatcher.recordDiffer;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Tests for JooqRecordDifferMatcher
 */
public class JooqRecordDifferMatcherTest {

    private static ModResyncQueueRecord expectedRecord;
    private static JooqRecordDifferMatcher recordDifferMatcher;

    @BeforeClass
    public static void initDiffer() {
        expectedRecord = new ModResyncQueueRecord()
                .setObjectId(BigInteger.valueOf(RandomUtils.getNextInt()))
                .setObjectType(ModResyncQueueObjectType.video_addition)
                .setRemoderate(1);
        recordDifferMatcher = recordDiffer(expectedRecord);
    }

    @Test
    public void alwaysOkDifferCompare() {
        assertThat("матчер не обнаружит разницу", recordDifferMatcher.matches(expectedRecord), is(true));
    }

    @Test
    public void checkDescribeToMessage() {
        Description description = new StringDescription();
        recordDifferMatcher.describeTo(description);
        assertThat("матчер выдает корректное описание", description.toString(),
                equalTo("the same objects of type ModResyncQueueRecord"));
    }

    @Test
    public void matchesWithNullRecord() {
        assertThat("матчер обнаружит разницу", recordDifferMatcher.matches(null), is(false));

        Description description = new StringDescription();
        recordDifferMatcher.describeMismatch(null, description);
        assertThat("матчер выдает корректное описание", description.toString(), equalTo("was null"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getExceptionWhenExpectedRecordIsNull() {
        JooqRecordDifferMatcher matcher = recordDiffer(null);
    }

    @Test
    public void matchesWithDifferentRecord() {
        ModResyncQueueRecord actualRecord = new ModResyncQueueRecord()
                .setObjectId(expectedRecord.getObjectId().add(BigInteger.TEN))
                .setObjectType(expectedRecord.getObjectType())
                .setRemoderate(expectedRecord.getRemoderate());
        assertThat("матчер обнаружит разницу", recordDifferMatcher.matches(actualRecord), is(false));

        Description description = new StringDescription();
        recordDifferMatcher.describeMismatch(actualRecord, description);
        String expectedMessage = String.format(
                "the following fields are not the same:\nCHANGED object_id: %s but expected %s",
                actualRecord.getObjectId().toString(), expectedRecord.getObjectId().toString());
        assertThat("матчер выдает корректное описание", description.toString(),
                equalTo(expectedMessage));
    }

    @Test
    public void matchesWithCustomStrategy() {
        ModResyncQueueRecord actualRecord = new ModResyncQueueRecord()
                .setObjectId(expectedRecord.getObjectId())
                .setObjectType(expectedRecord.getObjectType())
                .setRemoderate(expectedRecord.getRemoderate())
                .setPriority(123); //Этого поля нет в expectedRecord

        assertThat("матчер не обнаружит разницу",
                recordDifferMatcher
                        .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())
                        .matches(actualRecord),
                is(true));
    }
}