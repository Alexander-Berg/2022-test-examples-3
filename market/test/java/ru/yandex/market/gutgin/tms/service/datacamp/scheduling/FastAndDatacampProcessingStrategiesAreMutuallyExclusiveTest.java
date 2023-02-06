package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import java.util.function.Predicate;

import org.junit.Test;

import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;

import static org.assertj.core.api.Assertions.assertThat;

public class FastAndDatacampProcessingStrategiesAreMutuallyExclusiveTest {

    private final Predicate<DatacampOfferDao.OfferInfo> isApplicableAddition = DatacampOfferDao.OfferInfo::isNew;

    private final OfferProcessingStrategy cskuOfferProcessingStrategy = new CskuProcessingStrategy(
            isApplicableAddition,
            OfferProcessingStrategy.Priority.DEFAULT
    );

    private final OfferProcessingStrategy fastCardOfferProcessingStrategy = new FastOffersProcessingStrategy(
            isApplicableAddition,
            OfferProcessingStrategy.Priority.DEFAULT);

    @Test
    public void testStrategiesAreMutuallyExclusiveForDisabledCategory() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
                0L, null, null, null,
                null,
                null, null, null, 0L, true,
            false, false, false);

        boolean cskuOfferProcessingStrategySuitable = cskuOfferProcessingStrategy.isSuitable(offerInfo);
        boolean fastCardOfferProcessingStrategySuitable = fastCardOfferProcessingStrategy.isSuitable(offerInfo);

        assertThat(cskuOfferProcessingStrategySuitable && fastCardOfferProcessingStrategySuitable).isFalse();
        assertThat(cskuOfferProcessingStrategySuitable || fastCardOfferProcessingStrategySuitable).isTrue();
    }
}
