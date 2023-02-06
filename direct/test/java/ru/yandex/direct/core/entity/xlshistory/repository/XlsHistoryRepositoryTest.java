package ru.yandex.direct.core.entity.xlshistory.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.enums.XlsHistoryStatusemailed;
import ru.yandex.direct.dbschema.ppc.enums.XlsHistoryType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.XLS_HISTORY;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class XlsHistoryRepositoryTest {

    private static final int SHARD = 2;

    private final long id1 = RandomUtils.nextInt();
    private final long id2 = RandomUtils.nextInt();

    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private XlsHistoryRepository xlsHistoryRepository;


    @Before
    public void before() {
        insertXlsHistory(id1,
                100L,
                LocalDateTime.now().minusYears(10),
                "345c302f62e1331f3dd0250a9a064320",
                XlsHistoryStatusemailed.Yes,
                "345c302f62e1331f3dd0250a9a064320_1550769536.xlsx",
                XlsHistoryType.export);
        insertXlsHistory(id2,
                113L,
                LocalDateTime.now().minusYears(10).minusMinutes(1),
                "314c3cdffa0c2b7de3155b637e9c13bd",
                XlsHistoryStatusemailed.No,
                "314c3cdffa0c2b7de3155b637e9c13bd_1550772586.xls",
                XlsHistoryType.import_);
    }

    @After
    public void after() {
        xlsHistoryRepository.deleteById(SHARD, List.of(id1, id2));
    }


    /**
     * Тест: если в таблице XLS_HISTORY есть поля с определенными ID и отличающимися датами LOGDATE -> при запросе из
     * таблицы некоторого количества элементов, LOGDATE у которых находится между этими элементами и позднее(меньше) ->
     * получим коллекцию с ID элементов с более поздним LOGDATE и в которую не входят с более раним LOGDATE
     */
    @Test
    public void getIdsByLogdateLessThan() {
        LocalDateTime dateTime = LocalDateTime.now()
                .minusYears(10)
                .minusMinutes(1)
                .plusSeconds(1);
        List<Long> xlsHistoryIds = xlsHistoryRepository.getIdsByLogdateLessThan(SHARD, dateTime, 100);

        Long notExpected = id1;
        Long expected = id2;
        assertThat(xlsHistoryIds).contains(expected).doesNotContain(notExpected);
    }

    /**
     * Тест: если в таблице XLS_HISTORY удалить запросом по Id строку -> только она будет удалена
     */
    @Test
    public void deleteById() {
        Long notExpected = id1;
        Long expected = id2;
        xlsHistoryRepository.deleteById(SHARD, List.of(notExpected));

        LocalDateTime dateTime = LocalDateTime.now()
                .minusYears(10)
                .plusSeconds(1);
        List<Long> xlsHistoryIds = xlsHistoryRepository.getIdsByLogdateLessThan(SHARD, dateTime, 100);

        assertThat(xlsHistoryIds).contains(expected).doesNotContain(notExpected);
    }

    private void insertXlsHistory(long id, long cid, LocalDateTime logDate, String md5Hex,
                                  XlsHistoryStatusemailed statusEmailed, String filename, XlsHistoryType type) {
        dslContextProvider.ppc(SHARD)
                .insertInto(XLS_HISTORY,
                        XLS_HISTORY.ID,
                        XLS_HISTORY.CID,
                        XLS_HISTORY.LOGDATE,
                        XLS_HISTORY.MD5_HEX,
                        XLS_HISTORY.STATUS_EMAILED,
                        XLS_HISTORY.FILENAME,
                        XLS_HISTORY.TYPE)
                .values(id,
                        cid,
                        logDate,
                        md5Hex,
                        statusEmailed,
                        filename,
                        type)
                .execute();
    }
}
