package ru.yandex.autotests.direct.httpclient.data.banners.searchbanners;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

/**
 * Created by shmykov on 16.06.15.
 */
public class SearchBannersParameters extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "where")
    private String where;

    @JsonPath(requestPath = "what")
    private String what;

    @JsonPath(requestPath = "text_search")
    private String textSearch;


    public String getWhere() {
        return where;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getWhat() {
        return what;
    }

    public void setWhat(String what) {
        this.what = what;
    }

    public String getTextSearch() {
        return textSearch;
    }

    public void setTextSearch(String textSearch) {
        this.textSearch = textSearch;
    }
}