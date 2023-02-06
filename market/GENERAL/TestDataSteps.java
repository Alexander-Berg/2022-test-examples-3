package ru.yandex.autotests.market.billing.backend.steps;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ru.yandex.autotests.market.billing.backend.data.testdata.BillingTags;
import ru.yandex.autotests.market.test.data.experiment.TestData;
import ru.yandex.autotests.market.test.data.experiment.query.Collection;
import ru.yandex.autotests.market.test.data.mongo.MarketMongoKey;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 2/18/15
 */
public class TestDataSteps {

    private Collection collection = Collection.TEMP;

    private TestDataSteps() {}
    private static class TestDataStepsHolder{
        private static final TestDataSteps INSTANCE = new TestDataSteps();
    }
    public static TestDataSteps getInstance(){
        TestData.registerTag(BillingTags.class);
        return TestDataStepsHolder.INSTANCE;
    }

    @Step("Сохраняем бин с тэгом {1} в монгу во временную коллекцию TEMP")
    public <T> void insert(T bean, Enum<? extends MarketMongoKey> tag) {
        TestData.unknown().on(collection).withTag(tag).insert(bean);
    }

    @Step("Сохраняем бины с тэгом {1} в монгу во временную коллекцию TEMP")
    public <T> void insert(List<T> beans, Enum<? extends MarketMongoKey> tag) {
        TestData.unknown().on(collection).withTag(tag).insert(beans);
    }

    @Step("Достаём бины с тэгом {0} из монги из временной коллекции TEMP")
    public <T> List<T> find(Enum<? extends MarketMongoKey> tag, Class<T> bean) {
        return TestData.unknown().on(collection).withTag(tag).find(bean);
    }

    @Step("Достаём бины с тэгом {0} и условием {2} из монги из временной коллекции TEMP")
    public <T> List<T> find(Enum<? extends MarketMongoKey> tag, Class<T> bean, Criteria criteria) {
        return TestData.unknown().on(collection).withQuery(Query.query(criteria)).withTag(tag).find(bean);
    }

    @Step("Удаляем бины с тэгом {1} из временной коллекции TEMP монги")
    public <T> void remove(Enum<? extends MarketMongoKey> tag) {
        TestData.unknown().on(collection).withTag(tag).remove();
    }


}
