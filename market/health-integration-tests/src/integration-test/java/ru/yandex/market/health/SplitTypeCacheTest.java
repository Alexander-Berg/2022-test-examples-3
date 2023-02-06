package ru.yandex.market.health;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.yandex.market.clickhouse.ClickhouseTemplate;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.clickphite.ClickphiteMasterTimeTicker;
import ru.yandex.market.clickphite.whitelist.SplitTypeCache;
import ru.yandex.market.clickphite.whitelist.UndefinedSplitType;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class SplitTypeCacheTest extends IntegrationTestsBase {

    private static final String TABLE_NAME = "market.some_table";
    private static final String COLUMN_NAME = "some_column";

    @Value("${clickphite.whitelist.cache.expire-duration}")
    private long expireDuration;
    @Value("${clickphite.whitelist.cache.expire-time-unit}")
    private TimeUnit expireTimeUnit;

    private ClickhouseTemplate clickhouseTemplate;
    private SplitTypeCache splitTypeCache;
    private ClickphiteMasterTimeTicker clickphiteMasterTimeTicker;

    @Before
    public void setup() throws Exception {
        AnnotationConfigApplicationContext logshatterContext = new AnnotationConfigApplicationContext();
        initLogshatterContextAndTablesMinConfig(logshatterContext);
        clickhouseTemplate = logshatterContext.getBean(ClickhouseTemplate.class);
        // засыпаем, чтобы успела создаться необходимая база с таблицами в ClickHouse
        Thread.sleep(3000);
        fillTestData();
        AnnotationConfigApplicationContext clickphiteContext = createClickphiteContext();
        splitTypeCache = clickphiteContext.getBean(SplitTypeCache.class);
        clickphiteMasterTimeTicker = clickphiteContext.getBean(ClickphiteMasterTimeTicker.class);
    }

    @After
    public void teardown() throws Exception {
        dropDatabases(clickhouseTemplate);
        // засыпаем, чтобы ClickHouse успел удалить базу
        Thread.sleep(3000);
    }

    @Test
    public void string() {
        checkType(ColumnType.String);
    }

    @Test
    public void int64() {
        changeType(ColumnType.Int64);
        checkType(ColumnType.Int64);
    }

    @Test
    public void noData() {
        truncateTable();
        assertThatThrownBy(this::getType).isInstanceOf(UndefinedSplitType.class);
    }

    //когда мастер, происходит отсчёт времени нахождения значения в кэше и удаление из кэша
    @Test
    public void expireWhenMaster() throws InterruptedException {
        clickphiteMasterTimeTicker.setLeader(true);
        getType();
        //умножение на 2, чтобы наверняка необходимое время прошло
        Thread.sleep(2 * expireTimeUnit.toMillis(expireDuration));
        changeType(ColumnType.Int64);
        checkType(ColumnType.Int64);
    }

    //когда не мастер, отсчёт времени нахождения значения в кэше не происходит и значение само не удаляется
    @Test
    public void dontExpireWhenSlave() throws InterruptedException {
        getType();
        //умножение на 2, чтобы наверняка необходимое время прошло
        Thread.sleep(2 * expireTimeUnit.toMillis(expireDuration));
        changeType(ColumnType.Int64);
        checkType(ColumnType.String);
    }

    private void fillTestData() {
        final String query = String.format("insert into %s(date, %s) values ('%s', '%s')", TABLE_NAME, COLUMN_NAME,
            "2020-04-07", "1");
        clickhouseTemplate.update(query);
    }

    private void truncateTable() {
        clickhouseTemplate.update("truncate table " + TABLE_NAME);
    }

    private void checkType(ColumnType columnType) {
        assertEquals(columnType, getType());
    }

    private ColumnType getType() {
        return splitTypeCache.get(new SplitTypeCache.Id(TABLE_NAME, COLUMN_NAME));
    }

    private void changeType(ColumnType columnType) {
        final String query = String.format("alter table %s modify column %s %s", TABLE_NAME, COLUMN_NAME, columnType);
        clickhouseTemplate.update(query);
    }

}
