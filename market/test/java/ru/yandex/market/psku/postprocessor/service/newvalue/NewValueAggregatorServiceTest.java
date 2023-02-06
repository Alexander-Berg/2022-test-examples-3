package ru.yandex.market.psku.postprocessor.service.newvalue;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.psku.postprocessor.config.ManualTestConfig;

@ContextConfiguration(classes = ManualTestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class NewValueAggregatorServiceTest {

    @Autowired
    private NewValueAggregatorService newValueAggregatorService;

    //manual starter of find new value process for dev purpose, uses YT, do not remove ignore in master
    @Ignore
    @Test
    public void startManualFindingOfNewValue() {
        newValueAggregatorService.aggregateAndSave();
    }
}
