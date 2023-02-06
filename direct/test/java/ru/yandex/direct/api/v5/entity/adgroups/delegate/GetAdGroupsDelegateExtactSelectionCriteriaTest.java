package ru.yandex.direct.api.v5.entity.adgroups.delegate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.AdGroupAppIconStatusSelectionEnum;
import com.yandex.direct.api.v5.adgroups.AdGroupStatusSelectionEnum;
import com.yandex.direct.api.v5.adgroups.GetRequest;
import com.yandex.direct.api.v5.general.AdGroupTypesEnum;
import com.yandex.direct.api.v5.general.ServingStatusEnum;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.ServingStatus;
import ru.yandex.direct.core.entity.adgroup.container.AdGroupsSelectionCriteria;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupAppIconStatus;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupStatus;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Api5Test
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class GetAdGroupsDelegateExtactSelectionCriteriaTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final List<Long> listForAdGroupIds = asList(1L, 2L, 3L);
    private static final Set<Long> setForAdGroupIds = new HashSet<>(listForAdGroupIds);

    private static final List<Long> listForAdGroupIdsWithDuplicate = asList(1L, 1L, 2L);
    private static final Set<Long> setForAdGroupIdsWithDuplicate = new HashSet<>(asList(1L, 2L));

    private static final List<Long> listForCampaignIds = asList(10L, 20L, 0L);
    private static final Set<Long> setForCampaignIds = new HashSet<>(listForCampaignIds);

    private static final List<Long> listForCampaignIdsWithDuplicate = asList(10L, 10L, 20L);
    private static final Set<Long> setForCampaignIdsWithDuplicate = new HashSet<>(asList(10L, 20L));

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria selectionCriteria;

    @Parameterized.Parameter(2)
    public AdGroupsSelectionCriteria expectedSelectionCriteria;

    @Autowired
    private GetAdGroupsDelegate delegate;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"with adgroup ids", buildSelectionCriteria().withIds(listForAdGroupIds),
                        new AdGroupsSelectionCriteria().withAdGroupIds(setForAdGroupIds)},
                {"with adgroup ids with duplicate", buildSelectionCriteria().withIds(listForAdGroupIdsWithDuplicate),
                        new AdGroupsSelectionCriteria().withAdGroupIds(setForAdGroupIdsWithDuplicate)},
                {"with campaign ids", buildSelectionCriteria().withIds(listForCampaignIds),
                        new AdGroupsSelectionCriteria().withAdGroupIds(setForCampaignIds)},
                {"with campaign ids with duplicate", buildSelectionCriteria().withIds(listForCampaignIdsWithDuplicate),
                        new AdGroupsSelectionCriteria().withAdGroupIds(setForCampaignIdsWithDuplicate)},
                {"with all criterias", buildSelectionCriteria()
                        .withIds(1L)
                        .withCampaignIds(10L)
                        .withTypes(AdGroupTypesEnum.TEXT_AD_GROUP)
                        .withStatuses(AdGroupStatusSelectionEnum.ACCEPTED)
                        .withAppIconStatuses(AdGroupAppIconStatusSelectionEnum.ACCEPTED)
                        .withServingStatuses(ServingStatusEnum.ELIGIBLE)
                        .withNegativeKeywordSharedSetIds(2L, 6L),
                        new AdGroupsSelectionCriteria()
                                .withAdGroupIds(1L)
                                .withCampaignIds(10L)
                                .withAdGroupTypes(AdGroupType.BASE)
                                .withAdGroupStatuses(AdGroupStatus.ACCEPTED)
                                .withAdGroupAppIconStatuses(AdGroupAppIconStatus.ACCEPTED)
                                .withAdGroupServingStatuses(ServingStatus.ELIGIBLE)
                                .withNegativeKeywordSharedSetIds(2L, 6L)},
        };
    }

    private static com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria buildSelectionCriteria() {
        return new com.yandex.direct.api.v5.adgroups.AdGroupsSelectionCriteria();
    }

    @Test
    public void test() {
        assertThat(delegate.extractSelectionCriteria(new GetRequest().withSelectionCriteria(selectionCriteria)))
                .usingRecursiveComparison()
                .isEqualTo(expectedSelectionCriteria);
    }
}
