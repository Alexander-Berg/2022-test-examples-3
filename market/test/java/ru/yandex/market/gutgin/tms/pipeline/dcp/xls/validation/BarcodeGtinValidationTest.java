package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.gutgin.tms.service.GlobalVendorsCachingService;
import ru.yandex.market.gutgin.tms.service.goodcontent.ParamValueHelper;
import ru.yandex.market.gutgin.tms.utils.goodcontent.GoodParameterCreator;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BarcodeGtinValidationTest extends BaseDcpExcelValidationTest {
    private static final ImmutableList<String> VALID_BARCODES =
        ImmutableList.of("797266714467", "87280500", "93737412187479", "8147933775581");

    private static final ImmutableList<String> INVALID_BARCODES =
        ImmutableList.of("123", "2147933775581", "8147933775585", "899121530013513485");

    private static final long BOOK_CATEGORY_ID = 90846L;

    private GlobalVendorsCachingService globalVendorsCachingService;
    private ParamValueHelper paramValueHelper;

    BarcodeGtinValidation validation;

    @Before
    public void setUp() {
        super.setUp();

        paramValueHelper = mock(ParamValueHelper.class);
        when(paramValueHelper.extractVendor(anyLong(), anyList()))
            .thenReturn(GoodParameterCreator.ExtractParameterResult.value(
                ModelStorage.ParameterValue.newBuilder().setOptionId(12).build()
            ));

        globalVendorsCachingService = mock(GlobalVendorsCachingService.class);
        when(globalVendorsCachingService.getVendor(anyLong()))
            .thenReturn(Optional.of(MboVendors.GlobalVendor.newBuilder().setIsRequireGtinBarcodes(true).build()));

        validation = new BarcodeGtinValidation(globalVendorsCachingService, paramValueHelper, true);
    }

    @Test
    public void shouldHasNoErrorsWithValidBarcodes() {
        List<GcRawSku> gcRawSkus = VALID_BARCODES.stream()
            .map(this::createGcRawSku)
            .collect(Collectors.toList());
        Validation.Result<GcRawSku> result = validation.validate(gcRawSkus);

        assertEquals(0, result.getInvalidValues().size());
        assertEquals(0, result.getMessages().size());
    }

    @Test
    public void shouldHasErrorsWithInvalidBarcodes() {
        List<GcRawSku> gcRawSkus = INVALID_BARCODES.stream()
            .map(this::createGcRawSku)
            .collect(Collectors.toList());
        Validation.Result<GcRawSku> result = validation.validate(gcRawSkus);

        assertEquals(INVALID_BARCODES.size(), result.getInvalidValues().size());
        assertEquals(INVALID_BARCODES.size(), result.getMessages().size());

        assertThat(result.getMessages())
            .extracting(MessageInfo::toString)
            .allMatch(m -> m.contains("для этого производителя нужно передавать " +
                "штрихкод в формате GTIN. Укажите правильное значение для поля"));
    }

    @Test
    public void mixed() {
        List<GcRawSku> gcRawSkus = Stream.concat(INVALID_BARCODES.stream(), VALID_BARCODES.stream())
            .map(this::createGcRawSku)
            .collect(Collectors.toList());
        Validation.Result<GcRawSku> result = validation.validate(gcRawSkus);

        assertEquals(INVALID_BARCODES.size(), result.getInvalidValues().size());
        assertEquals(INVALID_BARCODES.size(), result.getMessages().size());
    }

    @Test
    public void testIsbn() {
        List<GcRawSku> gcRawSkus = ImmutableList.of("978-5-9909805-1-8", "3-16-148410-X").stream()
                .map(this::createGcRawSku)
                .peek(rawSku -> rawSku.getData().setCategoryId(BOOK_CATEGORY_ID))
                .collect(Collectors.toList());
        Validation.Result<GcRawSku> result = validation.validate(gcRawSkus);

        assertEquals(1, result.getInvalidValues().size());
        assertEquals(1, result.getMessages().size());
    }

    protected GcRawSku createGcRawSku(String barcode) {
        RawSku data = RawSku.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setShopSku(barcode)
            .addRawParamValue(ParameterValueComposer.BARCODE_ID, ParameterValueComposer.BARCODE, barcode)
            .build();
        GcRawSku gcRawSku = new GcRawSku();
        gcRawSku.setFileProcessId(processId);
        gcRawSku.setCreateDate(Timestamp.from(Instant.now()));
        gcRawSku.setData(data);
        return gcRawSku;
    }
}