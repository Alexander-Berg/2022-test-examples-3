package ru.yandex.autotests.innerpochta.util.passport;

import io.restassured.builder.RequestSpecBuilder;
import ru.yandex.autotests.passport.api.core.api.PassportApis;
import ru.yandex.testpers.passport.api.blackbox.ApiBlackbox;
import ru.yandex.testpers.passport.api.bundle.ApiBundle;

import static ru.yandex.autotests.passport.api.common.Utils.getRandomUserIp;
import static ru.yandex.autotests.passport.api.common.Properties.props;
import static ru.yandex.autotests.passport.api.common.data.PassportEnv.TEAM;
import static ru.yandex.autotests.passport.api.core.api.CommonApiSettings.basePassportSpec;
import static ru.yandex.autotests.innerpochta.tvm.TvmTicketsProvider.ticketsProvider;
import static ru.yandex.autotests.passport.api.core.utilitydata.PassportUris.blackboxUri;
import static ru.yandex.autotests.passport.api.core.utilitydata.PassportUris.passportInternalApiUri;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_X_YA_SERVICE_TICKET;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.HEADER_YA_CONSUMER_CLIENT_IP;
import static ru.yandex.autotests.passport.api.core.utilitydata.TermsAndConstants.STR_CONSUMER;

/**
 * @author pavponn
 */
public class PassportApisV2 extends PassportApis {

    public static ApiBundle apiBundleCorp() {
        return ApiBundle.bundle(ApiBundle.Config.bundleConfig()
            .withReqSpecSupplier(() -> getPassportApiSpecificationBuilderForCorp("/1/bundle")));
    }

    public static ApiBundle apiBundle() {
        return ApiBundle.bundle(ApiBundle.Config.bundleConfig()
                .withReqSpecSupplier(() -> getPassportApiSpecificationBuilder("/1/bundle")));
    }

    public static ApiBlackbox apiBlackbox() {
        return ApiBlackbox.blackbox(ApiBlackbox.Config.blackboxConfig()
                .withReqSpecSupplier(() -> getBlackboxApiSpecificationBuilder()));
    }

    public static RequestSpecBuilder getBlackboxApiSpecificationBuilder() {
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
                .addRequestSpecification(basePassportSpec())
                .setBaseUri(blackboxUri())
                .addHeader(HEADER_X_YA_SERVICE_TICKET, ticketsProvider().getBlackboxTicket());
        return requestSpecBuilder;
    }

    public static RequestSpecBuilder getPassportApiSpecificationBuilder(String basePath) {
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
                .addRequestSpecification(basePassportSpec())
                .addHeader(HEADER_YA_CONSUMER_CLIENT_IP, getRandomUserIp())
                .addQueryParam(STR_CONSUMER, props().getPassportConsumer())
                .setBaseUri(passportInternalApiUri())
                .setBasePath(basePath)
                .addHeader(HEADER_X_YA_SERVICE_TICKET, ticketsProvider().getPassportApiTicket());
        return requestSpecBuilder;
    }

    private static RequestSpecBuilder getPassportApiSpecificationBuilderForCorp(String basePath) {
        props().setPassportEnv(TEAM);
        RequestSpecBuilder requestSpecBuilder = new RequestSpecBuilder()
            .addRequestSpecification(basePassportSpec())
            .addHeader(HEADER_YA_CONSUMER_CLIENT_IP, getRandomUserIp())
            .addQueryParam(STR_CONSUMER, props().getPassportConsumer())
            .setBaseUri(passportInternalApiUri())
            .setBasePath(basePath)
            .addHeader(HEADER_X_YA_SERVICE_TICKET, ticketsProvider().getPassportApiTicket());
        return requestSpecBuilder;
    }
}
