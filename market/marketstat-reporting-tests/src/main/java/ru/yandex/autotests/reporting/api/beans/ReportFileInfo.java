package ru.yandex.autotests.reporting.api.beans;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.NAME;
import static ru.yandex.autotests.reporting.api.beans.ReportingApiParam.URL;

/**
 * Created by kateleb on 17.11.16.
 */
public class ReportFileInfo {
    private String name;
    private String url;

    public ReportFileInfo(JsonObject fileInfo) {
        if (hasFilesInfo(fileInfo)) {
            this.name = fileInfo.get(NAME).getAsString();
            this.url = fileInfo.get(URL).getAsString();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private boolean hasFilesInfo(JsonObject response) {
        return !(response == null || response.get(NAME) == null || response.get(URL) == null);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, false);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }
}
