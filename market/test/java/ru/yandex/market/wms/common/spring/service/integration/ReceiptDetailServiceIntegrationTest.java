package ru.yandex.market.wms.common.spring.service.integration;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ItemLabelType;
import ru.yandex.market.wms.common.spring.dto.ReceiptSkuDto;
import ru.yandex.market.wms.common.spring.enums.ReceivingItemType;
import ru.yandex.market.wms.common.spring.exception.AltSkuNotFoundException;
import ru.yandex.market.wms.common.spring.exception.ReceiptNotFoundException;
import ru.yandex.market.wms.common.spring.exception.UitFromAnotherOrderException;
import ru.yandex.market.wms.common.spring.exception.UitFromAnotherReturnException;
import ru.yandex.market.wms.common.spring.service.receiptDetails.ReceiptDetailService;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReceiptDetailServiceIntegrationTest extends ReceivingIntegrationTest {

    @Autowired
    private ReceiptDetailService receiptDetailService;

    @Test
    public void serviceCreatedWithoutExternalDependencies() {
        receiptDetailService.toString();
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-uit.xml")
    void getReceiptDetailsByBarcodeUit() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("1", "3", null);
        assertions.assertThat(detailsByBarcode.size()).isEqualTo(1);
        assertions.assertThat(detailsByBarcode)
                .hasOnlyOneElementSatisfying(dto -> {
                    assertions.assertThat(dto.getReceiptKey()).isEqualTo("1");
                    assertions.assertThat(dto.getLineNumber()).isEqualTo("2");
                    assertions.assertThat(dto.getLabelScanned()).isEqualTo(ItemLabelType.UIT);
                    assertions.assertThat(dto.getCreationLot()).isEqualTo("0000500350");
                });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-uit.xml")
    void getReceiptDetailsByBarcodeUitReturns() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("2", "4", "CART036");
        assertions.assertThat(detailsByBarcode.size()).isEqualTo(1);
        assertions.assertThat(detailsByBarcode)
                .hasOnlyOneElementSatisfying(dto -> {
                    assertions.assertThat(dto.getReceiptKey()).isEqualTo("2");
                    assertions.assertThat(dto.getLineNumber()).isEqualTo("1");
                    assertions.assertThat(dto.getLabelScanned()).isNull();
                    assertions.assertThat(dto.getLot()).isEmpty();
                });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-uit.xml")
    void getReceiptDetailsByBarcodeUitReturnsDifferentBox() {
        assertThrows(UitFromAnotherReturnException.class,
                () -> receiptDetailService.getReceiptSkusByBarcode("2", "4", "CART037"));
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-uit.xml")
    void getReceiptDetailsByBarcodeUitOrdersDifferentBox() {
        assertThrows(UitFromAnotherOrderException.class,
                () -> receiptDetailService.getReceiptSkusByBarcode("3", "4", "CART036"));
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before.xml")
    void getReceiptDetailsByBarcodeNoSkusToProcess() {
        assertThrows(AltSkuNotFoundException.class,
                () -> receiptDetailService.getReceiptSkusByBarcode("0000012345", "3", null));
    }

    @Test
    void getReceiptDetailsByBarcodeNoReceiptToProcess() {
        assertThrows(ReceiptNotFoundException.class,
                () -> receiptDetailService.getReceiptSkusByBarcode(null, "3", null));
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus-no-master.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusNoMasterSkus() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("1", "1", null);
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("1");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("2");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000359");
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusNoReceiptDetailsNoReceipt() {
        assertThrows(ReceiptNotFoundException.class,
                () -> receiptDetailService.getReceiptSkusByBarcode(null, "1", null));
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusNoReceiptDetails() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("2", "2", null);
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("2");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("-1");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000362");
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusNoReceiptDetails3P() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("3", "2", null);
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("3");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("-1");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000363");
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusNoReceiptDetails3PNoSkusFound() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("3", "1", null);
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("3");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("-1");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("");
            assertions.assertThat(dto.getItemTypes().size()).isEqualTo(1);
            assertions.assertThat(dto.getItemTypes()).contains(ReceivingItemType.UNKNOWN_EAN);
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusHasReceiptDetails() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("1", "1", null);
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("1");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("1");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000360");
            assertions.assertThat(dto.getLabelScanned()).isEqualTo(ItemLabelType.EAN);
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusHasReceiptDetailsReturns() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("4", "1", "CART035");
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("4");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("1");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000360");
            assertions.assertThat(dto.getLabelScanned()).isEqualTo(ItemLabelType.EAN);
            assertions.assertThat(dto.getReturnId()).isEqualTo("RETURNID");
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusTwoReturnsWithSameUits() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("5", "4", "CART036");
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("5");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000360");
            assertions.assertThat(dto.getLabelScanned()).isNull();
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-simple-skus.xml")
    void getReceiptDetailsByBarcodeWithSimpleSkusAndBomsHasReceiptDetails() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("2", "4", null);
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("2");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("-1");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000364");
        });
    }

    @Test
    @DatabaseSetup("/db/service/receipt-detail/before-assortment-skus.xml")
    void getReceiptDetailsByBarcodeWithAssortmentSkuHasSortService() {
        List<ReceiptSkuDto> detailsByBarcode = receiptDetailService.getReceiptSkusByBarcode("1", "1", null);
        assertions.assertThat(detailsByBarcode).hasOnlyOneElementSatisfying(dto -> {
            assertions.assertThat(dto.getReceiptKey()).isEqualTo("1");
            assertions.assertThat(dto.getLineNumber()).isEqualTo("2");
            assertions.assertThat(dto.getSku().getSku()).isEqualTo("ROV0000000000000000371");
        });
    }
}
