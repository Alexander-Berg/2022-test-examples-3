package ru.yandex.market.billing.tasks;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.NamedHistoryMdsS3Client;
import ru.yandex.market.core.billing.commission.DbSupplierCategoryFeeDao;
import ru.yandex.market.core.billing.commission.SupplierCategoryFee;
import ru.yandex.market.core.date.Period;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.order.model.MbiBlueOrderType;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PushSupplierCategoryFeesExecutorTest {

    private static final String MDS_FILENAME = "supplier-category-fees.xml";
    private static final String EXPECTED_CONTENT_FILENAME = "PushSupplierCategoryFeesExecutorTest.expectedOutput.xml";
    private static final Period DUMMY_PERIOD = new Period(Instant.now().minusSeconds(1000), Instant.now());

    private static final ImmutableList<SupplierCategoryFee> VALID_INPUT = ImmutableList.of(
            new SupplierCategoryFee(90401L, null, 3000, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(1L, 2L, 3, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(10L, null, 30, DUMMY_PERIOD, BillingServiceType.FEE),
            new SupplierCategoryFee(90401L, 2L, 300, DUMMY_PERIOD, BillingServiceType.FEE)
    );

    private NamedHistoryMdsS3Client historyMdsS3Client;
    private PushSupplierCategoryFeesExecutor executor;

    @Test
    public void testExecutorValidInput() {
        prepareForTest(VALID_INPUT);

        executor.doJob(null);

        verify(historyMdsS3Client, times(1)).upload(eq(MDS_FILENAME), any(ContentProvider.class));
    }

    @Test
    public void testExecutorNotValidInput() {
        ImmutableList<SupplierCategoryFee> fees = ImmutableList.of(
                new SupplierCategoryFee(1L, 2L, 3, DUMMY_PERIOD, BillingServiceType.FEE),
                new SupplierCategoryFee(10L, null, 30, DUMMY_PERIOD, BillingServiceType.FEE)
        );
        prepareForTest(fees);

        try {
            executor.doJob(null);
            fail("Exception was not thrown.");
        } catch (IllegalStateException e) {
            assertEquals("There is no fee for the root category for supplier_id = null", e.getMessage());
        }

        verify(historyMdsS3Client, times(0)).upload(eq(MDS_FILENAME), any(ContentProvider.class));
    }

    private void prepareForTest(List<SupplierCategoryFee> expected) {
        DbSupplierCategoryFeeDao supplierCategoryFeeDao = mock(DbSupplierCategoryFeeDao.class);
        doReturn(expected).when(supplierCategoryFeeDao).getFee(LocalDate.now(), MbiBlueOrderType.FULFILLMENT);

        historyMdsS3Client = mock(NamedHistoryMdsS3Client.class);
        doAnswer(invocation -> {
            final String expectedOutput = IOUtils.readInputStream(
                    getClass().getResourceAsStream(EXPECTED_CONTENT_FILENAME)
            );

            ContentProvider provider = invocation.getArgument(1);
            InputStream inputStream = provider.getInputStream();
            final String actualOutput = IOUtils.readInputStream(inputStream);

            XMLUnit.setIgnoreWhitespace(true);

            Document expectedXml = XMLUnit.buildControlDocument(expectedOutput);
            Document actualXml = XMLUnit.buildTestDocument(actualOutput);

            Diff diff = XMLUnit.compareXML(expectedXml, actualXml);
            assertTrue(diff.toString(), diff.identical());

            return null;
        }).when(historyMdsS3Client).upload(anyString(), any(ContentProvider.class));

        executor = new PushSupplierCategoryFeesExecutor(
                historyMdsS3Client, supplierCategoryFeeDao, mock(EnvironmentService.class)
        );
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyNoRootCategoryForNullSupplier() {
        PushSupplierCategoryFeesExecutor.verifyNullSupplierHasFeeForRootCategory(ImmutableList.of(
                new SupplierCategoryFee(1L, 2L, 3, DUMMY_PERIOD, BillingServiceType.FEE),
                new SupplierCategoryFee(10L, null, 30, DUMMY_PERIOD, BillingServiceType.FEE),
                new SupplierCategoryFee(90401L, 2L, 300, DUMMY_PERIOD, BillingServiceType.FEE)
        ));
    }
}
