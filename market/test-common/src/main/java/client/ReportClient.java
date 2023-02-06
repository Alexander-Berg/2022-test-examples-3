package client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import api.ReporterApi;
import dto.requests.checkouter.RearrFactor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Assertions;
import retrofit2.Response;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.parser.json.AbstractReportJsonParser;
import ru.yandex.market.common.report.parser.json.ActualDeliveryJsonParser;
import ru.yandex.market.common.report.parser.json.DeliveryRouteJsonParser;
import ru.yandex.market.common.report.parser.json.OfferInfoMarketReportJsonParser;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;

import static toolkit.DateUtil.REPORT_TIME;
import static toolkit.Retrofits.RETROFIT;

@Slf4j
@Resource.Classpath({"delivery/report.properties"})
public class ReportClient {
    private final ReporterApi reporterApi;

    @Property("reportblue.host")
    private String host;

    public ReportClient() {
        PropertyLoader.newInstance().populate(this);
        reporterApi = RETROFIT.getRetrofit(host).create(ReporterApi.class);
    }

    @SneakyThrows
    public List<FoundOffer> offerInfo(long fesh, long feedId, String offerId, String rearrFactor) {
        log.debug("Calling Report: offerinfo...");
        Response<ResponseBody> execute = reporterApi.offerInfo(
            "127.0.0.1",
            "offerinfo",
            String.format("%s-%s", feedId, offerId),
            fesh,
            213,
            1,
            18,
            0,
            0,
            100,
            1,
            1,
            0,
            0,
            1,
            1,
            "decrypted",
            "checkout",
            "checkouter",
            "specifiedForOffer",
            0,
            "BLUE",
            rearrFactor,
            0,
            0
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось выполнить запрос к offerInfo");
        Assertions.assertNotNull(execute.body(), "Не удалось получить объект FoundOffer");
        return parseResult(new OfferInfoMarketReportJsonParser(), execute);
    }

    @SneakyThrows
    public ActualDelivery actualDelivery(long regionId, String wareMd5) {
        log.debug("Calling Report: actualDelivery...");
        Response<ResponseBody> execute = reporterApi.actualDelivery(
            "actual_delivery",
            regionId,
            wareMd5 + ":1",
            RearrFactor.GLOBAL.getValue()
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось выполнить запрос к actualDelivery");
        Assertions.assertNotNull(execute.body(), "Не удалось получить объект ActualDelivery");
        return parseResult(new ActualDeliveryJsonParser(), execute);
    }

    @SneakyThrows
    public DeliveryRoute deliveryRoute(
        long regionId,
        String wareMd5,
        LocalDateTime intervalFrom,
        LocalDateTime intervalTo,
        Long pickupPoint,
        DeliveryType deliveryType
    ) {
        log.debug("Calling Report: deliveryRoute...");
        Response<ResponseBody> execute = reporterApi.deliveryRoute(
            "delivery_route",
            regionId,
            wareMd5 + ":1",
            pickupPoint,
            REPORT_TIME.format(intervalFrom) + "-" + REPORT_TIME.format(intervalTo),
            deliveryType.name().toLowerCase(Locale.ROOT),
            RearrFactor.GLOBAL.getValue()
        ).execute();
        Assertions.assertTrue(execute.isSuccessful(), "Не удалось выполнить запрос к deliveryRoute");
        Assertions.assertNotNull(execute.body(), "Не удалось получить объект deliveryRoute");
        return parseResult(new DeliveryRouteJsonParser(), execute);
    }

    private <T> T parseResult(
        AbstractReportJsonParser<T> parser,
        Response<ResponseBody> executionResult
    ) throws IOException {
        return parser.parse(
            new ByteArrayInputStream(executionResult.body().string().getBytes(StandardCharsets.UTF_8))
        );
    }
}
