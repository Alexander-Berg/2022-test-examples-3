package ru.yandex.market.core.asyncreport;

import java.time.Instant;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;

import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.Matchers.hasEntry;

@ParametersAreNonnullByDefault
public class ReportInfoMatchers {
    public static Matcher<ReportInfo<ReportsType>> hasId(String id) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(ReportInfo::getId, id, "id")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasPartnerId(long partnerId) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(r -> r.getReportRequest().getEntityId(), partnerId, "partnerId")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasReportType(ReportsType reportType) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(r -> r.getReportRequest().getReportType(), reportType, "reportType")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasParamsItem(String key, Object value) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(r -> r.getReportRequest().getParams(), hasEntry(key, value), "params")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasRequestCreatedAt(Instant createAt) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(ReportInfo::getRequestCreatedAt, createAt, "createAt")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasState(ReportState state) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(ReportInfo::getState, state, "state")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasStateUpdatedAt(Instant stateUpdateAt) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(ReportInfo::getStateUpdatedAt, stateUpdateAt, "stateUpdateAt")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasUrlToDownload(@Nullable String urlToDownload) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(ReportInfo::getUrlToDownload, urlToDownload, "urlToDownload")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasDescription(@Nullable String description) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(ReportInfo::getDescription, description, "description")
                .build();
    }

    public static Matcher<ReportInfo<ReportsType>> hasTouchedAt(Instant touchedAt) {
        return MbiMatchers.<ReportInfo<ReportsType>>newAllOfBuilder()
                .add(ReportInfo::getTouchedAt, touchedAt, "touchedAt")
                .build();
    }
}
