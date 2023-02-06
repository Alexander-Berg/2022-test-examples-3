package ru.yandex.market.mboc.common.masterdata.repository;

import ru.yandex.market.mbo.lightmapper.test.IntGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.masterdata.model.CargoType;

public class CargoTypeRepositoryMock extends IntGenericMapperRepositoryMock<CargoType> implements CargoTypeRepository {
    public CargoTypeRepositoryMock() {
        super(null, CargoType::getId);
    }

    @Override
    public CargoType findByMboParameterId(Long mboParameterId) {
        return findAll().stream().filter(c -> mboParameterId.equals(c.getMboParameterId())).findAny().orElse(null);
    }
}
