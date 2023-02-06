package ru.yandex.autotests.innerpochta.utils.rules;

import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.*;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.browsermob.proxy.util.TrustEverythingSSLTrustManager;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.oauth.OAuthRequestInterceptor;
import ru.yandex.autotests.innerpochta.wmi.core.oper.akita.AkitaAuth;
import ru.yandex.autotests.innerpochta.wmi.core.oper.akita.UidAndTvmTicket;
import ru.yandex.autotests.lib.junit.rules.passport.Passport;
import ru.yandex.autotests.lib.junit.rules.passport.PassportRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.UpdateHCFieldRule;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static ch.lambdaj.Lambda.*;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.utils.SettingsProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.CookieMatcher.сookieExist;


public class HttpClientManagerRule extends TestWatcher {


    public static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru; rv:1.9.0.19) " +
            "Gecko/2010031422 Firefox/3.0.19 (.NET CLR 3.5.30729) YB/5.1.1, WMI-TESTS";
    /**
     * Хранилище кук.
     * Необходимо для того чтобы залогиниться 1 раз,
     * а дальше просто использовать куки
     */
    private CookieStore cookieStore;

    /**
     * Получаем токен
     */
    private HttpRequestInterceptor interc;

    /**
     * Токен, нужен для логирования
     */
    private String token;

    /**
     * Текущий аккаунт
     */
    private Account account = null;

    /**
     * Логгер
     */
    private Logger logger = Logger.getLogger(HttpClientManagerRule.class);

    private UidAndTvmTicket auth_ = null;
    private long authTime_ = 0;

    public static String xOriginalHost() {
        return "mail.yandex.ru";
    }

    public UidAndTvmTicket account() {
        long now = System.currentTimeMillis();
        long threeMinutesInMsecs = 180000;
        if (auth_ == null || now - authTime_ > threeMinutesInMsecs) {
            auth_ = new AkitaAuth(authHC(), props().akitaUri(), xOriginalHost());
        }
        authTime_ = System.currentTimeMillis();
        return auth_;
    }

    /**
     * Паспортный хост
     */
    private URI passportHost = props().passportHost();

    private HttpClientManagerRule() {
        cookieStore = new BasicCookieStore();
    }

    public static HttpClientManagerRule auth() {
        return new HttpClientManagerRule();
    }

    public HttpClientManagerRule onHost(String passportHost) {
        this.passportHost = URI.create(passportHost);
        return this;
    }

    @Override
    protected void starting(Description description) {
        if (description.getTestClass().isAnnotationPresent(Credentials.class)) {
            Credentials credentials = description.getTestClass().getAnnotation(Credentials.class);
            if (!isEmpty(credentials.login())) {
                with(credentials.login(), credentials.pwd());
            }
            login();
        }
    }

    public HttpClientManagerRule with(String login, String password) {
        account = new Account(login, password).domain("yandex-team.ru");
        return this;
    }

    public DefaultHttpClient oAuth() {
        DefaultHttpClient hcWithOAuth = notAuthHC();
        try {
            token = OAuthRequestInterceptor.getToken(account);
            interc = OAuthRequestInterceptor.getOAuthInterceptor(token);
        } catch (Exception e) {
            //в тех тестах, где oauth не нужен, невозможность его получить неважна
            logger.warn(format("Не удалось получить oauth токен (%s)", e.getMessage()));
        }
        hcWithOAuth.addRequestInterceptor(interc);
        return hcWithOAuth;
    }

    public DefaultHttpClient requiredOAuth() throws Exception {
        DefaultHttpClient hcWithOAuth = notAuthHC();
        token = OAuthRequestInterceptor.getToken(account);
        interc = OAuthRequestInterceptor.getOAuthInterceptor(token);
        hcWithOAuth.addRequestInterceptor(interc);
        return hcWithOAuth;
    }

    public String getToken() {
        return token;
    }

    public DefaultHttpClient oAuth(String token) {
        DefaultHttpClient hcWithOAuth = notAuthHC();
        hcWithOAuth.addRequestInterceptor(new OAuthRequestInterceptor(token));
        return hcWithOAuth;
    }

    public void login(String login, String passw, Account account, DefaultHttpClient hc) {
        try {
            new PassportRule(hc).onHost(passportHost.toURL()).withCredentials(account).login();
        } catch (MalformedURLException e) {
            throw new RuntimeException("", e);
        }
    }

    public HttpClientManagerRule login() {
        if (account == null) {
            throw new RuntimeException("Account not set");
        }
        DefaultHttpClient hc = new DefaultHttpClient(getConnManager());
        CookieSpecFactory csf = getEasyCookieSpecFactory();
        hc.getCookieSpecs().register("easy", csf);
        hc.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
        login(account.getLogin(), account.getPassword(), account, hc);
        cookieStore = hc.getCookieStore();
        assumeThat(cookieStore, allOf(сookieExist(UpdateHCFieldRule.COOKIE_NAME),
                сookieExist(UpdateHCFieldRule.SSL_COOKIE_NAME)));
        return this;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    private CookieSpecFactory getEasyCookieSpecFactory() {
        return new CookieSpecFactory() {
            public CookieSpec newInstance(HttpParams params) {
                return new BrowserCompatSpec() {
                    @Override
                    public boolean match(Cookie cookie, CookieOrigin origin) {
                        return true;
                    }

                    @Override
                    public void validate(Cookie cookie, CookieOrigin origin)
                            throws MalformedCookieException {
                        // Oh, I am easy
                    }
                };
            }
        };
    }

    /**
     * Метод, возвращающий новый httpClient с уже проставленными куками и поддержкой ссш
     *
     * @return DefaultHttpClient    - клиент
     */
    public DefaultHttpClient authHC() {
        try {
            logger.debug(format("Получаем новый авторизованный экземпляр HttpClient для %s " +
                            "[[a class='btn btn-primary btn-mini' href=%s]]LOGIN[[/a]]",
                    account, Passport.login(account, passportHost.toURL(), props().settingsUri().getHost()).rawUrl()
            ));
        } catch (MalformedURLException e) {
            throw new RuntimeException("", e);
        }

        DefaultHttpClient hc = new DefaultHttpClient(getConnManager());
        CookieSpecFactory csf = getEasyCookieSpecFactory();
        hc.getCookieSpecs().register("easy", csf);
        hc.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
        hc.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, (int) TimeUnit.SECONDS.toMillis(30));
        hc.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, (int) TimeUnit.SECONDS.toMillis(30));

        hc.setCookieStore(cookieStore);
        hc.getParams().setParameter(HttpClientParams.USER_AGENT, TEST_USER_AGENT);
        return hc;
    }

    /**
     * Метод, возвращающий новый httpClient с поддержкой ссш, но без кук!
     *
     * @return DefaultHttpClient    - клиент
     */
    public DefaultHttpClient notAuthHC() {

        DefaultHttpClient hc = new DefaultHttpClient(getConnManager());
        hc.getParams().setParameter(HttpClientParams.USER_AGENT, TEST_USER_AGENT);

        return hc;
    }


    /**
     * Достает из аккаунт менеджера текущий акк
     *
     * @return Account - текущий проинициализированный аккаунт
     */
    public Account acc() {
        return account;
    }
    
    public String cookie(String name) {
        Cookie cookie = selectFirst(cookieStore.getCookies(), having(on(Cookie.class).getName(), equalTo(name)));
        return defaultIfEmpty(notNull(cookie, "Запрашиваемой куки %s нет среди кук", name).getValue(), "");
    }

    //------------------------------------------------------------------------------------------------

    /**
     * Настраивает схемы подключения. Для случая с https назначает верифер,
     * который игнорирует сертификаты
     * Для http использует плейнсокет
     *
     * @return Потокобезопасный менеджер подключений
     */
    public static ClientConnectionManager getConnManager() {
        // Если ходим по ssh, то назначаем новый верифер, принимающий любые сертификаты
        // Иначе, просто открываем плейнсокет

        // Сложная хрень, призванная игнорировать все связанное с сертификатами
        try {
            SSLContext ctx = SSLContext.getInstance(SSLSocketFactory.TLS);
            X509TrustManager tm = new TrustEverythingSSLTrustManager();

            ctx.init(null, new TrustManager[]{tm}, null);

            SSLSocketFactory socketFactory = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = new SchemeRegistry();

            registry.register(new Scheme("https", 443, socketFactory));
            registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
            return new PoolingClientConnectionManager(registry);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }//-------------------------------------------------------------------------------------
}
