package ru.yandex.direct.core.entity.adgroup.service.complex.internal;

import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupAddItem;
import ru.yandex.direct.core.entity.adgroup.container.InternalAdGroupUpdateItem;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestGroups.activeInternalAdGroup;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Component
public class ComplexInternalAdGroupServiceTestHelper {
    private static final String AD_GROUP_OLD_NAME = "old-name";
    private static final String AD_GROUP_NEW_NAME = "new-name";
    private static final long AD_GROUP_NEW_LEVEL = 888L;

    private final AdGroupRepository adGroupRepository;
    private final AdGroupAdditionalTargetingRepository additionalTargetingRepository;

    ComplexInternalAdGroupServiceTestHelper(
            AdGroupRepository adGroupRepository,
            AdGroupAdditionalTargetingRepository additionalTargetingRepository) {
        this.adGroupRepository = adGroupRepository;
        this.additionalTargetingRepository = additionalTargetingRepository;
    }

    public static InternalAdGroupAddItem internalAdGroupAddItemWithoutTargetings(Long campaignId) {
        return new InternalAdGroupAddItem()
                .withAdGroup(activeInternalAdGroup(campaignId)
                        .withName(AD_GROUP_OLD_NAME)
                        .withStatusBsSynced(StatusBsSynced.NO))
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(emptyList());
    }

    public static InternalAdGroupUpdateItem internalAdGroupUpdateItemWithoutTargetings(Long adGroupId) {
        ModelChanges<InternalAdGroup> modelChanges = new ModelChanges<>(adGroupId, InternalAdGroup.class)
                .process(AD_GROUP_NEW_NAME, InternalAdGroup.NAME)
                .process(AD_GROUP_NEW_LEVEL, InternalAdGroup.LEVEL);
        return new InternalAdGroupUpdateItem()
                .withAdGroupChanges(modelChanges)
                .withAdditionalTargetings(emptyList())
                .withRetargetingConditions(emptyList());
    }

    public void checkInternalAdGroupAndTargetingInDb(int shard, AdGroup expectedAdGroup,
                                                     List<AdGroupAdditionalTargeting> additionalTargetings) {
        Long adGroupId = expectedAdGroup.getId();

        List<AdGroup> adGroups = adGroupRepository.getAdGroups(shard, singletonList(adGroupId));
        assertThat("группа успешно добавлена", adGroups,
                contains(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));

        List<AdGroupAdditionalTargeting> targetings = additionalTargetingRepository.getByAdGroupId(shard, adGroupId);

        if (additionalTargetings.isEmpty()) {
            assertThat("в группе не должно быть таргетингов", targetings, empty());
        } else {
            //noinspection unchecked
            assertThat("добавлены правильные таргетинги", targetings,
                    containsInAnyOrder(mapList(additionalTargetings,
                            targeting -> {
                                BeanDifferMatcher
                                        beanMatcher = beanDiffer(targeting).useCompareStrategy(onlyExpectedFields());
                                // обход обсобенностей beanDiffer при использовании в матчере contains
                                //noinspection unchecked
                                return both(instanceOf(targeting.getClass())).and(beanMatcher);
                            })));
        }
    }
}
