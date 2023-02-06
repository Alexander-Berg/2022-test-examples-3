package ru.yandex.market.mbi.api.servlets.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtPlacementStatsServletTestLoader extends FunctionalTest {
    @Autowired
    private RestTemplate restTemplate;

    private static final Pattern FLOAT_REGULAR = Pattern.compile("[-+]?[0-9]*\\.[0-9]{0,}0{2,}");

    public static String reformate(String stringLine, boolean withPointZero) {
        String[] stringList = stringLine.split(";");
        List<String> stringListAfterReformate = new ArrayList<>();
        for (String string : stringList) {
            if (FLOAT_REGULAR.matcher(string).matches()) {
                stringListAfterReformate.add(string
                        .replaceAll("0+$", "")
                        .replaceAll("\\.$", withPointZero ? ".0" : ""));

            } else {
                stringListAfterReformate.add(string.toUpperCase(Locale.ROOT));
            }
        }
        String stringAfterReformate = String.join(";", stringListAfterReformate);

        // Я посмотрел, что в заголовке нет цифр, а в данных он есть, и по этому принципу разделил
        if (stringAfterReformate.matches("^\\D+$")) {
            return stringAfterReformate;
        } else {
            return stringAfterReformate + ";";
        }
    }

    public void loadAndCompare(String url, String fileName) {
        final String response = restTemplate.getForObject(url, String.class);
        final List<String> responseAsStrings;
        if (StringUtils.isEmpty(response)) {
            responseAsStrings = Collections.emptyList();
        } else {
            responseAsStrings = Arrays.stream(response.split("\n"))
                    .map(stringLine -> reformate(stringLine, true))
                    .collect(Collectors.toList());
        }
        assertThat(responseAsStrings)
                .containsExactly(
                        StringTestUtil.getString(
                                ExtPlacementStatsServletTestLoader.class,
                                fileName
                        ).split("\n"));
    }

}
