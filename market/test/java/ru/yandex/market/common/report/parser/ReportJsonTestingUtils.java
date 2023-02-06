package ru.yandex.market.common.report.parser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author: belmatter
 */
public class ReportJsonTestingUtils {

    public static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        final SerializationConfig serializationConfig = mapper.getSerializationConfig();
        final VisibilityChecker<?> visibilityChecker = serializationConfig.getDefaultVisibilityChecker();
        mapper.setVisibility(
                visibilityChecker
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
        );
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    public static JSONObject makeReportResponseCorrectJson(String report) throws JSONException {
        JSONObject objectExpected = new JSONObject(report);
        int i = 0;
        while (i < objectExpected.getJSONArray("filters").length()) {
            JSONObject tempObject = objectExpected.getJSONArray("filters").getJSONObject(i);
            //tempObject.remove("valuesGroups");
            i++;
        }
        i = 0;
        JSONObject tempResultObject;
        while (i < objectExpected.getJSONObject("search").getJSONArray("results").length()) {
            tempResultObject = objectExpected
                    .getJSONObject("search")
                    .getJSONArray("results")
                    .getJSONObject(i);
            tempResultObject.remove("meta");
            int tempCounter = 0;
            if (!tempResultObject.isNull("navnodes")) {
                while (tempCounter < tempResultObject.getJSONArray("navnodes").length()) {
                    tempResultObject.getJSONArray("navnodes").getJSONObject(tempCounter).remove("rootNavnode");
                    tempCounter++;
                }
            }
            if (!tempResultObject.isNull("vendor")){
            if (tempResultObject.getJSONObject("vendor").has("logo")) {
                tempResultObject.getJSONObject("vendor").getJSONObject("logo").remove("thumbnails");
            }
            }
            i++;
        }
        return objectExpected;
    }

}
