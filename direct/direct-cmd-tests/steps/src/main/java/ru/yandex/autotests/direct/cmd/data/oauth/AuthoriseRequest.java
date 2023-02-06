package ru.yandex.autotests.direct.cmd.data.oauth;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

public class AuthoriseRequest extends BasicDirectRequest {

    @SerializeKey("csrf")
    private String csrf;
    @SerializeKey("request_id")
    private String requestId;
    @SerializeKey("granted_scopes")
    private String grantedScopes;
    @SerializeKey("response_type")
    private String responseType;
    @SerializeKey("client_id")
    private String clientId;
    @SerializeKey("code")
    private String code;
    @SerializeKey("force_confirm")
    private String forceConfirm;
    @SerializeKey("redirect_uri")
    private String redirectUri;


    public String getForceConfirm() {
        return forceConfirm;
    }

    public AuthoriseRequest withForceConfirm(String forceConfirm) {
        this.forceConfirm = forceConfirm;
        return this;
    }

    public String getCsrf() {
        return csrf;
    }

    public String getRequestId() {
        return requestId;
    }

    public AuthoriseRequest withRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    public String getGrantedScopes() {
        return grantedScopes;
    }

    public AuthoriseRequest withGrantedScopes(String grantedScopes) {
        this.grantedScopes = grantedScopes;
        return this;
    }

    public String getResponseType() {
        return responseType;
    }

    public AuthoriseRequest withResponseType(String responseType) {
        this.responseType = responseType;
        return this;
    }

    public String getClientId() {
        return clientId;
    }

    public AuthoriseRequest withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getCode() {
        return code;
    }

    public AuthoriseRequest withCode(String code) {
        this.code = code;
        return this;
    }

    public AuthoriseRequest withCsrf(String csrf) {
        this.csrf = csrf;
        return this;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public AuthoriseRequest withRedirectUri(String redirect_uri) {
        this.redirectUri = redirect_uri;
        return this;
    }
}
