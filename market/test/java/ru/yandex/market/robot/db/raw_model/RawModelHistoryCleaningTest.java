package ru.yandex.market.robot.db.raw_model;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.postgresql.util.PGTimestamp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;
import ru.yandex.market.robot.db.raw_model.tables.MarketRelationHistoryTable;
import ru.yandex.market.robot.shared.raw_model.Status;
import ru.yandex.market.test.db.DatabaseTester;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
@RunWith(JUnitParamsRunner.class)
public class RawModelHistoryCleaningTest {
    private static final int SOME_MARKET_CATEGORY_ID = 90888;
    private static final int SOME_VENDOR_ID = 123;
    private static final int SOME_MODEL_ID = 321;
    private static final Status SOME_STATUS = Status.CONFIRMED;
    private static final int SOME_FIRST_VERSION_NUMBER = 1;
    private static final int SOME_LAST_VERSION_NUMBER = 2;
    private static final Status SOME_MARKET_CATEGORY_STATUS = Status.CONFIRMED;
    private static final int SOME_LAST_MODEL_UPDATE_TIME = 500;
    private static final int NUMBER_OF_OLD_ROWS = 5;
    private static final int DAYS_TO_REMOVE = 4;

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    DatabaseTester dataBase;

    @Autowired
    RawModelStorage rawModelStorage;

    @Before
    public void initializeModelData() {
        PGTimestamp t1 = new PGTimestamp(System.currentTimeMillis());
        PGTimestamp t2 = new PGTimestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DAYS_TO_REMOVE + 2));
        dataBase.modify(MarketRelationHistoryTable.entries()
            .repeatingValues(SOME_MARKET_CATEGORY_ID, SOME_VENDOR_ID, t1, SOME_MODEL_ID, SOME_STATUS,
                SOME_FIRST_VERSION_NUMBER, SOME_LAST_VERSION_NUMBER, SOME_MARKET_CATEGORY_STATUS,
                SOME_LAST_MODEL_UPDATE_TIME).times(2)
            .repeatingValues(SOME_MARKET_CATEGORY_ID, SOME_VENDOR_ID, t2, SOME_MODEL_ID, SOME_STATUS,
                SOME_FIRST_VERSION_NUMBER, SOME_LAST_VERSION_NUMBER, SOME_MARKET_CATEGORY_STATUS,
                SOME_LAST_MODEL_UPDATE_TIME).times(NUMBER_OF_OLD_ROWS)
        );
    }

    @Test
    public void whenCleaningTableHistoryShouldDeleteOnlyOldEntries() {
        assertEquals(rawModelStorage.historyClean(RawModelStorage.HistoryTable.MARKET_RELATION_HISTORY, DAYS_TO_REMOVE),
            NUMBER_OF_OLD_ROWS);
    }
}
