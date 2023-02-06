package ru.yandex.direct.web.entity.excel.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import ru.yandex.direct.common.mds.MdsHolder;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.excel.processing.exception.ExcelValidationException;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.inside.mds.MdsFileKey;
import ru.yandex.misc.io.InputStreamSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.excel.processing.validation.defects.ExcelDefectIds.FILE_NOT_EXIST_IN_MDS;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;
import static ru.yandex.direct.web.entity.excel.model.internalad.InternalAdImportRequest.EXCEL_FILE_KEY;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class ExcelWebUtilTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void checkGenerateMdsPath() {
        ClientId clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        Long campaignId = RandomNumberUtils.nextPositiveLong();
        String campaignName = RandomStringUtils.randomAlphabetic(73);

        String path = ExcelWebUtil.generateMdsPath(clientId, campaignId, campaignName);

        LocalDate now = LocalDate.now();
        String expectedPattern = String.format("^internal-ad-export/%d/%s-\\d{6}/%d-%s.xlsx$",
                clientId.asLong(), now.format(DATE_FORMATTER), campaignId, campaignName);
        assertThat(path)
                .containsPattern(expectedPattern);
    }

    private static Object[][] parametrizedTestDataForCampaignName() {
        return new Object[][]{
                {"name 73", "name_73"},
                {"abc!@#$%^&*//()-=+~`{}[]±§<>1234", "abc_1234"},
                {"ABCD            АбВг", "ABCD_АбВг"}
        };
    }

    @Test
    @TestCaseName("campaignName = {0}, expectedCampaignName = {1}")
    @Parameters(method = "parametrizedTestDataForCampaignName")
    public void checkSanitizeCampaignName(String campaignName, String expectedCampaignName) {
        String sanitizedCampaignName = ExcelWebUtil.sanitizeCampaignName(campaignName);

        assertThat(sanitizedCampaignName)
                .isEqualTo(expectedCampaignName);
    }

    @Test
    public void checkDownloadExcelFileFromMds_WhenFileNotExistInMds() {
        var fileKey = new MdsFileKey(RandomNumberUtils.nextPositiveInteger(), RandomStringUtils.randomAlphanumeric(10));
        MdsHolder mdsHolder = mock(MdsHolder.class);
        var inputStreamSource = mock(InputStreamSource.class);
        doReturn(false)
                .when(inputStreamSource).exists();
        doReturn(inputStreamSource)
                .when(mdsHolder).download(fileKey);

        var expectedValidationException = ExcelValidationException
                .create(FILE_NOT_EXIST_IN_MDS, path(field(EXCEL_FILE_KEY)));
        thrown.expect(equalTo(expectedValidationException));
        ExcelWebUtil.downloadExcelFileFromMds(mdsHolder, fileKey);
    }

}
