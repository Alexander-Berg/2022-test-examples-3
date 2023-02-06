package ru.yandex.market.crm.triggers.test.helpers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.domain.ResultJsonWrapper;
import ru.yandex.market.crm.core.services.external.pers.basket.PersBasketClient.WishlistItemKey;
import ru.yandex.market.crm.core.services.external.pers.basket.PersBasketItem;
import ru.yandex.market.crm.core.services.external.pers.basket.PersBasketResponse;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;

import static ru.yandex.market.mcrm.http.HttpRequest.get;
import static ru.yandex.market.mcrm.http.HttpRequest.post;

/**
 * @author apershukov
 */
@Component
public class PersBasketTestHelper {

    public static PersBasketItem product(long modelId) {
        return new PersBasketItem(
                RandomUtils.nextInt(1, 1000),
                123L,
                "white",
                "product",
                String.valueOf(modelId),
                null, null, null, null
        );
    }

    private static boolean sameItem(WishlistItemKey lhs, PersBasketItem rhs) {
        return Objects.equals(lhs.getReferenceType(), rhs.getReferenceType()) &&
                Objects.equals(lhs.getReferenceId(), rhs.getReferenceId());
    }

    private static String toPersColor(Color color) {
        switch (color) {
            case BLUE:
                return "blue";
            case GREEN:
                return "white";
            default:
                throw new IllegalArgumentException("Unsupported color " + color);
        }
    }

    private static String toPersUidType(UidType type) {
        switch (type) {
            case PUID:
                return "UID";
            case YUID:
                return "YANDEXUID";
            case UUID:
                return "UUID";
            default:
                throw new IllegalArgumentException("Unsupported uid type " + type);
        }
    }

    private static String url(Object... parts) {
        return "http://pers-basket.vs.market.yandex.net:34510/" +
                Stream.of(parts).map(Object::toString).collect(Collectors.joining("/"));
    }

    private final HttpEnvironment httpEnvironment;
    private final JsonDeserializer jsonDeserializer;
    private final JsonSerializer jsonSerializer;

    public PersBasketTestHelper(HttpEnvironment httpEnvironment,
                                JsonDeserializer jsonDeserializer,
                                JsonSerializer jsonSerializer) {
        this.httpEnvironment = httpEnvironment;
        this.jsonDeserializer = jsonDeserializer;
        this.jsonSerializer = jsonSerializer;
    }

    public void prepareWishlist(Uid uid, Color color, PersBasketItem... items) {
        String url = url("items", toPersUidType(uid.getType()), uid.getValue(), "existing");
        HttpRequest query = post(url).param("rgb", toPersColor(color));

        httpEnvironment.when(query).then(request -> {
            List<WishlistItemKey> keys = jsonDeserializer.readObject(new TypeReference<>() {}, request.getBody());

            List<PersBasketItem> filteredItems = Stream.of(items)
                    .filter(item -> keys.stream().anyMatch(key -> sameItem(key, item)))
                    .collect(Collectors.toList());

            PersBasketResponse response = new PersBasketResponse(filteredItems.size(), filteredItems);

            return ResponseBuilder.newBuilder()
                    .body(jsonSerializer.writeObjectAsBytes(response))
                    .build();
        });
    }

    public void prepareWishlistCount(Uid uid, int count) {
        String url = url("items", toPersUidType(uid.getType()), uid.getValue(), "count");

        ResultJsonWrapper<Integer> response = new ResultJsonWrapper<>(count);

        httpEnvironment.when(get(url)).then(
                ResponseBuilder.newBuilder()
                        .body(jsonSerializer.writeObjectAsBytes(response))
                        .build()
        );
    }
}
