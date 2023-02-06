package ru.yandex.market.mbo.billing.tarif;

import ru.yandex.market.mbo.gwt.models.billing.ExternalTariff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ayratgdl
 * @date 28.06.18
 */
public class ExternalTariffDAOMock implements ExternalTariffDAO {
    private Map<Long, ExternalTariff> tariffs = new HashMap<>();
    private long nextId = 1;

    @Override
    public List<ExternalTariff> loadAll() {
        return new ArrayList<>(tariffs.values());
    }

    @Override
    public Long save(ExternalTariff tariff) {
        Long id;
        if (tariff.getId() != null) {
            id = tariff.getId();
        } else {
            id = nextId++;
            tariff = new ExternalTariff(id, tariff.getName(), tariff.getPrice());
        }
        tariffs.put(id, tariff);
        return id;
    }

    @Override
    public boolean delete(long id) {
        return tariffs.remove(id) != null;
    }
}
