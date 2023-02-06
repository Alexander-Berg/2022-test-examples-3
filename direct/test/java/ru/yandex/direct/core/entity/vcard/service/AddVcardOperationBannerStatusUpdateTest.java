package ru.yandex.direct.core.entity.vcard.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.repository.BannerCommonRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.entity.vcard.model.PointPrecision;
import ru.yandex.direct.core.entity.vcard.model.PointType;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.repository.VcardRepository;
import ru.yandex.direct.core.entity.vcard.service.validation.AddVcardValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestVcards;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.geosearch.model.Precision;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.textBannerData;
import static ru.yandex.direct.core.testing.data.TestVcards.autoPoint;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddVcardOperationBannerStatusUpdateTest {

    private static final long TWO_SECONDS = 2000;

    @Autowired
    private Steps steps;

    @Autowired
    private AddVcardValidationService addVcardValidationService;

    @Autowired
    private VcardRepository vcardRepository;

    @Autowired
    private VcardHelper vcardHelper;

    @Autowired
    private BannerCommonRepository bannerCommonRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private GeosearchClient geosearchClient;
    private Long clientUid;
    private ClientId clientId;
    private Long campaignId;
    private int shard;
    private AdGroupInfo adGroupInfo;
    private PointOnMap manualPoint;

    public AddVcardOperationBannerStatusUpdateTest() {
        geosearchClient = mock(GeosearchClient.class);
        setGeocoderResponse(defaultGeocoderResponse());
    }

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        shard = campaignInfo.getShard();
        clientId = campaignInfo.getClientId();
        clientUid = campaignInfo.getUid();
        campaignId = campaignInfo.getCampaignId();
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        manualPoint = autoPoint();
    }

    @Test
    public void prepareAndApply_CreateBannerWithVcard_AddUpdatedVcard_BannerStatusBsSyncedUpdated() {
        AddVcardOperation operation = createOperation(singletonList(fullVcard(clientUid, campaignId)
                .withManualPoint(manualPoint)));
        MassResult<Long> massResult = operation.prepareAndApply();

        Long vcardId = massResult.get(0).getResult();

        TextBannerInfo textBannerInfo = steps.bannerSteps().createBanner(activeTextBanner(
                textBannerData().withVcardId(vcardId)), adGroupInfo);

        Long bannerId = textBannerInfo.getBannerId();
        BannerWithSystemFields oldBanner = retrieveActualBanner(bannerId);

        assumeThat("statusBsSynced должен быть YES", oldBanner.getStatusBsSynced(), is(StatusBsSynced.YES));

        manualPoint.setX(BigDecimal.valueOf(100.0));

        AddVcardOperation operation2 = createOperation(singletonList(fullVcard(clientUid, campaignId)
                .withManualPoint(manualPoint)));
        operation2.prepareAndApply();
        BannerWithSystemFields newBanner = retrieveActualBanner(bannerId);

        assertThat("statusBsSynced должен смениться на NO", newBanner.getStatusBsSynced(), is(StatusBsSynced.NO));
    }

    @Test
    public void prepareAndApply_CreateBannerWithVcard_AddUpdatedVcard_BannerLastChangeUpdated()
            throws InterruptedException {
        AddVcardOperation operation = createOperation(singletonList(fullVcard(clientUid, campaignId)
                .withManualPoint(manualPoint)));
        MassResult<Long> massResult = operation.prepareAndApply();

        Long vcardId = massResult.get(0).getResult();

        TextBannerInfo textBannerInfo = steps.bannerSteps().createBanner(activeTextBanner(
                textBannerData().withVcardId(vcardId)), adGroupInfo);

        Long bannerId = textBannerInfo.getBannerId();
        BannerWithSystemFields oldBanner = retrieveActualBanner(bannerId);

        manualPoint.setX(BigDecimal.valueOf(100.0));

        AddVcardOperation operation2 = createOperation(
                singletonList(fullVcard(clientUid, campaignId).withManualPoint(manualPoint)));

        // Задержка нужна для того, чтобы lastChange мог измениться на новое значение.
        Thread.sleep(TWO_SECONDS);

        operation2.prepareAndApply();
        BannerWithSystemFields newBanner = retrieveActualBanner(bannerId);

        assertThat("lastChange должен поменяться", oldBanner.getLastChange(),
                not(is((newBanner.getLastChange()))));
    }

    @Test
    public void prepareAndApply_CreateBannerWithVcard_AddDuplicatedVcard_BannerStatusBsSyncedNotChanged() {
        AddVcardOperation operation = createOperation(singletonList(fullVcard(clientUid, campaignId)
                .withManualPoint(manualPoint)));
        MassResult<Long> massResult = operation.prepareAndApply();

        Long vcardId = massResult.get(0).getResult();

        TextBannerInfo textBannerInfo = steps.bannerSteps().createBanner(activeTextBanner(
                textBannerData().withVcardId(vcardId)), adGroupInfo);

        Long bannerId = textBannerInfo.getBannerId();
        BannerWithSystemFields oldBanner = retrieveActualBanner(bannerId);

        assumeThat("statusBsSynced должен быть YES", oldBanner.getStatusBsSynced(), is(StatusBsSynced.YES));

        AddVcardOperation operation2 = createOperation(singletonList(fullVcard(clientUid, campaignId)
                .withManualPoint(manualPoint)));

        operation2.prepareAndApply();
        BannerWithSystemFields newBanner = retrieveActualBanner(bannerId);

        assertThat("statusBsSynced не должен измениться", newBanner.getStatusBsSynced(), is(StatusBsSynced.YES));
    }

    @Test
    public void prepareAndApply_CreateBannerWithVcard_AddDuplicatedVcard_BannerLastChangeNotChanged()
            throws InterruptedException {
        AddVcardOperation operation = createOperation(singletonList(fullVcard(clientUid, campaignId)
                .withManualPoint(manualPoint)));
        MassResult<Long> massResult = operation.prepareAndApply();

        Long vcardId = massResult.get(0).getResult();

        TextBannerInfo textBannerInfo = steps.bannerSteps().createBanner(activeTextBanner(
                textBannerData().withVcardId(vcardId)), adGroupInfo);

        Long bannerId = textBannerInfo.getBannerId();
        BannerWithSystemFields oldBanner = retrieveActualBanner(bannerId);

        AddVcardOperation operation2 = createOperation(
                singletonList(fullVcard(clientUid, campaignId).withManualPoint(manualPoint)));

        // Задержка нужна для того, чтобы lastChange мог измениться на новое значение.
        Thread.sleep(TWO_SECONDS);

        operation2.prepareAndApply();
        BannerWithSystemFields newBanner = retrieveActualBanner(bannerId);
        assertThat("lastChange не должен поменяться", oldBanner.getLastChange(), is(newBanner.getLastChange()));
    }

    private BannerWithSystemFields retrieveActualBanner(Long bannerId) {
        return bannerTypedRepository.getSafely(shard, singletonList(bannerId), BannerWithSystemFields.class).get(0);
    }

    private AddVcardOperation createOperation(List<Vcard> vcards) {
        return new AddVcardOperation(Applicability.PARTIAL, false, vcards,
                addVcardValidationService, vcardRepository, vcardHelper, clientUid,
                UidClientIdShard.of(clientUid, clientId, shard), bannerCommonRepository);
    }

    private GeoObject defaultGeocoderResponse() {
        return geocoderResponse(TestVcards.autoPoint(), TestVcards.DEFAULT_POINT_TYPE,
                TestVcards.DEFAULT_PRECISION);
    }

    private GeoObject geocoderResponse(PointOnMap pointOnMap, PointType pointType, PointPrecision precision) {
        Kind geosearchKind = Kind.valueOf(pointType.name());
        Precision geocoderPrecision = Precision.valueOf(precision.name());
        return new GeoObject.Builder()
                .withX(pointOnMap.getX()).withY(pointOnMap.getY())
                .withX1(pointOnMap.getX1()).withY1(pointOnMap.getY1())
                .withX2(pointOnMap.getX2()).withY2(pointOnMap.getY2())
                .withPrecision(geocoderPrecision)
                .withKind(geosearchKind)
                .build();
    }

    private void setGeocoderResponse(@Nullable GeoObject geocoderResponse) {
        when(geosearchClient.getMostRelevantGeoData(any()))
                .thenReturn(geocoderResponse);
    }
}
