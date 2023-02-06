package ru.yandex.market.robot.db.raw_model;

import com.ninja_squad.dbsetup.generator.ValueGenerators;
import com.ninja_squad.dbsetup.operation.Insert;
import io.qameta.allure.Step;
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
import ru.yandex.market.test.util.random.RandomBean;
import ru.yandex.market.robot.db.RawModelStorage;
import ru.yandex.market.robot.db.raw_model.config.MarketRawModelAccessorsTestConfig;
import ru.yandex.market.robot.db.raw_model.tables.Columns;
import ru.yandex.market.robot.db.raw_model.tables.MarketRelationModelTable;
import ru.yandex.market.robot.db.raw_model.tables.ModelStatusBinder;
import ru.yandex.market.robot.shared.raw_model.MarketModel;
import ru.yandex.market.robot.shared.raw_model.Status;
import ru.yandex.market.test.db.DatabaseTester;

import java.util.Arrays;

import static ru.yandex.market.robot.db.raw_model.tables.Columns.*;
import static ru.yandex.market.test.db.DatabaseTester.Utils.table;

/**
 * @author jkt on 07.12.17.
 */

@TestExecutionListeners({
    DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class,
    TransactionalTestExecutionListener.class})
@ContextConfiguration(classes = MarketRawModelAccessorsTestConfig.class)
@Transactional
public class SaveMarketModelsTest {

    private static final String MARKET_RELATION_MODEL_HISTORY = "market_relation_model_history";

    private static final int MODEL_ID = 3;
    private static final int ANOTHER_MODEL_ID = 44;


    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();


    @Autowired
    DatabaseTester dataBase;

    @Autowired
    RawModelStorage rawModelStorage;

    @Test
    public void whenSavingShouldAddModelToMarketRelationModel() {
        MarketModel model = randomModel(MODEL_ID);

        saveMarketModels(model);

        dataBase.checkStateMatches(MarketRelationModelTable.entryFor(model));
    }

    @Test
    public void whenSavingMultipleModelsShouldAddMultipleEntries() {
        dataBase.modify(
            marketRelationModel()
                .columns(Columns.MODEL_ID)
                .repeatingValues(MODEL_ID).times(5)
                .repeatingValues(ANOTHER_MODEL_ID).times(5));

        saveMarketModels(randomModel(MODEL_ID), randomModel(ANOTHER_MODEL_ID));

        dataBase.checkStateMatches(
            MarketRelationModelTable.entry()
                .columns(Columns.MODEL_ID)
                .values(MODEL_ID)
                .values(ANOTHER_MODEL_ID)
        );
    }

    @Test
    public void whenSavingModelShouldClearOldEntriesForModel() {
        dataBase.modify(
            marketRelationModel()
                .columns(Columns.MODEL_ID)
                .repeatingValues(MODEL_ID).times(5)
        );

        saveMarketModels(randomModel(MODEL_ID));

        dataBase.checkStateMatches(
            MarketRelationModelTable.entry()
                .columns(Columns.MODEL_ID)
                .values(MODEL_ID)
        );
    }

    @Test
    public void whenSavingModelShouldNotAffectOtherModels() {
        dataBase.modify(
            marketRelationModel()
                .columns(Columns.MODEL_ID)
                .repeatingValues(ANOTHER_MODEL_ID).times(5)
        );

        saveMarketModels(randomModel(MODEL_ID));

        dataBase.checkStateMatches(
            MarketRelationModelTable.entry()
                .columns(Columns.MODEL_ID)
                .values(MODEL_ID)
                .repeatingValues(ANOTHER_MODEL_ID).times(5)
        );
    }

    @Test
    public void whenSavingModelShouldNotAddAnythingToMarketRelationModelHistory() {
        saveMarketModels(randomModel(MODEL_ID));

        dataBase.checkStateMatches(
            table(MARKET_RELATION_MODEL_HISTORY)
                .columns(Columns.MODEL_ID)
        );
    }

    private MarketModel randomModel(int modelId) {
        MarketModel marketModel = RandomBean.generateComplete(MarketModel.class);
        marketModel.setId(modelId);

        return marketModel;
    }

    @Step("Сохраняем модели {marketModels}")
    private void saveMarketModels(MarketModel... marketModels) {
        rawModelStorage.saveMarketModels(Arrays.asList(marketModels));
    }


    private Insert.Builder marketRelationModel() {
        return MarketRelationModelTable.entry()
            .withGeneratedValue(MARKET_MODEL_ID, ValueGenerators.sequence())
            .withGeneratedValue(MARKET_MODIFICATION_ID, ValueGenerators.sequence())
            .withDefaultValue(STATUS, Status.UNCONFIRMED)
            .withDefaultValue(CREATED, false)
            .withDefaultValue(FIRST_VERSION_NUMBER, 1)
            .withDefaultValue(LAST_VERSION_NUMBER, 1)
            .withDefaultValue(PICTURES_COUNT, 1)

            .withBinder(new ModelStatusBinder(), STATUS);
    }
}
