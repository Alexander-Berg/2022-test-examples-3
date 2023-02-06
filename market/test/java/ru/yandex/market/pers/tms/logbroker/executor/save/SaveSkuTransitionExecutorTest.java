package ru.yandex.market.pers.tms.logbroker.executor.save;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.concurrent.TimeoutException;

public class SaveSkuTransitionExecutorTest extends SaveTransitionExecutorTest {
    @Autowired
    SaveSkuTransitionExecutor saveSkuTransitionExecutor;

    @Test
    public void testConsuming() throws TimeoutException, InterruptedException {
        testConsumingSkeleton(saveSkuTransitionExecutor, ModelStorage.ModelTransition.ModelType.SKU);
    }

    @Test
    public void testConsumingSameData() throws TimeoutException, InterruptedException {
        testConsumingSameDataSkeleton(saveSkuTransitionExecutor, ModelStorage.ModelTransition.ModelType.SKU);
    }

    @Test
    public void testErrorWhileConsuming() throws TimeoutException, InterruptedException {
        testErrorWhileConsumingSkeleton(saveSkuTransitionExecutor, ModelStorage.ModelTransition.ModelType.SKU);
    }

    @Test
    public void testCheckSaveAfterConsume() throws TimeoutException, InterruptedException {
        testCheckSaveAfterConsumeSkeleton(saveSkuTransitionExecutor, ModelStorage.ModelTransition.ModelType.SKU);
    }
}