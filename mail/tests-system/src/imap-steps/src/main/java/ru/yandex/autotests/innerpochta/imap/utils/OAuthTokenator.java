package ru.yandex.autotests.innerpochta.imap.utils;

import java.io.IOException;
import java.util.Base64;

import static com.jayway.restassured.RestAssured.given;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * User: lanwen
 * Date: 18.04.14
 * Time: 19:11
 */
public class OAuthTokenator {

//    public static final String DEFAULT_ALLOW_ALL_CLIENT_ID = "a11f21f2c09f4c409cd2ba8b810dc651";
//    public static final String DEFAULT_ALLOW_ALL_CLIENT_SECRET = "1c9f75b487d9436eb1d77f98795df0e3";

    //IMAPtokenTest привязан к юзеру AuthenticateCommonTest
    public static final String DEFAULT_ALLOW_ALL_CLIENT_ID = "0a182e6526d04c00a3286ece6cd7fc72";
    public static final String DEFAULT_ALLOW_ALL_CLIENT_SECRET = "8c5212cfafbd4c749a389586008105c2";

    public static final String OAUTH_URI = "https://oauth-test.yandex.ru";

    private String clientId = DEFAULT_ALLOW_ALL_CLIENT_ID;
    private String clientSecret = DEFAULT_ALLOW_ALL_CLIENT_SECRET;


    private String login;
    private String password;


    private String cachedToken;


    private OAuthTokenator(String login, String pwd) {
        this.login = login;
        this.password = pwd;
    }

    public static OAuthTokenator oauthFor(String login, String pwd) {
        return new OAuthTokenator(login, pwd);
    }

    public static String verify(String token) {
        return given().baseUri(OAUTH_URI).queryParam("access_token", token).get("/verify_token").asString();
    }

    /**
     * MPROTO-292
     * https://developers.google.com/gmail/xoauth2_protocol
     * base64("user=" {User} "^Aauth=Bearer " {Access Token} "^A^A")
     * <p>
     * выполняет команду вида:
     * perl -MMIME::Base64 -E 'say encode_base64("user=parshikov.oauth2\@yandex.ru\cAauth=Bearer
     * 83dee240ae7d4dd781d930003e1f4918\cA\cA", "")'
     *
     * @param login
     * @param pwd
     * @return строчку следующего вида
     * dXNlcj1sb2ctaW5pbWFwdGVzdERjQHlhbmRleC5ydQFhdXRoPUJlYXJlciBjYzdiYTNmNjBmYWQ0ZDQwODY0OWFjODhmZDc5OGEwNQEB
     * @throws IOException
     */
    public static String getXOAuth2(String login, String pwd) throws IOException {
        return getXOAuth2(login, "", pwd);
    }

    public static String getXOAuth2(String login, String domain, String pwd) throws IOException {
        String token = oauthFor(login, pwd).token();
        if (!domain.isEmpty()) {
            domain = domain + "\\";
        }

        String tokenStr = String.format("user=%s%s\u0001auth=Bearer %s\u0001\u0001", login, domain, token);
        byte[] encoded = Base64.getEncoder().encode(tokenStr.getBytes());

        return new String(encoded);
    }

    public OAuthTokenator clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public OAuthTokenator clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public String token() {
        if (isEmpty(cachedToken)) {
            cachedToken = given()
                    .baseUri(OAUTH_URI)
                    .param("grant_type", "password")
                    .param("username", login)
                    .param("password", password)
                    .param("client_id", clientId)
                    .param("client_secret", clientSecret)
                    .post("/token").then().extract().jsonPath().getString("access_token");
        }
        return cachedToken;
    }

    public String verify() {
        return verify(token());
    }
}
