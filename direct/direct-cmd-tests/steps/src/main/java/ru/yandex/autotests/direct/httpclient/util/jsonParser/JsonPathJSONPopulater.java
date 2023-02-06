package ru.yandex.autotests.direct.httpclient.util.jsonParser;

import java.util.List;

import javax.xml.xpath.XPathExpressionException;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.googlecode.jxquery.JSONXMLBuilder;
import com.googlecode.jxquery.utils.Constants;
import org.w3c.dom.Document;

import ru.yandex.autotests.direct.httpclient.core.DirectResponse;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 25.02.15
 */
public class JsonPathJSONPopulater {

    private static final List<String> DEFAULT_REMOVE_PARAMS = ImmutableList.of("static_file_hashsums");

    private static final boolean FROM_JSON = true;

    public static <Vo extends Object> Vo evaluateResponse(DirectResponse response, Vo vo) {
        return eval(response.getResponseContent().asString(), vo, BeanType.RESPONSE, DEFAULT_REMOVE_PARAMS);
    }

    public static <Vo extends Object> Vo eval(String xml, Vo vo, BeanType beanType, List<String> removeParams) {
        String newXml = xml;
        if (!removeParams.isEmpty()) {
            JsonElement jsonElement = new Gson().fromJson(xml, JsonElement.class);
            removeParams.stream().filter(removeParam -> jsonElement.isJsonObject()).forEach(removeParam -> {
                JsonObject jsonObject = (JsonObject) jsonElement;
                jsonObject.remove(removeParam);
            });
            newXml = new Gson().toJson(jsonElement);
        }

        //имя поля в json не может содержать /
        String filteredXml = newXml
                .replaceAll("\"([0-9a-zA-Z._-]*/+[0-9a-zA-Z._-]*)*\":", "\"aa1\":").
                        replaceAll("\"([0-9a-zA-Z._-]*:+[0-9a-zA-Z._-]*)*\":", "\"aa1\":").
                //имя поля в json не должно начинаться с цифры
                        replaceAll("\"(\\d[0-9a-zA-Z._-]*)\":", "\"a$1\":").
                //и не содержать $
                        replaceAll("\"\\$(.*)\\$\":", "\"$1\":");

        Document doc = JSONXMLBuilder.toXML(filteredXml);

        try {
            return JsonPathXMLPopulater.eval(Constants.XML_ROOT, doc, vo, FROM_JSON, beanType);
        } catch (XPathExpressionException e) {
            throw new BackEndClientException("Ошибка при обработке json: " + e.getMessage());
        }
    }

    public static <Vo extends Object> Vo eval(String xml, Vo vo, BeanType beanType) {
        return eval(xml, vo, beanType, DEFAULT_REMOVE_PARAMS);
    }
}
