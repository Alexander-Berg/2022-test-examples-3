package ru.yandex.direct.core.entity.addition.callout.repository;

import java.time.LocalDateTime;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.banner.model.AdditionType;
import ru.yandex.direct.core.entity.banner.model.BannerAddition;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerAdditionsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestCalloutRepository;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CalloutsByBannerIdsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestCalloutRepository calloutRepository;

    @Autowired
    private OldBannerAdditionsRepository bannerAdditionsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    // test entities:
    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private int shard;

    private CampaignInfo campaignInfo1;
    private CampaignInfo campaignInfo2;
    private CampaignInfo campaignInfo3;
    private AdGroupInfo adGroupInfo1;
    private AdGroupInfo adGroupInfo2;
    private AdGroupInfo adGroupInfo3;

    @Before
    public void before() {
        clientInfo1 = steps.clientSteps().createDefaultClient();
        shard = clientInfo1.getShard();

        clientInfo2 = steps.clientSteps().createDefaultClient();
        // проверим, что шард одинаковый
        assertThat(clientInfo2.getShard())
                .isEqualTo(shard)
                .isEqualTo(ClientSteps.DEFAULT_SHARD);

        // кампании: 2 в одном клиенте, одна в другом. У каждой по 1 группе
        campaignInfo1 = steps.campaignSteps().createActiveTextCampaign(clientInfo1);
        campaignInfo2 = steps.campaignSteps().createActiveTextCampaign(clientInfo1);
        campaignInfo3 = steps.campaignSteps().createActiveTextCampaign(clientInfo2);
        adGroupInfo1 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo1);
        adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo2);
        adGroupInfo3 = steps.adGroupSteps().createDefaultAdGroup(campaignInfo3);
    }

    @Test
    public void findAllCalloutIds() {
        // баннеры: 1 и 2 в одной группе, 3 в другой компании, 4 в другом клиенте
        TextBannerInfo bannerInfo1 = steps.bannerSteps().createActiveTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = steps.bannerSteps().createActiveTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo3 = steps.bannerSteps().createActiveTextBanner(adGroupInfo2);
        TextBannerInfo bannerInfo4 = steps.bannerSteps().createActiveTextBanner(adGroupInfo3);

        Callout callout1 = getDefaultCallout(clientInfo1);
        Callout callout2 = getDefaultCallout(clientInfo1);
        Callout callout3 = getDefaultCallout(clientInfo2);
        Callout callout4 = getDefaultCallout(clientInfo2);
        calloutRepository.add(shard, asList(callout1, callout2, callout3, callout4));

        // колауты: 1й для двух баннеров, 2й и 3й для одного, для 4го баннера 2 колаута
        connectCallout(bannerInfo1, callout1, 0);
        connectCallout(bannerInfo2, callout1, 1);
        connectCallout(bannerInfo3, callout2, 2);
        connectCallout(bannerInfo4, callout3, 4);
        connectCallout(bannerInfo4, callout4, 5);

        var allBannerIds = StreamEx.of(bannerInfo1, bannerInfo2, bannerInfo3, bannerInfo4)
                .map(info -> info.getBannerId()).toList();

        // ищем по всем баннерам
        assertThat(calloutRepository.getUniqueExistingCalloutIdsByBannerIds(shard, allBannerIds))
                .containsOnly(
                        callout1.getId(), callout2.getId(), callout3.getId(), callout4.getId()
                );
    }

    @Test
    public void findTwoCalloutsForOneBanner() {
        // баннеры: 1 и 2 в одной группе, 3й в отдельном клиенте
        TextBannerInfo bannerInfo1 = steps.bannerSteps().createActiveTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = steps.bannerSteps().createActiveTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo3 = steps.bannerSteps().createActiveTextBanner(adGroupInfo3);

        Callout callout1 = getDefaultCallout(clientInfo1);
        Callout callout2 = getDefaultCallout(clientInfo1);
        Callout callout3 = getDefaultCallout(clientInfo1);
        Callout callout4 = getDefaultCallout(clientInfo2);
        calloutRepository.add(shard, asList(callout1, callout2, callout3, callout4));

        // колауты: 1й и 2й для 1го баннера, 1й, 3й и 4й для других баннеров
        connectCallout(bannerInfo1, callout1, 0);
        connectCallout(bannerInfo1, callout2, 1);
        connectCallout(bannerInfo2, callout1, 2);
        connectCallout(bannerInfo2, callout3, 3);
        connectCallout(bannerInfo3, callout4, 4);

        // только баннер 1
        assertThat(calloutRepository.getUniqueExistingCalloutIdsByBannerIds(shard,
                singletonList(
                        bannerInfo1.getBannerId()
                )))
                .containsOnly(
                        callout1.getId(), callout2.getId()
                );
    }


    @Test
    public void filterDeletedCallouts() {
        // баннеры: 1 и 2 в разных компаниях и клиентах
        TextBannerInfo bannerInfo1 = steps.bannerSteps().createActiveTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = steps.bannerSteps().createActiveTextBanner(adGroupInfo3);

        Callout callout1 = getDefaultCallout(clientInfo1);
        Callout callout2 = getDefaultCallout(clientInfo1);
        Callout callout3 = getDefaultCallout(clientInfo2);
        Callout callout4 = getDefaultCallout(clientInfo2);

        calloutRepository.add(shard, asList(callout1, callout2, callout3, callout4));
        calloutRepository.setDeleted(shard, asList(callout2.getId(), callout4.getId()), true);

        // по 2 колаута на баннер
        connectCallout(bannerInfo1, callout1, 0);
        connectCallout(bannerInfo1, callout2, 1);
        connectCallout(bannerInfo2, callout3, 2);
        connectCallout(bannerInfo2, callout4, 3);

        // по всем баннерам
        assertThat(calloutRepository.getUniqueExistingCalloutIdsByBannerIds(shard,
                asList(
                        bannerInfo1.getBannerId(), bannerInfo2.getBannerId()
                )))
                .containsOnly(
                        callout1.getId(), callout3.getId()
                );
    }

    @Test
    public void filterOtherAdditions() {
        // баннеры: 1 и 2 в разных компаниях
        TextBannerInfo bannerInfo1 = steps.bannerSteps().createActiveTextBanner(adGroupInfo1);
        TextBannerInfo bannerInfo2 = steps.bannerSteps().createActiveTextBanner(adGroupInfo2);

        Callout callout1 = getDefaultCallout(clientInfo1);
        Callout callout2 = getDefaultCallout(clientInfo1);
        calloutRepository.add(shard, asList(callout1, callout2));

        long additionId = Math.max(callout1.getId(), callout2.getId()) + 1;

        connectCallout(bannerInfo1, callout1, 0);
        addBannerAddition(bannerInfo1, AdditionType.DISCLAIMER, 1, additionId++);
        addBannerAddition(bannerInfo1, AdditionType.EXPERIMENT, 2, additionId++);
        connectCallout(bannerInfo2, callout2, 3);
        addBannerAddition(bannerInfo2, AdditionType.DISCLAIMER, 4, additionId++);
        addBannerAddition(bannerInfo2, AdditionType.EXPERIMENT, 5, additionId);

        // по всем баннерам
        assertThat(calloutRepository.getUniqueExistingCalloutIdsByBannerIds(shard,
                asList(
                        bannerInfo1.getBannerId(), bannerInfo2.getBannerId()
                )))
                .containsOnly(
                        callout1.getId(), callout2.getId()
                );
    }


    /**
     * Соединить колаут с баннером.
     *
     * @param sequenceNum порядковый номер для сортировки
     */
    private void connectCallout(TextBannerInfo bannerInfo, Callout callout, long sequenceNum) {
        BannerAddition bannerAddition = new BannerAddition()
                .withAdditionType(AdditionType.CALLOUT)
                .withSequenceNum(sequenceNum)
                .withBannerId(bannerInfo.getBannerId())
                .withId(callout.getId());
        bannerAdditionsRepository
                .addOrUpdateBannerAdditions(dslContextProvider.ppc(shard), singletonList(bannerAddition));
    }

    private void addBannerAddition(TextBannerInfo bannerInfo, AdditionType additionType, long sequenceNum,
                                   long additionId) {
        BannerAddition bannerAddition = new BannerAddition()
                .withAdditionType(additionType)
                .withSequenceNum(sequenceNum)
                .withBannerId(bannerInfo.getBannerId())
                .withId(additionId);
        bannerAdditionsRepository
                .addOrUpdateBannerAdditions(dslContextProvider.ppc(shard), singletonList(bannerAddition));
    }

    private Callout getDefaultCallout(ClientInfo clientInfo) {
        return new Callout()
                .withClientId(clientInfo.getClientId().asLong())
                .withText(RandomStringUtils.randomAlphanumeric(5))
                .withCreateTime(LocalDateTime.now());
    }
}
