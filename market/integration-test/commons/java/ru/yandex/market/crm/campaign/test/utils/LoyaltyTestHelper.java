package ru.yandex.market.crm.campaign.test.utils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.external.loyalty.BunchCheckResponse;
import ru.yandex.market.crm.external.loyalty.Coin;
import ru.yandex.market.crm.external.loyalty.CoinBunchSaveRequest;
import ru.yandex.market.crm.external.loyalty.WalletBunchSaveRequest;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.ResponseBuilder;

import static ru.yandex.market.crm.campaign.util.TimeUtils.MOSCOW_ZONE;
import static ru.yandex.market.mcrm.http.HttpRequest.get;
import static ru.yandex.market.mcrm.http.HttpRequest.post;

/**
 * @author apershukov
 */
@Component
public class LoyaltyTestHelper {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .withZone(MOSCOW_ZONE);

    public static class IssueCoinsBunchData {
        private final String bunchId;
        private final Consumer<CoinBunchSaveRequest> saveRequestValidator;
        private final Function<String, BunchCheckResponse> statusResponseBuilder;

        public IssueCoinsBunchData(String bunchId,
                                   Consumer<CoinBunchSaveRequest> saveRequestValidator,
                                   Function<String, BunchCheckResponse> statusResponseBuilder) {
            this.bunchId = bunchId;
            this.saveRequestValidator = saveRequestValidator;
            this.statusResponseBuilder = statusResponseBuilder;
        }
    }

    private final HttpEnvironment httpEnvironment;
    private final JsonDeserializer jsonDeserializer;
    private final JsonSerializer jsonSerializer;

    public LoyaltyTestHelper(HttpEnvironment httpEnvironment,
                             JsonDeserializer jsonDeserializer,
                             JsonSerializer jsonSerializer) {
        this.httpEnvironment = httpEnvironment;
        this.jsonDeserializer = jsonDeserializer;
        this.jsonSerializer = jsonSerializer;
    }

    private String url(String path) {
        return "http://market-loyalty.vs.market.yandex.net:35815" + path;
    }

    public void preparePromoInfo(long promoId, Coin coin) {
        httpEnvironment.when(
                get(url("/promo/coin/" + promoId))
        ).then(
                ResponseBuilder.newBuilder()
                        .body(jsonSerializer.writeObjectAsString(coin))
                        .build()
        );
    }

    public void issueCoinsBunch(String bunchId,
                                Consumer<CoinBunchSaveRequest> saveRequestValidator,
                                Function<String, BunchCheckResponse> statusResponseBuilder) {
        // Первый запрос - начало генерации монеток
        httpEnvironment.when(
                post(url("/bunchRequest/save"))
        ).then(request -> {
            CoinBunchSaveRequest body = jsonDeserializer.readObject(
                    CoinBunchSaveRequest.class,
                    request.getBody()
            );
            saveRequestValidator.accept(body);
            return ResponseBuilder.newBuilder()
                    .body(bunchId)
                    .build();
        });
        // Следующий запрос (запросы) - проверка статуса процесса генерации
        httpEnvironment.when(
                get(url("/bunchRequest/status/" + bunchId))
        ).then(request -> {
            BunchCheckResponse response = statusResponseBuilder.apply(bunchId);
            return ResponseBuilder.newBuilder()
                    .body(jsonSerializer.writeObjectAsString(response))
                    .build();
        });
    }

    public void issueCashbackBunch(String bunchId,
                                   Consumer<WalletBunchSaveRequest> saveRequestValidator,
                                   Function<String, BunchCheckResponse> statusResponseBuilder) {
        // Первый запрос - начало генерации кешбэка
        httpEnvironment.when(
                post(url("/bunchRequest/wallet/save"))
        ).then(request -> {
            WalletBunchSaveRequest body = jsonDeserializer.readObject(
                    WalletBunchSaveRequest.class,
                    request.getBody()
            );
            saveRequestValidator.accept(body);
            return ResponseBuilder.newBuilder()
                    .body(bunchId)
                    .build();
        });
        // Следующий запрос (запросы) - проверка статуса процесса генерации
        httpEnvironment.when(
                get(url("/bunchRequest/wallet/status/" + bunchId))
        ).then(request -> {
            BunchCheckResponse response = statusResponseBuilder.apply(bunchId);
            return ResponseBuilder.newBuilder()
                    .body(jsonSerializer.writeObjectAsString(response))
                    .build();
        });
    }

    public void issueCoinsSeveralBunches(IssueCoinsBunchData... data) {
        Queue<String> bunches = Arrays.stream(data)
                .map(d -> d.bunchId)
                .collect(Collectors.toCollection(LinkedList::new));
        Queue<Consumer<CoinBunchSaveRequest>> validators = Arrays.stream(data)
                .map(d -> d.saveRequestValidator)
                .collect(Collectors.toCollection(LinkedList::new));

        // Первый запрос - начало генерации монеток
        httpEnvironment.when(
                post(url("/bunchRequest/save"))
        ).then(request -> {
            CoinBunchSaveRequest body = jsonDeserializer.readObject(
                    CoinBunchSaveRequest.class,
                    request.getBody()
            );
            var validator = validators.remove();
            var id = bunches.remove();
            validator.accept(body);
            return ResponseBuilder.newBuilder()
                    .body(id)
                    .build();
        });

        // Следующий запрос (запросы) - проверка статуса процесса генерации
        Arrays.asList(data).forEach(d -> httpEnvironment.when(get(url("/bunchRequest/status/" + d.bunchId)))
                .then(request -> {
                    BunchCheckResponse response = d.statusResponseBuilder.apply(d.bunchId);
                    return ResponseBuilder.newBuilder()
                            .body(jsonSerializer.writeObjectAsString(response))
                            .build();
                })
        );
    }

    public static YTreeMapNode buildActiveAuthCoinRow(Long coinId, Long puid,
                                                      @Nullable String mergeTag,
                                                      TemporalAccessor startDate, TemporalAccessor endDate) {
        return YTree.mapBuilder()
                .key("coin_id").value(coinId)
                .key("uid").value(puid)
                .key("merge_tag").value(mergeTag)
                .key("coin_status").value("ACTIVE")
                .key("title").value("bonus")
                .key("subtitle").value("subtitle")
                .key("image_link").value("https://avatars123.mdst.yandex.net/image_link")
                .key("coin_start_date").value(TIME_FORMATTER.format(startDate))
                .key("coin_end_date").value(TIME_FORMATTER.format(endDate))
                .buildMap();
    }

    public static YTreeMapNode buildActiveAuthCoinRow(long coinId, long puid,
                                                      @Nullable String mergeTag,
                                                      ZonedDateTime startDate) {
        return buildActiveAuthCoinRow(
                coinId,
                puid,
                mergeTag,
                startDate,
                startDate.plusDays(90)
        );
    }
}
