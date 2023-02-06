package ru.yandex.autotests.direct.cmd.excel.banners.imagebanner.creative.canvasbs;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import org.apache.ws.security.util.Base64;

import ru.yandex.autotests.direct.cmd.data.canvasbs.CanvasBs;
import ru.yandex.autotests.direct.cmd.data.canvasbs.CanvasCreativeBsResponse;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.directapi.darkside.model.ScriptParams;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.autotests.irt.testutils.RandomUtils;

public class CanvasCreativeBsTestHelper {

    private static final String NOT_FOUND = "Not Found";
    private static final String PREVIEW_URL =
            "https://avatars.mdst.yandex.net/get-media-adv-screenshooter/3347/itzown5e05lyaf10815xjqf5/orig";

    public static CanvasBs getCanvasCreativeBs(Long creativeId) {
        return new CanvasBs()
                .withCreativeId(creativeId)
                .withCreativeName(RandomUtils.getString(10))
                .withHeight(100)
                .withWidth(100)
                .withPreviewUrl(PREVIEW_URL)
                .withLivePreviewUrl(PREVIEW_URL)
                .withModerationInfo(new Object());
    }

    public static CanvasCreativeBsResponse getSucceedCanvasCreativeBsResponse(Long creativeId) {
        return new CanvasCreativeBsResponse()
                .withCreativeId(creativeId)
                .withCreative(getCanvasCreativeBs(creativeId))
                .withOk(Boolean.TRUE);
    }

    public static CanvasCreativeBsResponse getNotFoundCanvasCreativeBsResponse(Long creativeId) {
        return new CanvasCreativeBsResponse()
                .withCreativeId(creativeId)
                .withOk(Boolean.FALSE)
                .withMessage(NOT_FOUND);
    }

    public static List<CanvasCreativeBsResponse> getCanvasCreativeBsResponse(List<Long> succeedCreativeIds,
                                                                             List<Long> notFoundcreativeIds) {
        List<CanvasCreativeBsResponse> canvasCreativeBsResponse = new ArrayList<>();
        succeedCreativeIds.stream()
                .forEach(id -> canvasCreativeBsResponse.add(getSucceedCanvasCreativeBsResponse(id)));
        notFoundcreativeIds.stream()
                .forEach(id -> canvasCreativeBsResponse.add(getNotFoundCanvasCreativeBsResponse(id)));
        return canvasCreativeBsResponse;
    }

    public static void runProcessImageQueueWithCreative(DirectCmdRule cmdRule,
                                                        List<CanvasCreativeBsResponse> canvasCreatives, String client) {
        cmdRule.darkSideSteps().getRunScriptSteps().runProcessImageQueue(
                new ScriptParams()
                        .withUniq(12044)
                        .once()
                        .withShardId(TestEnvironment.newDbSteps().shardingSteps().getShardByLogin(client))
                        .withClientIds(User.get(client).getClientID())
                        .withCustomParam("--fake-creatives-base64",
                                Base64.encode(new Gson().toJson(canvasCreatives).getBytes())));
    }
}
