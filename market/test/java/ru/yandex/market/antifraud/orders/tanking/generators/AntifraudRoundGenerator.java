package ru.yandex.market.antifraud.orders.tanking.generators;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Value;

import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerDeviceIdRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerUserDeviceRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;

/**
 * @author dzvyagin
 */
public class AntifraudRoundGenerator {

    private final Random random = new Random();
    private List<Long> uids;
    private List<Long> promos;
    private Long coinId = 231231L;
    private Long orderId = 12312L;


    public Round generateLoyaltyRound() throws Exception {
        LoyaltyVerdictRequestDto body = LoyaltyVerdictRequestDto.builder()
                .uid(getUid())
                .coins(getCoins())
                .orderIds(getOrders())
                .reason("load_test")
                .build();
        return Round.builder()
                .label("antifraud/loyalty/detect")
                .method("POST")
                .url("/antifraud/loyalty/detect")
                .body(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(body))
                .build();
    }

    public Round generateOrderRound() throws Exception {
        OrderRequestDto body = OrderRequestDto.builder()
                .id(null)
                .coins(getCoins())
                .buyer(getBuyer())
                .items(getItems())
                .build();
        return Round.builder()
                .label("antifraud/detect")
                .method("POST")
                .url("/antifraud/detect")
                .body(AntifraudJsonUtil.OBJECT_MAPPER.writeValueAsString(body))
                .build();
    }

    private List<OrderItemRequestDto> getItems() {
        int items = 1 + random.nextInt(3);
        List<OrderItemRequestDto> rslt = new ArrayList<>();
        for (int i = 0; i < items; i++){
            OrderItemRequestDto item = generateItem();
            rslt.add(item);
        }
        return rslt;
    }

    private OrderItemRequestDto generateItem() {
        return OrderItemRequestDto.builder()
                .id((long) random.nextInt(1_000_000))
                .categoryId(random.nextInt(100_000))
                .bundleId(String.valueOf(random.nextInt(200_000)))
                .modelId(random.nextLong())
                .msku((long) random.nextInt(5_000_000))
                .price(BigDecimal.valueOf(random.nextInt(100_000)))
                .shopSku(String.valueOf(random.nextInt(10_000)))
                .count(1 + random.nextInt(2))
                .build();
    }

    private OrderBuyerRequestDto getBuyer() throws Exception {
        Long uid = getUid();
        return OrderBuyerRequestDto.builder()
                .uid(uid)
                .email(uid + "@yandex.ru")
                .normalizedPhone("+7" + uid)
                .userDevice(new OrderBuyerUserDeviceRequestDto(
                        OrderBuyerDeviceIdRequestDto.builder().build(),
                        false
                ))
                .build();
    }

    private Long getUid() throws URISyntaxException, IOException {
        if (uids == null){
            URL url = this.getClass().getClassLoader().getResource("test_uids.txt");
            uids = Files.readAllLines(Path.of(url.toURI())).stream().map(Long::parseLong).collect(Collectors.toList());
        }
        int pos = random.nextInt(uids.size());
        return uids.get(pos);
    }

    private List<Long> getOrders() {
        return List.of(this.orderId++);
    }

    private List<CoinDto> getCoins() {
        int coins = Math.max(0, random.nextInt(5) - 2);
        List<CoinDto> rslt = new ArrayList<>();
        for (int i = 0; i <coins; i++){
            Long promoId = getPromoId();
            Long coinId = this.coinId;
            this.coinId++;
            CoinDto coinDto = new CoinDto(coinId, promoId);
            rslt.add(coinDto);
        }
        return rslt;
    }

    private Long getPromoId() {
        if (promos == null){
            promos = new ArrayList<>();
            for (int i = 10002; i<10200; i++){
                promos.add((long) i);
            }
        }
        return promos.get(random.nextInt(promos.size()));
    }


    @Builder
    @Value
    public static class Round {
        private String label;
        private String method;
        private String url;
        @Builder.Default
        private String httpVersion = "HTTP/1.1";
        private Map<String, Supplier<String>> headers;
        private String body;

        public String getAmmoText() {
            StringBuilder sb = new StringBuilder();
            sb.append(label)
                    .append("\n")
                    .append(method)
                    .append(" ")
                    .append(url)
                    .append(" ")
                    .append(httpVersion)
                    .append("\n");
            for (var header : headers.entrySet()) {
                sb.append(header.getKey())
                        .append(": ")
                        .append(header.getValue().get())
                        .append("\n");
            }
            if (body != null) {
                sb.append("Content-Length: ")
                        .append(body.length())
                        .append("\n\n")
                        .append(body);
            }
            int length = getLength() ;
            return length + " " + sb.toString();
        }

        private int getLength(){
            int textLength = 0;
//        textLength += id.length() + 1;  // + new string symbol
            textLength += method.length() + 1 + url.length() +1 + httpVersion.length() +1;
            for (var entry : headers.entrySet()){
                textLength += entry.getKey().length();
                textLength += entry.getValue().get().length();
                textLength += 3; // + delimiter ": " and new string symbol
            }
            textLength += 2; // plus 2 strings delimiter
            if (body != null){
                textLength += body.length() +2;
            }
            return textLength;
        }
    }
}
