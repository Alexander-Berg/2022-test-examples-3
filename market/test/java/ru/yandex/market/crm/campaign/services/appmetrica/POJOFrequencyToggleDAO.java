package ru.yandex.market.crm.campaign.services.appmetrica;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ru.yandex.market.crm.campaign.services.sending.FrequencyToggleDAO;

/**
 * @author zloddey
 */
public class POJOFrequencyToggleDAO implements FrequencyToggleDAO {

    final Map<String, Boolean> flags = new HashMap<>();

    @Override
    public Optional<Boolean> get(String sendingId) {
        if (flags.containsKey(sendingId)) {
            return Optional.of(flags.get(sendingId));
        }
        return Optional.empty();
    }

    @Override
    public void set(String sendingId, boolean enable) {
        flags.put(sendingId, enable);
    }
}
