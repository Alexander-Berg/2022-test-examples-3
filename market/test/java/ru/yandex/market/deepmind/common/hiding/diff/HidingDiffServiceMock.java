package ru.yandex.market.deepmind.common.hiding.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HidingDiffServiceMock implements HidingDiffService {
    private final Map<String, HidingDiff> results = new HashMap<>();

    @Override
    public HidingDiff calculateDiff(String reason) {
        var defaultHiding = new HidingDiff()
            .setReasonKey(reason)
            .setNewSskus(List.of())
            .setRemovedSskus(List.of())
            .setNewSskusByCatman(Map.of());

        return results.getOrDefault(reason, defaultHiding);
    }

    @Override
    public HidingDiff calculateDiffForCorefix(String reason) {
        return calculateDiff(reason);
    }

    public void add(String reason, HidingDiff hidingDiff) {
        results.put(reason, hidingDiff);
    }
}
