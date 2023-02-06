package ru.yandex.market.partner.mvc.controller.supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.hamcrest.MatcherAssert.assertThat;

@ParametersAreNonnullByDefault
class AbstractSupplierFunctionalTest extends FunctionalTest {

    static void assertResponsesWithErrors(Executable httpAction, HttpStatus expected, String... expectedErrors) {
        assertResponsesWithErrors(httpAction, expected, MbiMatchers.jsonArrayEquals(expectedErrors));
    }

    static void assertResponsesWithErrors(Executable httpAction, HttpStatus expected, Matcher<String> errorsMatcher) {
        assertThat(
                Assertions.assertThrows(
                        HttpClientErrorException.class,
                        httpAction
                ),
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(expected),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyMatches(
                                        "errors",
                                        errorsMatcher
                                )
                        )
                )
        );
    }

    String supplierApplicationEditStatusUrl(long campaignId, long euid) {
        return baseUrl + "/partner/application/status?euid=" + euid + "&id=" + campaignId;
    }

    /**
     * just a convenient shortcut
     *
     * @param filePath file with to resource
     * @return file content as a string
     */
    String fromFile(String filePath) {
        return StringTestUtil.getString(getClass(), filePath);
    }
}
