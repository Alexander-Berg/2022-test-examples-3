package ru.yandex.autotests.reporting.api.beans;

import ru.yandex.autotests.market.stat.requests.LightweightRequest;

/**
 * Created by kateleb on 15.11.16.
 */
public class ReportingApiRequestFactory {

    public static ReportingApiRequest ping() {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.PING).withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getHelp() {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.HELP).withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getCategories() {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.CATEGORIES).withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getDomains() {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.DOMAINS)
                .withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getDomains(String queryPrefix, int limit) {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.DOMAINS)
                .with(ReportingApiParam.QUERY, queryPrefix)
                .with(ReportingApiParam.LIMIT, String.valueOf(limit))
                .withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getRegions() {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.REGIONS).withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getProfiles() {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.PROFILES).withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getJob(String id) {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.JOB)
                .with(ReportingApiParam.ID, id)
                .withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getJobs(String user, int limit) {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.JOBS)
                .with(ReportingApiParam.USER, user)
                .with(ReportingApiParam.LIMIT, String.valueOf(limit))
                .withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest getJobStatus(String id) {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.JOB_STATUS)
                .with(ReportingApiParam.ID, id)
                .withMethod(LightweightRequest.Method.GET);
    }

    public static ReportingApiRequest saveProfile(String name, ReportingApiProfile profile) {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.SAVE_PROFILE)
                .with(ReportingApiParam.NAME, name)
                .withBody(profile.toString())
                .withMethod(LightweightRequest.Method.POST);
    }

    public static ReportingApiRequest buildReport(String user, ReportingApiProfile profile) {
        return (ReportingApiRequest) ReportingApiRequest.instanceFor(ReportingHandle.BUILD_REPORT)
                .with(ReportingApiParam.USER, user)
                .withHeader("Content-Type", "application/json")
                .withHeader("Accept", "application/json")
                .withBody(profile.toString())
                .withMethod(LightweightRequest.Method.POST);
    }

}
