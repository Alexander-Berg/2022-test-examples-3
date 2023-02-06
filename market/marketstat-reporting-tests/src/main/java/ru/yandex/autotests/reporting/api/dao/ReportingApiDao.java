package ru.yandex.autotests.reporting.api.dao;

import ru.yandex.autotests.reporting.api.beans.BuildReportTmsJob;

/**
 * Created by kateleb on 17.11.16.
 */
public interface ReportingApiDao {

    BuildReportTmsJob getJobDetails(String name);
}
