package ru.yandex.autotests.reporting.api.beans;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ru.yandex.autotests.market.stat.util.JsonUtils;

import java.util.List;

/**
 * Created by kateleb on 15.11.16.
 */
public class ReportingApiProfile {

    private String shop;
    private String domain;
    private List<Integer> categories;
    private List<Integer> regions;
    private JsonObject components;

    public ReportingApiProfile() {
    }

    public ReportingApiProfile(String shop) {
        this.shop = shop;
        this.components = new JsonObject();
    }

    public ReportingApiProfile(JsonObject response) {
        if (canFormProfile(response)) {
            this.shop = response.get(ReportingApiParam.SHOP).getAsString();
            this.domain = response.get(ReportingApiParam.DOMAIN).getAsString();
            this.categories = JsonUtils.getNumbersFromJsonArray(response.get(ReportingApiParam.CATEGORIES));
            this.regions = JsonUtils.getNumbersFromJsonArray(response.get(ReportingApiParam.REGIONS));
            this.components = response.get(ReportingApiParam.COMPONENTS).getAsJsonObject();
        }
    }

    public ReportingApiProfile withShop(String shop) {
        this.shop = shop;
        return this;
    }

    public ReportingApiProfile withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public ReportingApiProfile withCategories(List<Integer> categories) {
        this.categories = categories;
        return this;
    }

    public ReportingApiProfile withRegions(List<Integer> regions) {
        this.regions = regions;
        return this;
    }

    public ReportingApiProfile withComponent(ReportingApiComponent component) {
        components.add(component.getName(), component.getValue());
        return this;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        this.shop = shop;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<Integer> getCategories() {
        return categories;
    }

    public void setCategories(List<Integer> categories) {
        this.categories = categories;
    }

    public List<Integer> getRegions() {
        return regions;
    }

    public void setRegions(List<Integer> regions) {
        this.regions = regions;
    }

    public JsonObject getComponents() {
        return components;
    }

    public void setComponents(JsonObject components) {
        this.components = components;
    }

    @Override
    public String toString() {
        return this.toJson();
    }

    private boolean canFormProfile(JsonObject response) {
        return !(response == null || response.get(ReportingApiParam.SHOP) == null || response.get(ReportingApiParam.DOMAIN) == null ||
                response.get(ReportingApiParam.REGIONS) == null || response.get(ReportingApiParam.CATEGORIES) == null);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
