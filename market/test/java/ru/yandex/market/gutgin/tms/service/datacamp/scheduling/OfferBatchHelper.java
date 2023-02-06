package ru.yandex.market.gutgin.tms.service.datacamp.scheduling;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.partner.content.common.db.dao.dcp.DatacampOfferDao;

public class OfferBatchHelper {

    private OfferBatchHelper() {
    }

    @NotNull
    public static DatacampOfferDao.OfferInfo getNoStrategyOffer(long id) {
        return new DatacampOfferDao.OfferInfo(id, 0, null, null, null, 0, null, null, null, null, false, false, false);
    }

    @NotNull
    public static DatacampOfferDao.OfferInfo getNewFastCardStrategyOffer(long id) {
        return new DatacampOfferDao.OfferInfo(id, 0, null, null, null, 0, null, null, null, null, false, false, true);
    }

    @NotNull
    public static DatacampOfferDao.OfferInfo getNewDatacampStrategyOffer(long id, long groupId) {
        return new DatacampOfferDao.OfferInfo(id, 0, null, groupId, null, 0, null, null, null, true, false, false, false);
    }

    @NotNull
    public static DatacampOfferDao.OfferInfo getEditDatacampStrategyOffer(long id, long groupId) {
        return new DatacampOfferDao.OfferInfo(id, 0, null, groupId, null, 0, 0L, null, null, true, false, false, false);
    }
}
