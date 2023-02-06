package ru.yandex.market.mbo.db;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;

public class GuruVendorsReaderStub extends GuruVendorsReader {

    private Map<Long, Long> local2GlobalVendorsMap;
    private Map<Long, Long> local2GHidMap;

    private Set<Entity> entities;

    public GuruVendorsReaderStub() {
        entities = new HashSet<>();
        local2GlobalVendorsMap = new HashMap<>();
        local2GHidMap = new HashMap<>();
    }

    public void addVendor(Long globalId, Long hid, OptionImpl option) {
        entities.add(new Entity(globalId, hid, option));
        local2GlobalVendorsMap.put(option.getId(), globalId);
        local2GHidMap.put(option.getId(), hid);
    }

    @Override
    public Map<Long, Long> loadLocal2GlobalVendorsMap() {
        return local2GlobalVendorsMap;
    }

    @Override
    public long getCategoryHidFromGuruVendorId(long localVendorId) {
        return local2GHidMap.get(localVendorId);
    }

    @Override
    public OptionImpl getLocalVendor(long categoryId, long globalVendorId) {
        Entity entity = entities.stream()
                                .filter(e -> e.hid.equals(categoryId) && e.globalId.equals(globalVendorId))
                                .findFirst()
                                .orElse(null);
        return entity == null ? null : entity.option;
    }

    class Entity {
        Long globalId;
        Long hid;
        OptionImpl option;

        Entity(Long globalId, Long hid, OptionImpl option) {
            this.globalId = globalId;
            this.hid = hid;
            this.option = option;
        }
    }
}
