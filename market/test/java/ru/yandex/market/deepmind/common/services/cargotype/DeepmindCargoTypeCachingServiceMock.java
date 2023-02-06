package ru.yandex.market.deepmind.common.services.cargotype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CargoTypeSnapshot;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 11.12.2019
 */
public class DeepmindCargoTypeCachingServiceMock implements DeepmindCargoTypeCachingService {
    private final Map<Long, CargoTypeSnapshot> types = new HashMap<>();

    public void put(CargoTypeSnapshot... type) {
        Stream.of(type).forEach(t -> types.put(t.getId(), t));
    }

    @Override
    public Set<Long> getMboParameterIds() {
        return types.values().stream()
            .map(CargoTypeSnapshot::getMboParameterId)
            .collect(Collectors.toSet());
    }

    @Override
    public Map<Long, Long> getMboParameterIdsToLmsIdsMap() {
        return types.values().stream()
            .collect(Collectors.toMap(CargoTypeSnapshot::getMboParameterId, CargoTypeSnapshot::getId, (a, b) -> a));
    }

    @Override
    public Map<Long, Long> getLmsIdsToMboParameterIdsMap() {
        return types.values().stream()
            .filter(c -> c.getMboParameterId() != null)
            .collect(Collectors.toMap(CargoTypeSnapshot::getId, CargoTypeSnapshot::getMboParameterId, (a, b) -> a));
    }

    @Override
    public List<CargoTypeSnapshot> all() {
        return new ArrayList<>(types.values());
    }

    @Override
    public Set<Long> getAllLmsIds() {
        return types.values().stream().map(CargoTypeSnapshot::getId).collect(Collectors.toSet());
    }
}
