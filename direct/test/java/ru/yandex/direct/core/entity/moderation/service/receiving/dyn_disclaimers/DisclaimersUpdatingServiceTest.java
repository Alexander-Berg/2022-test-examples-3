package ru.yandex.direct.core.entity.moderation.service.receiving.dyn_disclaimers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.DisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerAdditionsRepository;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.moderation.service.ModerationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestModerationRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DisclaimersUpdatingServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestModerationRepository testModerationRepository;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private OldBannerRepository bannerRepository;

    @Autowired
    private OldBannerAdditionsRepository bannerAdditionsRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    private DisclaimersUpdatingService disclaimersUpdatingService;
    private DisclaimersRepository disclaimersRepository;
    private int shard;
    private ClientInfo clientInfo;
    private ClientId clientId;

    private CampaignInfo campaignInfo;
    private OldTextBanner banner;

    private static int id = 100;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createDefaultCampaign();
        steps.campaignSteps().createCampaign(campaignInfo);
        clientInfo = campaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        banner = steps.bannerSteps().createBanner(
                activeTextBanner(null, null).withDisplayHref("New displayhref")
                        .withDisplayHrefStatusModerate(DisplayHrefStatusModerate.READY),
                campaignInfo
        ).getBanner();

        ShardHelper shardHelper = mock(ShardHelper.class);

        when(shardHelper.generateAdditionItemIds(anyInt())).thenAnswer(args -> {
            int size = args.getArgument(0);
            var result = LongStream.range(id, id + size).boxed().collect(Collectors.toList());
            id += size;
            return result;
        });

        disclaimersRepository = new DisclaimersRepository(shardHelper);
        disclaimersUpdatingService = new DisclaimersUpdatingService(disclaimersRepository);
    }


    @After
    public void after() {
        testModerationRepository.removeDisclaimers(shard);
    }


    @Test
    public void insertNewDisclaimer() {
        DisclaimerUpdateRequest disclaimerUpdateRequest = new DisclaimerUpdateRequest(clientId.asLong(),
                banner.getId(), "Новый дисклеймер");

        disclaimersUpdatingService.update(dslContextProvider.ppc(shard).configuration(),
                List.of(disclaimerUpdateRequest));

        Map<Long, String> result = bannerAdditionsRepository.getAdditionDisclaimerByBannerIds(shard, List.of(banner.getId()));

        assertThat(result).isNotEmpty();
        assertThat(result).containsKeys(banner.getId());
        assertThat(result.get(banner.getId())).isEqualTo("Новый дисклеймер");
    }

    @Test
    public void removeDisclaimer() {
        DisclaimerUpdateRequest disclaimerUpdateRequest = new DisclaimerUpdateRequest(clientId.asLong(),
                banner.getId(), "Новый дисклеймер");

        disclaimersUpdatingService.update(dslContextProvider.ppc(shard).configuration(),
                List.of(disclaimerUpdateRequest));

        DisclaimerUpdateRequest disclaimerUpdateRequest2 = new DisclaimerUpdateRequest(clientId.asLong(),
                banner.getId(), "");

        disclaimersUpdatingService.update(dslContextProvider.ppc(shard).configuration(),
                List.of(disclaimerUpdateRequest2));

        Map<Long, String> result = bannerAdditionsRepository.getAdditionDisclaimerByBannerIds(shard, List.of(banner.getId()));

        assertThat(result).doesNotContainKeys(banner.getId());
    }

    @Test
    public void updateDisclaimer() {
        DisclaimerUpdateRequest disclaimerUpdateRequest = new DisclaimerUpdateRequest(clientId.asLong(),
                banner.getId(), "Новый дисклеймер");

        disclaimersUpdatingService.update(dslContextProvider.ppc(shard).configuration(),
                List.of(disclaimerUpdateRequest));

        DisclaimerUpdateRequest disclaimerUpdateRequest2 = new DisclaimerUpdateRequest(clientId.asLong(),
                banner.getId(), "Еще один дисклеймер");

        disclaimersUpdatingService.update(dslContextProvider.ppc(shard).configuration(),
                List.of(disclaimerUpdateRequest2));

        Map<Long, String> result = bannerAdditionsRepository.getAdditionDisclaimerByBannerIds(shard, List.of(banner.getId()));

        assertThat(result).isNotEmpty();
        assertThat(result).containsKeys(banner.getId());
        assertThat(result.get(banner.getId())).isEqualTo("Еще один дисклеймер");
    }

}
