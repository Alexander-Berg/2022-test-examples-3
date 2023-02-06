package ru.yandex.market.antifraud.yql.validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.antifraud.model.SessionStatusEnum;
import ru.yandex.market.antifraud.yql.model.UnvalidatedDay;
import ru.yandex.market.antifraud.yql.model.YqlSession;
import ru.yandex.market.antifraud.yql.model.YqlSessionType;

public class YqlJoinWithDataTest {

    @Test
    public void testSessionSort() {
        Assert.assertArrayEquals(
            new long[]{5, 2, 4, 3, 1}, sortAndGetIds(
                create(1, YqlSessionType.NORMAL, 20181028, 1, UnvalidatedDay.Scale.ARCHIVE),
                create(2, YqlSessionType.DAYCLOSING, 20181028, 1, UnvalidatedDay.Scale.ARCHIVE),

                create(3, YqlSessionType.NORMAL, 20181029, 2, UnvalidatedDay.Scale.RECENT),
                create(4, YqlSessionType.NORMAL, 20181029, 3, UnvalidatedDay.Scale.RECENT),

                create(5, YqlSessionType.DAYCLOSING, 20181030, 1, UnvalidatedDay.Scale.RECENT)
            )
        );
    }

    @Test
    public void testFieldsString() {
        String[] fields = {"oh", "my", "god"};
        Assert.assertEquals(YqlJoinWithData.makeFieldsString(fields), "dataTable.oh as oh, dataTable.my as my, dataTable.god as god");
    }

    private YqlSession create(long id, YqlSessionType type, int day, long lastSeenId, UnvalidatedDay.Scale scale) {
        return YqlSession.builder()
            .id(id)
            .type(type)
            .status(SessionStatusEnum.FILTERS_EXECUTED)
            .day(new UnvalidatedDay(day, lastSeenId, scale))
            .build();
    }

    private long[] sortAndGetIds(YqlSession... sessions) {
        List<YqlSession> sessionList = new ArrayList<>(Arrays.asList(sessions));
        YqlJoinWithData.sort(sessionList);
        long ids[] = new long[sessionList.size()];
        for(int i = 0; i < ids.length; i++) {
            ids[i] = sessionList.get(i).getId();
        }
        return ids;
    }

}
