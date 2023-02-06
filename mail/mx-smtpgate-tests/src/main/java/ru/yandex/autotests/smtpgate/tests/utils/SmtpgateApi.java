package ru.yandex.autotests.smtpgate.tests.utils;


import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.response.Response;
import ru.yandex.autotests.innerpochta.RestAssuredLoggingFilter;

import static com.jayway.restassured.RestAssured.expect;
import static ru.yandex.autotests.smtpgate.tests.utils.SmtpgateProperties.smtpgateProps;

/**
 * User: alex89
 * Date: 03.02.2016
 * localhost:2000/append?src_email=yapoptest@yandex.ru
 * &fid=1&user_flags=Foo&user_flags=Bar&system_flags=Seen&date=1454436917
 */
public class SmtpgateApi {
    private RequestSpecBuilder requestSpecBuilder;
    //Общие парметры в теле json
    private String bodyOfLetter = "";

    private SmtpgateApi() {
        requestSpecBuilder = new RequestSpecBuilder().setBaseUri(smtpgateProps().getUrl());
        requestSpecBuilder.addFilter(RestAssuredLoggingFilter.log().common(true));
    }

    public static SmtpgateApi smtpgateApi() {
        return new SmtpgateApi();
    }

    public SmtpgateApi letter(String letter) {
        this.bodyOfLetter = letter;
        return this;
    }

    public RequestSpecBuilder builder() {
        return requestSpecBuilder;
    }

    public SmtpgateApi queryParam(String key, String value) {
        builder().addQueryParam(key, value);
        return this;
    }

    public SmtpgateApi srcEmail(String srcEmail) {
        return queryParam("src_email", srcEmail);
    }



    public Response append() {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/append");
    }

    public Response appendWithoutBody() {
        return expect().when().given().spec(builder().build())
                .post("/append");
    }

    public SmtpgateApi email(String email) {
        return queryParam("email", email);
    }

    public Response collect(String uid) {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/collect/" + uid);
    }


    //curl -X POST -v --upload-file mspam.txt
    // 'http://mxback-qa.cmail.yandex.net:2000/check_spam?from=yantester@yandex.ru&to=strongspamtest@yandex.ru
    // &subject=test&request_id=NlPDTZjq7Z-nGYWtfng&client_ip=93.158.191.22&so_type=in'

    public SmtpgateApi from(String value) {
        return queryParam("from", value);
    }

    public SmtpgateApi to(String value) {
        return queryParam("to", value);
    }

    public SmtpgateApi subject(String value) {
        return queryParam("subject", value);
    }

    public SmtpgateApi requestId(String value) {
        return queryParam("request_id", value);
    }

    public SmtpgateApi clientIp(String value) {
        return queryParam("client_ip", value);
    }

    public SmtpgateApi soType(String value) {
        return queryParam("so_type", value);
    }

    public SmtpgateApi source(String value) {
        return queryParam("source", value);
    }

    public SmtpgateApi karma(String value) {
        return queryParam("karma", value);
    }

    public SmtpgateApi karma_status(String value) {
        return queryParam("karma_status", value);
    }

    public SmtpgateApi uid(String value) {
        return queryParam("uid", value);
    }

    public SmtpgateApi addHeaders(String value) {
        return queryParam("add_headers", value);
    }

    public SmtpgateApi externalImapId(String value) {
        return queryParam("external_imap_id", value);
    }
    public SmtpgateApi forMailish() {
        return queryParam("for_mailish", "1");
    }

    public Response checkSpam() {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/check_spam");
    }

    public Response send() {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/send_mail");
    }

    public Response store() {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/store");
    }

    public Response save() {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/save");}

    public Response sendSystemMail() {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/send_system_mail");}

    public Response sendMail() {
        return expect().when().given().spec(builder().build())
                .body(bodyOfLetter.getBytes())
                .post("/send_mail");}


}
