package ru.yandex.market.partner.mvc.controller.moderation;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.Customization;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.mbi.util.MoreMbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.mbi.util.MoreMbiMatchers.jsonPropertyMatches;


@DbUnitDataSet(before = "moderationStateController.before.csv")
public class ModerationStateControllerFunctionalTest extends FunctionalTest {

    @ParameterizedTest
    @MethodSource("getTestData")
    public void testReturnCorrectStateAllValues(long datasourceId, boolean onlyPremod, String expectedResponseFilePath) {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                createUrl(onlyPremod), String.class, datasourceId);

        String expected = StringTestUtil.getString(getClass(), expectedResponseFilePath);
        assertThat(response, MoreMbiMatchers.responseBodyMatches(jsonPropertyMatches("result",
                MbiMatchers.jsonEquals(expected,
                        singletonList(new Customization("moderationStates[*].startDate", (v1, v2) -> true))))));
    }

    private static Stream<Arguments> getTestData() {
        return Stream.of(
                Arguments.of(101001L, false, "json/moderation.shop1.json"),
                Arguments.of(101002L, false, "json/moderation.shop2.json"),
                Arguments.of(4L, false, "json/moderation.notFound.json"),
                Arguments.of(101002L, true, "json/moderation.shop2.onlyPremod.json"),
                Arguments.of(101003L, false, "json/moderation.shop3.json"),
                Arguments.of(101004L, false, "json/moderation.shop4.json")
        );
    }

    private String createUrl(boolean onlyPremod) {
        StringBuilder builder = new StringBuilder(baseUrl);
        builder.append("/moderation/state?id={datasourceId}");

        if (onlyPremod) {
            builder.append("&premod_only=1");
        }

        return builder.toString();
    }
}
