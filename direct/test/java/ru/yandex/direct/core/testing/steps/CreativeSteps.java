package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.repository.CreativeRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.repository.TestBannerCreativeRepository;
import ru.yandex.direct.core.testing.repository.TestCreativeRepository;
import ru.yandex.direct.dbutil.sharding.ShardKey;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultBannerstorageCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCanvas;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpcVideoForCpcVideoBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmAudioAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOutdoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmOverlay;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmVideoAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultHtml5;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultInBannerCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultNonSkippableCpmVideoCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPriceSalesHtml5;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;

public class CreativeSteps {

    @Autowired
    ShardSupport shardSupport;

    @Autowired
    CreativeRepository creativeRepository;

    @Autowired
    TestCreativeRepository testCreativeRepository;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private TestBannerCreativeRepository testBannerCreativeRepository;

    public CreativeInfo addDefaultCanvasCreative(ClientInfo clientInfo) {
        Creative creative = defaultCanvas(clientInfo.getClientId(), null);
        long creativeId = testCreativeRepository.addCreative(clientInfo.getShard(), creative);
        creative.withId(creativeId);
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultCanvasCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultCanvas(clientInfo.getClientId(), creativeId);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultCanvasCreativeWithSize(ClientInfo clientInfo, Long height, Long width) {
        Creative creative = defaultCanvas(clientInfo.getClientId(), null).withHeight(height).withWidth(width);
        long creativeId = testCreativeRepository.addCreative(clientInfo.getShard(), creative);
        creative.withId(creativeId);
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultVideoAdditionCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultVideoAddition(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultInBannerCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultInBannerCreative(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultAudioAdditionCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultCpmAudioAddition(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultCpcVideoCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultCpcVideoForCpcVideoBanner(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultCpmOutdoorVideoCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultCpmOutdoorVideoAddition(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultCpmIndoorVideoCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultCpmIndoorVideoAddition(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addCpmVideoFrontpageCreative(ClientInfo clientInfo) {
        Creative creative = defaultCpmVideoAddition(clientInfo.getClientId(), getNextCreativeId());
        creative.setLayoutId(406L);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultCpmVideoAdditionCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultCpmVideoAddition(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultOverlayCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultCpmOverlay(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultBannerstorageCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultBannerstorageCreative(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultNonSkippableCreative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultNonSkippableCpmVideoCreative(clientInfo.getClientId(), creativeId);

        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultHtml5Creative(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultHtml5(clientInfo.getClientId(), creativeId);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultHtml5CreativeForFrontpage(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultHtml5(clientInfo.getClientId(), creativeId)
                .withWidth(1456L)
                .withHeight(180L);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultHtml5CreativeForGeoproduct(ClientInfo clientInfo, Long creativeId) {
        Creative creative = defaultHtml5(clientInfo.getClientId(), creativeId)
                .withWidth(640L)
                .withHeight(100L);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultHtml5CreativeForPriceSales(ClientInfo clientInfo, CpmPriceCampaign campaign) {
        Creative creative = defaultPriceSalesHtml5(clientInfo.getClientId(), getNextCreativeId(), campaign);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultHtml5CreativeWithSize(ClientInfo clientInfo, Long width, Long height) {
        Creative creative = defaultHtml5(clientInfo.getClientId(), getNextCreativeId())
                .withWidth(width)
                .withHeight(height);
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public CreativeInfo addDefaultVideoAdditionCreative(ClientInfo clientInfo) {
        return addDefaultVideoAdditionCreative(clientInfo, getNextCreativeId());
    }

    public CreativeInfo addDefaultAudioAdditionCreative(ClientInfo clientInfo) {
        return addDefaultAudioAdditionCreative(clientInfo, getNextCreativeId());
    }

    public CreativeInfo addDefaultCpmOutdoorVideoCreative(ClientInfo clientInfo) {
        return addDefaultCpmOutdoorVideoCreative(clientInfo, getNextCreativeId());
    }

    public CreativeInfo addDefaultCpmIndoorVideoCreative(ClientInfo clientInfo) {
        return addDefaultCpmIndoorVideoCreative(clientInfo, getNextCreativeId());
    }

    public CreativeInfo addDefaultHtml5CreativeForGeoproduct(ClientInfo clientInfo) {
        return addDefaultHtml5CreativeForGeoproduct(clientInfo, getNextCreativeId());
    }

    public CreativeInfo addDefaultHtml5CreativeForFrontpage(ClientInfo clientInfo) {
        return addDefaultHtml5CreativeForFrontpage(clientInfo, getNextCreativeId());
    }

    public CreativeInfo addDefaultPerformanceCreative(ClientInfo clientInfo) {
        Creative creative = defaultPerformanceCreative(clientInfo.getClientId(), getNextCreativeId());
        creativeRepository.add(clientInfo.getShard(), singletonList(creative));
        return new CreativeInfo().withCreative(creative).withClientInfo(clientInfo);
    }

    public void deleteCreativesByIds(int shard, Long... creativeIds) {
        testBannerCreativeRepository.deleteByCreativeIds(shard, creativeIds);
        testCreativeRepository.deleteCreativesByIds(shard, creativeIds);
    }

    public CreativeInfo createCreative() {
        return createCreative(null, null);
    }

    public CreativeInfo createCreative(ClientInfo clientInfo) {
        return createCreative(null, clientInfo);
    }

    public CreativeInfo createCreative(Creative creative, ClientInfo clientInfo) {
        return createCreative(new CreativeInfo()
                .withCreative(creative)
                .withClientInfo(clientInfo));
    }

    public CreativeInfo createCreative(CreativeInfo creativeInfo) {
        if (creativeInfo.getCreative() == null) {
            creativeInfo.withCreative(defaultVideoAddition(null, null));
        }
        if (creativeInfo.getClientInfo() == null) {
            creativeInfo.withClientInfo(clientSteps.createDefaultClient());
        }
        if (creativeInfo.getCreative().getId() == null) {
            Creative creative = creativeInfo.getCreative();
            creative.withClientId(creativeInfo.getClientId().asLong());

            long creativeId = testCreativeRepository.addCreative(creativeInfo.getShard(), creative);

            creative.withId(creativeId);

            shardSupport.saveValue(ShardKey.CREATIVE_ID, creativeId, ShardKey.CLIENT_ID, creative.getClientId());
        }

        return creativeInfo;
    }

    public Long getNextCreativeId() {
        return testCreativeRepository.getNextCreativeId();
    }

    public Long getNextCreativeGroupId(int shard) {
        return testCreativeRepository.getNextCreativeGroupId(shard);
    }

    public <V> void setCreativeProperty(CreativeInfo creativeInfo, ModelProperty<? super Creative, V> property,
                                        V value) {
        Creative creative = creativeInfo.getCreative();
        if (!Creative.allModelProperties().contains(property)) {
            throw new IllegalArgumentException(
                    "Model " + creative.getName() + " doesn't contain property " + property.name());
        }
        AppliedChanges<Creative> appliedChanges = new ModelChanges<>(creative.getId(), Creative.class)
                .process(value, property)
                .applyTo(creative);
        testCreativeRepository.update(creativeInfo.getShard(), singletonList(appliedChanges));
    }

    public void setCreativeModerateSendTime(CreativeInfo creativeInfo, LocalDateTime moderateSendTime) {
        testCreativeRepository.updateModerateSendTime(creativeInfo.getShard(),
                singleton(creativeInfo.getCreativeId()), moderateSendTime);
    }
}
