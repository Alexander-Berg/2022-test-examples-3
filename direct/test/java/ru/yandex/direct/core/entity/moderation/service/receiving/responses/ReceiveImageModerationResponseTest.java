package ru.yandex.direct.core.entity.moderation.service.receiving.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.moderation.model.ModerationDecision;
import ru.yandex.direct.core.entity.moderation.model.Verdict;
import ru.yandex.direct.core.entity.moderation.model.asset.BannerAssetModerationMeta;
import ru.yandex.direct.core.entity.moderation.model.image.ImageModerationResponse;
import ru.yandex.direct.core.entity.moderation.service.ModerationObjectType;
import ru.yandex.direct.core.entity.moderation.service.ModerationServiceNames;
import ru.yandex.direct.core.entity.moderation.service.receiving.ImageModerationReceivingService;
import ru.yandex.direct.core.entity.moderation.service.receiving.ModerationReceivingService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerImagesRecord;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.moderation.model.ModerationDecision.No;
import static ru.yandex.direct.core.entity.moderation.service.ModerationObjectType.IMAGES;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultBannerImage;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiveImageModerationResponseTest extends AbstractModerationResponseTest<BannerAssetModerationMeta,
        Verdict, ImageModerationResponse> {

    private static final long DEFAULT_VERSION = 10_000_000L;
    private static final List<Long> DEFAULT_REASONS = Arrays.asList(2L, 3L);

    @Autowired
    TestModerationRepository testModerationRepository;

    @Autowired
    Steps steps;

    @Autowired
    protected TestBannerRepository testBannerRepository;

    @Autowired
    ImageModerationReceivingService imageModerationReceivingService;

    private int shard;
    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private OldTextBanner banner;
    private BannerImageInfo<TextBannerInfo> imageInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        clientInfo = campaignInfo.getClientInfo();
        shard = clientInfo.getShard();

        TextBannerInfo textBannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null), campaignInfo);

        banner = textBannerInfo.getBanner();

        imageInfo = steps.bannerSteps().createBannerImage(textBannerInfo,
                steps.bannerSteps().createBannerImageFormat(clientInfo),
                defaultBannerImage(banner.getId(), randomAlphanumeric(16)).withBsBannerId(3L)
                        .withStatusModerate(OldStatusBannerImageModerate.SENT)
        );

        testModerationRepository.createBannerImageVersion(shard, banner.getId(), DEFAULT_VERSION);
    }

    private BannerImagesRecord getExpectedImageRecord(ImageModerationResponse response) {
        String verdict = StringUtils.capitalize(response.getResult().getVerdict().getString().toLowerCase());

        BannerImagesRecord record = new BannerImagesRecord();

        record.setBid(response.getMeta().getBannerId());
        record.setImageHash(imageInfo.getBannerImage().getImageHash());
        record.setStatusmoderate(BannerImagesStatusmoderate.valueOf(verdict));

        return record;
    }

    @Override
    protected int getShard() {
        return shard;
    }

    @Override
    protected void checkInDbForId(long bid, ImageModerationResponse response) {

        List<BannerImagesRecord> bannerImages = testModerationRepository.getBannerImages(shard,
                Collections.singleton(bid));

        assumeThat(bannerImages, not(empty()));

        BannerImagesRecord dbRecord = bannerImages.get(0);
        BannerImagesRecord expectedImageRecord = getExpectedImageRecord(response);

        assertEquals(dbRecord.getStatusmoderate(), expectedImageRecord.getStatusmoderate());
    }

    @Override
    protected ModerationReceivingService<ImageModerationResponse> getReceivingService() {
        return imageModerationReceivingService;
    }

    @Override
    protected long createObjectInDb(long version) {
        TextBannerInfo textBannerInfo = steps.bannerSteps()
                .createBanner(activeTextBanner(null, null), campaignInfo);

        steps.bannerSteps().createBannerImage(textBannerInfo,
                steps.bannerSteps().createBannerImageFormat(clientInfo),
                defaultBannerImage(textBannerInfo.getBannerId(), randomAlphanumeric(16)).withBsBannerId(3L)
                        .withStatusModerate(OldStatusBannerImageModerate.SENT)
        );

        testModerationRepository.createBannerImageVersion(shard, textBannerInfo.getBannerId(), version);

        return textBannerInfo.getBannerId();
    }

    @Override
    protected ModerationObjectType getObjectType() {
        return IMAGES;
    }

    @Override
    protected long getDefaultVersion() {
        return DEFAULT_VERSION;
    }

    @Override
    protected ImageModerationResponse createResponse(long bid, ModerationDecision status, @Nullable String language,
                                                     long version, Map<String, String> flags, List<Long> minusRegions,
                                                     ClientInfo clientInfo, List<ModerationReasonDetailed> reasons) {
        ImageModerationResponse response = new ImageModerationResponse();
        response.setService(ModerationServiceNames.DIRECT_SERVICE);
        response.setType(IMAGES);

        BannerAssetModerationMeta meta = new BannerAssetModerationMeta();
        meta.setClientId(clientInfo.getClientId().asLong());
        meta.setBannerId(bid);
        meta.setUid(clientInfo.getUid());
        meta.setVersionId(version);

        response.setMeta(meta);

        Verdict v = new Verdict();
        v.setVerdict(status);

        if (status == No) {
            v.setReasons(DEFAULT_REASONS);
        }

        response.setResult(v);

        return response;
    }

    @Override
    protected long getDefaultObjectId() {
        return imageInfo.getBannerInfo().getBannerId();
    }

    @Override
    protected ClientInfo getDefaultObjectClientInfo() {
        return clientInfo;
    }

    @Override
    protected void deleteDefaultObjectVersion() {
        testModerationRepository.deleteTurbolandingsVersion(getShard(), getDefaultObjectId());
    }
}
