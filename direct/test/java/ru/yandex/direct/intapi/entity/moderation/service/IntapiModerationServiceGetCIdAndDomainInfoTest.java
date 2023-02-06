package ru.yandex.direct.intapi.entity.moderation.service;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerModerationRepository;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.domain.model.CidAndDomain;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.user.service.validation.BlockUserValidationService;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.moderation.model.CidAndDomainInfo;
import ru.yandex.direct.intapi.entity.moderation.model.CidAndDomainInfoResponse;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@IntApiTest
@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class IntapiModerationServiceGetCIdAndDomainInfoTest {
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    Steps steps;
    @Autowired
    ShardHelper shardHelper;
    @Autowired
    PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    UserService userService;
    @Autowired
    BlockUserValidationService blockUserValidationService;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    BannerRelationsRepository bannerRelationsRepository;
    @Autowired
    BidRepository bidRepository;
    @Autowired
    CampaignRepository campaignRepository;
    @Mock
    BannerModerationRepository bannerModerationRepository;

    IntapiModerationService intapiModerationService;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public List<CidAndDomain> actualCidAndDomainInfos;

    @Parameterized.Parameter(2)
    @Nullable public Integer offset;

    @Parameterized.Parameter(3)
    @Nullable public Integer rowNumber;

    @Parameterized.Parameter(4)
    public List<CidAndDomainInfo> expectedCidAndDomainInfos;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> params() {
        return Arrays.asList(new Object[][]{
                {"list of one element, no offset or row number",
                    List.of(CidAndDomain.of(1234L, "ur.ofni.www")),
                        null, null,
                        List.of(new CidAndDomainInfo(1234L, "www.info.ru", emptySet(), emptyMap()))},
                {"offset is given, but rowNumber is not set",
                    List.of(CidAndDomain.of(1234L, "ur.ofni.www"),
                        CidAndDomain.of(1234L, "ur.ay.www")),
                        1, null,
                        List.of(new CidAndDomainInfo(1234L, "www.ya.ru", emptySet(), emptyMap()))},
                {"rowNumber is given, but offset is not set",
                        List.of(CidAndDomain.of(1234L, "ur.ofni.www"),
                                CidAndDomain.of(1234L, "ur.ay.www")),
                        null, 1,
                        List.of(new CidAndDomainInfo(1234L, "www.info.ru", emptySet(), emptyMap()))},
                {"offset and rowNumber are both out of boundary",
                        List.of(CidAndDomain.of(1234L, "ur.ofni.www")),
                        3, 5,
                       emptyList()},
        });
    }

    @Before
    public void before() {
        openMocks(this);
        intapiModerationService = new IntapiModerationService(shardHelper, ppcPropertiesSupport, campaignRepository,
                adGroupRepository, bannerModerationRepository, bannerRelationsRepository, bidRepository, userService,
                blockUserValidationService);
    }

    @Test
    public void checkLimitAndOffset() {
        var campaignInfo = steps.campaignSteps().createDefaultCampaign();

        doReturn(listToMap(actualCidAndDomainInfos, identity(), r -> emptySet()))
                .when(bannerModerationRepository)
                .getCidAndBannersInfo(anyInt(), any());
        doReturn(emptyMap())
                .when(bannerModerationRepository)
                .getDomainAndMinusGeoByCampaignIds(anyInt(), any());

        List<CidAndDomainInfo> cidAndDomainInfoResponse = ((CidAndDomainInfoResponse)
                intapiModerationService.getCidAndDomainInfoByUid(campaignInfo.getUid(), null, offset, rowNumber))
                .getInfoList();
        assertThat(cidAndDomainInfoResponse)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyElementsOf(expectedCidAndDomainInfos);
    }
}
