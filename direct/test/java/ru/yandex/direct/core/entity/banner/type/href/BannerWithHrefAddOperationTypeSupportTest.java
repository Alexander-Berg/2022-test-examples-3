package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;
import java.util.List;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupForBannerOperation;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.container.BannerAdditionalActionsContainer;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.entity.trustedredirects.service.TrustedRedirectsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.banner.model.BannerWithHref.DOMAIN;
import static ru.yandex.direct.core.entity.banner.model.BannerWithHref.HREF;
import static ru.yandex.direct.core.testing.data.TestDomain.testDomain;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerWithHrefAddOperationTypeSupportTest {

    @Autowired
    private Steps steps;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private DomainRepository domainRepository;
    @Autowired
    private DomainService domainService;
    @Autowired
    private TrustedRedirectsService trustedRedirectService;
    @Autowired
    private BannerCommonRepository bannerCommonRepository;
    @Autowired
    private BannersUrlHelper bannersUrlHelper;

    @Mock
    private AdGroupRepository adGroupRepository;

    private BannerHrefAndDomainProcessor processor;
    private BannerWithHrefAddOperationTypeSupport addSupport;

    private BannersAddOperationContainerImpl parametersContainer;
    private int shard;
    private ClientInfo clientInfo;

    @Before
    public void setUp() {
        initMocks(this);

        clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();
        parametersContainer = createContainer(clientInfo, RbacRole.CLIENT);

        processor = new BannerHrefAndDomainProcessor(domainService, bannerCommonRepository, trustedRedirectService,
                adGroupRepository, bannersUrlHelper);
        addSupport = new BannerWithHrefAddOperationTypeSupport(processor);
    }

    @Test
    public void beforeExecutionEncodesHrefAsUnicode() {
        List<BannerWithHref> banners = asList(
                createNewBannerWithHref(null),
                createNewBannerWithHref("http://xn--41a.xn--p1ag"));
        addSupport.beforeExecution(parametersContainer, banners);

        assertThat(banners, contains(
                hasProperty(HREF.name(), nullValue()),
                hasProperty(HREF.name(), is("http://я.ру"))
        ));
    }

    @Test
    public void beforeExecutionFillsDomainFromHref() {
        List<BannerWithHref> banners = asList(
                createNewBannerWithHref("http://ya.ru/bla-bla"),
                createNewBannerWithHref("http://ya.ru/bla-bla").withDomain("domain.ru"));
        addSupport.beforeExecution(parametersContainer, banners);

        assertThat(banners, contains(
                hasProperty(DOMAIN.name(), is("ya.ru")),
                hasProperty(DOMAIN.name(), is("ya.ru"))
        ));
    }

    @Test
    public void beforeExecutionFillsDomainFromHrefWithInternalRole() {
        parametersContainer = createContainer(clientInfo, RbacRole.SUPER);

        List<BannerWithHref> banners = asList(
                createNewBannerWithHref("http://ya.ru/bla-bla"),
                createNewBannerWithHref("http://ya.ru/bla-bla").withDomain("domain.ru"));
        addSupport.beforeExecution(parametersContainer, banners);

        assertThat(banners, contains(
                hasProperty(DOMAIN.name(), is("ya.ru")),
                hasProperty(DOMAIN.name(), is("domain.ru"))
        ));
    }

    @Test
    public void beforeExecutionFillsDomainIdForExistingDomain() {
        parametersContainer = createContainer(clientInfo, RbacRole.SUPER);

        var domain1 = testDomain();
        var domain2 = testDomain();

        // моделируем ситуацию, когда в БД есть только один домен из двух, второй должен добавиться "сам"
        domainRepository.addDomains(shard, singletonList(domain1));

        List<BannerWithHref> banners = asList(
                createNewBannerWithHref("http://ya.ru").withDomain(domain1.getDomain()),
                createNewBannerWithHref("http://ya.ru").withDomain(domain2.getDomain()));
        addSupport.beforeExecution(parametersContainer, banners);

        var domainToId = domainRepository.getDomainsToIdsFromPpcDict(asList(domain1.getDomain(), domain2.getDomain()));
        assertThat(mapList(banners, BannerWithHref::getDomainId), contains(
                domainToId.get(domain1.getDomain()), domainToId.get(domain2.getDomain())
        ));
        assertThat(domainRepository.getExistingDomainIds(shard, domainToId.values()), hasSize(2));
    }

    @Test
    public void addToAdditionalActionsContainerShouldAddAdGroupsForBSResyncIfHrefContainsCoefGoalContextIdOrPhraseId() {
        var specialParams = asList("coef_goal_context_id", "phrase_id", "phraseid", "param127", "retargeting_id");
        var bannersWithSpecialParamsInHref = mapList(specialParams, param ->
                createNewBannerWithHref("http://ya.ru?test={" + param + "}").withAdGroupId(nextPositiveLong()));

        var banners = StreamEx.of(null, "http://ya.ru")
                .map(href -> createNewBannerWithHref(href).withAdGroupId(nextPositiveLong()))
                .toList();
        banners.addAll(bannersWithSpecialParamsInHref);

        BannerAdditionalActionsContainer additionalActionsContainer = new BannerAdditionalActionsContainer(
                clientInfo.getClientId(), null);
        addSupport.addToAdditionalActionsContainer(additionalActionsContainer, parametersContainer, banners);

        assertThat(additionalActionsContainer.getAdGroupsIdsForBSResync(), containsInAnyOrder(
                mapList(bannersWithSpecialParamsInHref, b -> equalTo(b.getAdGroupId()))
        ));
    }

    @Test
    public void updateRelatedEntitiesInTransactionShouldSetHasPhraseIdHrefsPropertyToAdGroups() {
        List<AdGroupForBannerOperation> adGroups = asList(
                adGroup().withHasPhraseIdHref(false),
                adGroup().withHasPhraseIdHref(true),
                adGroup().withHasPhraseIdHref(false),
                adGroup().withHasPhraseIdHref(null)
        );
        parametersContainer.setIndexToAdGroupMap(EntryStream.of(adGroups).toMap());

        var banners = asList(
                createNewBannerWithHref("http://ya.ru").withAdGroupId(adGroups.get(0).getId()),
                createNewBannerWithHref("http://ya.ru?test={phrase_id}").withAdGroupId(adGroups.get(1).getId()),
                createNewBannerWithHref("http://ya.ru?test={phrase_id}").withAdGroupId(adGroups.get(2).getId()),
                createNewBannerWithHref("http://ya.ru?test={phraseid}").withAdGroupId(adGroups.get(3).getId())
        );
        addSupport.updateRelatedEntitiesInTransaction(dslContextProvider.ppc(shard), parametersContainer, banners);

        ArgumentCaptor<Collection<Long>> changedGroupsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(adGroupRepository).setHasPhraseIdHrefs(any(), changedGroupsCaptor.capture());
        verifyNoMoreInteractions(adGroupRepository);
        assertThat(changedGroupsCaptor.getValue(), containsInAnyOrder(
                adGroups.get(2).getId(), adGroups.get(3).getId()));
    }

    private BannersAddOperationContainerImpl createContainer(ClientInfo clientInfo, RbacRole rbacRole) {
        return new BannersAddOperationContainerImpl(clientInfo.getShard(), clientInfo.getUid(), rbacRole,
                clientInfo.getClientId(), clientInfo.getUid(), clientInfo.getUid(), null, emptySet(),
                ModerationMode.FORCE_SAVE_DRAFT, rbacRole.isInternal(), false,
                true);
    }

    public static BannerWithHref createNewBannerWithHref(String href) {
        return new ContentPromotionBanner().withHref(href);
    }

    private static AdGroup adGroup() {
        return new AdGroup().withId(nextPositiveLong());
    }
}

