package ru.yandex.autotests.direct.cmd.steps.stat;

import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.stat.ShowStat;
import ru.yandex.autotests.direct.cmd.data.stat.filter.DeleteStatFilterRequest;
import ru.yandex.autotests.direct.cmd.data.stat.filter.SaveStatFilterRequest;
import ru.yandex.autotests.direct.cmd.data.stat.report.*;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.qatools.allure.annotations.Step;

public class StatSteps extends DirectBackEndSteps {

    @Step("Получение статистического отчета по кампании (login = {0}, dateFrom = {1}, dateTo = {2}")
    public ShowStatResponse showStatReport(String login, String dateFrom, String dateTo) {
        ShowStatRequest showStatRequest = new ShowStatRequest();
        showStatRequest.setUlogin(login);
        showStatRequest.setShowStat(ShowStat.SHOW.getName());
        showStatRequest.setDateFrom(dateFrom);
        showStatRequest.setDateTo(dateTo);
        return showStatReport(showStatRequest, ShowStatResponse.class);
    }

    @Step("Получение статистического отчета по кампании")
    public <T> T showStatReport(ShowStatRequest request, Class<T> classOfT) {
        return post(CMD.SHOW_STAT, request, classOfT);
    }

    @Step("Сохранение статистического отчета по кампании (login = {1}, dateFrom = {2}, dateTo = {3}, name = {4}")
    public SaveStatReportResponse saveStatReport(String login, String dateFrom, String dateTo, String name) {
        SaveStatReportRequest saveStatReportRequest = new SaveStatReportRequest();
        saveStatReportRequest.setReportName(name);
        saveStatReportRequest.setUlogin(login);
        saveStatReportRequest.setDateFrom(dateFrom);
        saveStatReportRequest.setDateTo(dateTo);
        return saveStatReport(saveStatReportRequest, SaveStatReportResponse.class);
    }

    @Step("Сохранение статистического отчета по кампании")
    public <T> T saveStatReport(SaveStatReportRequest request, Class<T> classOfT) {
        return post(CMD.SAVE_STAT_REPORT, request, classOfT);
    }

    @Step("Удаление статистического отчета по названию (reportId = {1}")
    public CommonResponse deleteStatReport(String reportId) {
        DeleteStatReportRequest request = new DeleteStatReportRequest();
        request.setReportId(reportId);
        return post(CMD.DELETE_STAT_REPORT, request, CommonResponse.class);
    }

    @Step("Сохранение фильтра для статистики")
    public <T> T saveStatFilter(SaveStatFilterRequest request, Class<T> classOfT) {
        return post(CMD.SAVE_STAT_FILTER, request, classOfT);
    }

    @Step("Удаление фильтра для статистики (reportName = {1}")
    public CommonResponse deleteStatFilter(String reportName) {
        DeleteStatFilterRequest request = new DeleteStatFilterRequest();
        request.setFilterName(reportName);
        return post(CMD.DELETE_STAT_FILTER, request, CommonResponse.class);
    }
}
