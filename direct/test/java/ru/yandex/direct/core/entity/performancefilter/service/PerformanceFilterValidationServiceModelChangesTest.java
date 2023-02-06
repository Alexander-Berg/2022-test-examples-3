package ru.yandex.direct.core.entity.performancefilter.service;

import java.math.BigDecimal;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter;
import ru.yandex.direct.core.entity.performancefilter.repository.PerformanceFilterRepository;
import ru.yandex.direct.core.testing.data.TestPerformanceFilters;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.testing.matchers.validation.Matchers;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_CAMP;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPA_PER_FILTER;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_CAMP;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_AVG_CPC_PER_FILTER;
import static ru.yandex.direct.core.entity.campaign.model.StrategyName.AUTOBUDGET_ROI;
import static ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter.AUTOBUDGET_PRIORITY;
import static ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter.PRICE_CPA;
import static ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter.PRICE_CPC;
import static ru.yandex.direct.core.entity.performancefilter.service.PerformanceFilterDefects.inconsistentCampaignStrategy;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.core.validation.assertj.ValidationResultConditions.warning;

@RunWith(JUnitParamsRunner.class)
public class PerformanceFilterValidationServiceModelChangesTest {
    private static final long CID = 111L;
    private static final long FEED_ID = 222L;
    private static final long AD_GROUP_ID = 333L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(444L);
    private static final long FILTER_ID = 555L;
    private static final BigDecimal PRICE_VALUE = BigDecimal.valueOf(20.0d);
    private static final Integer PRIORITY_VALUE = 2;

    private AdGroup adGroup = defaultPerformanceAdGroup(CID, FEED_ID).withId(AD_GROUP_ID);
    private DbStrategy strategy = (DbStrategy) new DbStrategy().withStrategyName(AUTOBUDGET_AVG_CPA_PER_CAMP);
    private ModelChanges<PerformanceFilter> modelChanges = new ModelChanges<>(FILTER_ID, PerformanceFilter.class);
    private List<ModelChanges<PerformanceFilter>> modelChangesList = singletonList(modelChanges);
    private PerformanceFilter oldFilter =
            TestPerformanceFilters.defaultPerformanceFilter(AD_GROUP_ID, FEED_ID)
                    .withId(FILTER_ID);
    private PerformanceFilterValidationService performanceFilterValidationService;

    @Before
    public void setUp() throws Exception {
        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByClientIdStrictly(any(ClientId.class))).thenReturn(1);
        AdGroupRepository adGroupRepository = mock(AdGroupRepository.class);
        when(adGroupRepository.getAdGroupSimple(anyInt(), any(ClientId.class), anyCollection()))
                .thenReturn(singletonMap(AD_GROUP_ID, adGroup));
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        when(campaignRepository.getStrategyByFilterIds(anyInt(), anyCollection()))
                .thenReturn(singletonMap(FILTER_ID, strategy));
        PerformanceFilterRepository performanceFilterRepository = mock(PerformanceFilterRepository.class);
        when(performanceFilterRepository.getFiltersById(anyInt(), anyCollection()))
                .thenReturn(singletonList(oldFilter));

        performanceFilterValidationService = new PerformanceFilterValidationService(shardHelper,
                mock(ClientService.class),
                adGroupRepository,
                campaignRepository,
                performanceFilterRepository,
                mock(PerformanceFilterStorage.class),
                mock(CampaignSubObjectAccessCheckerFactory.class));
    }

    @Test
    public void validateModelChanges_success_whenEmptyChangesList() {
        ValidationResult<List<ModelChanges<PerformanceFilter>>, Defect> vr =
                performanceFilterValidationService.validateModelChanges(CLIENT_ID, emptyList());
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @Test
    public void validateModelChanges_success_whenNoChanges() {
        ValidationResult<List<ModelChanges<PerformanceFilter>>, Defect> vr = validateModelChanges();
        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @SuppressWarnings("unused")
    private Object getValidateModelChangesParameters() {
        return Arrays.array(
                new Object[]{AUTOBUDGET_AVG_CPA_PER_CAMP, PRICE_CPA, PRICE_VALUE, true},
                new Object[]{AUTOBUDGET_AVG_CPA_PER_CAMP, PRICE_CPC, PRICE_VALUE, false},
                new Object[]{AUTOBUDGET_AVG_CPA_PER_CAMP, AUTOBUDGET_PRIORITY, PRIORITY_VALUE, false},

                new Object[]{AUTOBUDGET_AVG_CPA_PER_FILTER, PRICE_CPA, PRICE_VALUE, true},
                new Object[]{AUTOBUDGET_AVG_CPA_PER_FILTER, PRICE_CPC, PRICE_VALUE, false},
                new Object[]{AUTOBUDGET_AVG_CPA_PER_FILTER, AUTOBUDGET_PRIORITY, PRIORITY_VALUE, false},

                new Object[]{AUTOBUDGET_AVG_CPC_PER_CAMP, PRICE_CPC, PRICE_VALUE, true},
                new Object[]{AUTOBUDGET_AVG_CPC_PER_CAMP, PRICE_CPA, PRICE_VALUE, false},
                new Object[]{AUTOBUDGET_AVG_CPC_PER_CAMP, AUTOBUDGET_PRIORITY, PRIORITY_VALUE, false},

                new Object[]{AUTOBUDGET_AVG_CPC_PER_FILTER, PRICE_CPC, PRICE_VALUE, true},
                new Object[]{AUTOBUDGET_AVG_CPC_PER_FILTER, PRICE_CPA, PRICE_VALUE, false},
                new Object[]{AUTOBUDGET_AVG_CPC_PER_FILTER, AUTOBUDGET_PRIORITY, PRIORITY_VALUE, false},

                new Object[]{AUTOBUDGET_ROI, AUTOBUDGET_PRIORITY, PRIORITY_VALUE, true},
                new Object[]{AUTOBUDGET_ROI, PRICE_CPA, PRICE_VALUE, false},
                new Object[]{AUTOBUDGET_ROI, PRICE_CPC, PRICE_VALUE, false}
        );
    }

    @Test
    @Parameters(method = "getValidateModelChangesParameters")
    public <T> void validateModelChanges(StrategyName strategyName, ModelProperty<PerformanceFilter, T> property,
                                         T value, boolean expectedEmptyWarnings) {
        strategy.withStrategyName(strategyName);
        modelChanges.process(value, property);

        ValidationResult<List<ModelChanges<PerformanceFilter>>, Defect> vr = validateModelChanges();

        String reason = String.format("StrategyName=%s, ModelProperty=%s, expectedEmptyWarnings=%s",
                strategyName.name(), property.name(), expectedEmptyWarnings);
        if (expectedEmptyWarnings) {
            MatcherAssert.assertThat(reason, vr, Matchers.hasNoErrorsAndWarnings());
        } else {
            assertThat(vr).as(reason).has(warning(inconsistentCampaignStrategy()));
        }
    }

    private ValidationResult<List<ModelChanges<PerformanceFilter>>, Defect> validateModelChanges() {
        return performanceFilterValidationService.validateModelChanges(CLIENT_ID, modelChangesList);
    }


}
