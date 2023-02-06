package ru.yandex.market.mbo.licensor2.name;

import ru.yandex.market.mbo.gwt.models.IdAndName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 06.11.17
 */
public class NameLicensorServiceMock implements NameLicensorService {
    private List<IdAndName> licensors = new ArrayList<>();
    private List<IdAndName> franchises = new ArrayList<>();
    private List<IdAndName> personages = new ArrayList<>();
    private List<IdAndName> vendors = new ArrayList<>();
    private List<IdAndName> categories = new ArrayList<>();

    @Override
    public List<IdAndName> getAllLicensors() {
        return new ArrayList<>(licensors);
    }

    @Override
    public List<IdAndName> getAllFranchises() {
        return new ArrayList<>(franchises);
    }

    @Override
    public List<IdAndName> getAllPersonages() {
        return new ArrayList<>(personages);
    }

    @Override
    public List<IdAndName> getAllCategories() {
        return new ArrayList<>(categories);
    }

    @Override
    public List<IdAndName> getVendors(Collection<Long> vendorIds) {
        return vendors.stream()
            .filter(vendor -> vendorIds.contains(vendor.getId()))
            .collect(Collectors.toList());
    }

    public NameLicensorServiceMock addLicensors(IdAndName... addedLicensors) {
        licensors.addAll(Arrays.asList(addedLicensors));
        return this;
    }

    public NameLicensorServiceMock addFranchises(IdAndName... addedFranchises) {
        franchises.addAll(Arrays.asList(addedFranchises));
        return this;
    }

    public NameLicensorServiceMock addPersonages(IdAndName... addedPersonages) {
        personages.addAll(Arrays.asList(addedPersonages));
        return this;
    }

    public NameLicensorServiceMock addVendors(IdAndName... addedVendors) {
        vendors.addAll(Arrays.asList(addedVendors));
        return this;
    }

    public NameLicensorServiceMock addCategories(IdAndName... addedCategories) {
        categories.addAll(Arrays.asList(addedCategories));
        return this;
    }
}
