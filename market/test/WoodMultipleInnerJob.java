package ru.yandex.market.tsum.pipelines.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ru.yandex.market.tsum.pipe.engine.definition.resources.Produces;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipelines.common.jobs.tsum.TsumMultipleInnerReleaseJob;
import ru.yandex.market.tsum.pipelines.wood.resources.Wood;

@Produces(multiple = {Wood.class})
public class WoodMultipleInnerJob extends TsumMultipleInnerReleaseJob {
    @Override
    protected Map<String, Collection<? extends Resource>> getLaunchToResourcesMap() {
        Collection<Wood> testResources = new ArrayList<>();
        testResources.add(new Wood(2, false));
        Collection<Wood> prodResources = new ArrayList<>();
        prodResources.add(new Wood(6, false));
        return Map.of(
            "TESTING", testResources,
            "PRODUCTION", prodResources
        );
    }

    @Override
    protected Set<Class<? extends Resource>> getProducedResourcesClasses() {
        Set<Class<? extends Resource>> targetResources = new HashSet<>();
        targetResources.add(Wood.class);
        return targetResources;
    };

    @Override
    public UUID getSourceCodeId() {
        return UUID.fromString("6c56cf27-edd9-4a28-9582-7d7d1c36ad37");
    }
}
