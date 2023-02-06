package ru.yandex.market.marketpromo.core.dao;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;

import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateDatacampOfferList;

public class DatacampOfferDaoUpdateBatchTest extends ServiceTestBase {

    @Autowired
    private DatacampOfferDao offerDao;

    @ParameterizedTest
    @ValueSource(ints = {500, 1000, 3000, 5000})
    void shouldReplaceBatchOnExisted(int bathSize) {
        offerDao.replace(generateDatacampOfferList(bathSize));
        offerDao.replace(generateDatacampOfferList(bathSize));
    }

}
