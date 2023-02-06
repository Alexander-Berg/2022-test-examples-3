package ru.yandex.direct.core.testing.steps;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.testing.fake.PerformanceFiltersRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.info.PerformanceFilterInfo;
import ru.yandex.direct.dbschema.ppc.enums.BidsPerformanceTargetFunnel;
import ru.yandex.direct.dbschema.ppc.tables.records.BidsPerformanceRecord;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestPerformanceFilters.defaultPerformanceFilter;

public class PerformanceFiltersSteps {

    @Autowired
    private PerformanceFiltersRepository performanceFiltersRepository;

    @Autowired
    private PerformanceFilterRepository performanceFilterRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private ShardHelper shardHelper;

    public static BidsPerformanceRecord getDefaultBidsPerformanceRecord() {
        BidsPerformanceRecord record = new BidsPerformanceRecord();
        record.setTargetFunnel(BidsPerformanceTargetFunnel.same_products);
        record.setName("Some test filter " + RandomStringUtils.randomAlphanumeric(7));
        record.setConditionJson("{\"price <->\":[\"0.00-3000.00\"]}");
        return record;
    }

    public BidsPerformanceRecord addDefaultBidsPerformance(AdGroupInfo adGroupInfo) {
        return addDefaultBidsPerformance(adGroupInfo.getShard(), adGroupInfo.getAdGroupId());
    }

    public BidsPerformanceRecord addDefaultBidsPerformance(int shard, Long adGroupId) {
        BidsPerformanceRecord record = getDefaultBidsPerformanceRecord();

        return addBidsPerformance(shard, adGroupId, record);
    }

    public BidsPerformanceRecord addBidsPerformance(AdGroupInfo adGroupInfo, BidsPerformanceRecord record) {
        return addBidsPerformance(adGroupInfo.getShard(), adGroupInfo.getAdGroupId(), record);
    }

    public BidsPerformanceRecord addBidsPerformance(int shard, Long adGroupId, BidsPerformanceRecord record) {
        Long newId = shardHelper.generatePerformanceFilterIds(1).get(0);
        record.setPid(adGroupId);
        record.setPerfFilterId(newId);

        performanceFiltersRepository.addBidsPerformanceRecord(shard, record);
        return record;
    }

    public PerformanceFilterInfo createDefaultPerformanceFilter() {
        return addPerformanceFilter(new PerformanceFilterInfo());
    }

    public PerformanceFilterInfo createDefaultPerformanceFilter(ClientInfo clientInfo) {
        PerformanceAdGroupInfo adGroupInfo = adGroupSteps.createDefaultPerformanceAdGroup(clientInfo);
        return addPerformanceFilter(adGroupInfo);
    }

    public PerformanceFilterInfo createPerformanceFilter(CampaignInfo campaignInfo, PerformanceFilter performanceFilter) {
        PerformanceAdGroupInfo adGroupInfo = adGroupSteps.createDefaultPerformanceAdGroup(campaignInfo);
        return addPerformanceFilter(new PerformanceFilterInfo()
                .withAdGroupInfo(adGroupInfo)
                .withFilter(performanceFilter));
    }

    public PerformanceFilterInfo addPerformanceFilter(PerformanceAdGroupInfo adGroupInfo) {
        return addPerformanceFilter(new PerformanceFilterInfo().withAdGroupInfo(adGroupInfo));
    }

    public PerformanceFilterInfo addPerformanceFilter(PerformanceFilterInfo filterInfo) {
        PerformanceFilter filter = filterInfo.getFilter();
        if (filter == null) {
            PerformanceAdGroupInfo adGroupInfo = filterInfo.getAdGroupInfo();
            if (adGroupInfo == null || adGroupInfo.getAdGroupId() == null) {
                adGroupInfo = adGroupSteps.createDefaultPerformanceAdGroup();
                filterInfo.withAdGroupInfo(adGroupInfo);
            }
            filter = defaultPerformanceFilter(adGroupInfo.getAdGroupId(), adGroupInfo.getFeedId());
            filterInfo.withFilter(filter);
        }
        if (filter.getPid() == null) {
            filter.withPid(filterInfo.getAdGroupId());
        }
        if (filter.getFeedId() == null) {
            filter.withFeedId(filterInfo.getAdGroupInfo().getFeedId());
        }
        int shard = filterInfo.getShard();
        performanceFilterRepository.addPerformanceFilters(shard, singletonList(filter));
        return filterInfo;
    }

    public Long addPerformanceFilter(int shard, PerformanceFilter filter) {
        return performanceFilterRepository.addPerformanceFilters(shard, singletonList(filter)).get(0);
    }

    public <V> void setPerformanceFilterProperty(PerformanceFilterInfo filterInfo, ModelProperty<?
            super PerformanceFilter, V> property, V value) {
        if (!PerformanceFilter.allModelProperties().contains(property)) {
            throw new IllegalArgumentException(
                    "Model " + PerformanceFilter.class.getName() + " doesn't contain property " + property.name());
        }
        PerformanceFilter filter = filterInfo.getFilter();
        AppliedChanges<PerformanceFilter> appliedChanges = new ModelChanges<>(filter.getId(), PerformanceFilter.class)
                .process(value, property)
                .applyTo(filter);
        performanceFilterRepository.update(filterInfo.getShard(), singletonList(appliedChanges));
    }

}
