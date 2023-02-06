package ru.yandex.market.mbo.db.modelstorage.compatibility.dao;

import ru.yandex.market.mbo.db.modelstorage.compatibility.Compatibility;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ModelCompatibilityDAOMock implements ModelCompatibilityDAO {

    private final List<Compatibility> compatibilities = new LinkedList<>();

    private final AtomicInteger idSeq = new AtomicInteger();

    @Override
    public List<Compatibility> getCompatibilities() {
        return compatibilities;
    }

    @Override
    public List<Compatibility> getCompatibilitiesByModelId(long modelId) {
        return getCompatibilitiesByModelIds(Collections.singleton(modelId));
    }

    @Override
    public List<Compatibility> getCompatibilitiesByModelIds(Collection<Long> modelIds) {
        return compatibilities.stream()
            .filter(c -> modelIds.contains(c.getModelId1()) || modelIds.contains(c.getModelId2()))
            .collect(Collectors.toList());
    }

    @Override
    public Compatibility getCompatibility(long id) {
        return compatibilities.stream()
            .filter(c -> c.getId() == id)
            .findFirst()
            .orElse(null);
    }

    @Override
    public List<Compatibility> getCompatibilities(Collection<Long> ids) {
        return compatibilities.stream()
            .filter(c -> ids.contains(c.getId()))
            .collect(Collectors.toList());
    }

    @Override
    public Compatibility getCompatibility(long modelId1, long modelId2) {
        return compatibilities.stream()
            .filter(c -> c.getModelId1() == modelId1 && c.getModelId2() == modelId2)
            .findAny()
            .orElse(null);
    }

    @Override
    public void deleteAndSaveCompatibilities(long modelId, List<Compatibility> compatibilities) {
        deleteCompatibilities(modelId);
        saveCompatibilities(compatibilities);
    }

    @Override
    public int deleteCompatibilities(long modelId) {
        List<Compatibility> modelCompatibilities = getCompatibilitiesByModelId(modelId);
        this.compatibilities.removeAll(modelCompatibilities);
        return modelCompatibilities.size();
    }

    @Override
    public int deleteCompatibilities(Collection<Long> modelIds) {
        return modelIds.stream()
            .mapToInt(this::deleteCompatibilities)
            .sum();
    }

    @Override
    public int recoverCompatibilities(Collection<Long> compatibilityId) {
        return (int) compatibilities.stream()
            .filter(c -> compatibilityId.contains(c.getId()))
            .peek(c -> c.setValid(true))
            .count();
    }

    @Override
    public int markCompatibilitiesValid(long modelId, boolean valid) {
        List<Compatibility> modelCompatibilities = getCompatibilitiesByModelId(modelId);
        modelCompatibilities.forEach(c -> c.setValid(valid));
        return modelCompatibilities.size();
    }

    @Override
    public void saveCompatibilities(Collection<Compatibility> compatibilities) {
        compatibilities.forEach(c -> c.setId(idSeq.getAndIncrement()));
        this.compatibilities.addAll(compatibilities);
    }

    @Override
    public List<Compatibility> getInvalidCompatibilities() {
        return compatibilities.stream()
            .filter(cm -> !cm.isValid())
            .collect(Collectors.toList());
    }

}
