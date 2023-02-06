package ru.yandex.market.stock;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.currency.CurrencyService;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author imelnikov
 * @since 16.03.2022
 */
public class FixedListRateExportExecutorTest extends FunctionalTest {

    @Autowired
    private CurrencyService currencyService;


    @Test
    @DbUnitDataSet(
            before = "FixedListRateExportExecutorTest.before.csv"
    )
    public void exportCurrencies() throws Exception {
        NamedHistoryMdsS3Client mdsS3Client = mock(NamedHistoryMdsS3Client.class);

        EnvironmentService environmentService = mock(EnvironmentService.class);
        when(environmentService.getBooleanValue(anyString(), anyBoolean())).thenReturn(true);

        FixedListRateExportExecutor executor = new FixedListRateExportExecutor(mdsS3Client,
                currencyService, "currencies-file-name.xml", environmentService, () -> Arrays.asList(
                Currency.RUR,
                Currency.BYN,
                Currency.UAH,
                Currency.USD,
                Currency.EUR,
                Currency.GBP,
                Currency.TRY,
                Currency.KZT,
                Currency.UE));


        AtomicReference<String> currency = new AtomicReference<>();
        when(mdsS3Client.upload(anyString(), any()))
                .thenAnswer((Answer<ResourceLocation>) invocation -> {
                    String path = invocation.getArgument(0);
                    ContentProvider content = invocation.getArgument(1);
                    currency.set(IOUtils.readInputStream(content.getInputStream()));
                    return null;
                });

        executor.doJob(null);

        String expected = IOUtils.readInputStream(getClass().getResourceAsStream("currency_rate.xml"), "UTF-8");
        XMLUnit.setIgnoreWhitespace(true);
        Diff diff = XMLUnit.compareXML(expected, currency.get());
        assertTrue(diff.toString(), diff.identical());
    }
}
