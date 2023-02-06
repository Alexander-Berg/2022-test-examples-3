package ru.yandex.direct.web.entity.inventori.service;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;

import ru.yandex.direct.asynchttp.AsyncHttpExecuteException;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.PageBlock;
import ru.yandex.direct.inventori.model.request.ProfileCorrection;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.VideoCreative;
import ru.yandex.direct.web.core.entity.inventori.model.ReachIndoorResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Ignore //todo выключил в рамках рефакторинга, переделать или удалить в рамках DIRECT-104384
public class InventoriWebServiceReachIndoorTest extends ReachIndoorBaseTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void getReachIndoorForecast_inventoriOkResponse() {
        inventoriSuccessResponse();

        ReachIndoorResult actualResult = inventoriWebService.getReachIndoorForecast(defaultRequest());
        ReachIndoorResult expectedResult = (ReachIndoorResult) new ReachIndoorResult()
                .withReach(1000L)
                .withOtsCapacity(2000L);

        assertThat(actualResult, beanDiffer(expectedResult).useCompareStrategy(REACH_INDOOR_RESULT_STRATEGY));
    }

    @Test
    public void getReachIndoorForecast_inventoriLessThanResponse() {
        inventoriSuccessLessThanResponse();

        ReachIndoorResult actualResult = inventoriWebService.getReachIndoorForecast(defaultRequest());
        ReachIndoorResult expectedResult = (ReachIndoorResult) new ReachIndoorResult()
                .withReachLessThan(3000L);

        assertThat(actualResult, beanDiffer(expectedResult).useCompareStrategy(REACH_INDOOR_RESULT_STRATEGY));
    }

    @Test(expected = IllegalStateException.class)
    public void getReachIndoorForecast_inventoriBadResponse() {
        inventoriBadResponse();
        inventoriWebService.getReachIndoorForecast(defaultRequest());
    }

    @Test(expected = AsyncHttpExecuteException.class)
    public void getReachIndoorForecast_inventoriExceptionResponse() {
        inventoriExceptionResponse();

        inventoriWebService.getReachIndoorForecast(defaultRequest());
    }

    @Test
    public void getReachIndoorForecast_inventoriCalledWithCorrectParameters() {
        inventoriSuccessResponse();
        inventoriWebService.getReachIndoorForecast(defaultRequest());

        ArgumentCaptor<Target> target = ArgumentCaptor.forClass(Target.class);
        ArgumentCaptor<String> requestId = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> campaignId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> adgroupId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> clientLogin = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> operatorLogin = ArgumentCaptor.forClass(String.class);
        verify(inventoriClient, times(1)).getIndoorPrediction(requestId.capture(),
                target.capture(), campaignId.capture(), adgroupId.capture(), clientLogin.capture(),
                operatorLogin.capture());

        Target expectedTarget = new Target()
                .withGroupType(GroupType.INDOOR)
                .withRegions(emptySet())
                .withVideoCreatives(asList(
                        new VideoCreative(60000, null, Sets.newSet(
                                new BlockSize(16, 9),
                                new BlockSize(16, 9))),
                        new VideoCreative(1500, null, Sets.newSet(
                                new BlockSize(4, 3)))
                ))
                .withPageBlocks(asList(
                        new PageBlock(PAGE_ID_1, singletonList(PAGE_1_BLOCK_ID_1)),
                        new PageBlock(PAGE_ID_2, asList(PAGE_2_BLOCK_ID_1, PAGE_2_BLOCK_ID_2))
                ))
                .withProfileCorrections(asList(
                        ProfileCorrection.builder()
                                .withGender(ProfileCorrection.Gender.MALE)
                                .withAge(ProfileCorrection.Age._0_17)
                                .withCorrection(110)
                                .build(),
                        ProfileCorrection.builder()
                                .withAge(ProfileCorrection.Age._18_24)
                                .withCorrection(120)
                                .build()
                ))
                .withExcludedDomains(emptySet())
                .withCryptaGroups(emptyList())
                .withAudienceGroups(emptyList());
        softly.assertThat(target.getValue()).is(matchedBy(beanDiffer(expectedTarget)));
        softly.assertThat(requestId.getValue()).isNotNull();
        softly.assertThat(campaignId.getValue()).isEqualTo(campaignInfo.getCampaignId());
        softly.assertThat(adgroupId.getValue()).isEqualTo(adGroupInfo.getAdGroupId());
        softly.assertThat(clientLogin.getValue()).isEqualTo(user.getLogin());
        softly.assertThat(operatorLogin.getValue()).isEqualTo(operator.getLogin());
    }
}
