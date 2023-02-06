package ru.yandex.autotests.httpclient.lite.example.data;

import ru.yandex.autotests.httpclient.lite.core.AbstractFormParameters;
import ru.yandex.autotests.httpclient.lite.core.FormParameter;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class YandexSearchParameters extends AbstractFormParameters {
    @FormParameter("text")
    private String query;

    @Override
    protected String getFormFieldName(Field field) {
        return super.getFormFieldName(field);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    List<YandexSearchParameters> lst;

    public List<YandexSearchParameters> getLst() {
        return lst;
    }

    public void setLst(List<YandexSearchParameters> lst) {
        this.lst = lst;
    }
}
