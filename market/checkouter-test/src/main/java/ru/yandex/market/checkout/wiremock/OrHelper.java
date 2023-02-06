package ru.yandex.market.checkout.wiremock;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * @author mmetlov
 */
public class OrHelper implements Helper<Boolean> {

    public static final String NAME = "or";

    @Override
    public Boolean apply(Boolean context, Options options) throws IOException {
        boolean res = false;
        res |= context;
        for (Object o : options.params) {
            res |= ((Boolean) o);
        }
        return res;
    }
}
