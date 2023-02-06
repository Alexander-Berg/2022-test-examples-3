package ru.yandex.direct.core.entity.adgroupadditionaltargeting;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ClidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InternalNetworkAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsDefaultYandexSearchAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledApp;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.AdGroupAdditionalTargetingUtils.additionalTargetingDescriptor;

public class AdGroupAdditionalTargetingUtilsTest {
    public static final InternalNetworkAdGroupAdditionalTargeting BOOLEAN_TARGETING_WITH_IDS_1 =
            new InternalNetworkAdGroupAdditionalTargeting()
                    .withAdGroupId(1L)
                    .withId(10L)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

    public static final InternalNetworkAdGroupAdditionalTargeting BOOLEAN_TARGETING_WITH_IDS_2 =
            new InternalNetworkAdGroupAdditionalTargeting()
                    .withAdGroupId(2L)
                    .withId(20L)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

    public static final IsDefaultYandexSearchAdGroupAdditionalTargeting BOOLEAN_TARGETING_TRUE_WITH_IDS_3 =
            new IsDefaultYandexSearchAdGroupAdditionalTargeting()
                    .withAdGroupId(2L)
                    .withId(20L)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);

    public static final InternalNetworkAdGroupAdditionalTargeting BOOLEAN_FILTERING_1 =
            new InternalNetworkAdGroupAdditionalTargeting()
                    .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ALL);

    public static final YandexUidsAdGroupAdditionalTargeting LIST_TARGETING_WITH_ID =
            new YandexUidsAdGroupAdditionalTargeting()
                    .withAdGroupId(1L)
                    .withId(10L)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(List.of("11", "22"));

    public static final YandexUidsAdGroupAdditionalTargeting LIST_TARGETING =
            new YandexUidsAdGroupAdditionalTargeting()
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(List.of("11", "22"));

    public static final YandexUidsAdGroupAdditionalTargeting LIST_FILTERING =
            new YandexUidsAdGroupAdditionalTargeting()
                    .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                    .withValue(List.of("11", "22"));

    // null в примерах ниже, чтобы проверить работу на совсем валидных данных
    // LinkedHashSet, чтобы сохранить порядок
    public static final ClidsAdGroupAdditionalTargeting SET_TARGETING_WITH_NULL =
            new ClidsAdGroupAdditionalTargeting()
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(new LinkedHashSet<>(Arrays.asList(null, 2L, 3L, 4L, 5L)));

    public static final ClidsAdGroupAdditionalTargeting SET_TARGETING_WITH_ID_AND_NULL =
            new ClidsAdGroupAdditionalTargeting()
                    .withAdGroupId(1L)
                    .withId(10L)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(new LinkedHashSet<>(Arrays.asList(5L, null, 2L, 3L, 4L)));

    public static final MobileInstalledAppsAdGroupAdditionalTargeting SET_TARGETING_MOBILE_INSTALLED_APP =
            new MobileInstalledAppsAdGroupAdditionalTargeting()
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(new LinkedHashSet<>(List.of(
                            new MobileInstalledApp().withMobileContentId(11L).withStoreUrl("aaaa"),
                            new MobileInstalledApp().withMobileContentId(12L).withStoreUrl("bbbb")
                    )));

    public static final MobileInstalledAppsAdGroupAdditionalTargeting SET_TARGETING_MOBILE_INSTALLED_APP_WITH_ID =
            new MobileInstalledAppsAdGroupAdditionalTargeting()
                    .withAdGroupId(1L)
                    .withId(10L)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(new LinkedHashSet<>(List.of(
                            new MobileInstalledApp().withMobileContentId(12L).withStoreUrl("bbbb"),
                            new MobileInstalledApp().withMobileContentId(11L).withStoreUrl("aaaa")
                    )));

    @Test
    public void additionalTargetingDescriptor_BooleanSameValue() {
        assertThat(additionalTargetingDescriptor(BOOLEAN_TARGETING_WITH_IDS_1),
                equalTo(additionalTargetingDescriptor(BOOLEAN_TARGETING_WITH_IDS_1)));
    }

    @Test
    public void additionalTargetingDescriptor_BooleanSameValueWithDifferentId() {
        assertThat(additionalTargetingDescriptor(BOOLEAN_TARGETING_WITH_IDS_1),
                equalTo(additionalTargetingDescriptor(BOOLEAN_TARGETING_WITH_IDS_2)));
    }

    @Test
    public void additionalTargetingDescriptor_ListSameValue() {
        assertThat(additionalTargetingDescriptor(LIST_TARGETING),
                equalTo(additionalTargetingDescriptor(LIST_TARGETING)));
    }

    @Test
    public void additionalTargetingDescriptor_ListSameValueWithDifferentId() {
        assertThat(additionalTargetingDescriptor(LIST_TARGETING),
                equalTo(additionalTargetingDescriptor(LIST_TARGETING_WITH_ID)));
    }

    @Test
    public void additionalTargetingDescriptor_BooleanSameValueButDifferentType() {
        assertThat(additionalTargetingDescriptor(BOOLEAN_TARGETING_WITH_IDS_2),
                not(equalTo(additionalTargetingDescriptor(BOOLEAN_TARGETING_TRUE_WITH_IDS_3))));
    }

    @Test
    public void additionalTargetingDescriptor_BooleanDifferentValue() {
        assertThat(additionalTargetingDescriptor(BOOLEAN_TARGETING_WITH_IDS_1),
                not(equalTo(additionalTargetingDescriptor(BOOLEAN_FILTERING_1))));
    }

    @Test
    public void additionalTargetingDescriptor_ListDifferentTargeting() {
        assertThat(additionalTargetingDescriptor(LIST_TARGETING),
                not(equalTo(additionalTargetingDescriptor(LIST_FILTERING))));
    }

    @Test
    public void additionalTargetingDescriptor_SetSameValue() {
        assertThat(additionalTargetingDescriptor(SET_TARGETING_WITH_NULL),
                equalTo(additionalTargetingDescriptor(SET_TARGETING_WITH_NULL)));
    }

    @Test
    public void additionalTargetingDescriptor_SetSameValueWithDifferentId() {
        assertThat(additionalTargetingDescriptor(SET_TARGETING_WITH_NULL),
                equalTo(additionalTargetingDescriptor(SET_TARGETING_WITH_ID_AND_NULL)));
    }

    @Test
    public void additionalTargetingDescriptor_SetMobileAppSameValue() {
        assertThat(additionalTargetingDescriptor(SET_TARGETING_MOBILE_INSTALLED_APP),
                equalTo(additionalTargetingDescriptor(SET_TARGETING_MOBILE_INSTALLED_APP)));
    }

    @Test
    public void additionalTargetingDescriptor_SetMobileAppSameValueWithDifferentId() {
        assertThat(additionalTargetingDescriptor(SET_TARGETING_MOBILE_INSTALLED_APP),
                equalTo(additionalTargetingDescriptor(SET_TARGETING_MOBILE_INSTALLED_APP_WITH_ID)));
    }
}
