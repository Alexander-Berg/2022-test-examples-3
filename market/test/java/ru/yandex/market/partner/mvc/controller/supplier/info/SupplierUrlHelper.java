package ru.yandex.market.partner.mvc.controller.supplier.info;

import java.util.Optional;

import javax.annotation.Nullable;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author fbokovikov
 */
class SupplierUrlHelper {

    private static final String SUPPLIERS_BASE_PART = "/suppliers";

    private SupplierUrlHelper() {
    }

    static String getSuppliersFullInfoURI(String baseUrl, long euid, @Nullable String query) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(baseUrl + SUPPLIERS_BASE_PART)
                .path("/full-info")
                .queryParam("euid", euid);
        Optional.ofNullable(query).ifPresent(q -> uriBuilder.queryParam("query", q));
        return uriBuilder.toUriString();
    }
}
