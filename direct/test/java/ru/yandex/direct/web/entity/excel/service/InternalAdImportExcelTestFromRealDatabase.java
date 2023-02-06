package ru.yandex.direct.web.entity.excel.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.excel.processing.model.internalad.ExcelImportResult;
import ru.yandex.direct.grid.processing.util.ResponseConverter;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.web.configuration.DirectWebConfiguration;
import ru.yandex.direct.web.entity.excel.ExcelTestUtils;
import ru.yandex.direct.web.entity.excel.model.ExcelImportResultInfo;
import ru.yandex.direct.web.entity.excel.model.UploadedImageInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportInfo;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportMode;
import ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.validation.ValidationUtils.hasValidationIssues;

/**
 * Тест для проверки импорта из файла расположенного локально - EXCEL_FILE_PATH
 * !!!ТОЛЬКО ДЛЯ РУЧНОГО ЗАПУСКА - ИСПОЛЬЗУЕТСЯ РЕАЛЬНАЯ БАЗА
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {DirectWebConfiguration.class})
@Ignore("only for manual runs, because it connects to real database")
@ParametersAreNonnullByDefault
public class InternalAdImportExcelTestFromRealDatabase {

    //TODO поправить путь и CLIENT_ID на нужный
    private static final String EXCEL_FILE_PATH = "/Users/xy6er/Downloads/статусы.xlsx";
    private static final UidAndClientId OWNER = UidAndClientId.of(975549875L, ClientId.fromLong(110119191));

    @Autowired
    private InternalAdExcelWebService internalAdExcelWebService;

    @Autowired
    private ClientService clientService;

    private ObjectWriter objectWriter;

    private Long operatorUid;
    private MockMultipartFile excelFile;


    @Before
    public void initTestData() throws IOException {
        excelFile = ExcelTestUtils.createMockExcelFile(new FileInputStream(EXCEL_FILE_PATH));

        objectWriter = JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter();

        Client client = clientService.getClient(OWNER.getClientId());
        assertThat(client)
                .isNotNull();
        operatorUid = client.getChiefUid();
    }


    @Test
    public void checkGetDataFromExcelFileAndUploadFileToMds() throws IOException {
        Result<InternalAdImportInfo> result = internalAdExcelWebService.getDataFromExcelFileAndUploadFileToMds(
                OWNER.getClientId(), InternalAdImportMode.AD_GROUPS_WITH_ADS, excelFile);

        assertThat(hasValidationIssues(result))
                .isFalse();

        InternalAdImportInfo importInfo = result.getResult();

        assertThat(importInfo)
                .isNotNull();
        assertThat(importInfo.getAdGroups())
                .isNotNull();

        System.out.println(objectWriter.writeValueAsString(importInfo));
    }

    @Test
    public void checkImportInternal() throws JsonProcessingException {
        Result<InternalAdImportInfo> result = internalAdExcelWebService.getDataFromExcelFileAndUploadFileToMds(
                OWNER.getClientId(), InternalAdImportMode.AD_GROUPS_WITH_ADS, excelFile);
        assertThat(hasValidationIssues(result))
                .isFalse();

        InternalAdImportRequest request = getInternalAdImportRequest(result.getResult());

        Result<ExcelImportResult> importResult =
                internalAdExcelWebService.importInternal(operatorUid, OWNER, request);
        assertThat(hasValidationIssues(importResult))
                .isFalse();

        ExcelImportResult excelImportResult = importResult.getResult();
        assertThat(excelImportResult)
                .isNotNull();

        assertThat(excelImportResult.hasValidationIssues())
                .isFalse();

        ExcelImportResultInfo response = toExcelImportResponse(importResult.getResult());
        System.out.println(objectWriter.writeValueAsString(response));
    }

    private InternalAdImportRequest getInternalAdImportRequest(InternalAdImportInfo result) {
        return new InternalAdImportRequest()
                .withExcelFileKey(result.getExcelFileKey())
                .withOnlyValidation(false)
                .withImportMode(InternalAdImportMode.AD_GROUPS_WITH_ADS)
                .withAdsImages(List.of(new UploadedImageInfo()
                        .withFileName("somePic.jpg")
                        .withImageHash(RandomStringUtils.randomAlphanumeric(11))));
    }

    static ExcelImportResultInfo toExcelImportResponse(ExcelImportResult result) {
        return new ExcelImportResultInfo()
                .withAddedOrUpdatedAdGroupIds(
                        ResponseConverter.getSuccessfullyResults(result.getAdGroupsResult(), String::valueOf))
                .withAddedOrUpdatedAdIds(
                        ResponseConverter.getSuccessfullyResults(result.getAdsResult(), String::valueOf))
                .withMutationId("null")
                .withExcelFileUrl(result.getExcelFileUrl());
    }

}
