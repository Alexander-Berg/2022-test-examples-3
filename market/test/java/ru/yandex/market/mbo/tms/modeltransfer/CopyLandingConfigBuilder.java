package ru.yandex.market.mbo.tms.modeltransfer;

import ru.yandex.market.mbo.gwt.models.transfer.step.CategoryPair;
import ru.yandex.market.mbo.gwt.models.transfer.step.CopyLandingConfig;
import ru.yandex.market.mbo.gwt.models.transfer.step.LandingCopyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author danfertev
 * @since 22.11.2018
 */
public class CopyLandingConfigBuilder {
    private List<LandingCopyList> lists = new ArrayList<>();

    public static CopyLandingConfigBuilder newBuilder() {
        return new CopyLandingConfigBuilder();
    }

    public CopyLandingConfigBuilder landings(long sourceCategoryId, long targetCategoryId, long... landingIds) {
        lists.add(new LandingCopyList(new CategoryPair(sourceCategoryId, targetCategoryId),
            Arrays.stream(landingIds).boxed().collect(Collectors.toList())));
        return this;
    }

    public CopyLandingConfig build() {
        return new CopyLandingConfig(lists);
    }
}
