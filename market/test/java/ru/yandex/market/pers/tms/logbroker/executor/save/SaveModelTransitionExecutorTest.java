package ru.yandex.market.pers.tms.logbroker.executor.save;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.concurrent.TimeoutException;

public class SaveModelTransitionExecutorTest extends SaveTransitionExecutorTest {
    @Autowired
    SaveModelTransitionExecutor saveModelTransitionExecutor;
    
    @Test
    public void testConsuming() throws TimeoutException, InterruptedException {
        testConsumingSkeleton(saveModelTransitionExecutor, ModelStorage.ModelTransition.ModelType.MODEL);
    }

    @Test
    public void testConsumingSameData() throws TimeoutException, InterruptedException {
        testConsumingSameDataSkeleton(saveModelTransitionExecutor, ModelStorage.ModelTransition.ModelType.MODEL);
    }

    @Test
    public void testErrorWhileConsuming() throws TimeoutException, InterruptedException {
        testErrorWhileConsumingSkeleton(saveModelTransitionExecutor, ModelStorage.ModelTransition.ModelType.MODEL);
    }

    @Test
    public void testCheckSaveAfterConsume() throws TimeoutException, InterruptedException {
        testCheckSaveAfterConsumeSkeleton(saveModelTransitionExecutor, ModelStorage.ModelTransition.ModelType.MODEL);
    }
}