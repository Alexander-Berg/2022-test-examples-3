package ru.yandex.direct.core.entity.vcard.service;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import one.util.streamex.StreamEx;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.old.OldDynamicBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.vcard.container.AssignVcardRequest;
import ru.yandex.direct.core.entity.vcard.container.SaveVcardRequest;
import ru.yandex.direct.core.entity.vcard.container.UnassignVcardRequest;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.steps.VcardSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.Result;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.core.testing.data.TestVcards.vcardUserFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ManageVcardsServiceTest {

    private static final CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields();

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private BannerSteps bannerSteps;

    @Autowired
    private VcardSteps vcardSteps;

    @Autowired
    private ManageVcardService manageVcardService;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private VcardRepository vcardRepository;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private UserInfo userInfo;
    private ClientInfo clientInfo;
    private long operatorUid;
    private long clientUid;
    private ClientId clientId;
    private int shard;

    @Before
    public void before() {
        userInfo = userSteps.createDefaultUser();
        clientInfo = userInfo.getClientInfo();
        operatorUid = clientInfo.getUid();
        clientUid = clientInfo.getUid();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    // save
    @Test
    public void saveVcard_forTextBanner_Test() {
        TextBannerInfo bannerInfo = createTextBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        SaveVcardRequest request = new SaveVcardRequest()
                .withBannerIds(singletonList(bannerId))
                .withCampaignId(campaignId)
                .withVcard(validVcard(campaignId));

        Long newVcardId = saveVcard(request, expectedVcard(campaignId));
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), newVcardId);
    }

    @Test
    public void saveVcard_forDynamicBanner_Test() {
        DynamicBannerInfo bannerInfo = createDynamicBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        SaveVcardRequest request = new SaveVcardRequest()
                .withBannerIds(singletonList(bannerId))
                .withCampaignId(campaignId)
                .withVcard(validVcard(campaignId));

        Long newVcardId = saveVcard(request, expectedVcard(campaignId));
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), newVcardId);
    }

    @Test
    public void saveVcard_byBannerIds_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        Long bannerId1 = createTextBannerWithVcardId(campaignInfo, null).getBannerId();
        Long bannerId2 = createTextBannerWithVcardId(campaignInfo, null).getBannerId();
        Long bannerId3 = createTextBannerWithVcardId(campaignInfo, null).getBannerId();

        // добавляем визитку в баннеры bannerId1 и bannerId2
        // в баннере bannerId3 vcard_id должен остаться равным null
        SaveVcardRequest request = new SaveVcardRequest()
                .withBannerIds(asList(bannerId1, bannerId2))
                .withCampaignId(campaignId)
                .withVcard(validVcard(campaignId));

        Long newVcardId = saveVcard(request, expectedVcard(campaignId));

        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), newVcardId);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId3), null);
    }

    @Test
    public void saveVcard_byVcardId_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        Long oldVcardId = vcardSteps.createVcard(fullVcard().withHouse("15"), campaignInfo).getVcardId();

        Long bannerId1 = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();
        Long bannerId2 = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();
        Long bannerId3 = createTextBannerWithVcardId(campaignInfo, null).getBannerId();

        // добавляем визитку в баннеры bannerId1 и bannerId2
        // в баннере bannerId3 vcard_id должен остаться равным null
        SaveVcardRequest request = new SaveVcardRequest()
                .withVcardId(oldVcardId)
                .withCampaignId(campaignId)
                .withVcard(validVcard(campaignId));

        Long newVcardId = saveVcard(request, expectedVcard(campaignId));

        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), newVcardId);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId3), null);
    }

    @Test
    public void saveVcard_byBannerIds_forInvalidVcard_Test() {
        TextBannerInfo bannerInfo = createTextBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        SaveVcardRequest request = new SaveVcardRequest()
                .withBannerIds(singletonList(bannerId))
                .withCampaignId(campaignId)
                .withVcard(invalidVcard(campaignId));

        saveVcard(request, null);
        // vcard_id в баннере должен остаться равным null
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    @Test
    public void saveVcard_byVcardId_forInvalidVcard_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        Long oldVcardId = vcardSteps.createVcard(fullVcard().withHouse("15"), campaignInfo).getVcardId();
        Long bannerId = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();

        SaveVcardRequest request = new SaveVcardRequest()
                .withVcardId(oldVcardId)
                .withCampaignId(campaignId)
                .withVcard(invalidVcard(campaignId));

        saveVcard(request, null);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), oldVcardId);
    }

    @Test
    public void saveVcard_forOtherClientBanner_Test() {
        // баннер относится к другому клиенту
        TextBannerInfo bannerInfo = bannerSteps.createActiveTextBanner();
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        SaveVcardRequest request = new SaveVcardRequest()
                .withBannerIds(singletonList(bannerId))
                .withCampaignId(campaignId)
                .withVcard(validVcard(campaignId));

        saveVcard(request, null);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    @Test
    public void saveVcard_forDifferentCampaigns_Test() {
        TextBannerInfo bannerInfo = createTextBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();

        // баннеры относятся к разным кампаниям
        Long bannerId1 = bannerInfo.getBannerId();
        Long bannerId2 = createTextBannerWithVcardId(null, null).getBannerId();

        SaveVcardRequest request = new SaveVcardRequest()
                .withBannerIds(asList(bannerId1, bannerId2))
                .withCampaignId(campaignId)
                .withVcard(validVcard(campaignId));

        saveVcard(request, null);
        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), null);
    }

    // assign
    @Test
    public void assignVcard_forTextBanner_Test() {
        TextBannerInfo bannerInfo = createTextBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        Long vcardId = createValidVcard(bannerInfo.getCampaignInfo());

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        Long newVcardId = assignVcard(request, expectedVcard(campaignId));
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), newVcardId);
    }

    @Test
    public void assignVcard_forDynamicBanner_Test() {
        DynamicBannerInfo bannerInfo = createDynamicBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        Long vcardId = createValidVcard(bannerInfo.getCampaignInfo());

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        Long newVcardId = assignVcard(request, expectedVcard(campaignId));
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), newVcardId);
    }

    @Test
    public void assignVcard_whenVcardFromOtherCampaign_Test() {
        TextBannerInfo bannerInfo = createTextBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        CampaignInfo otherCampaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long vcardId = createValidVcard(otherCampaignInfo);

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        Long newVcardId = assignVcard(request, expectedVcard(campaignId));
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), newVcardId);
    }

    @Test
    public void assignVcard_forTextBannersList_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long vcardId = createValidVcard(campaignInfo);

        Long bannerId1 = createTextBannerWithVcardId(campaignInfo, null).getBannerId();
        Long bannerId2 = createTextBannerWithVcardId(campaignInfo, null).getBannerId();
        Long bannerId3 = createTextBannerWithVcardId(campaignInfo, null).getBannerId();

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(asList(bannerId1, bannerId2))
                .withVcardId(vcardId);

        Long newVcardId = assignVcard(request, expectedVcard(campaignId));

        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), newVcardId);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId3), null);
    }

    @Test
    public void assignVcard_forDynamicBannersList_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveDynamicCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long vcardId = createValidVcard(campaignInfo);

        Long bannerId1 = createDynamicBannerWithVcardId(campaignInfo, null).getBannerId();
        Long bannerId2 = createDynamicBannerWithVcardId(campaignInfo, null).getBannerId();
        Long bannerId3 = createDynamicBannerWithVcardId(campaignInfo, null).getBannerId();

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(asList(bannerId1, bannerId2))
                .withVcardId(vcardId);

        Long newVcardId = assignVcard(request, expectedVcard(campaignId));

        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), newVcardId);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId3), null);
    }

    @Test
    public void assignVcard_forBannerWithVcard_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        Long vcardId = createValidVcard(campaignInfo);
        Long bannerId = createTextBannerWithVcardId(campaignInfo, vcardId).getBannerId();

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        Long newVcardId = assignVcard(request, expectedVcard(campaignId));
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), newVcardId);
    }

    @Test
    public void assignVcard_DissociateVcard_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        // создаем баннер с визиткой с lastDissociation < now()
        Long oldVcardId = vcardSteps
                .createVcard(fullVcard().withLastDissociation(now().minusDays(1L)), campaignInfo)
                .getVcardId();
        Long bannerId = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();

        Long vcardId = createValidVcard(campaignInfo);
        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        Long newVcardId = assignVcard(request, expectedVcard(campaignId));
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), newVcardId);

        Vcard oldVcard = vcardRepository.getVcards(shard, clientUid, singletonList(oldVcardId)).get(0);
        softly.assertThat(oldVcard.getLastDissociation())
                .as("для старой визитки должна быть проставлена дата отвязки lastDissociation = now()")
                .isCloseTo(now(), within(2L, ChronoUnit.MINUTES));
    }

    @Test
    public void assignVcard_whenOtherClientVcard_Test() {
        TextBannerInfo bannerInfo = createTextBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        ClientInfo otherClientInfo = clientSteps.createDefaultClient();
        Long vcardId = vcardSteps.createVcard(fullVcard(), otherClientInfo).getVcardId();

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        assignVcard(request, null);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    @Test
    public void assignVcard_forArchivedCampaign_Test() {
        CampaignInfo campaignInfo = campaignSteps.createCampaign(
                activeTextCampaign(null, null).withArchived(true), clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        Long vcardId = createValidVcard(campaignInfo);
        Long bannerId = createTextBannerWithVcardId(campaignInfo, null).getBannerId();

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        assignVcard(request, null);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    @Test
    public void assignVcard_forInvalidVcard_Test() {
        TextBannerInfo bannerInfo = createTextBannerWithVcardId(null, null);
        Long campaignId = bannerInfo.getCampaignId();
        Long bannerId = bannerInfo.getBannerId();

        Long vcardId = vcardSteps
                .createVcard(fullVcard(invalidVcard(null), null), bannerInfo.getCampaignInfo())
                .getVcardId();

        AssignVcardRequest request = new AssignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId))
                .withVcardId(vcardId);

        assignVcard(request, null);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    // unassign
    @Test
    public void unassignVcard_forTextBanner_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        Long oldVcardId = createValidVcard(campaignInfo);
        Long bannerId = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();

        UnassignVcardRequest request = new UnassignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId));

        unassignVcard(request, true);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    @Test
    public void unassignVcard_forDynamicBanner_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveDynamicCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();

        Long oldVcardId = createValidVcard(campaignInfo);
        Long bannerId = createDynamicBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();

        UnassignVcardRequest request = new UnassignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId));

        unassignVcard(request, true);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    @Test
    public void unassignVcard_forTextBannersList_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long oldVcardId = createValidVcard(campaignInfo);

        Long bannerId1 = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();
        Long bannerId2 = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();
        Long bannerId3 = createTextBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();

        UnassignVcardRequest request = new UnassignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(asList(bannerId1, bannerId2));

        unassignVcard(request, true);

        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), null);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId3), oldVcardId);
    }

    @Test
    public void unassignVcard_forDynamicBannersList_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveDynamicCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long oldVcardId = createValidVcard(campaignInfo);

        Long bannerId1 = createDynamicBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();
        Long bannerId2 = createDynamicBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();
        Long bannerId3 = createDynamicBannerWithVcardId(campaignInfo, oldVcardId).getBannerId();

        UnassignVcardRequest request = new UnassignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(asList(bannerId1, bannerId2));

        unassignVcard(request, true);

        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), null);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId3), oldVcardId);
    }

    @Test
    public void unassignVcard_forBannerWithoutVcard_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long bannerId = createTextBannerWithVcardId(campaignInfo, null).getBannerId();

        UnassignVcardRequest request = new UnassignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(singletonList(bannerId));

        unassignVcard(request, true);
        checkThatAllBannersHaveThisVcardId(singletonList(bannerId), null);
    }

    @Test
    public void unassignVcard_forTextBannerWithoutHref_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);
        Long campaignId = campaignInfo.getCampaignId();
        Long oldVcardId = createValidVcard(campaignInfo);

        Long bannerId1 = bannerSteps
                .createBanner(activeTextBanner().withVcardId(oldVcardId), campaignInfo)
                .getBannerId();
        Long bannerId2 = bannerSteps
                .createBanner(activeTextBanner().withVcardId(oldVcardId).withHref(null), campaignInfo)
                .getBannerId();

        UnassignVcardRequest request = new UnassignVcardRequest()
                .withCampaignId(campaignId)
                .withBannerIds(asList(bannerId1, bannerId2));

        // у bannerId2 нет href, визитка не должна удаляться из баннеров
        unassignVcard(request, false);
        checkThatAllBannersHaveThisVcardId(asList(bannerId1, bannerId2), oldVcardId);
    }

    // get
    @Test
    public void getVcards_Test() {
        Vcard userFieldsVcard = vcardUserFields(null).withCountry("Россия").withMetroId(20347L);
        VcardInfo vcardInfo = vcardSteps.createVcard(fullVcard(userFieldsVcard, null), clientInfo);

        List<Vcard> actualVcards =
                manageVcardService.getVcards(operatorUid, clientId, singletonList(vcardInfo.getVcardId()));
        assumeThat("визитка должна быть найдена", actualVcards, hasSize(1));

        Vcard expectedVcard = vcardUserFields(vcardInfo.getCampaignId())
                .withCountry("Россия")
                .withCountryGeoId(225L)
                .withMetroId(20347L)
                .withMetroName("Невский проспект");
        assertThat(actualVcards.get(0), beanDiffer(expectedVcard).useCompareStrategy(compareStrategy));
    }

    @Test
    public void getVcards_whenVcardIdsAreNotNull_Test() {
        Long vcardId1 = vcardSteps.createVcard(fullVcard().withHouse("1"), clientInfo).getVcardId();
        Long vcardId2 = vcardSteps.createVcard(fullVcard().withHouse("2"), clientInfo).getVcardId();
        Long vcardId3 = vcardSteps.createVcard(fullVcard().withHouse("3"), clientInfo).getVcardId();

        List<Vcard> actualVcards = manageVcardService.getVcards(operatorUid, clientId, asList(vcardId1, vcardId2));
        List<Long> actualVcardIds = StreamEx.of(actualVcards).map(Vcard::getId).toList();

        assertThat(actualVcardIds, containsInAnyOrder(vcardId1, vcardId2));
    }

    @Test
    public void getVcards_whenVcardIdsAreNull_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);

        Long vcardId1 = vcardSteps.createVcard(fullVcard().withHouse("1"), campaignInfo).getVcardId();
        Long vcardId2 = vcardSteps.createVcard(fullVcard().withHouse("2"), campaignInfo).getVcardId();
        Long vcardId3 = vcardSteps.createVcard(fullVcard().withHouse("3"), campaignInfo).getVcardId();

        createTextBannerWithVcardId(campaignInfo, vcardId1);
        createTextBannerWithVcardId(campaignInfo, vcardId2);
        createTextBannerWithVcardId(campaignInfo, vcardId1);

        List<Vcard> actualVcards = manageVcardService.getVcards(operatorUid, clientId, null);
        List<Long> actualVcardIds = StreamEx.of(actualVcards).map(Vcard::getId).toList();

        assertThat("должны быть найдены используемые в баннерах визитки",
                actualVcardIds, containsInAnyOrder(vcardId1, vcardId2));
    }

    @Test
    public void getVcardsUsesCount_Test() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign(clientInfo);

        Long vcardId1 = vcardSteps.createVcard(fullVcard().withHouse("1"), campaignInfo).getVcardId();
        Long vcardId2 = vcardSteps.createVcard(fullVcard().withHouse("2"), campaignInfo).getVcardId();
        Long vcardId3 = vcardSteps.createVcard(fullVcard().withHouse("3"), campaignInfo).getVcardId();

        createTextBannerWithVcardId(campaignInfo, vcardId1);
        createTextBannerWithVcardId(campaignInfo, vcardId1);
        createTextBannerWithVcardId(campaignInfo, vcardId3);

        Map<Long, Long> vcardsUsesCount = manageVcardService.getVcardsUsesCount(
                clientId, asList(vcardId1, vcardId2, vcardId3));

        softly.assertThat(vcardsUsesCount.get(vcardId1)).as("количество баннеров с vcard_id = " + vcardId1)
                .isEqualTo(2L);
        softly.assertThat(vcardsUsesCount.get(vcardId3)).as("количество баннеров с vcard_id = " + vcardId3)
                .isEqualTo(1L);
    }

    private Vcard validVcard(Long campaignId) {
        return vcardUserFields(campaignId).withHouse("10");
    }

    private Vcard invalidVcard(Long campaignId) {
        return vcardUserFields(campaignId).withOgrn("123");
    }

    private Vcard expectedVcard(Long campaignId) {
        return validVcard(campaignId).withCampaignId(campaignId).withUid(clientUid);
    }

    private Long createValidVcard(CampaignInfo campaignInfo) {
        return vcardSteps
                .createVcard(fullVcard(validVcard(null), null), campaignInfo)
                .getVcardId();
    }

    private TextBannerInfo createTextBannerWithVcardId(@Nullable CampaignInfo campaignInfo, @Nullable Long vcardId) {
        if (campaignInfo == null) {
            campaignInfo = new CampaignInfo()
                    .withCampaign(activeTextCampaign(null, null))
                    .withClientInfo(clientInfo);
        }
        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeTextAdGroup(null))
                .withCampaignInfo(campaignInfo);

        OldTextBanner banner = activeTextBanner(null, null).withVcardId(vcardId);
        return bannerSteps.createBanner(banner, adGroupInfo);
    }

    private DynamicBannerInfo createDynamicBannerWithVcardId(@Nullable CampaignInfo campaignInfo, @Nullable Long vcardId) {
        if (campaignInfo == null) {
            campaignInfo = new CampaignInfo()
                    .withCampaign(activeDynamicCampaign(null, null))
                    .withClientInfo(clientInfo);
        }
        AdGroupInfo adGroupInfo = new AdGroupInfo()
                .withAdGroup(activeDynamicTextAdGroup(null))
                .withCampaignInfo(campaignInfo);

        OldDynamicBanner banner = activeDynamicBanner(null, null).withVcardId(vcardId);
        DynamicBannerInfo bannerInfo = bannerSteps.createBanner(banner, adGroupInfo);
        bannerSteps.createBannerImage(bannerInfo);
        return bannerInfo;
    }

    private Long saveVcard(SaveVcardRequest request, @Nullable Vcard expectedVcard) {
        Result<Long> result = manageVcardService.saveVcard(operatorUid, clientId, request);
        checkOperationResult(result, expectedVcard);
        return result.getResult();
    }

    private Long assignVcard(AssignVcardRequest request, @Nullable Vcard expectedVcard) {
        Result<Long> result = manageVcardService.assignVcard(operatorUid, clientId, request);
        checkOperationResult(result, expectedVcard);
        return result.getResult();
    }

    private void unassignVcard(UnassignVcardRequest request, boolean isSuccessful) {
        Result<Long> result = manageVcardService.unassignVcard(operatorUid, clientId, request);
        assumeThat("результат операции должен быть " + (isSuccessful ? "положительным" : "отрицательным"),
                result.isSuccessful(), is(isSuccessful));
    }

    private void checkOperationResult(Result<Long> result, @Nullable Vcard expectedVcard) {
        if (expectedVcard != null) {
            assumeThat("результат операции должен быть положительным",
                    result.isSuccessful(), is(true));
            Long newVcardId = result.getResult();
            expectedVcard.withId(newVcardId);

            Vcard actualVcard = vcardRepository.getVcards(shard, clientUid, singletonList(newVcardId)).get(0);
            assumeThat(actualVcard, beanDiffer(expectedVcard).useCompareStrategy(compareStrategy));
        } else {
            assumeThat("результат операции должен быть отрицательным",
                    result.isSuccessful(), is(false));
        }
    }

    /**
     * Проверка, что во всех переданных баннерах номер визитки равен заданному
     *
     * @param bannerIds - id баннеров которые нужно проверить
     * @param vcardId   - номер визитки (может быть null)
     */
    private void checkThatAllBannersHaveThisVcardId(List<Long> bannerIds, @Nullable Long vcardId) {
        List<OldBanner> banners = bannerRepository.getBanners(shard, bannerIds);

        List<Long> actualBannerIdsWithVcardId = StreamEx.of(banners)
                .select(OldBannerWithVcard.class)
                .filter(b -> Objects.equals(vcardId, b.getVcardId()))
                .map(OldBanner::getId).toList();

        softly.assertThat(actualBannerIdsWithVcardId).as("номер визитки в баннерах должен соответствовать ожидаемому")
                .containsExactlyInAnyOrder(bannerIds.toArray(new Long[0]));
    }
}
