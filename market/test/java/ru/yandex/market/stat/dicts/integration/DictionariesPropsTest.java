package ru.yandex.market.stat.dicts.integration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.integration.conf.PropertiesDictionariesUTestConfig;
import ru.yandex.market.stat.dicts.records.DistrPartners;
import ru.yandex.market.stat.dicts.utils.YtServiceUtils;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Created by kateleb on 25.05.17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PropertiesDictionariesUTestConfig.class)
@ActiveProfiles("integration-tests")
public class DictionariesPropsTest {

    @Test
    public void testModelsSchema() {
        YTreeListNode schema = YtServiceUtils.getTableSchema(Dictionary.fromClass(DistrPartners.class));
        Assert.assertThat("Can't find schema for models!", schema, notNullValue());
    }
}
