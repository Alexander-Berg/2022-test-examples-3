package ru.yandex.market.loyalty.admin.tms;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import ru.yandex.market.loyalty.admin.exception.MarketLoyaltyAdminException;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.result_validation.YtResultValidator;
import ru.yandex.market.loyalty.admin.yt.YtClient;
import ru.yandex.market.loyalty.core.config.YtArnold;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.DEFAULT_DEREF_PATH;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.modificationTimeAttribute;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.rowCountAttribute;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.mockYtClientAttributes;
import static ru.yandex.market.loyalty.admin.utils.YtTestHelper.mockYtClientDereferenceLink;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(YtJobResultValidationProcessor.class)
public class YtJobResultValidationProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private YtJobResultValidationProcessor ytJobResultValidationProcessor;
    @SpyBean
    @YtHahn
    @Autowired
    private YtClient hahnClient;
    @SpyBean
    @YtArnold
    @Autowired
    private YtClient arnoldClient;
    @YtHahn
    @MockBean
    private JdbcTemplate ytJdbcTemplate;

    private void configureYtClientMocks(YtClient ytClient, Instant modificationTime, long rowCount) {
        mockYtClientDereferenceLink(ytClient);
        mockYtClientAttributes(
                ytClient, DEFAULT_DEREF_PATH,
                modificationTimeAttribute(modificationTime),
                rowCountAttribute(rowCount)
        );
    }

    @Test
    public void shouldValidatePromocodeCoinExportWithSuccess() throws Exception {
        configureYtClientMocks(hahnClient, clock.instant(), 0);
        configureYtClientMocks(arnoldClient, clock.instant(), 0);

        ytJobResultValidationProcessor.promocodeResultValidation();
    }

    @Test
    public void shouldValidatePromocodeCoinExportWithErrorOfModificationTime() {
        var failTime = clock.instant().minus(Duration.ofHours(1));   // first valid time = now() - 40min
        configureYtClientMocks(hahnClient, failTime, 0);

        var exception = assertThrows(MarketLoyaltyAdminException.class,
                ytJobResultValidationProcessor::promocodeResultValidation);
        assertTrue(
                "Wrong time in the exception: " + exception,
                exception.getMessage().contains(failTime.toString())
        );
    }

    @Test
    public void shouldValidatePromocodeCoinExportWithErrorOfRowsCount() {
        var failRowCount = -1;
        configureYtClientMocks(hahnClient, clock.instant(), failRowCount);

        var exception = assertThrows(MarketLoyaltyAdminException.class,
                ytJobResultValidationProcessor::promocodeResultValidation);

        assertTrue(
                "Wrong row count in the exception: " + exception,
                exception.getMessage().contains("Count from YT: " + failRowCount)
        );
    }

    @Test
    public void shouldValidateAllPromosExportWithSuccess() throws Exception {
        configureYtClientMocks(hahnClient, clock.instant(), 0);
        configureYtClientMocks(arnoldClient, clock.instant(), 0);

        ytJobResultValidationProcessor.allPromosYtExporterResultValidation();
    }

    @Test
    public void shouldValidateAllPromosExportWithErrorOfModificationTime() {
        var failTime = clock.instant().minus(Duration.ofDays(2));   // first valid time = now() - 1day
        configureYtClientMocks(hahnClient, failTime, 0);

        var exception = assertThrows(MarketLoyaltyAdminException.class,
                ytJobResultValidationProcessor::allPromosYtExporterResultValidation);
        assertTrue(
                "Wrong time in the exception: " + exception,
                exception.getMessage().contains(failTime.toString())
        );
    }

    @Test
    public void shouldValidateAllPromosExportWithErrorOfRowsCount() {
        var failRowCount = -1;
        configureYtClientMocks(hahnClient, clock.instant(), failRowCount);

        var exception = assertThrows(MarketLoyaltyAdminException.class,
                ytJobResultValidationProcessor::allPromosYtExporterResultValidation);

        assertTrue(
                "Wrong row count in the exception: " + exception,
                exception.getMessage().contains("Count from YT: " + failRowCount)
        );
    }

    @Test
    public void shouldThrowValidationExceptionForIncorrectAnaplanId() {
        configureYtClientMocks(hahnClient, clock.instant(), 0);
        configureYtClientMocks(arnoldClient, clock.instant(), 0);

        when(
                ytJdbcTemplate.query(
                        contains("SELECT $incorrect_anaplan_id_count AS incorrect_anaplan_id_count, " +
                                "$duplicate_promo_count AS duplicate_promokey_count"),
                        any(ResultSetExtractor.class)
                )
        )
                .thenReturn(YtResultValidator.CheckResult.builder().incorrectAnaplanId(1).duplicatePromokeyCount(0).build());

        MarketLoyaltyAdminException exception = assertThrows(MarketLoyaltyAdminException.class,
                () -> ytJobResultValidationProcessor.allPromosYtExporterResultValidation());

        assertThat(exception.getMessage(),
                equalTo("Validation failed. There are 1 promo with anaplanId not equal to shopPromoId."));
    }

    @Test
    public void shouldThrowValidationExceptionForDuplicatePromoKeys() {
        configureYtClientMocks(hahnClient, clock.instant(), 0);
        configureYtClientMocks(arnoldClient, clock.instant(), 0);

        when(
                ytJdbcTemplate.query(
                        contains("SELECT $incorrect_anaplan_id_count AS incorrect_anaplan_id_count, " +
                                "$duplicate_promo_count AS duplicate_promokey_count"),
                        any(ResultSetExtractor.class)
                )
        )
                .thenReturn(YtResultValidator.CheckResult.builder().incorrectAnaplanId(0).duplicatePromokeyCount(1).build());

        MarketLoyaltyAdminException exception = assertThrows(MarketLoyaltyAdminException.class,
                () -> ytJobResultValidationProcessor.allPromosYtExporterResultValidation());

        assertThat(exception.getMessage(),
                equalTo("Validation failed. There are 1 promo keys having duplicate."));
    }

    @Test
    public void shouldThrowValidationExceptionForDuplicatePromoKeysAndIncorrectAnaplanId() {
        configureYtClientMocks(hahnClient, clock.instant(), 0);
        configureYtClientMocks(arnoldClient, clock.instant(), 0);

        when(
                ytJdbcTemplate.query(
                        contains("SELECT $incorrect_anaplan_id_count AS incorrect_anaplan_id_count, " +
                                "$duplicate_promo_count AS duplicate_promokey_count"),
                        any(ResultSetExtractor.class)
                )
        )
                .thenReturn(YtResultValidator.CheckResult.builder().incorrectAnaplanId(1).duplicatePromokeyCount(1).build());

        MarketLoyaltyAdminException exception = assertThrows(MarketLoyaltyAdminException.class,
                () -> ytJobResultValidationProcessor.allPromosYtExporterResultValidation());

        assertThat(exception.getMessage(),
                equalTo("Validation failed. There are 1 promo keys having duplicate. " +
                        "There are 1 promo with anaplanId not equal to shopPromoId."));
    }
}
