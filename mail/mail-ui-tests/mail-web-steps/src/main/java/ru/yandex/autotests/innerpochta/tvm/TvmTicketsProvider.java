package ru.yandex.autotests.innerpochta.tvm;

import io.restassured.http.Cookies;
import io.restassured.response.Response;
import ru.yandex.autotests.passport.api.core.api.oauth.data.OAuthToken;
import ru.yandex.autotests.passport.api.core.cookie.SessionIdCookie;
import ru.yandex.autotests.passport.api.core.cookie.YandexCookies;
import ru.yandex.autotests.passport.api.core.objects.UserWithProps;
import ru.yandex.autotests.passport.api.core.tvm.TvmClientsSet;
import ru.yandex.autotests.passport.api.core.tvm.TvmLibraryLoader;
import ru.yandex.library.ticket_parser2.ServiceContext;

import java.time.Instant;
import java.util.function.Function;

import static ru.yandex.autotests.passport.api.common.Properties.props;
import static ru.yandex.autotests.innerpochta.util.props.TvmProperties.tvmProps;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.validatedWith;
import static ru.yandex.autotests.passport.api.core.api.JSONResponses.shouldBeJS200WithoutErrorsAndWarnings;
import static ru.yandex.autotests.passport.api.core.api.PassportApis.apiBlackbox;
import static ru.yandex.autotests.passport.api.core.api.PassportApis.apiTVM2;
import static ru.yandex.autotests.passport.api.core.tvm.TvmClients.getClientsForEnv;
import static ru.yandex.testpers.passport.tvm.api.ticket.ApiTicket.GrantTypeParam.CLIENT_CREDENTIALS;

/**
 * @author gladnik (Nikolai Gladkov)
 */
public class TvmTicketsProvider {

    private static final ThreadLocal<String> blackboxTicketManager = new ThreadLocal<>();
    private static final ThreadLocal<String> passportApiTicketManager = new ThreadLocal<>();
    private static final ThreadLocal<String> oauthApiTicketManager = new ThreadLocal<>();

    private TvmTicketsProvider(int tvmClientId, String tvmClientSecret) {
        ServiceContext serviceContext = new ServiceContext(tvmClientSecret, tvmClientId);
        TvmClientsSet dstClients = getClientsForEnv(props().getPassportEnv());
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String dst = dstClients.toString();
        String requestSignature = serviceContext.signCgiParamsForTvm(ts, dst);

        Response response = apiTVM2()
                .ticket()
                .withGrantType(CLIENT_CREDENTIALS.value())
                .withSrc(String.valueOf(tvmClientId))
                .withDst(dst)
                .withTs(ts)
                .withSign(requestSignature)
                .post(Function.identity());
        blackboxTicketManager.set(response.jsonPath().getString(
                String.format("%s.%s", dstClients.getBlackboxClient().getTvmClientId(), "ticket")
        ));
        passportApiTicketManager.set(response.jsonPath().getString(
                String.format("%s.%s", dstClients.getPassportClient().getTvmClientId(), "ticket")
        ));
        oauthApiTicketManager.set(response.jsonPath().getString(
                String.format("%s.%s", dstClients.getOauthClient().getTvmClientId(), "ticket")
        ));
    }

    private static TvmTicketsProvider ticketsProvider;

    public static TvmTicketsProvider ticketsProvider() {
        if (ticketsProvider == null) {
            synchronized (TvmTicketsProvider.class) {
                if (ticketsProvider == null) {
                    if (props().useBundledTvmLibrary()) {
                        TvmLibraryLoader.loadFromJar();
                    }
                    ticketsProvider = new TvmTicketsProvider(tvmProps().getTvmClientId(), tvmProps().getTvmClientSecret());
                }
            }
        }
        return ticketsProvider;
    }

    public TvmTicketsProvider reset() {
        ticketsProvider = new TvmTicketsProvider(tvmProps().getTvmClientId(), tvmProps().getTvmClientSecret());
        return this;
    }

    public String getBlackboxTicket() {
        if (blackboxTicketManager.get() == null) {
            reset();
        }
        return blackboxTicketManager.get();
    }

    public String getPassportApiTicket() {
        if (passportApiTicketManager.get() == null) {
            reset();
        }
        return passportApiTicketManager.get();
    }

    public String getOauthApiTicket() {
        if (oauthApiTicketManager.get() == null) {
            reset();
        }
        return oauthApiTicketManager.get();
    }

    public String getUserTicketWithPassword(UserWithProps user) {
        return apiBlackbox()
                .login()
                .withDefaults()
                .withXYaServiceTicketHeader(getBlackboxTicket())
                .withGetUserTicket("yes")
                .withLogin(user.getLogin())
                .withPassword(user.getAuthPasswd())
                .post(validatedWith(shouldBeJS200WithoutErrorsAndWarnings())).jsonPath().getString("user_ticket");
    }

    public String getUserTicket(OAuthToken token) {
        return apiBlackbox()
                .oauth()
                .withDefaults()
                .withXYaServiceTicketHeader(getBlackboxTicket())
                .withGetUserTicket("yes")
                .withOauthToken(token.getAccessToken())
                .post(validatedWith(shouldBeJS200WithoutErrorsAndWarnings())).jsonPath().getString("user_ticket");
    }

    public String getUserTicket(Cookies cookies) {
        SessionIdCookie sessionIdCookie = YandexCookies.sessionIdFrom(cookies);
        return apiBlackbox()
                .sessionid()
                .withDefaults()
                .withXYaServiceTicketHeader(getBlackboxTicket())
                .withGetUserTicket("yes")
                .withHost(sessionIdCookie.getDomain().substring(1))  // substring drops heading dot character
                .withSessionid(sessionIdCookie.getValue())
                .post(validatedWith(shouldBeJS200WithoutErrorsAndWarnings())).jsonPath().getString("user_ticket");
    }

}
