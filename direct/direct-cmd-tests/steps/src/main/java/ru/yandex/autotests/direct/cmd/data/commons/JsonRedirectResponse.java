package ru.yandex.autotests.direct.cmd.data.commons;

import com.google.gson.annotations.SerializedName;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.steps.base.RedirectParser;

import java.util.Map;

public class JsonRedirectResponse {

    @SerializedName("result")
    private Result result;

    public class Result {

        @SerializedName("location")
        private String location;

        public String getLocation() {
            return location;
        }
    }

    public Result getResult() {
        return result;
    }

    public String getLocation() {
        return result.getLocation();
    }

    public Map<String, String> getLocationParams() {
        return RedirectParser.getUrlParams(getLocation());
    }

    public String getLocationParam(LocationParam locationParam) {
        return getLocationParams().get(locationParam.toString());
    }

    public Long getLocationParamAsLong(LocationParam locationParam) {
        String value = getLocationParam(locationParam);
        return value != null ? Long.parseLong(value) : null;
    }
}
