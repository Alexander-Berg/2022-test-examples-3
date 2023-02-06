package ru.yandex.market.replenishment.autoorder.service.excel;

import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.Currency;
import ru.yandex.market.replenishment.autoorder.model.VAT;
import ru.yandex.market.replenishment.autoorder.model.dto.TenderSupplierResponseForExcelDTO;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TenderSupplierResponseBaseExcelReaderTest extends FunctionalTest {

    @Test
    public void testExcelParsing_isOk() {
        TenderSupplierResponseExcelReader tenderSupplierResponseExcelReader = new TenderSupplierResponseExcelReader();
        InputStream bytes = this.getClass().getResourceAsStream("TenderSupplierResponseExcelReaderTest.base.xlsx");
        List<TenderSupplierResponseForExcelDTO> data = tenderSupplierResponseExcelReader.read(bytes);

        assertThat(data, hasSize(3));

        TenderSupplierResponseForExcelDTO item = data.get(0);
        assertNotNull(item);
        assertThat(item.getVendorName(), equalTo("Transcend"));
        assertThat(item.getCategory1(), equalTo("Накопители"));
        assertThat(item.getCategory2(), equalTo("Внешние жесткие диски и SSD"));
        assertThat(item.getCategory3(), equalTo("Внешние жесткие диски и SSD"));
        assertThat(item.getTitle(), equalTo("Внешний HDD Transcend StoreJet 25H3P 4 ТБ, фиолетовый"));
        assertThat(item.getMsku(), equalTo(100906032778L));
        assertThat(item.getSsku(), equalTo("TS4TSJ25H3P"));
        assertThat(item.getVendorCode(), equalTo("TS4TSJ25H3P"));
        assertThat(item.getBarcode(), equalTo("760557833604"));
        assertThat(item.getPurchQty(), equalTo(20));
        assertThat(item.getItems(), equalTo(20L));
        assertThat(item.getPrice(), equalTo(9951.09));
        assertThat(item.getVat(), equalTo(VAT.VAT_20));
        assertThat(item.getCurrency(), equalTo(Currency.EUR));
        assertThat(item.getComment(), equalTo("Comment 1"));


        item = data.get(1);
        assertNotNull(item);
        assertThat(item.getVendorName(), equalTo("Mirex"));
        assertThat(item.getCategory1(), equalTo("Накопители"));
        assertThat(item.getCategory2(), equalTo("USB Flash drive"));
        assertThat(item.getCategory3(), equalTo("USB Flash drive"));
        assertThat(item.getTitle(), equalTo("Флешка Mirex INTRO 8GB, стальной"));
        assertThat(item.getMsku(), equalTo(100358093750L));
        assertThat(item.getSsku(), equalTo("13600-ITRNTO08"));
        assertThat(item.getVendorCode(), equalTo("13600-ITRNTO08"));
        assertThat(item.getBarcode(), equalTo("4620001055968"));
        assertThat(item.getPurchQty(), equalTo(7));
        assertThat(item.getItems(), equalTo(7L));
        assertThat(item.getPrice(), equalTo(237.58));
        assertThat(item.getVat(), equalTo(VAT.VAT_20));
        assertThat(item.getCurrency(), equalTo(Currency.USD));
        assertThat(item.getComment(), equalTo("Comment 2"));

        item = data.get(2);
        assertNotNull(item);
        assertThat(item.getVendorName(), equalTo("A-DATA"));
        assertThat(item.getCategory1(), equalTo("Накопители"));
        assertThat(item.getCategory2(), equalTo("USB Flash drive"));
        assertThat(item.getCategory3(), equalTo("USB Flash drive"));
        assertThat(item.getTitle(), equalTo("Флешка ADATA UV240 16GB, черный"));
        assertThat(item.getMsku(), equalTo(100573868915L));
        assertThat(item.getSsku(), equalTo("AUV240-16G-RBK"));
        assertThat(item.getVendorCode(), equalTo("AUV240-16G-RBK"));
        assertThat(item.getBarcode(), equalTo("4713218465368"));
        assertThat(item.getPurchQty(), equalTo(5));
        assertThat(item.getItems(), equalTo(5L));
        assertThat(item.getPrice(), equalTo(290.77));
        assertThat(item.getVat(), equalTo(VAT.VAT_20));
        assertThat(item.getCurrency(), equalTo(Currency.RUB));
        assertThat(item.getComment(), equalTo("Comment 3"));
    }

    @Test
    public void testExcelParsing_throwsError() {
        final TenderSupplierResponseExcelReader tenderSupplierResponseExcelReader =
                new TenderSupplierResponseExcelReader();
        final InputStream bytes = this.getClass().getResourceAsStream(
                "TenderSupplierResponseExcelReaderTest.wrong.xlsx");
        assertThrows(UserWarningException.class,
                () -> tenderSupplierResponseExcelReader.read(bytes),
                "Error int value in row #11: For input string: \"0.5\".");
    }

    @Test
    public void testExcelParsing_zeroPriceAndQuantity() {
        final var reader = new TenderSupplierResponseExcelReader();
        final var bytes = this.getClass().getResourceAsStream("TenderSupplierResponseExcelReaderTest.zeroPrice.xlsx");
        var data = reader.read(bytes);

        assertThat(data, hasSize(2));
    }
}
