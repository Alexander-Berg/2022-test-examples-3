package ru.yandex.market.mboc.common.masterdata.repository;

import ru.yandex.market.mbo.lightmapper.test.EmptyGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.masterdata.model.ProcessedMasterDataMarker;

/**
 * @author moskovkin@yandex-team.ru
 * @since 25.03.19
 */
public class ProcessedMasterDataRepositoryMock
    extends EmptyGenericMapperRepositoryMock<ProcessedMasterDataMarker, ProcessedMasterDataMarker.Key> {

    public ProcessedMasterDataRepositoryMock() {
        super(ProcessedMasterDataMarker::getKey);
    }
}
