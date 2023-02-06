package ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdditionalTargetingValue;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.DeviceIdsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.FeaturesInPPAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasLCookieAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasPassportIdAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLang;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InterfaceLangsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.InternalNetworkAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsDefaultYandexSearchAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsPPLoggedInAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsVirusedAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledApp;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.PlusUserSegmentsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SearchTextAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ShowDatesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.SidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.UuidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexuidAgeAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YpCookiesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEngine;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.MobileContentContentType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.libs.timetarget.TimeTargetUtils;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUP_ADDITIONAL_TARGETINGS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupAdditionalTargetingRepositoryTest {
    @Autowired
    private Steps steps;

    @Autowired
    private AdGroupAdditionalTargetingRepository repoUnderTest;

    @Autowired
    private MobileContentRepository mobileContentRepository;

    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private Integer shard;
    private ClientId clientId;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void setUp() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign();
        shard = campaignInfo.getShard();
        clientId = campaignInfo.getClientId();
        adGroupInfo1 = steps.adGroupSteps().createDefaultInternalAdGroup().withCampaignInfo(campaignInfo);
        adGroupInfo2 = steps.adGroupSteps().createDefaultInternalAdGroup().withCampaignInfo(campaignInfo);
    }

    @Test
    public void addAdditionalTargetings_addMultipleNewTargetings() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, asList(targeting1, targeting2));
        assertThat("репозиторий должен вернуть два новых положительных id", ids,
                contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void addAdditionalTargetings_addAndGetHasLCookie() {
        HasLCookieAdGroupAdditionalTargeting targeting = new HasLCookieAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetHasPassportId() {
        HasPassportIdAdGroupAdditionalTargeting targeting = new HasPassportIdAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetIsVirused() {
        IsVirusedAdGroupAdditionalTargeting targeting = new IsVirusedAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetInternalNetwork() {
        InternalNetworkAdGroupAdditionalTargeting targeting = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetIsPPLoggedIn() {
        IsPPLoggedInAdGroupAdditionalTargeting targeting = new IsPPLoggedInAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetFeaturesInPP() {
        FeaturesInPPAdGroupAdditionalTargeting targeting = new FeaturesInPPAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of("4567654334567654", "9876545676543456"));
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetYandexUids() {
        YandexUidsAdGroupAdditionalTargeting targeting = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Arrays.asList("4567654334567654", "9876545676543456", "%42", "%0"));
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetYpCookies() {
        YpCookiesAdGroupAdditionalTargeting targeting = new YpCookiesAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of("4567654334567654", "9876545676543456"));
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetIsDefaultYandexSearch() {
        IsDefaultYandexSearchAdGroupAdditionalTargeting targeting =
                new IsDefaultYandexSearchAdGroupAdditionalTargeting()
                        .withAdGroupId(adGroupInfo1.getAdGroupId())
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
    }

    @Test
    public void addAdditionalTargetings_addAndGetSids() {
        var targeting = new SidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of(1L, 333L));

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assertThat("должен быть создан корректный объект таргетинга", targetings,
                contains(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids),
                contains(isOneOf("[1, 333]", "[333, 1]")));
    }

    @Test
    public void addAdditionalTargetings_addAndGetUuids() {
        var targeting = new UuidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of("bb91c6e3d1bcb785bc8ffab48bed03e5", "c4e6538d6081e38922f2b971b6a69f29"));

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids), contains(isOneOf(
                "[\"bb91c6e3d1bcb785bc8ffab48bed03e5\", \"c4e6538d6081e38922f2b971b6a69f29\"]",
                "[\"c4e6538d6081e38922f2b971b6a69f29\", \"bb91c6e3d1bcb785bc8ffab48bed03e5\"]"
        )));
    }

    @Test
    public void addAdditionalTargetings_addAndGetDeviceIds() {
        var targeting = new DeviceIdsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of("69ecf662-a00e-4fe8-9a93-424de74f1b24", "c85406d70b8553116de4a8ade48b04fd"));

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids), contains(isOneOf(
                "[\"69ecf662-a00e-4fe8-9a93-424de74f1b24\", \"c85406d70b8553116de4a8ade48b04fd\"]",
                "[\"c85406d70b8553116de4a8ade48b04fd\", \"69ecf662-a00e-4fe8-9a93-424de74f1b24\"]"
        )));
    }

    @Test
    public void addAdditionalTargetings_addAndGetPlusUserSegments() {
        var targeting = new PlusUserSegmentsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of(1L, 44L));

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids), contains(isOneOf(
                "[1, 44]", "[44, 1]"
        )));
    }

    @Test
    public void addAdditionalTargetings_addAndGetSearchText() {
        var targeting = new SearchTextAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of("коронавирус"));

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids), contains(
                "[\"коронавирус\"]"
        ));
    }

    @Test
    public void addAdditionalTargetings_addMultipleNewTargetingsforDifferentAdGroups() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        InternalNetworkAdGroupAdditionalTargeting targeting2 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, asList(targeting1, targeting2));
        assertThat("репозиторий должен вернуть два новых положительных id", ids,
                contains(greaterThan(0L), greaterThan(0L)));
    }

    @Test
    public void addAdditionalTargetings_addAndGetMultipleNewTargetingsforDifferentAdGroups() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Arrays.asList("95645319497823", "4343437463761", "563474637647"));
        List<Long> ids = repoUnderTest.add(shard, clientId, asList(targeting1, targeting2));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должно быть создано два объекта", targetings, hasSize(2));
        targetings = repoUnderTest.getByAdGroupId(shard, adGroupInfo1.getAdGroupId());
        assumeThat("у первой группы должен быть один таргетинг", targetings, hasSize(1));
        assertThat("у первой группы должен быть первый таргетинг", targetings.get(0), equalTo(targeting1));
        targetings = repoUnderTest.getByAdGroupId(shard, adGroupInfo2.getAdGroupId());
        assumeThat("у второй группы должен быть один таргетинг", targetings, hasSize(1));
        assertThat("у второй группы должен быть второй таргетинг", targetings.get(0), equalTo(targeting2));
    }

    @Test
    public void addAdditionalTargetings_insertDifferentTargetings() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Arrays.asList("95645319497823", "4343437463761", "563474637647"));
        YandexUidsAdGroupAdditionalTargeting targeting3 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Arrays.asList("65345457452434", "6764549509405940"));
        InternalNetworkAdGroupAdditionalTargeting targeting4 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo2.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId, asList(targeting1, targeting2, targeting3, targeting4));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должно быть создано четыре объекта", targetings, hasSize(4));
        targetings = repoUnderTest.getByAdGroupId(shard, adGroupInfo1.getAdGroupId());
        assumeThat("у первой группы должно быть два таргетинга", targetings, hasSize(2));
        assertThat("у первой группы должны быть первый и второй таргетинги", targetings,
                containsInAnyOrder(targeting1, targeting2));
        targetings = repoUnderTest.getByAdGroupId(shard, adGroupInfo2.getAdGroupId());
        assumeThat("у второй группы должно быть два таргетинга", targetings, hasSize(2));
        assertThat("у второй группы должны быть третий и четвёртый таргетинги", targetings,
                containsInAnyOrder(targeting3, targeting4));
    }

    @Test
    public void addAdditionalTargetings_insertVersionedTargeting() {
        BrowserEnginesAdGroupAdditionalTargeting targeting = new BrowserEnginesAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(asList(
                        new BrowserEngine().withTargetingValueEntryId(1L).withMinVersion("1.1").withMaxVersion("2.2"),
                        new BrowserEngine().withTargetingValueEntryId(2L).withMinVersion("3.3").withMaxVersion("4.4")));
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assertThat("должен быть создан корректный объект таргетинга", targetings, contains(targeting));
        assertThat("в базе хранится корректно сериализованный объект",
                getDbValue(ids),
                contains("[{\"maxVersion\": 2002, \"minVersion\": 1001, \"targetingValueEntryId\": 1}, " +
                        "{\"maxVersion\": 4004, \"minVersion\": 3003, \"targetingValueEntryId\": 2}]"));
    }

    @Test
    public void addAdditionalTargetings_insertShowDatesTargeting() {
        ShowDatesAdGroupAdditionalTargeting targeting = new ShowDatesAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Set.of(
                        LocalDate.of(2019, 6, 25),
                        LocalDate.of(2028, 12, 31)));
        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assertThat("должен быть создан корректный объект таргетинга", targetings, contains(targeting));
        assertThat("в базе хранится корректно сериализованный объект",
                getDbValue(ids),
                contains(isOneOf(
                        "[\"_20190625______\", \"_20281231______\"]", "[\"_20281231______\", \"_20190625______\"]")));
    }

    @Test
    public void addAdditionalTargetings_insertMobileInstalledAppsTargeting() {
        var mobileInstalledApp = new MobileInstalledApp()
                .withStoreUrl("http://play.google.com/store/apps/details?id=ru.yandex.searchplugin");
        var targeting = new MobileInstalledAppsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        List<Long> ids = repoUnderTest.add(shard, clientId,
                Collections.singletonList(targeting.withValue(Set.of(mobileInstalledApp))));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        List<MobileContent> mobileContents = mobileContentRepository.getMobileContent(shard,
                adGroupInfo1.getClientId(), MobileContentContentType.app, null, LimitOffset.maxLimited());
        assumeThat("должен быть создан один объект таргетинга", targetings, hasSize(1));
        assumeThat("должен быть создан один объект MobileContent", mobileContents, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting.withValue(Set.of(mobileInstalledApp.withMobileContentId(mobileContents.get(0).getId())))));
        assertThat("должен быть создан корректный объект MobileContent", mobileContents.get(0).getStoreContentId(),
                equalTo("ru.yandex.searchplugin"));
    }

    @Test
    public void addAdditionalTargetings_addAndGetInterfaceLangsTargeting() {
        var interfaceLangs = Set.of(InterfaceLang.RU, InterfaceLang.UK);
        var targeting = new InterfaceLangsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(interfaceLangs);

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);

        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids), contains(isOneOf(
                "[\"ru\", \"uk\"]", "[\"uk\", \"ru\"]"
        )));
    }

    @Test
    public void addAdditionalTargetings_addAndGetTimeTargeting() {
        TimeTarget timeTarget = TimeTargetUtils.timeTarget24x7();
        var targeting = new TimeAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(List.of(timeTarget));

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);

        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids), contains(isOneOf(
                "[\"1ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "2ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "3ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "4ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "5ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "6ABCDEFGHIJKLMNOPQRSTUVWX" +
                        "7ABCDEFGHIJKLMNOPQRSTUVWX;p:o\"]"
        )));
    }

    @Test
    public void addAndDeleteAdditionalTargetings() {
        InternalNetworkAdGroupAdditionalTargeting targeting1 = new InternalNetworkAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY);
        YandexUidsAdGroupAdditionalTargeting targeting2 = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(Arrays.asList("95645319497823", "4343437463761", "563474637647"));
        List<Long> ids = repoUnderTest.add(shard, clientId, asList(targeting1, targeting2));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должно быть создано два объекта", targetings, hasSize(2));
        repoUnderTest.deleteByIds(shard, Collections.singletonList(ids.get(1)));
        targetings = repoUnderTest.getByIds(shard, ids);
        assumeThat("должен остаться один таргетинг после удаления", targetings, hasSize(1));
        assertThat("должен остаться только первый таргетинг после удаления второго", targetings.get(0),
                equalTo(targeting1));
    }

    @Test
    public void addAdditionalTargetings_addAndGetYandexuidAgeTargeting() {
        var targetingValue = AdditionalTargetingValue.of(123);
        var targeting = new YandexuidAgeAdGroupAdditionalTargeting()
                .withAdGroupId(adGroupInfo1.getAdGroupId())
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(targetingValue);

        List<Long> ids = repoUnderTest.add(shard, clientId, Collections.singletonList(targeting));
        List<AdGroupAdditionalTargeting> targetings = repoUnderTest.getByIds(shard, ids);

        assumeThat("должен быть создан один объект", targetings, hasSize(1));
        assertThat("должен быть создан корректный объект таргетинга", targetings.get(0),
                equalTo(targeting));
        assertThat("в базе хранится корректно сериализованный объект", getDbValue(ids), contains(isOneOf(
                String.format("{\"value\": %d}", targetingValue.getValue())
        )));
    }

    private Set<String> getDbValue(Collection<Long> ids) {
        return dslContextProvider.ppc(shard)
                .select(ADGROUP_ADDITIONAL_TARGETINGS.VALUE)
                .from(ADGROUP_ADDITIONAL_TARGETINGS)
                .where(ADGROUP_ADDITIONAL_TARGETINGS.ID.in(ids))
                .fetchSet(ADGROUP_ADDITIONAL_TARGETINGS.VALUE);
    }
}
