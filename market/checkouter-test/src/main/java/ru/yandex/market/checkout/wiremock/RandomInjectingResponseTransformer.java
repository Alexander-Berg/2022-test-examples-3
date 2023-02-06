package ru.yandex.market.checkout.wiremock;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.like;

/**
 * @author mkasumov
 */
public class RandomInjectingResponseTransformer extends ResponseDefinitionTransformer {

    private final Random random = new Random(System.currentTimeMillis());

    private Map<String, Function<Random, ?>> variableValueSuppliers;

    public void setVariableValueSuppliers(Map<String, Function<Random, ?>> variableValueSuppliers) {
        this.variableValueSuppliers = variableValueSuppliers;
    }

    @Override
    public ResponseDefinition transform(Request request, ResponseDefinition responseDefinition, FileSource files,
                                        Parameters parameters) {
        String newBody = replaceVariables(responseDefinition.getBody());
        return like(responseDefinition).but().withBody(newBody).build();
    }

    private String replaceVariables(String body) {
        if (body == null) {
            return null;
        }
        final String[] holder = {body};
        variableValueSuppliers.forEach((varName, valueFunction) -> {
            String placeholder = "%%" + varName + "%%";
            while (holder[0].contains(placeholder)) {
                holder[0] = holder[0].replaceFirst(placeholder, Objects.toString(valueFunction.apply(random)));
            }
        });
        return holder[0];
    }

    @Override
    public String getName() {
        return "balanceResponseTransformer";
    }
}
