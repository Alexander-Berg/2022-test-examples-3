package ru.yandex.direct.core.entity.mobileapp.service;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTracker;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppTrackerTrackingSystem;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository;
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppUpdateOperationTest {
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.whatsapp";
    private static final String NEW_NAME = "NEW NAME";
    private static final String NEW_DOMAIN = "ya.ru";
    private static final long NONEXISTENT_MOBILE_APP_ID = 8828388000000L;
    private static final String OLD_TRACKER_URL = "https://adjust.com/q1w2e3?ya_click_id={logid}&gps_adid={google_aid}";
    private static final String OLD_IMPRESSION_URL = "https://view.adjust.com/impression/q1w2e3?ya_click_id={logid}&gps_adid={google_aid}";
    private static final String NEW_TRACKER_URL = "https://app.appsflyer.com/112132123?pid=yandexdirect_int&clickid" +
            "={logid}";
    private static final String NEW_IMPRESSION_URL = "https://impression.appsflyer" +
            ".com/112132123?pid=yandexdirect_int&clickid={logid}";
    private static final MobileAppTracker NEW_TRACKER = new MobileAppTracker()
            .withTrackingSystem(MobileAppTrackerTrackingSystem.OTHER)
            .withUrl(NEW_TRACKER_URL)
            .withImpressionUrl(NEW_IMPRESSION_URL)
            .withUserParams(emptyList());

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private MobileAppRepository mobileAppRepository;

    @Autowired
    private DomainService domainService;

    @Autowired
    private MobileAppGoalsService mobileAppGoalsService;

    @Autowired
    private MobileAppUpdateValidationService mobileAppUpdateValidationService;

    private ClientInfo clientInfo;
    private Integer shard;
    private Long mobileAppId;

    @Before
    public void before() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        steps.trustedRedirectSteps().addValidCounters();

        MobileAppInfo mobileAppInfo = steps.mobileAppSteps().createMobileApp(
                clientInfo, STORE_URL, OLD_TRACKER_URL, OLD_IMPRESSION_URL);
        mobileAppId = mobileAppInfo.getMobileAppId();

        assert mobileAppId != null;
        assert !mobileAppInfo.getMobileApp().getName().equals(NEW_NAME);
        assert !mobileAppInfo.getMobileApp().getDomain().equals(NEW_DOMAIN);
        assert getMobileApp(mobileAppId).getTrackers().size() > 0;

    }

    @Test
    public void prepareAndApply_OnEmptyChanges_ReturnsSuccessfulEmptyResult() {
        MobileAppUpdateOperation operation = createUpdateOperation(Collections.emptyList());
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful());
        assertThat(result.getResult(), empty());
    }

    @Test
    public void prepareAndApply_ChangeNothing_ReturnsSuccessfulResult() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(createModelChanges().process(NEW_NAME, MobileApp.NAME)));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void prepareAndApply_ChangeName_ReturnsSuccessfulResultAndChangesName() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(createModelChanges().process(NEW_NAME, MobileApp.NAME)));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        MobileApp mobileApp = getMobileApp(mobileAppId);
        assertThat(mobileApp.getName(), equalTo(NEW_NAME));
    }

    @Test
    public void prepareAndApply_ChangeDomain_ReturnsSuccessfulResultAndChangesDomain() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(createModelChanges().process(NEW_DOMAIN, MobileApp.DOMAIN)));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        MobileApp mobileApp = getMobileApp(mobileAppId);
        assertThat(mobileApp.getDomain(), equalTo(NEW_DOMAIN));
    }

    @Test
    public void prepareAndApply_ChangeDomainToNull_ReturnsSuccessfulResultAndMakesDomainNull() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(createModelChanges().process(null, MobileApp.DOMAIN)));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        MobileApp mobileApp = getMobileApp(mobileAppId);
        assertThat(mobileApp.getDomain(), nullValue());
    }

    @Test
    public void prepareAndApply_ChangeTracker_ReturnsSuccessfulResultAndChangesTracker() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(createModelChanges().process(singletonList(NEW_TRACKER), MobileApp.TRACKERS)));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        MobileApp mobileApp = getMobileApp(mobileAppId);
        assertThat(mobileApp.getTrackers(), hasSize(1));
        assertThat(mobileApp.getTrackers().get(0).getUrl(), equalTo(NEW_TRACKER_URL));
    }

    @Test
    public void prepareAndApply_ChangeToEmptyTrackers_ReturnsSuccessfulResultAndDeletesLinkedTrackers() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(createModelChanges().process(emptyList(), MobileApp.TRACKERS)));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(true));

        MobileApp mobileApp = getMobileApp(mobileAppId);
        assertThat(mobileApp.getTrackers(), empty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void prepareAndApply_ChangeUnsupportedProperties_GetsException() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(createModelChanges().process(11L, MobileApp.DOMAIN_ID)));
        operation.prepareAndApply();
    }

    public void prepareAndApply_TryUpdateNonexistentMobileAppId_ReturnsError() {
        MobileAppUpdateOperation operation = createUpdateOperation(
                singletonList(new ModelChanges<>(NONEXISTENT_MOBILE_APP_ID, MobileApp.class)));
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result, isSuccessful(false));
    }

    private ModelChanges<MobileApp> createModelChanges() {
        return new ModelChanges<>(mobileAppId, MobileApp.class);
    }

    private MobileAppUpdateOperation createUpdateOperation(List<ModelChanges<MobileApp>> modelChanges) {
        return new MobileAppUpdateOperation(
                Applicability.FULL, dslContextProvider, domainService, mobileAppRepository,
                mobileAppUpdateValidationService, null, shard, clientInfo.getClientId(), modelChanges,
                mobileAppGoalsService);
    }

    private MobileApp getMobileApp(Long mobileAppId) {
        return mobileAppRepository.getMobileApps(shard, clientInfo.getClientId(), singleton(mobileAppId)).get(0);
    }
}
