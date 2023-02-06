package ru.yandex.direct.core.entity.metrika.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.mobileapp.model.AppmetrikaEventSubtype;
import ru.yandex.direct.core.entity.mobileapp.model.AppmetrikaEventType;
import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName;
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppMetrikaEvent;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;
import ru.yandex.direct.core.entity.mobileapp.model.MobileExternalTrackerEvent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;

import static com.google.common.collect.Sets.cartesianProduct;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType.GOOGLEPLAYSTORE;
import static ru.yandex.direct.core.entity.mobilegoals.model.AppmetrikaInternalEvent.ECOMMERCE_PURCHASE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.MOBILE;
import static ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType.ACTION;
import static ru.yandex.direct.core.testing.data.TestGroups.getDefaultStoreHref;
import static ru.yandex.direct.core.testing.data.TestGroups.getIosStoreHref;
import static ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent;
import static ru.yandex.direct.core.testing.data.TestMobileContents.iosMobileContent;

@CoreTest
@RunWith(Parameterized.class)
public class MobileGoalsServiceTest {
    public static final long APP_METRIKA_APP_ID = RandomUtils.nextLong(1, 100_000_000_000L);
    public static final long APP_METRIKA_APP_ID_1 = RandomUtils.nextLong(1, 100_000_000_000L);

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    protected Steps steps;

    @Autowired
    private MobileGoalsService mobileGoalsService;

    private ClientInfo clientInfo;

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<MobileApp> mobileApps;

    @Parameterized.Parameter(2)
    public List<Goal> expectedGoals;

    @Parameterized.Parameter(3)
    public MobileAppStoreType mobileAppStoreType;

    @Parameterized.Parameters(name = "{3} {0}")
    public static Collection<Object[]> params() {
        return cartesianProduct(
                Set.of(
                        new Object[]{"Событий нет",
                                emptyList(),
                                emptyList()
                        },
                        new Object[]{"Одно приложение с четырьмя событиями аппметрики, с наименованием и без",
                                List.of(new MobileApp()
                                        .withAppMetrikaApplicationId(APP_METRIKA_APP_ID)
                                        .withMobileExternalTrackerEvents(emptyList())
                                        .withMobileAppMetrikaEvents(
                                                List.of(new MobileAppMetrikaEvent()
                                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                                .withEventName("event name 1")
                                                                .withCustomName("custom name 1"),
                                                        new MobileAppMetrikaEvent()
                                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                                .withEventName("event name 2")
                                                                .withCustomName(""),
                                                        new MobileAppMetrikaEvent()
                                                                .withEventType(AppmetrikaEventType.ECOMMERCE)
                                                                .withEventSubtype(AppmetrikaEventSubtype.ADD_TO_CART)
                                                                .withEventName("")
                                                                .withCustomName("custom name 2"),
                                                        new MobileAppMetrikaEvent()
                                                                .withEventType(AppmetrikaEventType.ECOMMERCE)
                                                                .withEventSubtype(AppmetrikaEventSubtype.PURCHASE)
                                                                .withEventName("")
                                                                .withCustomName("")))),
                                List.of(new Goal().withName("custom name 1"),
                                        new Goal().withName("event name 2"),
                                        new Goal().withName("custom name 2"),
                                        new Goal().withName(ECOMMERCE_PURCHASE.name()))
                        },
                        new Object[]{"Два приложения с разным APP_METRIKA_APP_ID и одним событием аппметрики",
                                List.of(new MobileApp()
                                                .withAppMetrikaApplicationId(APP_METRIKA_APP_ID)
                                                .withMobileExternalTrackerEvents(emptyList())
                                                .withMobileAppMetrikaEvents(
                                                        List.of(new MobileAppMetrikaEvent()
                                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                                .withEventName("event name 1")
                                                                .withCustomName("custom name 1"))),
                                        new MobileApp()
                                                .withAppMetrikaApplicationId(APP_METRIKA_APP_ID_1)
                                                .withMobileExternalTrackerEvents(emptyList())
                                                .withMobileAppMetrikaEvents(
                                                        List.of(new MobileAppMetrikaEvent()
                                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                                .withEventName("event name 1")
                                                                .withCustomName("custom name 1")))
                                ),
                                List.of(new Goal().withName("custom name 1"),
                                        new Goal().withName("custom name 1"))
                        },
                        new Object[]{"Одно приложение с двумя событиями внешних трекеров, с наименованием и без",
                                List.of(new MobileApp()
                                        .withAppMetrikaApplicationId(null)
                                        .withMobileAppMetrikaEvents(emptyList())
                                        .withMobileExternalTrackerEvents(
                                                List.of(new MobileExternalTrackerEvent()
                                                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                                                .withCustomName("custom name 1"),
                                                        new MobileExternalTrackerEvent()
                                                                .withEventName(ExternalTrackerEventName.EVENT_2)
                                                                .withCustomName("")))),
                                List.of(new Goal().withName("custom name 1"),
                                        new Goal().withName(ExternalTrackerEventName.EVENT_2.name()))
                        },
                        new Object[]{"Два приложения - одно с событием аппметрики, другое с событием внешних трекеров",
                                List.of(new MobileApp()
                                                .withAppMetrikaApplicationId(null)
                                                .withMobileAppMetrikaEvents(emptyList())
                                                .withMobileExternalTrackerEvents(
                                                        List.of(new MobileExternalTrackerEvent()
                                                                .withEventName(ExternalTrackerEventName.EVENT_1)
                                                                .withCustomName("custom name 1"))),
                                        new MobileApp()
                                                .withAppMetrikaApplicationId(APP_METRIKA_APP_ID)
                                                .withMobileExternalTrackerEvents(emptyList())
                                                .withMobileAppMetrikaEvents(
                                                        List.of(new MobileAppMetrikaEvent()
                                                                .withEventType(AppmetrikaEventType.CLIENT)
                                                                .withEventSubtype(AppmetrikaEventSubtype.OTHER)
                                                                .withEventName("event name 1")
                                                                .withCustomName("custom name 2")))),
                                List.of(new Goal().withName("custom name 1"),
                                        new Goal().withName("custom name 2"))
                        }
                ),
                ImmutableSet.of(
                        new Object[]{MobileAppStoreType.APPLEAPPSTORE},
                        new Object[]{MobileAppStoreType.GOOGLEPLAYSTORE}
                )).stream().map(e -> ArrayUtils.addAll(e.get(0), e.get(1))).collect(toList());
    }

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        addFeature(FeatureName.IN_APP_MOBILE_TARGETING);
        addFeature(FeatureName.IN_APP_MOBILE_TARGETING_CUSTOM_EVENTS_FOR_EXTERNAL_TRACKERS);

        MobileContentInfo mobileContentInfo = new MobileContentInfo()
                .withClientInfo(clientInfo)
                .withMobileContent(mobileAppStoreType == GOOGLEPLAYSTORE ? androidMobileContent() :
                        iosMobileContent());

        MobileContent mobileContent = steps.mobileContentSteps()
                .createMobileContent(mobileContentInfo)
                .getMobileContent();
        String bundleId = MobileContentService.getStoreAppIdFromMobileContent(mobileContent);
        String storeHref = mobileAppStoreType == GOOGLEPLAYSTORE
                ? getDefaultStoreHref(mobileContent.getStoreContentId())
                : getIosStoreHref(mobileContent.getStoreContentId());

        for (MobileApp mobileApp : mobileApps) {
            mobileApp
                    .withName(mobileContentInfo.getMobileContent().getStoreContentId())
                    .withStoreHref(storeHref)
                    .withDisplayedAttributes(emptySet())
                    .withTrackers(emptyList());

            mobileApp.getMobileAppMetrikaEvents()
                    .forEach(e -> e.withAppMetrikaAppId(mobileApp.getAppMetrikaApplicationId())
                            .withBundleId(bundleId)
                            .withStoreType(mobileAppStoreType)
                            .withIsDeleted(false));

            mobileApp.getMobileExternalTrackerEvents()
                    .forEach(e -> e.withIsDeleted(false));

            MobileAppInfo mobileAppInfo = new MobileAppInfo()
                    .withMobileContentInfo(mobileContentInfo)
                    .withMobileApp(mobileApp);

            steps.mobileAppSteps().createMobileApp(mobileAppInfo);
        }
    }

    private void addFeature(FeatureName featureName) {
        steps.featureSteps().addFeature(featureName);
        Long featureId = steps.featureSteps().getFeatures().stream()
                .filter(f -> f.getFeatureTextId().equals(featureName.getName()))
                .map(Feature::getId)
                .findFirst()
                .get();

        ClientFeature featureIdToClientId =
                new ClientFeature()
                        .withClientId(clientInfo.getClientId())
                        .withId(featureId)
                        .withState(FeatureState.ENABLED);
        steps.featureSteps().addClientFeature(featureIdToClientId);
    }

    @Test
    public void getAllAvailableInAppMobileGoals() {
        expectedGoals.forEach(e -> e
                .withType(MOBILE)
                .withMetrikaCounterGoalType(ACTION)
                .withMobileAppId(mobileApps.size() == 1 ? mobileApps.get(0).getId() : null)
                .withMobileAppName(mobileApps.size() == 1 ? mobileApps.get(0).getName() : null)
        );
        List<Goal> actualMobileGoals = mobileGoalsService.getAllAvailableInAppMobileGoals(clientInfo.getClientId());
        assertThat(actualMobileGoals)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .ignoringCollectionOrder()
                .isEqualTo(expectedGoals);
    }
}
