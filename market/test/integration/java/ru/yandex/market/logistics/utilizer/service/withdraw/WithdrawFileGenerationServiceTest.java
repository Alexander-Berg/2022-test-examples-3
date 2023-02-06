package ru.yandex.market.logistics.utilizer.service.withdraw;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.service.mds.MdsS3Service;
import ru.yandex.market.logistics.utilizer.util.ExcelAssertion;
import ru.yandex.market.mboc.http.DeliveryParams;
import ru.yandex.market.mboc.http.MboMappingsForDelivery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class WithdrawFileGenerationServiceTest extends AbstractContextualTest {

    private static final Long SUPPLIER_ID_100500 = 100500L;
    private static final Long SUPPLIER_ID_100501 = 100501L;
    private static final Long MARKET_SKU_1 = 1L;
    private static final Long MARKET_SKU_3 = 3L;

    private static final String SHOP_SKU_1 = "sku1";
    private static final String SHOP_SKU_3 = "sku3";
    private static final String MARKET_NAME_1 = "market_name_1";
    private static final String MARKET_NAME_3 = "market_name_3";
    private static final String TITLE_SKU_1 = "title_1";
    private static final String TITLE_SKU_3 = "title_3";

    private static final String WITHDRAW_GENERATION_PATH = "fixtures/service/withdraw-generation/";

    private final ExcelAssertion excelAssertion = new ExcelAssertion(softly);

    @Autowired
    private WithdrawFileGenerationService withdrawFileGenerationService;

    @Autowired
    private DeliveryParams deliveryParams;

    @Autowired
    private MdsS3Service mdsS3Service;

    /**
     * У поставщика два UtilizationCycle в CREATED и FINALIZED статусах.
     * Под утилизацию есть стоки для складов:
     * 172 склад - EXPIRED, DEFECT
     * 171 склад - EXPIRED
     * Генерируется три файла
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/withdraw-generation/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/withdraw-generation/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void generateWithdrawFilesForFinalizedCycle() throws IOException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(dataCampClient.searchOffers(anyLong(), any())).thenReturn(createOfferResponse(Map.of(SHOP_SKU_1, 100L)));

        runInExternalTransaction(() -> withdrawFileGenerationService.generateFiles(100500), false);

        ArgumentCaptor<StreamContentProvider> captorData = ArgumentCaptor.forClass(StreamContentProvider.class);
        ArgumentCaptor<String> captorFileName = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mdsS3Service, times(3)).uploadFile(captorFileName.capture(), captorData.capture());

        List<String> fileNames = captorFileName.getAllValues();
        List<StreamContentProvider> allValues = captorData.getAllValues();
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            InputStream fileData = allValues.get(i).getInputStream();
            excelAssertion.assertXlsx(fileData, WITHDRAW_GENERATION_PATH + "1/" + fileName);
        }

        verify(deliveryParams).searchFulfilmentSskuParams(any());
        verify(dataCampClient).searchOffers(anyLong(), any());
        softly.assertThat(fileNames).hasSize(3);
        softly.assertThat(allValues).hasSize(3);
    }

    /**
     * У поставщика один UtilizationCycle в CREATED статусе.
     * Под утилизацию есть стоки для складов:
     * 172 склад - DEFECT
     * Генерируется один файл
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/withdraw-generation/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/withdraw-generation/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void generateWithdrawFilesForCreatedCycle() throws IOException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(dataCampClient.searchOffers(anyLong(), any()))
                .thenReturn(createOfferResponse(Map.of(SHOP_SKU_1, 100L, SHOP_SKU_3, 50L)));

        runInExternalTransaction(() -> withdrawFileGenerationService.generateFiles(100501), false);

        ArgumentCaptor<StreamContentProvider> captorData = ArgumentCaptor.forClass(StreamContentProvider.class);
        ArgumentCaptor<String> captorFileName = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mdsS3Service, times(1)).uploadFile(captorFileName.capture(), captorData.capture());

        List<String> fileNames = captorFileName.getAllValues();
        List<StreamContentProvider> allValues = captorData.getAllValues();
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            InputStream fileData = allValues.get(i).getInputStream();
            excelAssertion.assertXlsx(fileData, WITHDRAW_GENERATION_PATH + "2/" + fileName);
        }

        verify(deliveryParams).searchFulfilmentSskuParams(any());
        verify(dataCampClient).searchOffers(anyLong(), any());
        softly.assertThat(fileNames).hasSize(1);
        softly.assertThat(allValues).hasSize(1);
    }

    /**
     * У поставщика только один UtilizationCycle в TRANSFERRED статусе.
     * UtilizationCycleState в ACTIVE статусе, переводим в DEPRECATED
     * Генерируется пустой файл
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/withdraw-generation/3/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/withdraw-generation/3/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void generateWithdrawFilesForTransferredCycle() throws IOException {
        runInExternalTransaction(() -> withdrawFileGenerationService.generateFiles(100501), false);

        ArgumentCaptor<StreamContentProvider> captorData = ArgumentCaptor.forClass(StreamContentProvider.class);
        ArgumentCaptor<String> captorFileName = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mdsS3Service, times(1)).uploadFile(captorFileName.capture(), captorData.capture());

        List<String> fileNames = captorFileName.getAllValues();
        List<StreamContentProvider> allValues = captorData.getAllValues();
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            InputStream fileData = allValues.get(i).getInputStream();
            excelAssertion.assertXlsx(fileData, WITHDRAW_GENERATION_PATH + "3/" + fileName);
        }

        verifyNoMoreInteractions(deliveryParams);
        verifyZeroInteractions(dataCampClient);

        softly.assertThat(fileNames).hasSize(1);
        softly.assertThat(allValues).hasSize(1);
    }

    /**
     * У поставщика три UtilizationCycle в CREATED, FINALIZED и TRANSFERRED статусах.
     * Под утилизацию есть стоки для складов:
     * 172 склад - EXPIRED, DEFECT
     * 171 склад - EXPIRED
     * Генерируется три файла
     * UtilizationCycleState в DEPRECATED статусе, для него файл не перегенеривается
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/withdraw-generation/4/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/withdraw-generation/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void generateWithdrawFilesForFinalizedTransferredAndCreatedCycles() throws IOException {
        MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse paramsResponse = buildMappingResponse();
        when(deliveryParams.searchFulfilmentSskuParams(any())).thenReturn(paramsResponse);
        when(dataCampClient.searchOffers(anyLong(), any()))
                .thenReturn(createOfferResponse(Map.of(SHOP_SKU_1, 100L, SHOP_SKU_3, 50L)));

        runInExternalTransaction(() -> withdrawFileGenerationService.generateFiles(100500), false);

        ArgumentCaptor<StreamContentProvider> captorData = ArgumentCaptor.forClass(StreamContentProvider.class);
        ArgumentCaptor<String> captorFileName = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mdsS3Service, times(3)).uploadFile(captorFileName.capture(), captorData.capture());

        List<String> fileNames = captorFileName.getAllValues();
        List<StreamContentProvider> allValues = captorData.getAllValues();
        for (int i = 0; i < fileNames.size(); i++) {
            String fileName = fileNames.get(i);
            InputStream fileData = allValues.get(i).getInputStream();
            excelAssertion.assertXlsx(fileData, WITHDRAW_GENERATION_PATH + "4/" + fileName);
        }

        verify(deliveryParams).searchFulfilmentSskuParams(any());
        softly.assertThat(fileNames).hasSize(3);
        softly.assertThat(allValues).hasSize(3);
    }

    private MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse buildMappingResponse() {

        return MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse.newBuilder()
                .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                        .setSupplierId(SUPPLIER_ID_100500.intValue())
                        .setShopSku(SHOP_SKU_1)
                        .setMarketSkuId(MARKET_SKU_1)
                        .setShopTitle(TITLE_SKU_1)
                        .setMskuTitle(MARKET_NAME_1)
                        .build())
                .addFulfilmentInfo(MboMappingsForDelivery.OfferFulfilmentInfo.newBuilder()
                        .setSupplierId(SUPPLIER_ID_100501.intValue())
                        .setShopSku(SHOP_SKU_3)
                        .setMarketSkuId(MARKET_SKU_3)
                        .setShopTitle(TITLE_SKU_3)
                        .setMskuTitle(MARKET_NAME_3)
                        .build())
                .build();
    }
}
