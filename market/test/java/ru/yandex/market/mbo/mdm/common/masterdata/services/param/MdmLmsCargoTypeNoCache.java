package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.Collection;

import ru.yandex.market.mboc.common.masterdata.model.CargoType;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;

public class MdmLmsCargoTypeNoCache implements MdmLmsCargoTypeCache {
    private final CargoTypeRepository cargoTypeRepository;

    public MdmLmsCargoTypeNoCache(CargoTypeRepository cargoTypeRepository) {
        this.cargoTypeRepository = cargoTypeRepository;
    }

    @Override
    public Collection<CargoType> getAll() {
        return cargoTypeRepository.findAll();
    }

    @Override
    public void refresh() {
    }
}
