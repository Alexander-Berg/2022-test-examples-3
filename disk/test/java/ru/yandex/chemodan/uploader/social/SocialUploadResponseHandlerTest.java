package ru.yandex.chemodan.uploader.social;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author akirakozov
 */
public class SocialUploadResponseHandlerTest {

    @Test
    public void parseEmptyResponse() {
        SocialUploadResponseHandler handler = new SocialUploadResponseHandler();
        Assert.isFalse(handler.isFacebookTokenError(""));
    }

    @Test
    public void parseNonJsonResponse() {
        SocialUploadResponseHandler handler = new SocialUploadResponseHandler();
        Assert.isFalse(handler.isFacebookTokenError("Error ocurred"));
    }

    @Test
    public void parseOAuthExceptionResponse() {
        SocialUploadResponseHandler handler = new SocialUploadResponseHandler();
        String response =
            "{\"error\": { \"message\": \"Error validating access token...\","
                + "\"type\": \"OAuthException\", \"code\": 190,\"error_subcode\": 460 } }";
        Assert.isTrue(handler.isFacebookTokenError(response));
    }
}
