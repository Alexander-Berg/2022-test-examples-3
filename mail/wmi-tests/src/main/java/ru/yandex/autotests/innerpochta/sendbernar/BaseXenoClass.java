package ru.yandex.autotests.innerpochta.sendbernar;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.specification.ResponseSpecification;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.runners.model.MultipleFailureException;
import ru.yandex.autotests.innerpochta.beans.sendbernar.WriteAttachmentResponse;
import ru.yandex.autotests.innerpochta.wmi.core.consts.Scopes;
import ru.yandex.autotests.innerpochta.wmi.core.exceptions.RetryException;
import ru.yandex.autotests.innerpochta.wmi.core.oper.akita.AkitaAuth;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.composedraft.ApiComposeDraft;
import ru.yandex.autotests.innerpochta.wmi.core.sendbernar.composemessage.ApiComposeMessage;
import ru.yandex.autotests.lib.junit.rules.retry.RetryRule;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.apiSendbernar;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.WriteAllureParamsRule.writeParamsForAllure;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.XRequestIdRule.xRequestIdRule;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.downloadFile;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;


public class BaseXenoClass {
    private String caller = "xeno";

    private static HttpClientManagerRule authClient = auth().with("XenoHelers").login();

    @Rule
    public WriteAllureParamsRule writeAllureParamsRule = writeParamsForAllure();

    @Rule
    public XRequestIdRule setXRequestId = xRequestIdRule();

    @Rule
    public RetryRule retryRule = RetryRule.retry().ifException(RetryException.class)
            .or()
            .ifException(MultipleFailureException.class)
            .or()
            .ifException(AssertionError.class)
            .every(1, TimeUnit.SECONDS).times(1);

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

    String uploadedId() {
        byte[] content;

        try {
            content = FileUtils.readFileToByteArray(downloadFile(BaseSendbernarClass.IMG_URL_JPG, getRandomString(), authClient.authHC()));
        } catch (IOException ex) {
            return null;
        }

        return apiSendbernar(getUserTicket()).writeAttachment()
                .withUid(authClient.account().uid())
                .withCaller(caller)
                .withFilename("not_rotate.jpg")
                .withReq((req) -> req.setBody(content))
                .post(shouldBe(ok200()))
                .as(WriteAttachmentResponse.class)
                .getId();
    }

    // yndx.xeno@mail.ru - simple123456
    public String uid() throws Exception {
        Scopes scope = props().testingScope();
        if (scope.equals(Scopes.PRODUCTION)) {
            return "647137773";
        } else if (scope.equals(Scopes.TESTING)) {
            return "4012523648";
        } else {
            throw new Exception("no xeno account for scope "+scope.toString());
        }
    }

    private String ticket = null;

    private String getUserTicket() {
        if (ticket == null) {
            ticket = new AkitaAuth(authClient.authHC()).userTicket();
        }

        return ticket;
    }

    ApiComposeDraft composeDraft() throws Exception {
        return apiSendbernar(getUserTicket())
                .composeDraft()
                .withUid(uid())
                .withCaller(caller);
    }

    ApiComposeMessage composeMessage() throws Exception {
        return apiSendbernar(getUserTicket())
                .composeMessage()
                .withUid(uid())
                .withCaller(caller);
    }
}
