package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

import ru.yandex.market.mboc.common.masterdata.model.CargoType;

public class MdmLmsCargoTypeCacheMock implements MdmLmsCargoTypeCache {

    private final Set<CargoType> storage;

    public MdmLmsCargoTypeCacheMock(Collection<CargoType> params) {
        storage = new HashSet<>(params);
    }

    @Override
    public Collection<CargoType> getAll() {
        return storage;
    }

    public void add(CargoType param) {
        Preconditions.checkArgument(!storage.contains(param));
        storage.add(param);
    }


    public void addAll(List<CargoType> params) {
        params.forEach(this::add);
    }

    @Override
    public void refresh() {

    }

}
