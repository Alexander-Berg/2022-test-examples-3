package ru.yandex.direct.api.v5.entity.adgroups;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.adgroups.ObjectFactory;
import com.yandex.direct.api.v5.adgroups.TextAdGroupFeedParamsUpdate;
import com.yandex.direct.api.v5.general.ArrayOfLong;

@ParametersAreNonnullByDefault
public class AdGroupTestUtils {

    public static TextAdGroupFeedParamsUpdate getTextAdGroupFeedParamsUpdate(@Nullable Long feedId,
                                                                             @Nullable List<Long> feedCategoryIds) {
        return new TextAdGroupFeedParamsUpdate()
                .withFeedId(feedId)
                .withFeedCategoryIds(new ObjectFactory()
                        .createTextAdGroupFeedParamsUpdateFeedCategoryIds(new ArrayOfLong().withItems(feedCategoryIds)));
    }

}
