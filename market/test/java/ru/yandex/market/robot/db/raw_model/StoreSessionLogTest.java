package ru.yandex.market.robot.db.raw_model;

import com.ninja_squad.dbsetup.operation.Insert;
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
import ru.yandex.utils.ObjectCounter;

import static ru.yandex.market.test.db.DatabaseTester.Utils.entries;
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
public class StoreSessionLogTest {

    private static final int CATEGORY_ID = 11;
    private static final int VENDOR_ID = 222;
    private static final int OFFERS_COUNT = 1;

    private static final long MODEL_ID = 3;
    private static final int MATCHED_COUNT = 10;

    private static final long MODEL_ID_2 = 4;
    private static final int MATCHED_COUNT_2 = 5;


    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();


    @Autowired
    DatabaseTester dataBase;

    @Autowired
    RawModelStorage rawModelStorage;

    @Test
    public void whenStoresLogShouldUpdateSessionLog() {
        rawModelStorage.storeSessionLog(CATEGORY_ID, VENDOR_ID, new ObjectCounter<>(), OFFERS_COUNT);

        dataBase.checkStateMatches(
            SessionLogTable.entries()
                .values(CATEGORY_ID, VENDOR_ID, OFFERS_COUNT));
    }

    @Test
    public void whenMultipleModelsMatchShouldAddMultipleMatchingLogEntries() {
        ObjectCounter<Long> matchedModels = new ObjectCounter<>();
        matchedModels.increase(MODEL_ID, MATCHED_COUNT);
        matchedModels.increase(MODEL_ID_2, MATCHED_COUNT_2);

        rawModelStorage.storeSessionLog(CATEGORY_ID, VENDOR_ID, matchedModels, OFFERS_COUNT);

        dataBase.checkStateMatches(
            MatchingLogTable.entries()
                .values(CATEGORY_ID, VENDOR_ID, MODEL_ID, MATCHED_COUNT)
                .values(CATEGORY_ID, VENDOR_ID, MODEL_ID_2, MATCHED_COUNT_2)
        );
    }

    @Test
    public void whenSessionLogEntryAlreadyExistShouldDuplicateEntry() {
        Insert sessionLogEntry = SessionLogTable.entries().values(CATEGORY_ID, VENDOR_ID, OFFERS_COUNT).build();
        dataBase.modify(insert(sessionLogEntry));

        rawModelStorage.storeSessionLog(CATEGORY_ID, VENDOR_ID, new ObjectCounter<>(), OFFERS_COUNT);

        dataBase.checkStateMatches(entries(sessionLogEntry, sessionLogEntry));
    }
}
