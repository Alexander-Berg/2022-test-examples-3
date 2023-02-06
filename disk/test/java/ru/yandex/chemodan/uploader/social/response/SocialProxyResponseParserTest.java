package ru.yandex.chemodan.uploader.social.response;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.uploader.social.SocialTaskInfo;
import ru.yandex.chemodan.uploader.social.SocialTaskInfo.FailReason;
import ru.yandex.chemodan.uploader.social.SocialTaskInfo.State;
import ru.yandex.chemodan.uploader.social.response.SocialUploadCommitResultResponse.SocialPhotoIdResult;
import ru.yandex.chemodan.uploader.social.response.SocialUploadInfoResponse.SocialUploadInfo;
import ru.yandex.misc.io.cl.ClassLoaderUtils;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.TestBase;

/**
 * @author akirakozov
 */
public class SocialProxyResponseParserTest extends TestBase {

    @Test
    public void parseUploadRequestResponse() {
        SocialUploadInfo uploadInfo = new SocialUploadInfo(
                "https://graph.facebook.com/1921105717126/photos?access_token=asdf",
                "source",
                Cf.map("message", "photo_name"));
        SocialTaskInfo taskInfo = new SocialTaskInfo(
                Option.of(11845650L), Option.of("facebook"), 0.006,
                State.SUCCESS, Option.empty());

        String response = ClassLoaderUtils.loadText(SocialProxyResponseParser.class, "uploadRequestResponse.js");

        Assert.equals(
                new SocialUploadInfoResponse(taskInfo, Option.of(uploadInfo)),
                new SocialProxyResponseParser().parseUploadInfoResponse(response));
    }

    @Test
    public void parseUploadRequestFailedResponse() {
        FailReason reason = new FailReason(
                "invalid_parameters",
                "Some passed parameters are missing or invalid",
                "`aid` is a required parameter",
                "external");

        SocialTaskInfo taskInfo = new SocialTaskInfo(
                Option.empty(), Option.empty(), 0.006,
                State.FAILURE, Option.of(reason));

        String response = ClassLoaderUtils.loadText(SocialProxyResponseParser.class, "uploadRequestFailedResponse.js");
        new SocialProxyResponseParser().parseUploadInfoResponse(response);

        Assert.equals(
                new SocialUploadInfoResponse(taskInfo, Option.empty()),
                new SocialProxyResponseParser().parseUploadInfoResponse(response));
    }

    @Test
    public void parseUploadCommitResponse() {
        SocialPhotoIdResult result = new SocialPhotoIdResult("547890607710");
        SocialTaskInfo taskInfo = new SocialTaskInfo(
                Option.of(11845312L), Option.of("odnoklassniki"), 0.047,
                State.SUCCESS, Option.empty());

        String response = ClassLoaderUtils.loadText(SocialProxyResponseParser.class, "uploadCommitResponse.js");

        Assert.equals(
                new SocialUploadCommitResultResponse(taskInfo, Option.of(result)),
                new SocialProxyResponseParser().parseUploadCommitResponse(response));
    }

}
