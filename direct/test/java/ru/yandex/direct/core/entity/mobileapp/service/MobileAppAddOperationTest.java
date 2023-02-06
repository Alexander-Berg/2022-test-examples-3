package ru.yandex.direct.core.entity.mobileapp.service;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.mobileapp.MobileAppConverter;
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.entity.mobilecontent.service.MobileContentService;
import ru.yandex.direct.core.entity.mobilegoals.MobileAppGoalsService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileContentInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppAddOperationTest {
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.whatsapp";
    private static final String FIELD_DOMAIN_ID = "domainId";

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private MobileAppRepository mobileAppRepository;

    @Autowired
    private MobileContentService mobileContentService;

    @Autowired
    private MobileContentRepository mobileContentRepository;

    @Autowired
    private MobileAppAddValidationService mobileAppAddValidationService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private MobileAppConverter mobileAppConverter;

    @Autowired
    private MobileAppGoalsService mobileAppGoalsService;

    @Mock
    private User operator;

    private ClientInfo clientInfo;
    private Integer shard;
    private Long publisherDomainId;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(operator.getRole()).thenReturn(RbacRole.SUPER);

        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        steps.trustedRedirectSteps().addValidCounters();

        MobileContentInfo mobileContentInfo = steps.mobileContentSteps().createMobileContent(clientInfo, STORE_URL);
        publisherDomainId = checkNotNull(mobileContentInfo.getMobileContent().getPublisherDomainId());
    }

    @Test
    public void prepareAndApply_OnEmptyChanges_ReturnsSuccessfulEmptyResult() {
        MassResult<Long> result = createAddOperationAndRun(Collections.emptyList());
        assertThat(result, isSuccessful());
        assertThat(result.getResult(), empty());
    }

    @Test
    public void prepareAndApply_AddingValidMobileAppWithoutDomain_ReturnsSuccessfulResult() {
        MassResult<Long> result = createAddOperationAndRun(singletonList(validMobileAppWithDomain(null)));
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void prepareAndApply_AddingValidMobileAppWithoutDomain_SavedDomainAreCorrespondedToMobileContent() {
        MassResult<Long> result = createAddOperationAndRun(singletonList(validMobileAppWithDomain(null)));
        assumeThat(result, isSuccessful(true));
        Long createdMobileAppId = result.get(0).getResult();
        MobileApp createdMobileApp = getMobileApp(createdMobileAppId);
        assertThat(createdMobileApp,
                hasProperty(FIELD_DOMAIN_ID, both(notNullValue()).and(equalTo(publisherDomainId))));
    }

    @Test
    public void prepareAndApply_AddingValidMobileAppWithDomain_ReturnsSuccessfulResult() {
        MassResult<Long> result = createAddOperationAndRun(singletonList(validMobileAppWithDomain("some-domain.net")));
        assertThat(result, isSuccessful(true));
    }

    @Test
    public void prepareAndApply_AddingValidMobileAppWithDomain_SavedDomainAreNotCorrespondedToMobileContent() {
        MassResult<Long> result = createAddOperationAndRun(singletonList(validMobileAppWithDomain("some-domain.net")));
        assumeThat(result, isSuccessful(true));
        Long createdMobileAppId = result.get(0).getResult();
        MobileApp createdMobileApp = getMobileApp(createdMobileAppId);
        assertThat(createdMobileApp,
                hasProperty(FIELD_DOMAIN_ID, both(notNullValue()).and(not(equalTo(publisherDomainId)))));
    }

    private MobileApp validMobileAppWithDomain(@Nullable String domain) {
        return new MobileApp()
                .withName("some name")
                .withStoreHref(STORE_URL)
                .withDisplayedAttributes(emptySet())
                .withTrackers(emptyList())
                .withDomain(domain);
    }

    private MassResult<Long> createAddOperationAndRun(List<MobileApp> mobileApps) {
        return new MobileAppAddOperation(
                Applicability.FULL, dslContextProvider, mobileAppRepository, mobileContentService,
                mobileContentRepository, domainService, mobileAppAddValidationService, mobileAppConverter,
                operator, shard, clientInfo.getClientId(), mobileApps, mobileAppGoalsService, false)
                .prepareAndApply();
    }

    private MobileApp getMobileApp(Long mobileAppId) {
        return mobileAppRepository.getMobileApps(shard, clientInfo.getClientId(), singleton(mobileAppId)).get(0);
    }
}
