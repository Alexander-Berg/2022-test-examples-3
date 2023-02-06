package ru.yandex.market.ultracontroller.ext;

import ru.yandex.market.ultracontroller.dao.OfferEntity;
import ru.yandex.market.ultracontroller.utils.RequestLogEntity;

import java.util.List;

public class ListWorkerMock  extends ListWorker<OfferEntity> {

    public void work(List<OfferEntity> offers, RequestLogEntity logEntity) {

    }
}
