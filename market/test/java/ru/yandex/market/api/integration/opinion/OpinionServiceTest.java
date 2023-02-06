package ru.yandex.market.api.integration.opinion;

import java.util.Collections;

import javax.inject.Inject;

import io.netty.util.concurrent.Future;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.junit.Test;

import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.opinion.PublishModelOpinionRequest;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.internal.opinion.OpinionsSort;
import ru.yandex.market.api.internal.report.SortOrder;
import ru.yandex.market.api.opinion.Opinion;
import ru.yandex.market.api.opinion.OpinionService;
import ru.yandex.market.api.opinion.OpinionV2Converter;
import ru.yandex.market.api.opinion.PublishResult;
import ru.yandex.market.api.opinion.UsageTime;
import ru.yandex.market.api.opinion.Visibility;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.ApiStrings;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.PersGradeTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersStaticTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 *
 * Created by apershukov on 06.10.16.
 */
public class OpinionServiceTest extends BaseTest {
    @Inject
    private OpinionService opinionService;

    @Inject
    private PersGradeTestClient persGradeTestClient;

    @Inject
    private PersStaticTestClient persStaticTestClient;

    @Inject
    private ReportTestClient reportTestClient;

    @Test
    public void addModelOpinion() {
        long uid = 1234;
        OauthUser user = new OauthUser(uid);

        long modelId = 10495456;

        int regionId = 54;
        context.getRegionInfo().setRawRegionId(regionId);
        context.setPpList(IntLists.EMPTY_LIST);
        context.setClientVersionInfo(new KnownMobileClientVersionInfo(Platform.IOS, DeviceType.IPAD, SemanticVersion.MIN));

        PublishModelOpinionRequest inputRequest = new PublishModelOpinionRequest();
        inputRequest.setVisibility(Visibility.NAME);
        inputRequest.setUsageTime(UsageTime.FEW_YEARS);
        inputRequest.setGrade(1);
        inputRequest.setText("text");
        inputRequest.setPros("pros");
        inputRequest.setCons("cons");

        PublishModelOpinionRequest outputRequest = new PublishModelOpinionRequest();
        outputRequest.setVisibility(Visibility.NAME);
        outputRequest.setUsageTime(UsageTime.FEW_YEARS);
        outputRequest.setGrade(1);
        outputRequest.setText("text");
        outputRequest.setPros("pros");
        outputRequest.setCons("cons");
        outputRequest.setSource("market;ios;main");

        persGradeTestClient.getModelOpinion(
                uid,
                modelId,
                "opinion_59555258.xml"
        );

        persGradeTestClient.addModelOpinion(
                uid,
                ApiStrings.getBytes(OpinionV2Converter.toJson(modelId, regionId, outputRequest).toString()),
                "opinion_added.json"
        );

        reportTestClient.getModelInfoById(
            modelId,
            "model_10495456.json"
        );

        Future<PublishResult> future = opinionService.publishModelOpinion(modelId, user, inputRequest, genericParams);

        PublishResult result = Futures.waitAndGet(future);
        assertEquals(PublishResult.OK, result);
    }

    @Test
    public void addOpinionToNotExistingModel() {
        long uid = 1234;
        OauthUser user = new OauthUser(uid);

        long modelId = 11;

        PublishModelOpinionRequest request = new PublishModelOpinionRequest();
        request.setVisibility(Visibility.NAME);
        request.setUsageTime(UsageTime.FEW_YEARS);
        request.setGrade(1);
        request.setText("text");
        request.setPros("pros");
        request.setCons("cons");

        ContextHolder.get().getRegionInfo().setRawRegionId(54);

        reportTestClient.getModelInfoById(
            modelId,
            "model_not_found.json"
        );

        Future<PublishResult> future = opinionService.publishModelOpinion(modelId, user, request, genericParams);
        Futures.wait(future);

        assertFalse(future.isSuccess());
        assertEquals(NotFoundException.class, future.cause().getClass());
    }

    @Test
    public void pagingInOpinion() {
        long modelId = 10495456;
        OpinionsSort sort = new OpinionsSort(OpinionsSort.Type.DEFAULT, SortOrder.ASC);

        context.setVersion(Version.V2_0_0);

        reportTestClient.getModelInfoById(
            modelId,
            "model_10495456.json"
        );

        persStaticTestClient.getModelOpinion(
            modelId,
            "opinion_model_10495456.json"
        );


        PagedResult<Opinion> result = Futures.waitAndGet(opinionService.getModelOpinions(
            modelId,
            null,
            0,
            sort,
            new PageInfo(2, 7),
            null,
            Collections.emptyList()
        )).getOpinions();

        assertThat(result.getPageInfo().getNumber(), is(2));
        assertThat(result.getPageInfo().getCount(), is(7));

    }

}
