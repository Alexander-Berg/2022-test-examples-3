package ru.yandex.common.geocoder.model.request;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QueryHintsParamsTest {

    private static final URI URI_WITHOUT_HINTS = URI.create("http://uri-without-hints.net/path?");

    @Test
    @DisplayName("Создание Url с QueryHints параметрами")
    void createUrlWithHintsTest() {
        QueryHints hints = QueryHints.segmentedAddress(
                new LinkedHashSet<>(Arrays.asList("Новосибирск", "Николаева"))
        );
        URI actualURI = hints.augmentUrl(URI_WITHOUT_HINTS);
        assertThat(actualURI).hasHost("uri-without-hints.net")
                .hasPath("/path")
                .hasParameter("search_experimental_segmented_address", "Новосибирск##Николаева####1");
    }

    @Test
    @DisplayName("Создание Url с пустыми QueryHints параметрами")
    void createUrlWithNothingHintsTest() {
        QueryHints hints = QueryHints.segmentedAddress(Collections.emptySet());
        URI actualURI = hints.augmentUrl(URI_WITHOUT_HINTS);
        assertThat(actualURI).hasHost("uri-without-hints.net")
                .hasPath("/path")
                .hasNoParameter("search_experimental_segmented_address");
    }
}
