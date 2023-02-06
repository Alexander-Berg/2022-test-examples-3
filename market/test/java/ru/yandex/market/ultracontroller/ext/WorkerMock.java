package ru.yandex.market.ultracontroller.ext;

import ru.yandex.market.ultracontroller.dao.OfferEntity;
import ru.yandex.market.ultracontroller.utils.RequestLogEntity;

public class WorkerMock implements Worker<OfferEntity> {
    @Override
    public void work(OfferEntity o, RequestLogEntity logEntity) {

    }
}
