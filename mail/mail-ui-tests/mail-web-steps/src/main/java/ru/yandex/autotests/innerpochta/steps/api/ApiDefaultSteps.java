package ru.yandex.autotests.innerpochta.steps.api;

import com.google.gson.GsonBuilder;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.specification.RequestSpecification;
import ru.yandex.qatools.allure.annotations.Attachment;

import java.util.Map;

import static com.google.common.base.Joiner.on;
import static java.util.stream.Collectors.joining;

/**
 * Created by mabelpines
 */
public class ApiDefaultSteps {

    private static String formattingParams(Map<String, String> requestParams) {
        return requestParams.entrySet().stream()
            .map(entry -> on(":").join(entry.getKey(), entry.getValue()))
            .collect(joining("\n"));
    }

    @Attachment
    static String addSpecParamsToReport(RequestSpecification reqSpec) {
        RequestSpecificationImpl recSpecImpl = (RequestSpecificationImpl) reqSpec;
        return formattingParams(recSpecImpl.getRequestParams());
    }

    public static JsonPathConfig getJsonPathConfig() {
        return new JsonPathConfig(JsonPathConfig.jsonPathConfig()
            .gsonObjectMapperFactory((aClass, s) -> new GsonBuilder().create()));
    }
}
