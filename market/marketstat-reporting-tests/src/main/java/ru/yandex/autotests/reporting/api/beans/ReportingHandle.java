package ru.yandex.autotests.reporting.api.beans;

/**
 * Created by kateleb on 15.11.16
 */
public enum ReportingHandle {

    //others
    HELP("help"),
    PING("ping"),

    //dictionaries
    CATEGORIES("v1/categories"),
    DOMAINS("v1/domains"),
    REGIONS("v1/regions"),

    //profiles
    DROP_PROFILE("v1/drop_profile"),
    PROFILES("v1/profiles"),
    SAVE_PROFILE("v1/save_profile"),

    //report
    BUILD_REPORT("v1/build_report"),
    JOB("v1/job"),
    JOB_STATUS("v1/job_status"),
    JOBS("v1/jobs"),
    TERMINATE_JOBS("v1/terminate_job");

    private String handle;

    ReportingHandle(String handle) {
        this.handle = handle;
    }

    public String asString() {
        return handle;
    }

}

