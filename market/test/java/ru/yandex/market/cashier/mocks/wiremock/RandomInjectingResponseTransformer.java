package ru.yandex.market.cashier.mocks.wiremock;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.Random;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.like;
import static java.lang.Integer.toHexString;

/**
 * @author mkasumov
 */
public class RandomInjectingResponseTransformer extends ResponseDefinitionTransformer {

    private final Random random = new Random(System.currentTimeMillis());
    private static final String PLACEHOLDER = "%%RND_BALANCE_TRUST_ID%%";


    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        String newBody = replaceVariables(responseDefinition.getBody());
        return like(responseDefinition).but().withBody(newBody).build();
    }

    private String replaceVariables(String body) {
        if (body == null) {
            return null;
        }

        final String[] holder = {body};
        while (holder[0].contains(PLACEHOLDER)) {
            holder[0] = holder[0].replaceFirst(PLACEHOLDER, randomValue());
        }
        return holder[0];
    }

    private String randomValue() {
        return toHexString(random.nextInt()) + toHexString(random.nextInt()) + toHexString(random.nextInt());
    }


    @Override
    public String getName() {
        return "randomInjectingResponseTransformer";
    }
}
