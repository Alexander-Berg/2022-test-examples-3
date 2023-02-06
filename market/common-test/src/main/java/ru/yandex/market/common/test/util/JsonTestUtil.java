package ru.yandex.market.common.test.util;

import java.io.InputStream;

import javax.annotation.Nonnull;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Утилиты для работы с json в тестах.
 *
 * @author ivmelnik
 * @since 12.10.17
 */
public class JsonTestUtil {
    private static final JsonParser PARSER = new JsonParser();

    private JsonTestUtil() {
        throw new UnsupportedOperationException("Cannot instantiate an util class");
    }

    /**
     * Сравнить json из {@code response} с json из файла.
     * Использует {@link JSONAssert#assertEquals}, благодаря чему более устойчив к проверкам близкородственных json.
     *
     * @param response             ответ, содержащий json с элементом {@code result}
     * @param contextClass         класс, чей класслоадер может загрузить файл.
     * @param expectedJsonFileName имя json-файла
     * @throws AssertionError если одно из условий:
     *                        <ol>
     *                            <li>Код ответа {@code response} - не ОК (200) </li>
     *                            <li>Не равны json из ответа и из файла</li>
     *                        </ol>
     */
    public static void assertEquals(@Nonnull String expectedJsonFileName, @Nonnull Class<?> contextClass,
                                    @Nonnull ResponseEntity<String> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        try {
            String expectedResponse = StringTestUtil.getString(contextClass, expectedJsonFileName);
            String result = parseJson(response.getBody()).getAsJsonObject()
                    .get("result")
                    .toString();

            JSONAssert.assertEquals(expectedResponse, result, JSONCompareMode.NON_EXTENSIBLE);
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Сравнить json из {@code response} с json из файла.
     *
     * @param response     ответ, содержащий json с элементом {@code result}
     * @param contextClass класс, чей класслоадер может загрузить файл.
     * @param jsonFileName имя json-файла
     * @throws AssertionError если одно из условий:
     *                        <ol>
     *                            <li>Код ответа {@code response} - не ОК (200) </li>
     *                            <li>Не равны json из ответа и из файла</li>
     *                        </ol>
     */
    public static void assertEquals(ResponseEntity<String> response, Class contextClass, String jsonFileName) {
        assertEquals(response, parseJson(contextClass, jsonFileName));
    }

    /**
     * Сравнить json из {@code response} с json из входного потока.
     *
     * @param response    ответ, содержащий json с элементом {@code result}
     * @param inputStream входной поток с json
     * @throws AssertionError если одно из условий:
     *                        <ol>
     *                            <li>Код ответа {@code response} - не ОК (200) </li>
     *                            <li>Не равны json из ответа и из входного потока</li>
     *                        </ol>
     */
    public static void assertEquals(ResponseEntity<String> response, InputStream inputStream) {
        assertEquals(response, parseJson(inputStream));
    }

    /**
     * Сравнить json из {@code response} с json из строки.
     *
     * @param response ответ, содержащий json с элементом {@code result}
     * @param json     json-строка
     * @throws AssertionError если одно из условий:
     *                        <ol>
     *                            <li>Код ответа {@code response} - не ОК (200) </li>
     *                            <li>Не равны json из ответа и из из строки</li>
     *                        </ol>
     */
    public static void assertEquals(ResponseEntity<String> response, String json) {
        assertEquals(response, parseJson(json));
    }

    /**
     * Сравнить json из {@code response} с json {@code expected}.
     *
     * @param response ответ, содержащий json с элементом {@code result}
     * @param expected ожидаемый json-результат
     * @throws AssertionError если одно из условий:
     *                        <ol>
     *                            <li>Код ответа {@code response} - не ОК (200) </li>
     *                            <li>Не равны json из ответа и из файла</li>
     *                        </ol>
     */
    public static void assertEquals(ResponseEntity<String> response, JsonElement expected) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        JsonElement actual = parseJson(body).getAsJsonObject().get("result");
        assertThat(actual).isEqualTo(expected);
    }

    public static void assertEquals(String expected, String actual) {
        JsonElement actualJson = parseJson(actual);
        JsonElement expectedJson = parseJson(expected);
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    /**
     * Метод для сравнения сообщения об ошибке из ответа ручки.
     *
     * @param expected ожидаемый текст сообщения
     * @param actual   реальный ответ
     */
    public static void assertResponseErrorMessage(String expected, String actual) {
        JsonElement expectedJson = parseJson(expected);
        JsonElement actualJson = parseJson(actual).getAsJsonObject().get("errors");
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    /**
     * Метод для сравнения сообщения об ошибке из ответа ручки со строкой.
     */
    public static void assertResponseErrorMessage(HttpClientErrorException e, String expected) {
        JsonElement actual = parseJson(e.getResponseBodyAsString()).getAsJsonObject().get("errors");
        JsonElement expectedJson = parseJson(expected);
        assertThat(actual).isEqualTo(expectedJson);
    }

    /**
     * Метод для сравнения сообщения об ошибке из ответа ручки с содержимым файла.
     */
    public static void assertResponseErrorMessage(HttpClientErrorException e, Class contextClass, String jsonFileName) {
        JsonElement actual = parseJson(e.getResponseBodyAsString()).getAsJsonObject().get("errors");
        JsonElement expectedJson = parseJson(contextClass, jsonFileName);
        assertThat(actual).isEqualTo(expectedJson);
    }

    /**
     * @return заголовки с выставленным {@code Content-Type=application/json;}
     */
    @Nonnull
    private static HttpHeaders getJsonHttpHeaders() {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        return requestHeaders;
    }

    /**
     * Получить {@link JsonElement} из файла.
     *
     * @param contextClass класс, чей класслоадер может загрузить файл.
     * @param jsonFileName имя json-файла
     */
    @Nonnull
    public static JsonElement parseJson(Class contextClass, String jsonFileName) {
        return PARSER.parse(StringTestUtil.getString(contextClass, jsonFileName));
    }

    /**
     * Получить {@link JsonElement} из входного потока.
     *
     * @param inputStream входной поток с json
     */
    @Nonnull
    public static JsonElement parseJson(InputStream inputStream) {
        return PARSER.parse(StringTestUtil.getString(inputStream));
    }

    /**
     * Получить {@link JsonElement} из строки.
     *
     * @param json json-строка
     */
    @Nonnull
    public static JsonElement parseJson(String json) {
        return PARSER.parse(json);
    }

    /**
     * Сравнить json из потока и из файла.
     *
     * @param inputStream  входной поток
     * @param contextClass класс, чей класслоадер может загрузить файл.
     * @param jsonFileName имя json-файла
     */
    public static void compareJson(InputStream inputStream, Class contextClass, String jsonFileName) {
        JsonElement actualResult = parseJson(inputStream);
        JsonElement expectedResult = parseJson(contextClass, jsonFileName);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    /**
     * Сравнить json из строки и из файла.
     *
     * @param actualJson   json-строка
     * @param contextClass класс, чей класслоадер может загрузить файл.
     * @param jsonFileName имя json-файла
     */
    public static void compareJson(String actualJson, Class contextClass, String jsonFileName) {
        JsonElement actualResult = parseJson(actualJson);
        JsonElement expectedResult = parseJson(contextClass, jsonFileName);
        assertThat(actualResult).isEqualTo(expectedResult);
    }

    /**
     * Получить {@link HttpEntity} с json из файла.
     *
     * @param contextClass класс, чей класслоадер может загрузить файл.
     * @param jsonFileName имя json-файла
     */
    @Nonnull
    public static HttpEntity getJsonHttpEntity(Class contextClass, String jsonFileName) {
        HttpHeaders requestHeaders = getJsonHttpHeaders();
        String jsonStringBody = StringTestUtil.getString(contextClass, jsonFileName);
        return new HttpEntity<>(jsonStringBody, requestHeaders);
    }

    /**
     * Получить {@link HttpEntity} с json из входного потока.
     *
     * @param inputStream входной поток с json
     */
    @Nonnull
    public static HttpEntity getJsonHttpEntity(InputStream inputStream) {
        HttpHeaders requestHeaders = getJsonHttpHeaders();
        String jsonStringBody = StringTestUtil.getString(inputStream);
        return new HttpEntity<>(jsonStringBody, requestHeaders);
    }

    /**
     * Получить {@link HttpEntity} с json из строки.
     *
     * @param json json-строка
     */
    @Nonnull
    public static HttpEntity getJsonHttpEntity(String json) {
        HttpHeaders requestHeaders = getJsonHttpHeaders();
        return new HttpEntity<>(json, requestHeaders);
    }

    /**
     * Метод позволяет шаблонизировать json файл переменными.
     */
    public static JsonTemplateBuilder fromJsonTemplate(Class contextClass, String jsonTemplateFileName) {
        return new JsonTemplateBuilder(StringTestUtil.getString(contextClass, jsonTemplateFileName));
    }

    public static class JsonTemplateBuilder {
        private final String jsonText;

        private JsonTemplateBuilder(String jsonText) {
            this.jsonText = jsonText;
        }

        public JsonTemplateBuilder withVariable(String varName, String varValue) {
            return new JsonTemplateBuilder(jsonText.replaceAll("\\{" + varName + "}", varValue));
        }

        public JsonElement build() {
            return parseJson(jsonText);
        }

        @Override
        public String toString() {
            return jsonText;
        }
    }
}
