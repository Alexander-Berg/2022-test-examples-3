package ru.yandex.market.psku.postprocessor.service.uc;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.UltraControllerServiceMock;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.service.PskuInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 * @date 20.05.2020
 */
public class UCServiceTest {

    private UltraControllerService ultraControllerClient;
    @Mock
    private CategoryDataHelper categoryDataHelper;

    private UCService ucService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ultraControllerClient = Mockito.spy(new UltraControllerServiceMock());
        ucService = new UCService(ultraControllerClient, categoryDataHelper);
        when(categoryDataHelper.getParameterDefaultUnitMeasureO(anyLong(), anyLong()))
                .thenReturn(Optional.of("some"));
    }

    @Test
    public void testInvalidBarcodesFiltered() {
        final String badBarcode1 = "bad barcode";
        final String badBarcode2 = "1-2-3";
        final String badBarcode3 = "899121530013513485";
        final String emptyBarcode = "";
        final String goodBarcode1 = "797266714467";
        final String goodBarcode2 = "93737412187479";
        ModelStorage.Model noBarcodesModel = ModelStorage.Model.newBuilder().setSupplierId(2).build();
        ModelStorage.Model allBarcodesBadModel = ModelStorage.Model.newBuilder().setSupplierId(1)
                .addParameterValues(makeBarcodeParam(badBarcode1))
                .addParameterValues(makeBarcodeParam(badBarcode2))
                .addParameterValues(makeBarcodeParam(badBarcode3))
                .addParameterValues(makeBarcodeParam(emptyBarcode))
                .build();
        ModelStorage.Model allBarcodesGoodModel = ModelStorage.Model.newBuilder().setSupplierId(2)
                .addParameterValues(makeBarcodeParam(goodBarcode1))
                .addParameterValues(makeBarcodeParam(goodBarcode2))
                .build();
        ModelStorage.Model mixedModel = ModelStorage.Model.newBuilder().setSupplierId(2)
                .addParameterValues(makeBarcodeParam(goodBarcode1))
                .addParameterValues(makeBarcodeParam(badBarcode1))
                .addParameterValues(makeBarcodeParam(goodBarcode2))
                .addParameterValues(makeBarcodeParam(emptyBarcode))
                .build();
        List<PskuInfo> testPskuInfos = new ArrayList<>(Arrays.asList(
                PskuInfo.builder().setId(1L).setTitle("psku_1")
                        .setModel(noBarcodesModel).setCategoryName("test").build(),
                PskuInfo.builder().setId(2L).setTitle("psku_2")
                        .setModel(allBarcodesBadModel).setCategoryName("test").build(),
                PskuInfo.builder().setId(3L).setTitle("psku_3")
                        .setModel(allBarcodesGoodModel).setCategoryName("test").build(),
                PskuInfo.builder().setId(4L).setTitle("psku_4")
                        .setModel(mixedModel).setCategoryName("test").build()));
        UCQueryRequest ucRequest = new UCQueryRequest(testPskuInfos);
        ucService.callUC(ucRequest);

        ArgumentCaptor<UltraController.DataRequest> requestCaptor = ArgumentCaptor
                .forClass(UltraController.DataRequest.class);
        verify(ultraControllerClient).enrich(requestCaptor.capture());
        UltraController.DataRequest request = requestCaptor.getValue();

        // first and second offers should not have any barcodes:
        assertThat(request.getOffers(0).hasBarcode()).isFalse();
        assertThat(request.getOffers(1).hasBarcode()).isFalse();
        // third offer should have all barcodes:
        assertThat(request.getOffers(2).getBarcode())
                .isEqualTo(join(goodBarcode1, goodBarcode2));
        // 4th offer should have only valid barcodes
        assertThat(request.getOffers(3).getBarcode())
                .isEqualTo(join(goodBarcode1, goodBarcode2));
    }

    private ModelStorage.ParameterValue makeBarcodeParam(String value) {
        ModelStorage.LocalizedString paramStr = ModelStorage.LocalizedString.newBuilder()
                .setIsoCode("1").setValue(value).build();
        return ModelStorage.ParameterValue.newBuilder().setXslName("BarCode").addStrValue(paramStr).build();
    }

    private String join(String... barcodes) {
        return String.join("|", barcodes);
    }

}
