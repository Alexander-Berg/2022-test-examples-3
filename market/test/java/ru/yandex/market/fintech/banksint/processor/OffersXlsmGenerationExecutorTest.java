package ru.yandex.market.fintech.banksint.processor;

import java.time.Clock;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.OffersGenerationMapper;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceStatus;
import ru.yandex.market.fintech.banksint.service.installment.offer.OffersMdsS3Service;

class OffersXlsmGenerationExecutorTest extends FunctionalTest {

    @Autowired
    private Clock clock;

    @Qualifier("mockOffersExecutorService")
    @Autowired
    private ExecutorService executor;

    @Autowired
    private OffersMdsS3Service offersMdsS3Service;

    @Autowired
    private OffersGenerationMapper mapper;

    private OffersXlsmGenerationExecutor testExecutor;

    @BeforeEach
    void setUp() {
        testExecutor = new OffersXlsmGenerationExecutor(executor, clock, offersMdsS3Service, mapper);
    }

    @Test
    void testSubmit() {
        long shopId = 1;
        long businessId = 2;

        var info = testExecutor.submitNewXlsmTask(shopId, businessId);

        Assertions.assertNotNull(mapper.getInstallmentFileInfoByResourceId(info.getResourceId()));
        Assertions.assertEquals(ResourceStatus.PENDING, info.getStatus());
        Assertions.assertEquals(ResourceStatus.PENDING, mapper.getTaskStatus(info.getResourceId()));
    }

    @Test
    void testResubmit() {
        long shopId = 1;
        long businessId = 2;

        var info = testExecutor.submitNewXlsmTask(shopId, businessId);

        mapper.updateGenerationTaskStatus(info.getResourceId(), ResourceStatus.PROCESSING);
        Assertions.assertEquals(ResourceStatus.PROCESSING, mapper.getTaskStatus(info.getResourceId()));
        testExecutor.resubmitXlsmTask(info.getResourceId());
        Assertions.assertEquals(ResourceStatus.PENDING, mapper.getTaskStatus(info.getResourceId()));
    }

}
