package ru.yandex.mail.tests.sendbernar;


import com.google.gson.Gson;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Rule;
import ru.yandex.mail.common.api.RequestTraits;
import ru.yandex.mail.common.credentials.AccountWithScope;
import ru.yandex.mail.common.credentials.UserCredentials;
import ru.yandex.mail.common.rules.RetryRule;
import ru.yandex.mail.common.rules.WriteAllureParamsRule;
import ru.yandex.mail.common.rules.XRequestIdRule;
import ru.yandex.mail.common.utils.Random;
import ru.yandex.mail.tests.sendbernar.generated.ApiSendbernar;
import ru.yandex.mail.tests.sendbernar.generated.PartsJson;
import ru.yandex.mail.tests.sendbernar.generated.SaveDraftResponse;
import ru.yandex.mail.tests.sendbernar.generated.WriteAttachmentResponse;
import ru.yandex.mail.tests.sendbernar.generated.composedraft.ApiComposeDraft;
import ru.yandex.mail.tests.sendbernar.generated.composemessage.ApiComposeMessage;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasKey;
import static ru.yandex.mail.common.api.CommonApiSettings.shouldBe;
import static ru.yandex.mail.common.properties.CoreProperties.props;
import static ru.yandex.mail.common.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.mail.common.rules.XRequestIdRule.xRequestIdRule;
import static ru.yandex.mail.common.utils.Files.downloadFile;
import static ru.yandex.mail.common.utils.Files.downloadFileWithNonrandomFilename;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.attachOk200;
import static ru.yandex.mail.tests.sendbernar.SendbernarResponses.ok200;


@Features("SENDBERNAR")
@Stories("Xeno")
public abstract class BaseXenoClass {
    private String caller = "xeno";

    abstract AccountWithScope mainUser();

    UserCredentials authClient = new UserCredentials(mainUser());

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure(SendbernarProperties.properties().sendbernarUri(), props().scope());

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry()
            .ifException(Exception.class)
            .every(1, TimeUnit.SECONDS)
            .times(1);

    static ResponseSpecification okDraftComposed() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("from", not(Matchers.empty()))
                .expectBody("text", not(Matchers.empty()))
                .expectBody(".", hasKey("attachments"))
                .build();
    }

    static ResponseSpecification okMessageComposed() {
        return new ResponseSpecBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .expectBody("from", not(Matchers.empty()))
                .expectBody("text", not(Matchers.empty()))
                .expectBody("to", not(Matchers.empty()))
                .expectBody(".", hasKey("bcc"))
                .expectBody(".", hasKey("cc"))
                .build();
    }

    static ResponseSpecification hasRecipient(String name, String val) {
        return new ResponseSpecBuilder()
                .expectBody(name, equalTo(val))
                .build();
    }

    static ResponseSpecification nonEmptyAttaches() {
        return new ResponseSpecBuilder()
                .expectBody("attachments", not(Matchers.empty()))
                .build();
    }

    static ResponseSpecification hasMessageId(String msgid) {
        return new ResponseSpecBuilder()
                .expectBody("text", containsString(msgid))
                .build();
    }

    protected ApiSendbernar apiSendbernar() {
        return SendbernarApi.apiSendbernar(new RequestTraits()
                .withUrl(SendbernarProperties.properties().sendbernarUri())
                .withXRequestId(props().getCurrentRequestId())
        );
    }

    String uploadedId() {
        return uploadedId(BaseSendbernarClass.IMG_URL_JPG);
    }

    String uploadedId(String path) {
        return uploadedId(path, false);
    }

    String uploadedIdStrictFilename(String path) {
        return uploadedId(path, true);
    }

    private String uploadedId(String path, boolean strictFilename) {
        byte[] content;

        try {
            content = FileUtils.readFileToByteArray(
                    strictFilename
                            ? downloadFileWithNonrandomFilename(path, "name")
                            : downloadFile(path, "name")
            );
        } catch (IOException ex) {
            return null;
        }

        return apiSendbernar()
                .writeAttachment()
                .withUid(authClient.account().uid())
                .withCaller(caller)
                .withFilename("not_rotate.jpg")
                .withReq((req) -> req.setBody(content))
                .post(shouldBe(ok200()))
                .as(WriteAttachmentResponse.class)
                .getId();
    }

    String getPartsJson(File imageJpeg) throws Exception {
        byte[] content = FileUtils.readFileToByteArray(imageJpeg);

        String id = apiSendbernar().writeAttachment()
                .withUid(authClient.account().uid())
                .withCaller("test_qa")
                .withFilename(imageJpeg.getName())
                .withReq((req) -> req.setBody(content))
                .post(shouldBe(attachOk200()))
                .as(WriteAttachmentResponse.class)
                .getId();


        SaveDraftResponse resp = apiSendbernar().saveDraft()
                .withUid(authClient.account().uid())
                .withCaller("test_qa")
                .withTo(authClient.account().email())
                .withUploadedAttachStids(id)
                .withSubj(Random.string())
                .post(shouldBe(ok200()))
                .as(SaveDraftResponse.class);


        PartsJson p = new PartsJson()
                .withMid(Long.parseLong(resp.getStored().getMid()))
                .withHid(Double.parseDouble(resp.getAttachments().get(0).getHid()));


        return new Gson().toJson(p);
    }

    ApiComposeDraft composeDraft() throws Exception {
        return apiSendbernar()
                .composeDraft()
                .withUid(mainUser().get().uid())
                .withCaller(caller);
    }

    ApiComposeMessage composeMessage() throws Exception {
        return apiSendbernar()
                .composeMessage()
                .withUid(mainUser().get().uid())
                .withCaller(caller);
    }
}
