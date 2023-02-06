package ru.yandex.market.wms.common.spring.utils.uuid;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


public class FixedListTestUuidGenerator implements UuidGenerator {

    private List<UUID> fixedUUIDList;
    private int i = 0;

    public FixedListTestUuidGenerator(List<String> fixedUUIDString) {
        this.fixedUUIDList = fixedUUIDString.stream().map(UUID::fromString).collect(Collectors.toList());
        i = 0;
    }

    public void reset() {
        i = 0;
    }

    @Override
    public UUID generate() {
        if (i >= fixedUUIDList.size()) {
            i = 0;
        }
        return fixedUUIDList.get(i++);
    }
}
