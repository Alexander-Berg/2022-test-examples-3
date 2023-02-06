package ru.yandex.autotests.innerpochta.rules;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Cookies;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.rules.ExternalResource;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccountManager;
import ru.yandex.autotests.innerpochta.steps.beans.account.AccountInformation;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.autotests.lib.junit.rules.login.Credentials;
import ru.yandex.autotests.passport.api.common.data.YandexDomain;

import static org.apache.commons.lang3.Validate.notNull;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.accountInformationHandler;
import static ru.yandex.autotests.innerpochta.api.AccountInformationHandler.getAccInfo;
import static ru.yandex.autotests.innerpochta.util.MailConst.CORP_URL_PART;
import static ru.yandex.autotests.innerpochta.util.Utils.getYaUidCookie;
import static ru.yandex.autotests.innerpochta.util.handlers.AccountInfoConstants.ACCINFO_PARAM_CKEY;
import static ru.yandex.autotests.innerpochta.util.handlers.AccountInfoConstants.ACCINFO_PARAM_UID;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;

public class RestAssuredAuthRule extends ExternalResource implements Filter {

    private AccountInformation accInfo;
    private Credentials acc;
    private AccLockRule lock;
    private Cookies cookies;

    public RestAssuredAuthRule(AccLockRule lock) {
        this.lock = lock;
    }

    public RestAssuredAuthRule(Account acc) {
        this.acc = acc;
    }

    public static RestAssuredAuthRule auth(AccLockRule lock) {
        return new RestAssuredAuthRule(lock);
    }

    public static RestAssuredAuthRule auth(Account acc) {
        return new RestAssuredAuthRule(acc);
    }

    @Override
    public Response filter(FilterableRequestSpecification requestScpec, FilterableResponseSpecification responseSpec,
                           FilterContext filterCnxt) {
        notNull(cookies, "Не было попытки авторизации для вызова фильтра");
        requestScpec.cookies(cookies)
            .cookie(getYaUidCookie());
        return filterCnxt.next(requestScpec, responseSpec);
    }

    @Override
    protected void before() {
        if (lock != null)
            withAcc(lock.firstAcc());
        if (UrlProps.urlProps().getBaseUri().contains(CORP_URL_PART))
            loginToCorp();
        else
            login();
    }

    public RestAssuredAuthRule login() {
        cookies = AccountManager.getInstance().getAuthCookies(
            acc.getLogin(),
            acc.getPassword(),
            YandexDomain.RU
        );
        return this;
    }

    private RestAssuredAuthRule loginToCorp() {
        cookies = AccountManager.getInstance().getCorpAuthCookies(acc.getLogin(), acc.getPassword());
        return this;
    }

    public RestAssuredAuthRule withAcc(Account acc) {
        this.acc = acc;
        /*
        если проставляем другой аккаунт, хотим получать другой accInfo
        TODO сделать мапу
        */
        accInfo = null;
        return this;
    }

    public RequestSpecification getAuthSpec() {
        if (accInfo == null) {
            accInfo = getAccInfo(accountInformationHandler().withAuth(this).callAccountInformation());
        }
        return
            new RequestSpecBuilder()
                .setBaseUri(urlProps().getBaseUri().toString()).setRelaxedHTTPSValidation()
                .addFilter(this)
                .addParam(ACCINFO_PARAM_CKEY, accInfo.getCkey())
                .addParam(ACCINFO_PARAM_UID, accInfo.getUid())
                .build();
    }

    public RequestSpecification getAuthSpecWithQueryParam() {
        if (accInfo == null) {
            accInfo = getAccInfo(accountInformationHandler().withAuth(this).callAccountInformation());
        }
        return
            new RequestSpecBuilder()
                .setBaseUri(urlProps().getBaseUri().toString()).setRelaxedHTTPSValidation()
                .addFilter(this)
                .addQueryParam(ACCINFO_PARAM_UID, accInfo.getUid())
                .build();
    }

    public String getCkey() {
        if (accInfo == null) {
            accInfo = getAccInfo(accountInformationHandler().withAuth(this).callAccountInformation());
        }
        return accInfo.getCkey();
    }

    public String getLogin() {
        return acc.getLogin();
    }

    public String getPassword() {
        return acc.getPassword();
    }

    public Cookies getCookies() {
        return cookies;
    }
}
