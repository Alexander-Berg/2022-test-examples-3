package ru.yandex.market.mbo.licensor2.scheme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ayratgdl
 * @date 07.02.18
 */
public class LicensorExtraDAOMock implements LicensorExtraDAO {
    private HashMap<LFP, List<LicensorCase.Extra>> data = new HashMap<>();

    @Override
    public void createExtra(LFP source, LicensorCase.Extra extra) {
        getExtras(source).add(extra);
    }

    @Override
    public void deleteExtras(LFP source) {
        data.remove(source);
    }

    @Override
    public Map<LFP, List<LicensorCase.Extra>> getAllExtras() {
        HashMap<LFP, List<LicensorCase.Extra>> copy = new HashMap<>();

        for (Map.Entry<LFP, List<LicensorCase.Extra>> entry : data.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        return copy;
    }

    private List<LicensorCase.Extra> getExtras(LFP source) {
        return data.computeIfAbsent(source, key -> new ArrayList<>());
    }
}
