package ru.yandex.direct.intapi.entity.display.canvas;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.testing.info.BannerCreativeInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.display.canvas.model.GetUsedCreativesPageToken;
import ru.yandex.direct.intapi.entity.display.canvas.model.GetUsedCreativesRequest;
import ru.yandex.direct.intapi.entity.display.canvas.model.GetUsedCreativesResponse;
import ru.yandex.direct.intapi.entity.display.canvas.model.GetUsedCreativesSort;
import ru.yandex.direct.intapi.entity.display.canvas.model.GetUsedCreativesType;
import ru.yandex.direct.intapi.entity.display.canvas.service.DisplayCanvasUsedCreativesService;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DisplayCanvasUsedCreativeServiceTest {

    private static final Integer LIMIT = 1;

    @Autowired
    private Steps steps;

    @Autowired
    private DisplayCanvasUsedCreativesService displayCanvasUsedCreativesService;

    private ClientInfo clientInfo;
    private BannerCreativeInfo<OldTextBanner> canvasInfo;
    private BannerCreativeInfo<OldTextBanner> canvasInfo2;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        canvasInfo = steps.bannerCreativeSteps().createTextBannerCreative(clientInfo);
        canvasInfo2 = steps.bannerCreativeSteps().createTextBannerCreative(clientInfo);
    }

    @Test
    public void getUsedCreativesIds() {
        GetUsedCreativesRequest request = new GetUsedCreativesRequest().withClientId(clientInfo.getClientId().asLong())
                .withCreativeType(GetUsedCreativesType.VIDEO_ADDITION)
                .withLimit(LIMIT);
        GetUsedCreativesResponse usedCreatives = displayCanvasUsedCreativesService.getUsedCreatives(request);
        assertThat("ответ соответствует ожиданию", usedCreatives, beanDiffer(
                new GetUsedCreativesResponse(singletonList(canvasInfo.getCreativeId()),
                        new GetUsedCreativesPageToken().withSort(GetUsedCreativesSort.ASC)
                                .withCreativeId(canvasInfo.getCreativeId()))));
    }

    @Test
    public void getUsedCreativesIdsWithNextTokenAsc() {
        String nextToken = "ASC-" + canvasInfo.getCreativeId();
        GetUsedCreativesRequest request = new GetUsedCreativesRequest().withClientId(clientInfo.getClientId().asLong())
                .withCreativeType(GetUsedCreativesType.VIDEO_ADDITION)
                .withNextPageToken(nextToken)
                .withLimit(LIMIT);
        GetUsedCreativesResponse usedCreatives = displayCanvasUsedCreativesService.getUsedCreatives(request);
        assertThat("ответ соответствует ожиданию", usedCreatives, beanDiffer(
                new GetUsedCreativesResponse(singletonList(canvasInfo2.getCreativeId()),
                        new GetUsedCreativesPageToken().withSort(GetUsedCreativesSort.ASC)
                                .withCreativeId(canvasInfo2.getCreativeId()))));
    }

    @Test
    public void getUsedCreativesIdsWithNextTokenDesc() {
        String nextToken = "DESC-" + canvasInfo2.getCreativeId();
        GetUsedCreativesRequest request = new GetUsedCreativesRequest().withClientId(clientInfo.getClientId().asLong())
                .withCreativeType(GetUsedCreativesType.VIDEO_ADDITION)
                .withNextPageToken(nextToken)
                .withLimit(LIMIT);
        GetUsedCreativesResponse usedCreatives = displayCanvasUsedCreativesService.getUsedCreatives(request);
        assertThat("ответ соответствует ожиданию", usedCreatives, beanDiffer(
                new GetUsedCreativesResponse(singletonList(canvasInfo.getCreativeId()),
                        new GetUsedCreativesPageToken().withSort(GetUsedCreativesSort.DESC)
                                .withCreativeId(canvasInfo.getCreativeId()))));
    }
}
