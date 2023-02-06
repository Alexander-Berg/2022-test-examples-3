package ru.yandex.market.ff.controller.health;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.core.IsNot.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

class PagematchControllerTest extends MvcIntegrationTest {

    @Test
    void requestIsNotProcessedOnWarehousePositive() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(get("/pagematch")).andDo(print()).andReturn();

        String content = mvcResult.getResponse().getContentAsString();
        assertThat(content, not(isEmptyString()));

        String[] contentLines = content.split("\n");
        assertThat(contentLines, not(emptyArray()));

        // проверяем, что формат выдачи удовлетворяет
        // https://wiki.yandex-team.ru/market/development/health/logshatter/#pagematcher
        Arrays.stream(contentLines)
              .forEach(l -> assertThat(StringUtils.countMatches(l, "\t"), equalTo(2)));

        // проверяем, что id не повторяются
        Map<String, Long> frequencies = Arrays.stream(contentLines)
                .map(PagematchControllerTest::extractId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        frequencies.forEach(
                (k, v) -> assertThat("Id [" + k + "] is met " + v + " times", v, equalTo(1L))
        );
    }

    private static String extractId(String line) {
        return line.split("\t")[0];
    }

}
