package ru.yandex.market.ff.service;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.DailyLimitsType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.enums.SupplyLimitProcessingErrorType;
import ru.yandex.market.ff.exception.http.SupplyLimitProcessingException;
import ru.yandex.market.ff.model.bo.AvailableRequestSize;
import ru.yandex.market.ff.model.entity.DailyLimit;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static java.lang.ClassLoader.getSystemResourceAsStream;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class LimitServiceTest extends IntegrationTest {

    private static final String VALID_FILE_PATH = "service/limit/files";
    private static final String ERR_FILE_PATH = "service/limit/files_err";
    private static final String URL = "http://test.yandex.ru";

    @Autowired
    private LimitService limitService;

    @BeforeEach
    public void initMdsS3() throws Exception {
        when(mdsS3Client.getUrl(ArgumentMatchers.any())).thenReturn(new URL(URL));
    }

    @Test
    @DatabaseSetup("classpath:service/limit/supply-limit.xml")
    void testGetDailyLimit() {
        DailyLimit limit = limitService.getDailyLimit(145L, LocalDate.parse("2019-01-01"),
            SupplierType.THIRD_PARTY, DailyLimitsType.SUPPLY);
        assertions.assertThat(limit).isNotNull();
        assertions.assertThat(limit.getItemsCount()).isEqualTo(1000L);
        assertions.assertThat(limit.getPalletsCount()).isEqualTo(20L);

        DailyLimit limit2 = limitService.getDailyLimit(145L, LocalDate.parse("2019-01-01"),
                SupplierType.THIRD_PARTY, DailyLimitsType.WITHDRAW);
        assertions.assertThat(limit2).isNotNull();
        assertions.assertThat(limit2.getItemsCount()).isEqualTo(2000L);
        assertions.assertThat(limit2.getPalletsCount()).isEqualTo(40L);

        DailyLimit limit3 = limitService.getDailyLimit(145L, LocalDate.parse("2019-01-03"),
            SupplierType.THIRD_PARTY, DailyLimitsType.SUPPLY);
        assertions.assertThat(limit3).isNull();

        DailyLimit limit4 = limitService.getDailyLimit(145L, LocalDate.parse("2019-01-02"),
                SupplierType.THIRD_PARTY, DailyLimitsType.WITHDRAW);
        assertions.assertThat(limit4).isNull();

        DailyLimit limit5 = limitService.getDailyLimit(145L, LocalDate.parse("2019-01-01"),
            SupplierType.FIRST_PARTY, DailyLimitsType.SUPPLY);
        assertions.assertThat(limit5).isNotNull();
        assertions.assertThat(limit5.getItemsCount()).isEqualTo(2000L);
        assertions.assertThat(limit5.getPalletsCount()).isEqualTo(40L);

        DailyLimit limit6 = limitService.getDailyLimit(145L, LocalDate.parse("2000-01-01"),
            SupplierType.FIRST_PARTY, DailyLimitsType.SUPPLY);
        assertions.assertThat(limit6).isNull();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/add-limits.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/add-limits-after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void testAddLimits() throws IOException {
        MultipartFile file = getValidFile("quotas_add.xlsx");
        limitService.updateLimits(file, DailyLimitsType.SUPPLY);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/update-limits.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/update-limits-after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void testUpdateLimits() throws IOException {
        MultipartFile file = getValidFile("quotas_update.xlsx");
        limitService.updateLimits(file, DailyLimitsType.SUPPLY);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/update-withdraw-limits-before.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/update-withdraw-limits-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testUpdateWithdrawLimits() throws IOException {
        MultipartFile file = getValidFile("quotas_update.xlsx");
        limitService.updateLimits(file, DailyLimitsType.WITHDRAW);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/cancel-limits.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/cancel-limits-after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void testCancelLimits() throws IOException {
        MultipartFile file = getValidFile("quotas_cancel.xlsx");
        limitService.updateLimits(file, DailyLimitsType.SUPPLY);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/cancel-withdraw-limits-before.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/cancel-withdraw-limits-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testCancelWithdrawLimits() throws IOException {
        MultipartFile file = getValidFile("quotas_cancel.xlsx");
        limitService.updateLimits(file, DailyLimitsType.WITHDRAW);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/cancel-limits-with-movement.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/cancel-limits-with-movement-after.xml",
        assertionMode = NON_STRICT_UNORDERED)
    void testCancelSupplyLimitsWithMovement() throws IOException {
        MultipartFile file = getValidFile("quotas_with_movement_cancel.xlsx");
        limitService.updateLimits(file, DailyLimitsType.SUPPLY);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/update-limits.xml")
    })
    void testUpdateLimitsFormatError() throws IOException {
        MultipartFile file1 = getFileWithErrors("quotas_format_error_3p.xlsx");
        SupplyLimitProcessingException exception =
            assertThrows(SupplyLimitProcessingException.class,
                    () -> limitService.updateLimits(file1, DailyLimitsType.SUPPLY));
        assertions.assertThat(exception.getErrorType())
            .isEqualTo(SupplyLimitProcessingErrorType.INVALID_DOCUMENT_CONTENT);

        MultipartFile file2 = getFileWithErrors("quotas_format_error_1p.xlsx");
        exception =
            assertThrows(SupplyLimitProcessingException.class,
                    () -> limitService.updateLimits(file2, DailyLimitsType.SUPPLY));
        assertions.assertThat(exception.getErrorType())
            .isEqualTo(SupplyLimitProcessingErrorType.INVALID_DOCUMENT_CONTENT);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/update-limits.xml")
    })
    void testUpdateLimitsFormatErrorStop() throws IOException {
        MultipartFile file1 = getFileWithErrors("quotas_format_error_stop_3p.xlsx");
        SupplyLimitProcessingException exception =
            assertThrows(SupplyLimitProcessingException.class,
                    () -> limitService.updateLimits(file1, DailyLimitsType.SUPPLY));
        assertions.assertThat(exception.getErrorType())
            .isEqualTo(SupplyLimitProcessingErrorType.INVALID_DOCUMENT_CONTENT);

        MultipartFile file2 = getFileWithErrors("quotas_format_error_stop_1p.xlsx");
        exception =
            assertThrows(SupplyLimitProcessingException.class,
                    () -> limitService.updateLimits(file2, DailyLimitsType.SUPPLY));
        assertions.assertThat(exception.getErrorType())
            .isEqualTo(SupplyLimitProcessingErrorType.INVALID_DOCUMENT_CONTENT);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/limits-exceeded.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/limits-exceeded-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testUpdateLimitsWhenItExceeded() throws IOException {
        assertCorrectUpdateLimitsWhenItExceededByItems(DailyLimitsType.SUPPLY);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/withdraw-limits-exceeded.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/withdraw-limits-exceeded-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testUpdateWithdrawLimitsWhenItExceeded() throws IOException {
        assertCorrectUpdateLimitsWhenItExceededByItems(DailyLimitsType.WITHDRAW);
    }

    private void assertCorrectUpdateLimitsWhenItExceededByItems(DailyLimitsType limitsType) throws IOException {
        MultipartFile file1 = getFileWithErrors("quotas_exceeded_3p.xlsx");
        limitService.updateLimits(file1, limitsType);

        MultipartFile file2 = getFileWithErrors("quotas_exceeded_1p.xlsx");
        limitService.updateLimits(file2, limitsType);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/limits-exceeded.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/limits-pallets-exceeded-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testUpdateLimitsWhenItExceededByPalletsError() throws IOException {
        assertCorrectUpdateLimitsWhenItExceededByPallets(DailyLimitsType.SUPPLY);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:service/tanker/all-tanker-keys.xml"),
            @DatabaseSetup("classpath:service/limit/withdraw-limits-exceeded.xml")
    })
    @ExpectedDatabase(value = "classpath:service/limit/withdraw-limits-pallets-exceeded-after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testUpdateWithdrawLimitsWhenItExceededByPalletsError() throws IOException {
        assertCorrectUpdateLimitsWhenItExceededByPallets(DailyLimitsType.WITHDRAW);
    }

    private void assertCorrectUpdateLimitsWhenItExceededByPallets(DailyLimitsType limitsType) throws IOException {
        MultipartFile file1 = getFileWithErrors("quotas_exceeded_by_pallets_3p.xlsx");
        limitService.updateLimits(file1, limitsType);

        MultipartFile file2 = getFileWithErrors("quotas_exceeded_by_pallets_1p.xlsx");
        limitService.updateLimits(file2, limitsType);

        MultipartFile file3 = getFileWithErrors("quotas_exceeded_by_pallets_movement.xlsx");
        limitService.updateLimits(file3, limitsType);

        MultipartFile file4 = getFileWithErrors("quotas_exceeded_by_pallets_xdock_transport.xlsx");
        limitService.updateLimits(file4, limitsType);
    }

    @Test
    @DatabaseSetup("classpath:service/limit/supply-limits-for-some-days.xml")
    @ExpectedDatabase(value = "classpath:service/limit/supply-limits-for-some-days.xml",
            assertionMode = NON_STRICT)
    void getAvailableSupplySizeForDateForSupplyWorksCorrect() {
        assertGetAvailableSupplySizeForDateWorksCorrect(RequestType.SUPPLY);
    }


    @Test
    @DatabaseSetup("classpath:service/limit/supply-limits-for-some-days.xml")
    @ExpectedDatabase(value = "classpath:service/limit/supply-limits-for-some-days.xml",
            assertionMode = NON_STRICT)
    void getAvailableSupplySizeForDateForShadowSupplyWorksCorrect() {
        assertGetAvailableSupplySizeForDateWorksCorrect(RequestType.SHADOW_SUPPLY);
    }

    @Test
    @DatabaseSetup("classpath:service/limit/withdraw-limits-for-some-days.xml")
    @ExpectedDatabase(value = "classpath:service/limit/withdraw-limits-for-some-days.xml",
            assertionMode = NON_STRICT)
    void getAvailableSupplySizeForDateForWithdrawWorksCorrect() {
        assertGetAvailableSupplySizeForDateWorksCorrect(RequestType.WITHDRAW);
    }

    @Test
    @DatabaseSetup("classpath:service/limit/withdraw-limits-for-some-days.xml")
    @ExpectedDatabase(value = "classpath:service/limit/withdraw-limits-for-some-days.xml",
            assertionMode = NON_STRICT)
    void getAvailableSupplySizeForDateForShadowWithdrawWorksCorrect() {
        assertGetAvailableSupplySizeForDateWorksCorrect(RequestType.SHADOW_WITHDRAW);
    }

    private void assertGetAvailableSupplySizeForDateWorksCorrect(RequestType requestType) {
        LocalDate firstDate = LocalDate.of(2019, 12, 20); // нет лимита, но есть поставка
        LocalDate secondDate = LocalDate.of(2019, 12, 21); // есть и лимит, и поставка
        LocalDate thirdDate = LocalDate.of(2019, 12, 22); // есть лимит, но нет поставки
        LocalDate fourthDate = LocalDate.of(2019, 12, 23); // есть лимит c null в паллетах, есть поставка

        Map<LocalDate, AvailableRequestSize> availableSizeByDate = limitService
                .getAvailableRequestSizeForDate(SupplierType.FIRST_PARTY, 145,
                        asList(firstDate, secondDate, thirdDate, fourthDate), requestType);
        assertions.assertThat(availableSizeByDate.size()).isEqualTo(4);

        AvailableRequestSize firstSize = availableSizeByDate.get(firstDate);
        assertions.assertThat(firstSize).isNotNull();
        assertions.assertThat(firstSize.getAvailableItemsCount()).isNull();
        assertions.assertThat(firstSize.getAvailablePalletsCount()).isNull();

        AvailableRequestSize secondSize = availableSizeByDate.get(secondDate);
        assertions.assertThat(secondSize).isNotNull();
        assertions.assertThat(secondSize.getAvailableItemsCount()).isEqualTo(1999);
        assertions.assertThat(secondSize.getAvailablePalletsCount().longValue()).isEqualTo(19);

        AvailableRequestSize thirdSize = availableSizeByDate.get(thirdDate);
        assertions.assertThat(thirdSize).isNotNull();
        assertions.assertThat(thirdSize.getAvailableItemsCount()).isEqualTo(3000);
        assertions.assertThat(thirdSize.getAvailablePalletsCount().longValue()).isEqualTo(30);

        AvailableRequestSize fourthSize = availableSizeByDate.get(fourthDate);
        assertions.assertThat(fourthSize).isNotNull();
        assertions.assertThat(fourthSize.getAvailableItemsCount()).isEqualTo(1999);
        assertions.assertThat(fourthSize.getAvailablePalletsCount()).isNull();
    }

    private MockMultipartFile getFileWithErrors(String name) throws IOException {
        return getFile(ERR_FILE_PATH, name);
    }

    private MockMultipartFile getValidFile(String name) throws IOException {
        return getFile(VALID_FILE_PATH, name);
    }

    private MockMultipartFile getFile(String path, String name) throws IOException {
        String resourceFileName = String.format("%s/%s", path, name);
        return new MockMultipartFile("file", name, FileExtension.XLSX.getMimeType(),
            getSystemResourceAsStream(resourceFileName));
    }
}
