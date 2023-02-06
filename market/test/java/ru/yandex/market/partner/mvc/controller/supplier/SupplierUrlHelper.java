package ru.yandex.market.partner.mvc.controller.supplier;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Формирует url для запросов в Supplier Controllers.
 *
 * @author fbokovikov
 */
public final class SupplierUrlHelper {

    private static final String BASE_URL_PART = "/suppliers";

    private SupplierUrlHelper() {
        throw new UnsupportedOperationException();
    }

    public static String getClientApplicationURI(String baseUrl, long euid) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .path(BASE_URL_PART)
                .path("/applications")
                .queryParam("_user_id", euid)
                .toUriString();
    }
}
