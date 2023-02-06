package ru.yandex.market.mboc.common.offers.repository;

import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.offers.model.ProcessedOfferMarker;

/**
 * @author moskovkin@yandex-team.ru
 * @since 25.03.19
 */
public class ProcessedOffersRepositoryMock
    extends EmptyGenericMapperRepositoryMock<ProcessedOfferMarker, ProcessedOfferMarker.Key> {

    public ProcessedOffersRepositoryMock() {
        super(ProcessedOfferMarker::getKey);
    }
}
