package ru.yandex.market.api.server.limits.blacklist;

import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import io.netty.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.MockRequestBuilder;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.antifraud.MobileValidationError;
import ru.yandex.market.api.internal.antifraud.MobileValidationService;
import ru.yandex.market.api.internal.geo.GeobaseService;
import ru.yandex.market.api.server.LifecycleStatus;
import ru.yandex.market.api.server.LifecycleStatusService;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.IpList;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.UuidUserValidator;
import ru.yandex.market.api.server.sec.ValidationResult;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.sec.exceptions.AccessDeniedException;
import ru.yandex.market.api.server.sec.exceptions.AuthInfoNotFoundException;
import ru.yandex.market.api.server.sec.oauth.annotation.AuthSecured;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.memcached.MockMemcachedClient;

/**
 * @author dimkarp93
 */
@WithMocks
@WithContext
public class ParserBlockerTest extends UnitTestBase {

    private static final IpList NOTHING_ACCEPTABLE = new IpList() {
        @Override
        public boolean contains(@NotNull String ip) {
            return false;
        }
    };

    private enum CheckStatus {
        OK(true, () -> true, false),
        FAIL(false, () -> false, true),
        ;

        public boolean isOk;
        public ValidationResult result;
        public boolean isApiSystem;

        CheckStatus(boolean isOk,
                    ValidationResult result,
                    boolean isApiSystem) {
            this.isOk = isOk;
            this.result = result;
            this.isApiSystem = isApiSystem;
        }
    }

    @Mock
    private GeobaseService geoBaseService;
    @Mock
    private UuidUserValidator uuidUserValidator;
    @Mock
    private BlackListSupplierCachingProxy blackListSupplier;
    @Mock
    private WhiteListSupplierCachingProxy whiteListSupplier;
    @Mock
    private ClientHelper clientHelper;
    @Mock
    private MobileValidationService mobileValidationService;

    private MockClientHelper mockClientHelper;

    private ParserBlocker parserBlocker;

    private final String whiteMobileUuid = "caafa9b29ac04f329e59ecbb9344b75";
    private final String blueMobileUuid = "53385cccaed311eab3de0242ac13000";
    private final String partnersOnlyBlue = "11,32";
    private final String partnersOnlyWhite = "22,332";
    private final boolean jswAntirobotEnabled = false;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ContextHolder.update(ctx -> ctx.setUserIp("1.2.3.4"));
        LifecycleStatusService.INSTANCE.setStatus(LifecycleStatus.RUNNING);
        mockClientHelper = new MockClientHelper(clientHelper);

        parserBlocker = new ParserBlocker(
                whiteMobileUuid,
                blueMobileUuid,
                partnersOnlyBlue,
                partnersOnlyWhite,
                jswAntirobotEnabled,
                geoBaseService,
                uuidUserValidator,
                blackListSupplier,
                whiteListSupplier,
                new MockMemcachedClient(),
                clientHelper,
                mobileValidationService);
    }

    @Override
    public void tearDown() throws Exception {
        LifecycleStatusService.INSTANCE.setStatus(LifecycleStatus.STARTING);
    }

    @Test
    public void passAllCheck() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier,
                whiteListSupplier
        );
        mobileValidation(Result.newResult(null));

        doTest();
    }

    @Test
    public void wrongPartnerUrl() {
        expectBlockedByPartnerWrongHost();
        doTest(x -> x.host("api.market.yandex.ru").clientId("11"));
    }

    @Test
    public void goodPartnerUrl() {
        doTest(x -> x.host("mobile.market.yandex.ru").clientId("11"));
        doTest(x -> x.host("mobile.prestable.vs.market.yandex.ru").clientId("11"));
    }

    @Test
    public void ignoreUuidCheckExternal() {
        client(Client.Type.EXTERNAL);
        uuidConfig(CheckStatus.FAIL);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier,
                whiteListSupplier
        );

        doTest();
    }

    @Test
    public void ignoreUuidCheckInternal() {
        client(Client.Type.INTERNAL);
        uuidConfig(CheckStatus.FAIL);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier,
                whiteListSupplier
        );

        doTest();
    }

    @Test
    public void blockedByUuidCheckMobile() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.FAIL);

        expectBlockedByIncorrectUuid();

        doTest();
    }

    @Test
    public void notBlockedByMobileValidator() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier,
                whiteListSupplier
        );
        mobileValidation(Result.newError(new MobileValidationError("incorrect")));

        doTest(request -> request
                .param("uuid", "correctUuid")
                .header("X-JWS", "zzz")
        );
    }

    @Test
    public void notBlockedByMobileValidatorError() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier,
                whiteListSupplier
        );
        mobileValidation(Futures.newFailedFuture(new TimeoutException()));

        doTest(request -> request
                .param("uuid", "correctUuid")
                .header("X-JWS", "zzz")
        );
    }

    @Test
    public void blockedByIpBlackListMobile() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier
        );
        ipListConfig(
                NOTHING_ACCEPTABLE,
                whiteListSupplier
        );

        expectBlockedByIp();

        doTest(b -> b.param("uuid", "00000000000000000000000000000000"));
    }

    @Test
    public void whiteListIgnoreBlackListMobile() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                whiteListSupplier
        );
        ipListConfig(
                NOTHING_ACCEPTABLE,
                blackListSupplier
        );

        doTest(b -> b.param("uuid", "00000000000000000000000000000000"));
    }


    @Test
    public void blockedByTorMobile() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                NOTHING_ACCEPTABLE,
                whiteListSupplier,
                blackListSupplier
        );

        doTest(b -> b.param("uuid", "00000000000000000000000000000000"));
    }

    @Test
    public void blockedByUuidIfInWhiteListMobile() {
        client(Client.Type.MOBILE);
        uuidConfig(CheckStatus.FAIL);

        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                whiteListSupplier
        );

        ipListConfig(
                NOTHING_ACCEPTABLE,
                blackListSupplier
        );

        expectBlockedByIncorrectUuid();

        doTest();
    }

    @Test
    public void notBlockedOnWarmUpRequests() {
        LifecycleStatusService.INSTANCE.setStatus(LifecycleStatus.STARTING);

        doTest();
    }

    @Test
    public void shouldBlockWhiteUuid() {
        setWhite();
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier,
                whiteListSupplier
        );

        doTest(b -> b.param("uuid", blueMobileUuid));
        expectBlockedByParserUuid();
        doTest(b -> b.param("uuid", whiteMobileUuid));
    }

    @Test
    public void shouldBlockBlueUuid() {
        setBlue();
        uuidConfig(CheckStatus.OK);
        ipListConfig(
                IpList.ALL_ACCEPTABLE,
                blackListSupplier,
                whiteListSupplier
        );

        doTest(b -> b.param("uuid", whiteMobileUuid));
        expectBlockedByParserUuid();
        doTest(b -> b.param("uuid", blueMobileUuid));
    }

    private void doTest(Function<MockRequestBuilder, MockRequestBuilder> func) {
        HttpServletRequest request = func.apply(MockRequestBuilder
                .start()
                .pathInfo("redirect"))
                .build();
        Futures.waitAndGet(parserBlocker.apply(ContextHolder.get(), request));
    }

    private void doTest() {
        doTest(Function.identity());
    }

    private void client(Client.Type type) {
        ContextHolder.update(ctx -> {
            Client client = new Client();
            client.setType(type);

            ctx.setClient(client);
        });
    }

    private void setBlue() {
        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);
        client(Client.Type.MOBILE);
    }

    private void setWhite() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, true);
        client(Client.Type.MOBILE);
    }

    private void uuidConfig(CheckStatus status) {
        Mockito.when(
                uuidUserValidator.validate(
                        Mockito.any(Uuid.class),
                        Mockito.any(AuthSecured.class)
                )
        ).thenReturn(status.result);

        Mockito.when(
                uuidUserValidator.validate(
                        Mockito.any(Uuid.class)
                )
        ).thenReturn(status.result);
    }

    private void mobileValidation(Result<Void, MobileValidationError> validationResult) {
        mobileValidation(Futures.newSucceededFuture(validationResult));
    }

    private void mobileValidation(Future<Result<Void, MobileValidationError>> resultFuture) {
        Mockito.when(
                mobileValidationService.checkJwsToken(
                        Mockito.anyString(),
                        Mockito.anyString()
                )
        ).thenReturn(resultFuture);
    }

    private void ipListConfig(IpList ipList, Supplier<IpList>... ipListSuppliers) {
        for (Supplier<IpList> ipListSupplier : ipListSuppliers) {
            Mockito.when(
                    ipListSupplier.get()
            ).thenReturn(ipList);
        }
    }

    private void expectBlockedByIncorrectUuid() {
        exception.expect(AccessDeniedException.class);
        expectMessage("Uuid", "incorrect");
    }

    private void expectBlockedByParserUuid() {
        exception.expect(AccessDeniedException.class);
        expectMessage("Uuid", "blocked");
    }

    private void expectBlockedByIp() {
        exception.expect(AuthInfoNotFoundException.class);
    }

    private void expectBlockedByPartnerWrongHost() {
        exception.expect(AccessDeniedException.class);
        expectMessage("is using on the wrong host");
    }
}
