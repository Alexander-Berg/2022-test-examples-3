package ru.yandex.direct.core.entity.banner.service;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.thymeleaf.util.StringUtils;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.container.BannerRepositoryContainer;
import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerModifyRepository;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.internalads.service.InternalAdsProductService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewBannerInfo;
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adGroupNotFound;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyBannerServiceTest {

    @Autowired
    private CopyBannerService service;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private Steps steps;

    @Autowired
    private BannerModifyRepository bannerModifyRepository;

    @Autowired
    private InternalAdsProductService productService;

    @Test
    public void copyTextSameAdGroupSuccess() {
        var bannerInfo = steps.textBannerSteps().createDefaultTextBanner();
        var operator = bannerInfo.getClientInfo().getUid();
        var result = makeCopy(bannerInfo, operator);
        checkSuccessCopy(result, bannerInfo.getBanner(), bannerCompareExceptFields());
    }

    @Test
    public void copyInternalSameAdGroupSuccess() {
        var bannerInfo = createInternalBanner();
        var operator = bannerInfo.getClientInfo().getUid();
        var result = makeCopy(bannerInfo, operator);
        checkSuccessCopy(result, bannerInfo.getBanner(), bannerCompareExceptFields());
    }

    @Test
    public void copyToAnotherGroupSuccess() {
        var bannerInfo = createInternalBanner();
        var anotherAdGroup = steps.adGroupSteps().createActiveInternalAdGroup(bannerInfo.getCampaignInfo());
        var operator = bannerInfo.getClientInfo().getUid();

        var result = makeCopy(bannerInfo, operator, bannerInfo.getClientId(), anotherAdGroup.getAdGroupId());
        checkSuccessCopy(result, bannerInfo.getBanner(), bannerCompareExceptFields("adGroupId"));
        var copiedBanner = extractSingleBanner(result);

        assertThat(copiedBanner.getAdGroupId()).isEqualTo(anotherAdGroup.getAdGroupId());
        assertThat(copiedBanner.getAdGroupId()).isNotEqualTo(bannerInfo.getAdGroupId());
    }

    @Test
    public void copyToAnotherClientNoAccess() {
        var bannerInfo = createInternalBanner();
        var operator = bannerInfo.getClientInfo().getUid();
        var anotherAdGroup = createInternalBanner().getAdGroupInfo();

        var result = makeCopy(bannerInfo, operator, anotherAdGroup.getClientId(), anotherAdGroup.getAdGroupId());

        assertThat(result.getValidationResult().hasAnyErrors()).isTrue();
        assertThat(result.getValidationResult()
                .flattenErrors()
                .stream()
                .map(DefectInfo::getDefect)
        ).contains(adGroupNotFound());
    }

    @Test
    public void copyToAnotherClientBySuperSuccess() {
        var bannerInfo = createInternalBanner();
        var operator = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        var anotherAdGroup = createInternalBanner().getAdGroupInfo();

        var result = makeCopy(bannerInfo, operator.getUid(), anotherAdGroup.getClientId(),
                anotherAdGroup.getAdGroupId());

        checkSuccessCopy(result, bannerInfo.getBanner(), bannerCompareExceptFields("adGroupId", "campaignId"));

        var copiedBanner = extractSingleBanner(result);

        assertThat(copiedBanner.getAdGroupId()).isEqualTo(anotherAdGroup.getAdGroupId());
        assertThat(copiedBanner.getAdGroupId()).isNotEqualTo(bannerInfo.getAdGroupId());
    }

    @Test
    public void copyToAnotherClientByAgencySuccess() {
        var agency = steps.userSteps().createDefaultUserWithRole(RbacRole.AGENCY);
        var bannerInfo = createInternalBanner(agency);
        var anotherAgencyClientAdGroupIndo = createInternalBanner(agency).getAdGroupInfo();
        var operator = agency.getUid();

        var result = makeCopy(bannerInfo, operator, anotherAgencyClientAdGroupIndo.getClientId(),
                anotherAgencyClientAdGroupIndo.getAdGroupId());

        checkSuccessCopy(result, bannerInfo.getBanner(), bannerCompareExceptFields("adGroupId", "campaignId"));

        var copiedBanner = extractSingleBanner(result);

        assertThat(copiedBanner.getAdGroupId()).isEqualTo(anotherAgencyClientAdGroupIndo.getAdGroupId());
        assertThat(copiedBanner.getAdGroupId()).isNotEqualTo(bannerInfo.getAdGroupId());
    }

    @Test
    public void adGroupNotExists_failure() {
        var bannerInfo = createInternalBanner();
        var anotherAdGroupId = new Random().nextInt();
        var operator = bannerInfo.getClientInfo().getUid();

        var result = makeCopy(bannerInfo, operator, bannerInfo.getClientId(), anotherAdGroupId);
        assertThat(result.getValidationResult().hasAnyErrors()).isTrue();
        assertThat(result.getValidationResult()
                .flattenErrors()
                .stream()
                .map(DefectInfo::getDefect)
        ).contains(adGroupNotFound());
    }

    @Test
    public void copyFewGroupsToOne_success() {
        var client = steps.clientSteps().createDefaultClient();
        var bannersCount = 5;

        var banners = StreamEx.generate(() -> createInternalBanner(client))
                .limit(bannersCount)
                .collect(Collectors.toList());

        var bannerIds = mapList(banners, NewBannerInfo::getBannerId);

        var anotherGroup = createInternalBanner(client).getAdGroupId();

        var result = service.copyBanners(
                client.getUid(), bannerIds, client.getClientId(), client.getClientId(), anotherGroup);

        checkSuccessCopyAll(
                result, mapList(banners, NewBannerInfo::getBanner),
                bannerCompareExceptFields("adGroupId", "campaignId"));
    }

    @Test
    public void copyFewGroupsToSame_success() {
        var client = steps.clientSteps().createDefaultClient();
        var bannersCount = 5;

        var banners = StreamEx.generate(() -> createInternalBanner(client))
                .limit(bannersCount)
                .collect(Collectors.toList());

        var bannerIds = mapList(banners, NewBannerInfo::getBannerId);

        var result = service.copySameAdGroup(client.getUid(), bannerIds, client.getClientId());

        checkSuccessCopyAll(result, mapList(banners, NewBannerInfo::getBanner), bannerCompareExceptFields());
    }

    @Test
    public void copyModeratedAds_success() {
        var client = steps.clientSteps().createDefaultClient();
        createProductIfNoExists(client);

        var banner = createInternalBanner(client, BannerStatusModerate.YES);

        var result = service.copySameAdGroup(
                client.getUid(), List.of(banner.getBannerId()), client.getClientId());

        checkSuccessCopy(result, banner.getBanner(), bannerCompareExceptFields());
    }

    @Test
    public void copyStoppedAds_success() {
        var client = steps.clientSteps().createDefaultClient();

        var banner = createInternalBanner(client, BannerStatusModerate.YES);
        var container = new BannerRepositoryContainer(client.getShard());
        var changes = new ModelChanges<>(banner.getBannerId(), InternalBanner.class)
                .process(false, InternalBanner.STATUS_SHOW)
                .applyTo(banner.getBanner());
        bannerModifyRepository.update(container, List.of(changes));

        var result = service.copySameAdGroup(
                client.getUid(), List.of(banner.getBannerId()), client.getClientId());

        checkSuccessCopy(result, banner.getBanner(), bannerCompareExceptFields());
    }

    @Test
    public void copyArchivedAds_failure() {
        var client = steps.clientSteps().createDefaultClient();

        var banner = createInternalBanner(client, BannerStatusModerate.YES);
        var container = new BannerRepositoryContainer(client.getShard());
        var changes = new ModelChanges<>(banner.getBannerId(), InternalBanner.class)
                .process(true, InternalBanner.STATUS_ARCHIVED)
                .applyTo(banner.getBanner());
        bannerModifyRepository.update(container, List.of(changes));

        var result = service.copySameAdGroup(
                client.getUid(), List.of(banner.getBannerId()), client.getClientId());

        assertThat(result.getValidationResult().hasAnyErrors()).isTrue();

        assertThat(result.getValidationResult()
                .flattenErrors()
                .stream()
                .map(DefectInfo::getDefect)
                .map(Defect::defectId)
        ).contains(DefectIds.INVALID_VALUE);
    }


    private String[] bannerCompareExceptFields(String... fields) {
        //TODO "geoFlag", "language", "statusActive" проверить должны ли меняться поля
        var expectFields = new String[]{
                "domainId",
                "lastChange",
                "statusActive",
                "statusBsSynced",
                "bsBannerId",
                "language",
                "statusModerate",
                "statusPostModerate",
                "geoFlag",
                "id"
        };
        return (String[]) ArrayUtils.addAll(expectFields, fields);
    }

    private void checkSuccessCopy(MassResult<Long> result, Banner original, String[] except) {
        checkSuccessCopyAll(result, List.of(original), except);
    }

    private void checkSuccessCopyAll(MassResult<Long> result, List<Banner> original, String[] except) {
        assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
        assertThat(result.getResult().size()).isEqualTo(original.size());

        var copiedBannerIds = StreamEx.of(result.getResult())
                .map(Result::getResult)
                .collect(Collectors.toList());
        var copiedBanners = bannerService.getBannersByIds(copiedBannerIds);

        StreamEx.of(copiedBanners)
                .zipWith(original.stream())
                .forEach(entry -> {
                    assertThat(entry.getKey().getStatusBsSynced()).isEqualTo(StatusBsSynced.NO);
                    assertThat(entry.getKey().getStatusModerate()).isEqualTo(BannerStatusModerate.NEW);
                    assertThat(entry.getKey()).isEqualToIgnoringGivenFields(entry.getValue(), except);
                });
    }

    private BannerWithSystemFields extractSingleBanner(MassResult<Long> result) {
        assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
        assertThat(result.getResult().size()).isOne();

        var copiedBannerIds = result.getResult()
                .stream()
                .map(Result::getResult)
                .collect(Collectors.toList());

        var copiedBanners = bannerService.getBannersByIds(copiedBannerIds);

        return copiedBanners.get(0);
    }

    private MassResult<Long> makeCopy(
            NewBannerInfo info,
            long operatorId,
            ClientId clientToId,
            long adGroupToId) {
        return service.copyBanners(
                operatorId,
                List.of(info.getBannerId()),
                info.getClientId(),
                clientToId,
                adGroupToId
        );
    }

    private MassResult<Long> makeCopy(
            NewBannerInfo info,
            long operatorId) {
        return service.copySameAdGroup(
                operatorId,
                List.of(info.getBannerId()),
                info.getClientId()
        );
    }

    private NewBannerInfo createInternalBanner() {
        return createInternalBanner(steps.clientSteps().createDefaultClient());
    }

    private NewBannerInfo createInternalBanner(UserInfo agency) {
        var clientInfo = steps.clientSteps().createClientUnderAgency(agency, new ClientInfo());

        return createInternalBanner(clientInfo);
    }

    private NewBannerInfo createInternalBanner(ClientInfo clientInfo) {
        return createInternalBanner(clientInfo, BannerStatusModerate.NEW);
    }

    private NewBannerInfo createInternalBanner(ClientInfo clientInfo, BannerStatusModerate status) {
        createProductIfNoExists(clientInfo);
        var campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        var adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);

        NewInternalBannerInfo internalBannerInfo =
                steps.internalBannerSteps().createModeratedInternalBanner(adGroupInfo, status);
        var internalBanner = (InternalBanner) internalBannerInfo.getBanner();
        internalBanner.getModerationInfo().setSendToModeration(false);
        return internalBannerInfo;
    }

    private void createProductIfNoExists(ClientInfo info) {
        try {
            productService.getProduct(info.getClientId());
        } catch (IllegalArgumentException ignored) {
            var createdProduct = new InternalAdsProduct()
                    .withClientId(info.getClientId())
                    .withName("product name" + StringUtils.randomAlphanumeric(20))
                    .withDescription("product description")
                    .withOptions(Collections.emptySet());
            productService.createProduct(createdProduct);
        }
    }

}
