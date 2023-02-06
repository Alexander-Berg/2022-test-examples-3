package ru.yandex.market.logistics.iris.service.mdm.conversion;

import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MdmToIrisConversionServiceUnitTest {

    private MdmToIrisConversionServiceImpl conversionService;

    @Before
    public void init() {
        conversionService = mock(MdmToIrisConversionServiceImpl.class);
        when(conversionService.shouldDiscardBySource(any())).thenCallRealMethod();
    }

    @Test
    public void successWithoutSource() {
        MdmIrisPayload.ReferenceInformation source = MdmIrisPayload.ReferenceInformation.newBuilder().build();
        assertFalse(conversionService.shouldDiscardBySource(source));
    }

    @Test
    public void filterByType() {
        MdmIrisPayload.ReferenceInformation source = MdmIrisPayload.ReferenceInformation.newBuilder()
                .setSource(MdmIrisPayload.Associate.newBuilder()
                        .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
                        .setId(""))
                .build();

        assertTrue(conversionService.shouldDiscardBySource(source));
    }

    @Test
    public void filterBySource() {
        Stream.of(
                Pair.of("", false),
                Pair.of("msku:1729154512 supplier_id:123 shop_sku:1929716 warehouse:145", false),
                Pair.of("msku:1733333 supplier_id:12345 shop_sku:1929716 supplier:12345", true),
                Pair.of("msku:1733333 supplier:12345 shop_sku:1929716 supplier_id:12345", true),
                Pair.of("msku:1733333 mdm_operator:harry-potter", false),
                Pair.of("msku:1733333 auto:developer_tool", false)
        ).forEach(arguments -> {
            String id = arguments.getLeft();
            boolean expected = arguments.getRight();
            checkData(id, expected);
        });
    }

    private void checkData(String id, boolean expected) {
        MdmIrisPayload.ReferenceInformation source = MdmIrisPayload.ReferenceInformation.newBuilder()
                .setSource(MdmIrisPayload.Associate.newBuilder()
                        .setType(MdmIrisPayload.MasterDataSource.MDM)
                        .setId(id))
                .build();

        boolean actual = conversionService.shouldDiscardBySource(source);
        assertEquals(expected, actual);
    }
}
