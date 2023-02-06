package ru.yandex.market.fintech.banksint.processor.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.OffersGenerationMapper;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.InstallmentsFileInfo;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceStatus;
import ru.yandex.market.fintech.banksint.service.installment.offer.OffersMdsS3Service;
import ru.yandex.market.fintech.banksint.util.InstallmentsUtils;


class OffersXlsmGenerationTaskTest extends FunctionalTest {

    @Autowired
    private OffersMdsS3Service mdsS3Service;

    @Autowired
    private OffersGenerationMapper mapper;

    @Test
    public void testGenertion() {
        String id = InstallmentsUtils.generateResourceId();
        var info = InstallmentsFileInfo.builder()
                .setResourceId(id)
                .setName("name")
                .setShopId(1L)
                .setBusinessId(2L)
                .build();
        mapper.insertNewOfferGenerationTask(info);
        var task = new OffersXlsmGenerationTask(info, mdsS3Service, mapper);

        task.run();

        Assertions.assertEquals(ResourceStatus.DONE, mapper.getTaskStatus(id));
    }

}
