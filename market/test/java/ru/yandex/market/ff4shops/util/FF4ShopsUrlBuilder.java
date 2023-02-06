package ru.yandex.market.ff4shops.util;

import java.util.List;

import javax.annotation.Nonnull;

import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.ff4shops.api.model.auth.ClientRole;

/**
 * @author fbokovikov
 */
public final class FF4ShopsUrlBuilder {

    private static final String REFERENCE_PART = "/reference";

    private FF4ShopsUrlBuilder() {
    }

    @Nonnull
    public static String getReferenceUrl(int port, long serviceId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path(REFERENCE_PART)
            .path("/" + serviceId)
            .path("/getReferenceItems")
            .toUriString();
    }

    @Nonnull
    public static String getStocksUrl(int port, long serviceId, boolean isWhite) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path(REFERENCE_PART)
            .path("/" + serviceId)
            .path(isWhite ? "/getWhiteStocks" : "/getStocks")
            .toUriString();
    }

    @Nonnull
    public static String getFeedUpdateUrl(int port, long feedId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/feeds/" + feedId)
            .toUriString();
    }

    @Nonnull
    public static String getDebugStockUrl(int port, int supplierId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/partner")
            .path("/getDebugStock")
            .queryParam("supplier_id", supplierId)
            .toUriString();
    }

    @Nonnull
    public static String confirmOutbound(int port) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/partner")
            .path("/outbounds")
            .path("/confirm")
            .toUriString();
    }

    @Nonnull
    public static String getOutbounds(int port, List<String> yandexIds) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/partner")
            .path("/outbounds")
            .queryParam("outboundYandexIds", String.join(",", yandexIds))
            .encode()
            .toUriString();
    }

    @Nonnull
    public static String loadSupplierUrl(int port, int supplierId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/partner")
            .path("/loadSupplier")
            .queryParam("supplier_id", supplierId)
            .toUriString();
    }

    @Nonnull
    public static String getStocksDebugStatusUrl(int port, int supplierId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/partner")
            .path("/stocks/debug/status")
            .queryParam("supplier_id", supplierId)
            .toUriString();
    }

    @Nonnull
    public static String getPagematchUrl(int port) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port + "/")
            .path("pagematch")
            .toUriString();
    }

    @Nonnull
    public static String getSwaggerUrl(int port) {
        return UriComponentsBuilder.fromUriString("http://localhost")
            .port(port)
            .path("/partner")
            .path("/v2")
            .path("/api-docs")
            .toUriString();
    }

    @Nonnull
    public static String updateSupplierStateUrl(int port, int supplierId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/suppliers/" + supplierId)
            .path("/state")
            .toUriString();
    }

    @Nonnull
    public static String getPartnersByStocksByPiUrl(int port, boolean flagValue, String pageToken, int pageSize) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:" + port)
                .path("/partners/")
                .path("/having-stocks-by-pi")
                .queryParam("on", flagValue)
                .queryParam("pageSize", pageSize);

        if (pageToken != null) {
            builder.queryParam("pageToken", pageToken);
        }
        return builder.toUriString();
    }

    @Nonnull
    public static String stocksByPiUrl(int port, int partnerId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
                .path("/partners/" + partnerId)
                .path("/stocks-by-pi")
                .toUriString();
    }

    @Nonnull
    public static String stocksByPiUrl(int port, int partnerId, int uid) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
                .path("/partners/" + partnerId)
                .path("/stocks-by-pi")
                .queryParam("uid", uid)
                .toUriString();
    }

    @Nonnull
    public static String getSolomonJvmUrl(int port) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port + "/")
            .path("solomon-jvm")
            .toUriString();
    }

    @Nonnull
    public static String getSolomonUrl(int port) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port + "/")
            .path("solomon")
            .toUriString();
    }

    @Nonnull
    public static String getCourierUrl(int port, long orderId) {
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/orders")
            .path("/" + orderId)
            .path("/courier");
        return url.toUriString();
    }

    @Nonnull
    public static String getCourierWithAcceptCodeStatusUrl(int port, long orderId, boolean forceGetAcceptCode) {
        UriComponentsBuilder url = UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/orders")
            .path("/" + orderId)
            .path("/courierWithAcceptCodeStatus");
        if (forceGetAcceptCode) {
            url.queryParam("forceGetAcceptCode", true);
        }
        return url.toUriString();
    }

    @Nonnull
    public static String getCourierWithAcceptCodeStatusUrl(int port, long orderId) {
        return getCourierWithAcceptCodeStatusUrl(port, orderId, false);
    }

    @Nonnull
    public static String getRemovalPermissions(
        int port,
        long orderId,
        long clientId,
        long shopId,
        ClientRole clientRole
    ) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/orders")
            .path("/" + orderId)
            .path("/removalPermissions")
            .queryParam("clientRole", clientRole.name())
            .queryParam("clientId", clientId)
            .queryParam("shopId", shopId)
            .toUriString();
    }

    @Nonnull
    public static String getOrderInfo(int port, List<Long> orderIds) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/orders")
            .path("/extend-info")
            .path("/get-by-ids")
            .queryParam("orderIds", orderIds.toArray())
            .toUriString();
    }

    @Nonnull
    public static String getOrderHistory(int port, long orderId) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/getOrderHistory")
            .path("/" + orderId)
            .toUriString();
    }

    @Nonnull
    public static String getOrdersStatus(int port) {
        return UriComponentsBuilder.fromUriString("http://localhost:" + port)
            .path("/getOrdersStatus")
            .toUriString();
    }
}
