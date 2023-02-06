package ru.yandex.market.robot.db.raw_model;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
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
import ru.yandex.market.robot.db.raw_model.tables.MatchingLogTable;
import ru.yandex.market.robot.db.raw_model.tables.SessionLogTable;
import ru.yandex.market.test.db.DatabaseTester;

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static ru.yandex.market.test.db.DatabaseTester.Utils.insert;

/**
 * @author jkt on 07.12.17.
 */

@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
public class ClearSessionLogsTest {

    private static final int CATEGORY_ID = 11;
    private static final int ANOTHER_CATEGORY_ID = 22;

    private static final int VENDOR_ID = 222;
    private static final int OFFERS_COUNT = 1;

    private static final long MODEL_ID = 3;
    private static final int MATCHED_COUNT = 10;


    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();


    @Autowired
    DatabaseTester dataBase;

    @Autowired
    RawModelStorage rawModelStorage;

    @Test
    public void whenClearsLogShouldClearSessionLogForCategory() {
        dataBase.modify(
            insert(SessionLogTable.entries()
                .values(CATEGORY_ID, VENDOR_ID, OFFERS_COUNT)
                .values(ANOTHER_CATEGORY_ID, VENDOR_ID, OFFERS_COUNT))
        );

        rawModelStorage.clearSessionLogs(CATEGORY_ID);

        dataBase.checkStateMatches(
            SessionLogTable.entries()
                .values(ANOTHER_CATEGORY_ID, VENDOR_ID, OFFERS_COUNT)
        );
    }

    @Test
    public void whenClearsSessionLogShouldClearMultipleEntries() {
        dataBase.modify(
            insert(SessionLogTable.entries()
                .values(CATEGORY_ID, VENDOR_ID, OFFERS_COUNT)
                .values(CATEGORY_ID, VENDOR_ID, OFFERS_COUNT))
        );

        rawModelStorage.clearSessionLogs(CATEGORY_ID);

        dataBase.checkStateMatches(SessionLogTable.empty());
    }

    @Test
    public void whenClearsLogShouldClearMatchingLogForCategory() {
        dataBase.modify(
            insert(MatchingLogTable.entries()
                .values(CATEGORY_ID, VENDOR_ID, MODEL_ID, MATCHED_COUNT)
                .values(ANOTHER_CATEGORY_ID, VENDOR_ID, MODEL_ID, MATCHED_COUNT))
        );

        rawModelStorage.clearSessionLogs(CATEGORY_ID);

        dataBase.checkStateMatches(
            MatchingLogTable.entries()
                .values(ANOTHER_CATEGORY_ID, VENDOR_ID, MODEL_ID, MATCHED_COUNT)
        );
    }

    @Test
    public void whenClearsMatchingLogShouldClearMultipleEntries() {
        dataBase.modify(
            insert(MatchingLogTable.entries()
                .values(CATEGORY_ID, VENDOR_ID, MODEL_ID, MATCHED_COUNT)
                .values(CATEGORY_ID, VENDOR_ID, MODEL_ID, MATCHED_COUNT))
        );

        rawModelStorage.clearSessionLogs(CATEGORY_ID);

        dataBase.checkStateMatches(MatchingLogTable.empty());
    }

    @Test
    public void whenLogTablesAreEmptyShouldNotFail() {
        dataBase.modify(
            deleteAllFrom(SessionLogTable.NAME, MatchingLogTable.NAME)
        );

        rawModelStorage.clearSessionLogs(CATEGORY_ID);

        dataBase.checkStateMatches(
            MatchingLogTable.empty(),
            SessionLogTable.empty()
        );
    }
}
