package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import java.util.function.Predicate;

import org.junit.Test;

import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;

import static org.assertj.core.api.Assertions.assertThat;

public class DeduplicationOfferProcessingStrategyTest {

    private final Predicate<DatacampOfferDao.OfferInfo> isApplicableAddition = DatacampOfferDao.OfferInfo::isNew;

    private final OfferProcessingStrategy cskuProcessingStrategy = new CskuProcessingStrategy(isApplicableAddition,
        OfferProcessingStrategy.Priority.DEFAULT
    );

    private final OfferProcessingStrategy fastCardOfferProcessingStrategy = new FastOffersProcessingStrategy(
        isApplicableAddition,
        OfferProcessingStrategy.Priority.DEFAULT
    );

    private final OfferProcessingStrategy deduplicationStrategy =
        new DeduplicationOfferProcessingStrategy(OfferProcessingStrategy.Priority.DEFAULT);

    @Test
    public void checkDeduplicationTrueIfIsForceSend() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
            0L, null, null, null,
            null,
            null, null, null, 0L, true,
            true, false, false);

        boolean isDatacamp = cskuProcessingStrategy.isSuitable(offerInfo);
        boolean isFast = fastCardOfferProcessingStrategy.isSuitable(offerInfo);
        boolean isDeduplication = deduplicationStrategy.isSuitable(offerInfo);

        assertThat(!isDatacamp && !isFast && isDeduplication).isTrue();
    }

    @Test
    public void checkDeduplicationFalseIfIsNotForceSend() {
        DatacampOfferDao.OfferInfo offerInfo = new DatacampOfferDao.OfferInfo(
            0L, null, null, null,
            null,
            null, null, null, 0L, true,
            true, false, false);

        boolean isDatacamp = cskuProcessingStrategy.isSuitable(offerInfo);
        boolean isFast = fastCardOfferProcessingStrategy.isSuitable(offerInfo);
        boolean isDeduplication = deduplicationStrategy.isSuitable(offerInfo);

        assertThat((isDatacamp || isFast) && isDeduplication).isFalse();
    }

}
