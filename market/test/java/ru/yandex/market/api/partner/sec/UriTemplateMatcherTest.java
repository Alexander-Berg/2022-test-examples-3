package ru.yandex.market.api.partner.sec;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UriTemplateMatcherTest {
    @Test
    void getMatchedPattern() {
        // given
        var matcher = new UriTemplateMatcher();
        matcher.setUseContextPath(false);
        matcher.setPatterns(List.of(
                "/ping",
                "/resource/{id}",
                "/resource/{id}/subresources"
        ));
        Function<String, Optional<String>> getMatchedPattern = s -> matcher.getMatchedPattern(new MockHttpServletRequest("GET", s));

        // when
        assertThat(getMatchedPattern.apply("/ping")).contains("/ping");
        assertThat(getMatchedPattern.apply("/ping.json")).contains("/ping");
        assertThat(getMatchedPattern.apply("/pinggg")).isEmpty();
        assertThat(getMatchedPattern.apply("/nested/ping")).isEmpty();
        assertThat(getMatchedPattern.apply("/")).isEmpty();
        assertThat(getMatchedPattern.apply("")).isEmpty();

        assertThat(getMatchedPattern.apply("/resource")).isEmpty();
        assertThat(getMatchedPattern.apply("/resource/")).isEmpty();
        assertThat(getMatchedPattern.apply("/resource/123")).contains("/resource/{id}");
        assertThat(getMatchedPattern.apply("/resource/123/")).isEmpty();
        assertThat(getMatchedPattern.apply("/resource/123/subresources")).contains("/resource/{id}/subresources");
    }
}
