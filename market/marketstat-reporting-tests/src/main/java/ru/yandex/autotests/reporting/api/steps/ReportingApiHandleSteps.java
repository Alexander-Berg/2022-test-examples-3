package ru.yandex.autotests.reporting.api.steps;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.stat.util.JsonUtils;
import ru.yandex.autotests.reporting.api.beans.BuildReportJob;
import ru.yandex.autotests.reporting.api.beans.ReportingApiParam;
import ru.yandex.autotests.reporting.api.beans.ReportingApiProfile;
import ru.yandex.autotests.reporting.api.beans.ReportingApiProfileFactory;
import ru.yandex.autotests.reporting.api.beans.ReportingApiRequest;
import ru.yandex.autotests.reporting.api.beans.ReportingApiRequestFactory;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * Created by kateleb on 15.11.16.
 */
public class ReportingApiHandleSteps {

    @Step
    public String getHelp() {
        ReportingApiRequest request = ReportingApiRequestFactory.getHelp();
        return sendAndGetResponseAsString(request);
    }

    @Step
    public JsonObject getCategories() {
        ReportingApiRequest request = ReportingApiRequestFactory.getCategories();
        return sendAndGetValuesJson(request);
    }

    @Step("Get domains from Reporting Api for {0}")
    public List<String> getDomains(String prefix, int limit) {
        ReportingApiRequest request = ReportingApiRequestFactory.getDomains(prefix, limit);
        return sendAndGetValuesFromJson(request, "domains");
    }

    @Step
    public JsonObject getRegions() {
        ReportingApiRequest request = ReportingApiRequestFactory.getRegions();
        return sendAndGetValuesJson(request);
    }

    @Step
    public String getProfiles() {
        ReportingApiRequest request = ReportingApiRequestFactory.getProfiles();
        return sendAndGetResponseAsString(request);
    }

    @Step
    public List<BuildReportJob> getJobs(String user, int limit) {
        ReportingApiRequest request = ReportingApiRequestFactory.getJobs(user, limit);
        JsonArray jobsArray = sendAndGetValuesJson(request).get("jobs").getAsJsonArray();
        return JsonUtils.getElementsFromJsonArray(jobsArray).stream().map(e -> new BuildReportJob(e.getAsJsonObject())).collect(toList());
    }

    @Step
    public BuildReportJob getJobStatus(String id) {
        ReportingApiRequest request = ReportingApiRequestFactory.getJobStatus(id);
        JsonArray jobsArray = sendAndGetValuesJson(request).get("jobs").getAsJsonArray();
        return JsonUtils.getElementsFromJsonArray(jobsArray).stream().map(e -> new BuildReportJob(e.getAsJsonObject())).findAny().orElse(null);
    }

    @Step
    public BuildReportJob getJob(String id) {
        ReportingApiRequest request = ReportingApiRequestFactory.getJob(id);
        return new BuildReportJob(sendAndGetValuesJson(request));
    }

    @Step
    public JsonObject buildReportForProfile(String profile) {
        return buildReportForProfile(JsonUtils.parse(profile).getAsJsonObject());
    }

    @Step
    public JsonObject buildReportForProfile(JsonObject profile) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            new ReportingApiProfile(profile));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForCpc(String shop, String domain, List<Integer> categs, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.cpcSimpleProfile(shop, domain, categs, regions, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForPrices(String shop, String domain, List<Integer> categs, List<Integer> regions, int threshold, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.pricesOnlyProfile(shop, domain, categs, regions, threshold, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForForecaster(String shop, String domain, List<Integer> categs, List<Integer> regions, int periodLength) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.forecasterOnlyProfile(shop, domain, categs, regions, periodLength));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForCpa(String shop, String domain, List<Integer> categs, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.cpaSimpleProfile(shop, domain, categs, regions, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForAssortment(String shop, String domain, List<Integer> categs, List<Integer> regions, int numModels, boolean groupByMonths, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.profileWithAssortment(shop, domain, categs, regions, numModels, groupByMonths, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForAllComponents(String shop, String domain, List<Integer> categs, List<Integer> regions, int numModels, boolean groupByMonths, int clickThreshold,
                                                  int periodLength, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.allComponents(shop, domain, categs, regions, numModels, groupByMonths, clickThreshold, periodLength, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildForCpcAndCpa(String shop, String domain, List<Integer> categs, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.cpcCpaProfile(shop, domain, categs, regions, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForCpcAndAssortment(String shop, String domain, List<Integer> categs, List<Integer> regions, int numModels, boolean groupByMonths, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.cpcAssortmentSimpleProfile(shop, domain, categs, regions, numModels, groupByMonths, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForCpc1Slide(String shop, String domain, List<Integer> categs, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.cpc1stSlideSimpleProfile(shop, domain, categs, regions, eventtime));
        return sendAndGetValuesJson(request);
    }

    @Step
    public JsonObject buildReportForCpc2Slide(String shop, String domain, List<Integer> categs, List<Integer> regions, LocalDateTime... eventtime) {
        ReportingApiRequest request = ReportingApiRequestFactory.buildReport(ReportingApiParam.DEFAULT_USER,
            ReportingApiProfileFactory.cpc2ndSlideSimpleProfile(shop, domain, categs, regions, eventtime));
        return sendAndGetValuesJson(request);
    }

    private String sendAndGetResponseAsString(ReportingApiRequest request) {
        String response = request.send().bodyAsString().trim();
        Attacher.attachRequest(request);
        Attacher.attach("Response", response);
        return response;
    }


    private List<String> sendAndGetValuesFromJson(ReportingApiRequest request, String field) {
        String response = request.send().bodyAsString().trim();
        Attacher.attachRequest(request);
        Attacher.attach("Response", response);
        return Optional.ofNullable(JsonUtils.parse(response))
            .filter(JsonElement::isJsonObject).map(JsonElement::getAsJsonObject).map(jso -> jso.get(field))
            .filter(JsonElement::isJsonArray).map(JsonElement::getAsJsonArray)
            .map(jsa -> Lists.newArrayList(jsa).stream().map(JsonElement::getAsString).collect(toList()))
            .orElse(new ArrayList<>());
    }

    private JsonObject sendAndGetValuesJson(ReportingApiRequest request) {
        Attacher.attachRequest(request);
        String response = request.send().bodyAsString().trim();
        JsonObject resp;
        try {
            resp = JsonUtils.parse(response).getAsJsonObject();
            Attacher.attach("Response", response);
        } catch (Exception e) {
            resp = new JsonObject();
            resp.addProperty("error", e.getMessage());
            Attacher.attach("Response", response);
        }
        return resp;
    }

    @Step("Build report for type {0}")
    public JsonObject buildReportForSlides(ReportingApiParams req) {
        String shop = req.getShop();
        String domain = req.getDomain();
        List<Integer> categs = req.getCategories();
        List<Integer> regions = req.getRegions();
        LocalDateTime to = req.getPeriod();
        LocalDateTime from = to.minusMonths(req.getMonths() - 1);

        if (req.isCpaOnly()) return buildReportForCpa(shop, domain, categs, regions, from, to);
        if (req.isCpcOnlyFirstSlide()) return buildReportForCpc1Slide(shop, domain, categs, regions, from, to);
        if (req.isCpcOnlySecondSlide()) return buildReportForCpc2Slide(shop, domain, categs, regions, from, to);
        if (req.isCpcOnly()) return buildReportForCpc(shop, domain, categs, regions, from, to);
        if (req.isPriceOnly())
            return buildReportForPrices(shop, domain, categs, regions, req.getMinThreshold(), from, to);
        if (req.isForecasterOnly())
            return buildReportForForecaster(shop, domain, categs, regions, req.getPeriodsLength());
        if (req.isAssortmentOnly())
            return buildReportForAssortment(shop, domain, categs, regions, req.getNumModels(), req.isGroupByMonth(), from, to);
        if (req.buildAssortment() && req.buildCpc() && !req.buildCpa() && !req.buildPrice()) {
            return buildReportForCpcAndAssortment(shop, domain, categs, regions, req.getNumModels(), req.isGroupByMonth(), from, to);
        }
        if (req.allSlidesPresent()) {
            return buildReportForAllComponents(shop, domain, categs, regions, req.getNumModels(), req.isGroupByMonth(), req.getMinThreshold(), req.getPeriodsLength(), from, to);
        }
        if (!req.buildAssortment() && req.buildCpc() && req.buildCpa() && !req.buildPrice()) {
            return buildForCpcAndCpa(shop, domain, categs, regions, from, to);
        }
        throw new IllegalArgumentException("Unknown test type");
    }
}
