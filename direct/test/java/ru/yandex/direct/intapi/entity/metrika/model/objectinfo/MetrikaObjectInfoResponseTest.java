package ru.yandex.direct.intapi.entity.metrika.model.objectinfo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.intapi.entity.metrika.model.objectinfo.MetrikaTimeToken.DATE_TIME_FORMATTER;

@RunWith(Parameterized.class)
public class MetrikaObjectInfoResponseTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    private static final MetrikaTimeToken PREV_TIME_TOKEN = new MetrikaTimeToken(NOW.minusHours(20), 125L);

    private static final List<RetargetingConditionInfo> OBJECTS_INFO = asList(
            new RetargetingConditionInfo(3L, "name1", "desc", NOW.minusMinutes(40)),
            new RetargetingConditionInfo(123L, "name2", "desc", NOW),
            new RetargetingConditionInfo(54L, "name3", "desc", NOW),
            new RetargetingConditionInfo(28L, "name4", "desc", NOW.minusMinutes(10)));

    @Parameterized.Parameter(0)
    public List<RetargetingConditionInfo> objectsInfo;

    @Parameterized.Parameter(1)
    public Integer limit;

    @Parameterized.Parameter(2)
    public MetrikaTimeToken timeToken;

    @Parameterized.Parameter(3)
    public Matcher timeTokenMatcher;

    @Parameterized.Parameter(4)
    public Matcher objectListMatcher;

    @Parameterized.Parameters()
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                // objects  limit  prevTimeToken  expectedTimeTokenMatcher  expectedObjectsMatcher
                {OBJECTS_INFO, null, PREV_TIME_TOKEN, is(NOW.format(DATE_TIME_FORMATTER) + "/123"), hasSize(4)},
                {OBJECTS_INFO, null, null, is(NOW.format(DATE_TIME_FORMATTER) + "/123"), hasSize(4)},
                {OBJECTS_INFO, 3, null, is(NOW.format(DATE_TIME_FORMATTER) + "/54"), hasSize(3)},
                {OBJECTS_INFO, 10, null, is(NOW.format(DATE_TIME_FORMATTER) + "/123"), hasSize(4)},
                {OBJECTS_INFO, 0, null, nullValue(), hasSize(0)},
                {OBJECTS_INFO, 0, PREV_TIME_TOKEN, is(PREV_TIME_TOKEN.toString()), hasSize(0)},
                {new ArrayList<>(), null, PREV_TIME_TOKEN, is(PREV_TIME_TOKEN.toString()), notNullValue()},
                {new ArrayList<>(), null, null, nullValue(), notNullValue()},
                {new ArrayList<>(), 3, PREV_TIME_TOKEN, is(PREV_TIME_TOKEN.toString()), notNullValue()},
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    public void create_TimeTokenIsValid() {
        MetrikaObjectInfoResponse<RetargetingConditionInfo> resp =
                MetrikaObjectInfoResponse.create(objectsInfo, limit, timeToken);
        assertThat(resp.getTimeToken(), timeTokenMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void create_ObjectsInfoIsValid() {
        MetrikaObjectInfoResponse<RetargetingConditionInfo> resp =
                MetrikaObjectInfoResponse.create(objectsInfo, limit, timeToken);
        assertThat(resp.getObjects(), objectListMatcher);
    }
}
