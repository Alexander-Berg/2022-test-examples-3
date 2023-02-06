package ru.yandex.market.api.server.sec.cors;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.ContainerTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import javax.inject.Inject;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author dimkarp93
 */
@WithContext
public class CorsConfigTest extends ContainerTestBase {
    @Inject
    private CorsConfig corsConfig;

    @Test
    public void parseCorsFieldsVertical() {
        String partnerId = "1";
        Assert.assertEquals(
            "X-YANDEXUID, Authorization, X-User-Authorization, Content-Type",
            corsConfig.getAllowHeaders(partnerId)
        );

        Assert.assertEquals(
            "GET, OPTIONS",
            corsConfig.getAllowMethods(partnerId)
        );

        Assert.assertTrue(corsConfig.isAllowCredentials(partnerId));

        Assert.assertEquals(
            86400,
            corsConfig.getMaxAge(partnerId)
        );

        Assert.assertThat(
            corsConfig.getPatterns(partnerId).stream()
                .map(Pattern::pattern)
                .collect(Collectors.toList()),
            containsInAnyOrder(
                "^(?i)([^\\.]+\\.)*yandex\\.(ru|by|kz|ua|com|com\\.tr|com\\.ge|com\\.il|az|kg|lv|lt|md|tj|tm|fr|ee)$",
                "^(?i)([^\\.]+\\.)*ya\\.(ru)$",
                "^(?i)([^\\.]+\\.)*yandex-team\\.(ru)$"
            )
        );
    }

    @Test
    public void parseCorsFieldsToloka() {
        String partnerId = "-1";
        Assert.assertEquals(
            "X-YANDEXUID, Authorization, X-User-Authorization, Content-Type",
            corsConfig.getAllowHeaders(partnerId)
        );

        Assert.assertEquals(
            "GET, OPTIONS",
            corsConfig.getAllowMethods(partnerId)
        );

        Assert.assertTrue(corsConfig.isAllowCredentials(partnerId));

        Assert.assertEquals(
            86400,
            corsConfig.getMaxAge(partnerId)
        );

        Assert.assertThat(
            corsConfig.getPatterns(partnerId).stream()
                .map(Pattern::pattern)
                .collect(Collectors.toList()),
            containsInAnyOrder(
                "^(?i)([^\\.]+\\.)*yandex\\.(ru|by|kz|ua|com|com\\.tr|com\\.ge|com\\.il|az|kg|lv|lt|md|tj|tm|fr|ee)$",
                "^(?i)([^\\.]+\\.)*sandbox\\.iframe-toloka\\.(com)$",
                "^(?i)([^\\.]+\\.)*iframe-toloka\\.(com)$"
            )
        );
    }


    @Test
    public void corsDisallowedIfClientIsNull() {
        Assert.assertFalse(
            "cors must be disallowed if client is null",
            corsConfig.isAllowed(null)
        );
    }

    @Test
    public void corsDisallowedFor101() {
        Assert.assertFalse(
            "cors must be disallowed for 101",
            corsConfig.isAllowed("101")
        );
    }

    @Test
    public void corsAllowedFor1() {
        Assert.assertTrue(
            "cors must be allowed for 1",
            corsConfig.isAllowed("1")
        );
    }

    @Test
    public void corsAllowedForMinus2() {
        Assert.assertTrue(
            "cors must be allowed for -2",
            corsConfig.isAllowed("-2")
        );
    }

    @Test
    public void corsAllowedForMinus1() {
        Assert.assertTrue(
            "cors must be allowed for -1",
            corsConfig.isAllowed("-1")
        );
    }

    @Test
    public void corsAllowedFor2() {
        Assert.assertTrue(
            "cors must be allowed for 2",
            corsConfig.isAllowed("2")
        );
    }

}
