package ru.yandex.market.sec;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.web.ActionNameProvider;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты на {@link JavaSecInterceptor}.
 *
 * @author fbokovikov
 */
@ExtendWith(MockitoExtension.class)
class JavaSecInterceptorTest {

    @Mock
    private ActionNameProvider actionNameProvider;
    @Mock
    private EnvironmentService environmentService;

    private ShouldSkipJavaSec shouldSkipJavaSec;

    static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of("/ping", emptySet(), true),
                Arguments.of("/swagger-ui.html", emptySet(), true),
                Arguments.of("/webjars/springfox-swagger-ui/fonts/open-sans-v15-latin-700.woff2", emptySet(), true),
                Arguments.of("/swagger-resources", emptySet(), true),
                Arguments.of("/checker/result", emptySet(), true),
                Arguments.of("/campaignInfo", emptySet(), false),
                Arguments.of("/", emptySet(), false),
                Arguments.of(null, emptySet(), false),
                Arguments.of("shouldSkipDynamic", emptySet(), false),
                Arguments.of("shouldSkipDynamic", Set.of("shouldSkipDynamic"), true)
        );
    }

    @BeforeEach
    void setUp() {
        shouldSkipJavaSec = new ShouldSkipJavaSec(
                "mbi-partner",
                actionNameProvider,
                environmentService
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void shouldSkipJavaSec(String path, Set<String> dynamicIgnoredPaths, boolean expectedShouldSkip) {
        when(environmentService.getValues(
                eq("mbi-partner.javasec.whitelist.path"),
                eq(List.of())
        )).thenReturn(new ArrayList<>(dynamicIgnoredPaths));
        var req = new MockHttpServletRequest("GET", path);
        req.setPathInfo(path);
        boolean actualShouldSkip = shouldSkipJavaSec.test(req);
        Assertions.assertEquals(
                expectedShouldSkip,
                actualShouldSkip
        );
    }

    @Test
    void shouldSkipJavaSecForAction() {
        when(environmentService.getValues(
                eq("mbi-partner.javasec.whitelist.action"),
                eq(List.of())
        )).thenReturn(List.of("campaigns_campaignId"));
        when(actionNameProvider.getActionName(any())).thenReturn("campaigns_campaignId");
        shouldSkipJavaSec("/campaigns/1", Set.of(), true);

    }
}
