package ru.yandex.autotests.direct.cmd.steps.excel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import ru.yandex.autotests.direct.cmd.data.CMD;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.excel.ConfirmSaveCampRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ExportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.ImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsRequest;
import ru.yandex.autotests.direct.cmd.data.excel.PreImportCampXlsResponse;
import ru.yandex.autotests.direct.cmd.data.redirect.LocationParam;
import ru.yandex.autotests.direct.cmd.data.redirect.RedirectResponse;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBackEndSteps;
import ru.yandex.autotests.direct.cmd.steps.base.DirectBeanWrapper;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.autotests.httpclientlite.core.RequestBuilder;
import ru.yandex.autotests.httpclientlite.core.request.multipart.MultipartRequestBuilder;
import ru.yandex.autotests.irt.testutils.allure.AllureUtils;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.cmd.data.Headers.ACCEPT_JSON_HEADER;
import static ru.yandex.autotests.direct.cmd.data.Headers.X_REQUESTED_WITH_HEADER;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class ExcelSteps extends DirectBackEndSteps {

    @Step("GET cmd = exportCampXLS (экспорт кампании в excel-файл)")
    public File exportCampaign(ExportCampXlsRequest request) {
        File respFile = get(CMD.EXPORT_CAMP_XLS, request, File.class);
        File newFile = new File(respFile.toString() + "." + request.getXlsFormat());
        try {
            newFile.delete();
            FileUtils.moveFile(respFile, newFile);
            attachFileToAllure(newFile, request.getXlsFormat());
            return newFile;
        } catch (IOException e) {
            throw new DirectCmdStepsException("Error while renaming downloaded file", e);
        }
    }

    private void attachFileToAllure(File file, ExportCampXlsRequest.ExcelFormat format) {
        byte[] bytes;
        try {
            bytes = FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            AllureUtils.addTextAttachment("Error while reading downloaded file", "");
            return;
        }
        if (format == ExportCampXlsRequest.ExcelFormat.XLS) {
            AllureUtils.addXlsAttachment("excel файл", bytes);
        } else {
            AllureUtils.addXlsxAttachment("excel файл", bytes);
        }
    }

    @Step("экспорт кампании {0} пользователя {1} в excel-файл")
    public File exportXlsCampaign(Long cid, String ulogin) {
        return exportCampaign(new ExportCampXlsRequest()
                .withCid(cid.toString())
                .withXlsFormat(ExportCampXlsRequest.ExcelFormat.XLS)
                .withUlogin(ulogin));
    }

    @Step("Экспорт кампании в excel-файл со сбросом блокировки")
    public File exportCampaignIgnoringLock(ExportCampXlsRequest request) {
        ExportCampXlsRequest newRequest = request.deepCopy();
        newRequest.withReleaseCampLock(true);
        return exportCampaign(newRequest);
    }

    @Step("предзагрузка кампании из excel-файла для клиента {2}")
    public PreImportCampXlsResponse preImportCampaign(String fileName, PreImportCampXlsRequest.ImportFormat format, String client) {
        return preImportCampaign(new PreImportCampXlsRequest()
                .withXls(fileName)
                .withImportFormat(format)
                .withJson(true)
                .withUlogin(client));
    }

    @Step("POST cmd = preImportCampXLS (предзагрузка кампании из excel-файла)")
    public PreImportCampXlsResponse preImportCampaign(PreImportCampXlsRequest request) {
        return post(CMD.PRE_IMPORT_CAMP_XLS, request, PreImportCampXlsResponse.class);
    }

    @Step("Предзагрузка кампании из excel-файла с проверкой отсутствия ошибок")
    public PreImportCampXlsResponse safePreImportCampaign(PreImportCampXlsRequest request) {
        PreImportCampXlsResponse resp = preImportCampaign(request);
        if (resp.getsVarsName() == null) {
            throw new DirectCmdStepsException("Не удалось загрузить эксель файл. Ошибки: \n" +
                    StringUtils.join(resp.getErrors(), " \n"));
        }
        return resp;
    }

    @Step("POST cmd = importCampXLS (импорт кампании из excel-файла)")
    public ImportCampXlsResponse importCampaign(ImportCampXlsRequest request) {
        return get(CMD.IMPORT_CAMP_XLS, request, ImportCampXlsResponse.class);
    }

    @Step("импорт кампании {0} из excel-файла {2}, клиенту {4}")
    public ImportCampXlsResponse importCampaign(String cid, ImportCampXlsRequest.DestinationCamp dest,
                                                String fileName, String varsName, String client) {
        return importCampaign(new ImportCampXlsRequest()
                .withCid(cid)
                .withDestinationCamp(dest)
                .withSendToModeration(true)
                .withLostBanners("change")
                .withLostPhrases("change")
                .withsVarsName(varsName)
                .withXls(fileName)
                .withUlogin(client));
    }

    @Step("POST cmd = importCampXLS (импорт кампании из excel-файла со сбросом блокировки)")
    public ImportCampXlsResponse importCampaignIgnoringLock(ImportCampXlsRequest request) {
        ImportCampXlsRequest newRequest = request.deepCopy();
        newRequest.withReleaseCampLock(true);
        return get(CMD.IMPORT_CAMP_XLS, newRequest, ImportCampXlsResponse.class);
    }

    @Step("GET cmd = confirmSaveCampXLS (подтверждение сохраниния импортируемой кампании)")
    public RedirectResponse confirmSaveCampXls(ConfirmSaveCampRequest request) {
        return getWithoutGetVars(CMD.CONFIRM_SAVE_CAMP_XLS, request, RedirectResponse.class);
    }

    @Step("GET cmd = confirmSaveCampXLS (подтверждение сохраниния импортируемой кампании)")
    public ErrorResponse confirmSaveCampXlsErrorResponse(ConfirmSaveCampRequest request) {
        return get(CMD.CONFIRM_SAVE_CAMP_XLS, request, ErrorResponse.class);
    }

    @Step("Полный цикл импорта кампании из excel-файла")
    public RedirectResponse fullImportCampaign(PreImportCampXlsRequest preImportRequest,
                                               ImportCampXlsRequest importRequest,
                                               ConfirmSaveCampRequest confirmRequest) {

        PreImportCampXlsResponse preImportResponse = safePreImportCampaign(preImportRequest);
        ImportCampXlsRequest newRequest = importRequest.deepCopy();
        newRequest.withReleaseCampLock(true).withsVarsName(preImportResponse.getsVarsName());

        ImportCampXlsResponse importResponse = importCampaign(newRequest);

        ConfirmSaveCampRequest newConfirmRequest = confirmRequest.deepCopy();
        newConfirmRequest.withsVarsName(importResponse.getsVarsName());

        return confirmSaveCampXls(newConfirmRequest);
    }

    @Step("Полный цикл импорта кампании {2} клиенту {1} из эксель файла {0} с проверкой успешности (assume)")
    public RedirectResponse safeImportCampaignFromXls(File file, String uLogin, String cid,
                                                      ImportCampXlsRequest.DestinationCamp destinationCamp,
                                                      String... geo) {

        ExportCampXlsRequest.ExcelFormat format = file.getName().endsWith(".xls") ?
                ExportCampXlsRequest.ExcelFormat.XLS :
                ExportCampXlsRequest.ExcelFormat.XLSX;
        attachFileToAllure(file, format);

        String joinedGeo = geo.length == 0 ? "0" : StringUtils.join(geo, ",");

        PreImportCampXlsRequest preImportRequest = new PreImportCampXlsRequest()
                .withImportFormat(PreImportCampXlsRequest.ImportFormat.XLS)
                .withJson(true)
                .withXls(file.toString())
                .withUlogin(uLogin);

        ImportCampXlsRequest importRequest = new ImportCampXlsRequest()
                .withCid(cid)
                .withDestinationCamp(destinationCamp)
                .withGeo(joinedGeo)
                .withSendToModeration(true)
                .withLostBanners("change")
                .withLostPhrases("change")
                .withXls(file.getName())
                .withUlogin(uLogin);

        ConfirmSaveCampRequest confirmRequest = new ConfirmSaveCampRequest()
                .withCid(cid)
                .withConfirm(true)
                .withUlogin(uLogin);

        RedirectResponse redirectResponse = fullImportCampaign(preImportRequest, importRequest, confirmRequest);
        String err = String.format("в ответе %s получили редирект на %s",
                CMD.CONFIRM_SAVE_CAMP_XLS.getName(),
                CMD.IMPORT_CAMP_SUCCESS.getName());
        assumeThat(err, redirectResponse.getLocationParam(LocationParam.CMD), equalTo(CMD.IMPORT_CAMP_SUCCESS.getName()));
        return redirectResponse;
    }



    private <ResponseType> ResponseType getWithoutGetVars(CMD cmd,
                                                          Object bean,
                                                          Class<ResponseType> responseTypeClass) {
        DirectBeanWrapper beanWrapper = new DirectBeanWrapper(cmd, getCsrfToken(), bean).withGetVars(false);
        return executeRaw(RequestBuilder.Method.GET, beanWrapper, responseTypeClass);
    }

    @Override
    protected RequestBuilder buildRequestBuilder() {
        MultipartRequestBuilder requestBuilder = new MultipartRequestBuilder();
        requestBuilder.setHeaders(ACCEPT_JSON_HEADER, X_REQUESTED_WITH_HEADER);
        return requestBuilder;
    }
}
