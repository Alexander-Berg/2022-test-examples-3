package ru.yandex.autotests.innerpochta.api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;

import java.io.File;

import static io.restassured.RestAssured.given;
import static ru.yandex.autotests.innerpochta.api.RestAssuredLoggingFilter.log;
import static ru.yandex.autotests.innerpochta.util.handlers.SendJsonConstants.UPLOAD_ATTACH_ATTACHMENTS;
import static ru.yandex.autotests.innerpochta.util.props.ApiProps.apiProps;

public class UploadAttachmentHandler {

    private RequestSpecBuilder reqSpecBuilder;

    private UploadAttachmentHandler() {
        reqSpecBuilder = new RequestSpecBuilder();
    }

    public static UploadAttachmentHandler uploadAttachmentHandler() {
        return new UploadAttachmentHandler();
    }

    public UploadAttachmentHandler withAuth(RestAssuredAuthRule auth) {
        reqSpecBuilder.addRequestSpecification(auth.getAuthSpec());
        return this;
    }

    public UploadAttachmentHandler withFile(File file) {
        this.reqSpecBuilder.addMultiPart(UPLOAD_ATTACH_ATTACHMENTS, file);
        return this;
    }

    public Response callUploadAttachment() {
        return given().spec(reqSpecBuilder.build())
            .filter(log())
            .basePath(apiProps().uploadUrl())
            .post();
    }
}
