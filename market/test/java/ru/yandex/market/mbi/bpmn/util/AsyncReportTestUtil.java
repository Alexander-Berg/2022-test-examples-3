package ru.yandex.market.mbi.bpmn.util;

import java.time.Instant;
import java.util.Map;

import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.model.ReportInfo;
import ru.yandex.market.core.asyncreport.model.ReportRequest;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.param.model.EntityName;
import ru.yandex.market.mbi.asyncreport.ReportInfoDTO;

@SuppressWarnings("HideUtilityClassConstructor")
public class AsyncReportTestUtil {

    public static final String TEST_REPORT_ID = "test_report";

    public static ReportRequest getReportRequest(long entityId) {
        return getReportRequest(entityId, ReportsType.ASSORTMENT, Map.of());
    }

    public static ReportRequest getReportRequest(long entityId, ReportsType type, Map<String, Object> params) {
        return ReportRequest.<ReportsType>builder()
                .setReportType(type)
                .setEntityId(entityId)
                .setEntityName(EntityName.PARTNER)
                .setParams(params)
                .build();
    }

    public static ReportInfo getReportInfo(long entityId, ReportState reportState) {
        return getReportInfo(entityId, reportState, TEST_REPORT_ID);
    }

    public static ReportInfo getReportInfo(
            long entityId,
            ReportState reportState,
            String reportId
    ) {
        return getReportInfo(reportState, reportId, getReportRequest(entityId), null);
    }

    public static ReportInfoDTO getReportInfoDTO(long entityId, ReportState reportState) {
        return ReportInfoDTO.fromReportRequest(getReportInfo(reportState,
                TEST_REPORT_ID, getReportRequest(entityId), null));
    }

    public static ReportInfoDTO getReportInfoDTO(long entityId, ReportState reportState, String reportId) {
        return ReportInfoDTO.fromReportRequest(getReportInfo(reportState, reportId, getReportRequest(entityId), null));
    }

    public static ReportInfoDTO getReportInfoDTO(ReportState reportState, String reportId,
                                                 ReportRequest reportRequest) {
        return ReportInfoDTO.fromReportRequest(getReportInfo(reportState, reportId, reportRequest, null));
    }

    public static ReportInfoDTO getReportInfoDTO(ReportState reportState, String reportId,
                                                 ReportRequest reportRequest, Instant touchedAt) {
        return ReportInfoDTO.fromReportRequest(getReportInfo(reportState, reportId, reportRequest, touchedAt));
    }

    public static ReportInfo getReportInfo(ReportState reportState,
                                           String reportId,
                                           ReportRequest reportRequest,
                                           Instant touchedAt) {
        return ReportInfo.builder()
                .setId(reportId)
                .setState(reportState)
                .setReportRequest(reportRequest)
                .setRequestCreatedAt(Instant.now())
                .setStateUpdateAt(Instant.now())
                .setTouchedAt(touchedAt)
                .build();
    }
}
