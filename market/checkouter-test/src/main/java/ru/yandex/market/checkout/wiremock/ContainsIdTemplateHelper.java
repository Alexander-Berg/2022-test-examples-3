package ru.yandex.market.checkout.wiremock;

import java.io.IOException;
import java.util.Arrays;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.tomakehurst.wiremock.common.ListOrSingle;

/**
 * @author mmetlov
 */
public class ContainsIdTemplateHelper implements Helper<ListOrSingle<String>> {

    public static final String NAME = "containsId";

    @Override
    public Boolean apply(ListOrSingle<String> context, Options options) throws IOException {
        if (options.params.length != 1) {
            throw new IOException("wrong param number");
        }
        if (context == null || context.isEmpty()) {
            return false;
        }
        String query = context.get(0);
        Integer id = options.param(0);
        return Arrays.stream(query.split(",")).anyMatch(q -> {
            try {
                return Integer.valueOf(q).equals(id);
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }
}
