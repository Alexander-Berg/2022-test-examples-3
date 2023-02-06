package ru.yandex.direct.grid.core.util.adgroup;

import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupStates;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatchCategory;
import ru.yandex.direct.grid.core.entity.group.container.GdiGroupRelevanceMatch;
import ru.yandex.direct.grid.core.entity.group.container.GdiRelevanceMatchCategory;
import ru.yandex.direct.grid.core.entity.group.model.GdiDynamicGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiGroupBlGenerationStatus;
import ru.yandex.direct.grid.core.entity.group.model.GdiMobileContentGroup;
import ru.yandex.direct.grid.core.entity.group.model.GdiPerformanceGroup;

import static com.google.common.primitives.Longs.asList;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;

@ParametersAreNonnullByDefault
public class AdGroupUtils {

    public static AdGroup toCoreAdGroup(GdiGroup gdiGroup, long clientCountryRegionId) {
        AdGroup typedAdGroup;
        switch (gdiGroup.getType()) {
            case BASE:
                typedAdGroup = new TextAdGroup();
                break;
            case DYNAMIC:
                typedAdGroup = new DynamicTextAdGroup()
                        .withRelevanceMatchCategories(
                                toCoreRelevanceMatchCategories(
                                        ifNotNull(gdiGroup.getRelevanceMatch(),
                                                GdiGroupRelevanceMatch::getRelevanceMatchCategories)))
                        .withDomainUrl(((GdiDynamicGroup) gdiGroup).getMainDomain())
                        .withStatusBLGenerated(
                                toStatusBLGenerated(gdiGroup.getStatus().getBlGenerationStatus()));
                break;
            case MOBILE_CONTENT:
                typedAdGroup = new MobileContentAdGroup()
                        .withStoreUrl(((GdiMobileContentGroup) gdiGroup).getStoreHref());
                break;
            case PERFORMANCE:
                typedAdGroup = new PerformanceAdGroup()
                        .withFeedId(((GdiPerformanceGroup) gdiGroup).getFeedId())
                        .withStatusBLGenerated(toStatusBLGenerated(gdiGroup.getStatus().getBlGenerationStatus()));
                break;
            default:
                throw new IllegalStateException("Unsupported adGroup type: " + gdiGroup.getType());
        }

        return typedAdGroup
                .withId(gdiGroup.getId())
                .withCampaignId(gdiGroup.getCampaignId())
                .withType(gdiGroup.getType())
                .withGeo(asList(clientCountryRegionId))
                .withEffectiveGeo(asList(clientCountryRegionId))
                .withPageGroupTags(gdiGroup.getPageGroupTags())
                .withProjectParamConditions(gdiGroup.getProjectParamConditions())
                .withTargetTags(gdiGroup.getTargetTags());
    }

    @Nullable
    private static StatusBLGenerated toStatusBLGenerated(GdiGroupBlGenerationStatus blGenerationStatus) {
        if (blGenerationStatus == GdiGroupBlGenerationStatus.INAPPLICABLE) {
            return null;
        }
        return StatusBLGenerated.valueOf(blGenerationStatus.name());
    }

    @Nullable
    private static Set<RelevanceMatchCategory> toCoreRelevanceMatchCategories(
            @Nullable Set<GdiRelevanceMatchCategory> relevanceMatchCategories) {
        return mapSet(relevanceMatchCategories, category -> RelevanceMatchCategory.valueOf(category.getTypedValue()));
    }

    public static AdGroupStates getTestAdGroupState(Boolean archived, Boolean showing,
                                                    Boolean active, Boolean bsEverSynced) {
        return new AdGroupStates()
                .withArchived(archived)
                .withShowing(showing)
                .withActive(active)
                .withBsEverSynced(bsEverSynced);
    }
}
