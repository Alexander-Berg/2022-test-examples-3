package ru.yandex.market.tpl.core.domain.receipt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.receipt.FiscalReceiptStatus;
import ru.yandex.market.tpl.api.model.receipt.ReceiptAgentType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptFfdVersion;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemDiscountType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemMarkingCodeStatus;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemMeasurementUnit;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemPaymentType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemTaxType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptItemType;
import ru.yandex.market.tpl.api.model.receipt.ReceiptTaxSystem;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
public class ReceiptDataRepositoryTest {

    private final ReceiptDataRepository receiptDataRepository;
    private final ReceiptFiscalDataRepository fiscalDataRepository;

    @Test
    void saveReceiptDataFilled() {
        saveReceiptData(filledData());
    }

    @Test
    void saveReceiptDataEmpty() {
        saveReceiptData(emptyData());
    }

    @Test
    void saveReceiptFiscalDataFilled() {
        ReceiptData data = saveReceiptData(emptyData());
        saveReceiptFiscalData(filledFiscalData(data));
    }

    @Test
    void saveReceiptFiscalDataEmpty() {
        ReceiptData data = saveReceiptData(emptyData());
        saveReceiptFiscalData(emptyFiscalData(data));
    }

    ReceiptData saveReceiptData(ReceiptData data) {
        long id = receiptDataRepository.save(data).getId();
        receiptDataRepository.flush();
        ReceiptData result;
        assertThat(result = receiptDataRepository.getOne(id))
                .isEqualToIgnoringGivenFields(data, "id", "createdAt", "updatedAt");
        return result;
    }

    void saveReceiptFiscalData(ReceiptFiscalData data) {
        long id = fiscalDataRepository.save(data).getId();
        fiscalDataRepository.flush();
        assertThat(fiscalDataRepository.getOne(id))
                .isEqualToIgnoringGivenFields(data, "id", "createdAt", "updatedAt");
    }

    static ReceiptFiscalData filledFiscalData(ReceiptData receiptData) {
        var data = new ReceiptFiscalData();
        data.setReceiptData(receiptData);
        data.setFiscalRequestTime(Instant.ofEpochMilli(0L));
        data.setShiftNum("23846");
        data.setDocumentNum("232048");
        data.setFnSn("324972349");
        data.setFp("2347223487");
        data.setFd("324862384623");
        data.setDt(LocalDate.of(1990, 1, 11).atStartOfDay());
        data.setKktRn("4958732847");
        data.setKktSn("328462384");
        data.setOfdDesc("Яндекс ОФД");
        data.setOfdStatus(true);
        data.setTaxVat18(BigDecimal.TEN);
        data.setTotal(BigDecimal.valueOf(10000, 2));
        data.setAddress("Льва Толстого, 16");
        return data;
    }

    static ReceiptFiscalData emptyFiscalData(ReceiptData receiptData) {
        var data = new ReceiptFiscalData();
        data.setReceiptData(receiptData);
        data.setFiscalRequestTime(Instant.ofEpochMilli(0L));
        data.setDt(LocalDate.of(1990, 1, 11).atStartOfDay());
        data.setTotal(BigDecimal.valueOf(12312.3123));
        data.setFnSn("23423423");
        data.setFp("86876");
        data.setFd("43876534");
        return data;
    }

    public static ReceiptData filledData() {
        return filledData(ReceiptDataType.CHARGE);
    }

    public static ReceiptData filledData(ReceiptDataType type, String customerName) {
        var data = filledData(type);
        data.setCustomerName(customerName);
        return data;
    }

    public static ReceiptData filledData(ReceiptDataType type) {
        return filledData("my_receipt_id", type, filledClient());
    }
    public static ReceiptData filledData(String receiptId,
                                         ReceiptDataType type,
                                         ReceiptServiceClient client) {
        var data = new ReceiptData();
        data.setFfdVersion(ReceiptFfdVersion.FFD_1_05);
        data.setServiceClient(client);
        data.setReceiptId(receiptId);
        data.setType(type);
        data.setStatus(FiscalReceiptStatus.ERROR);
        data.setErrorDescription("Error receiving fiscal data");
        data.setItems(List.of(filledItem("товар 1"), filledItem("товар 2"), emptyItem("товар 3")));
        data.setPayload(Map.of(
                "objectA", Map.of(
                        "fieldAA", "abc",
                        "fieldAB", 2342
                ),
                "numberB", 23432,
                "textC", "2347293",
                "arrayD", List.of("1", "2", "3")
        ));
        data.setCashAmount(BigDecimal.TEN);
        data.setTaxType(ReceiptTaxSystem.OSN);
        data.setAgentType(ReceiptAgentType.PAYMENT_BANK_AGENT);
        data.setCustomerName("Иван Иванов");
        data.setCustomerInn("3249239847");
        data.setCustomerPhone("923874923874");
        data.setCustomerEmail("abc@abc.ru");
        data.setCashierName("Петр Петров");
        data.setCashierInn("239847239");
        data.setPaymentPlace("Льва Толстого, 16");
        data.setAdditionalReceiptDetails("my additional details");
        return data;
    }

    static ReceiptData emptyData() {
        var data = new ReceiptData();
        data.setServiceClient(emptyClient());
        data.setStatus(FiscalReceiptStatus.OK);
        data.setReceiptId("my_receipt_id");
        data.setType(ReceiptDataType.INCOME);
        data.setItems(List.of(emptyItem("товар 1"), emptyItem("товар 2")));
        data.setCardAmount(BigDecimal.TEN);
        return data;
    }

    private static ReceiptDataItem filledItem(String name) {
        var item = new ReceiptDataItem();
        item.setName(name);
        item.setPrice(BigDecimal.valueOf(34875, 2));
        item.setQuantity(BigDecimal.valueOf(34723, 4));
        item.setTax(ReceiptItemTaxType.VAT18);
        item.setType(ReceiptItemType.AGENT_FEE);
        item.setPaymentType(ReceiptItemPaymentType.CREDIT_AFTER_DELIVERY);
        item.setUnit("кг");
        item.setDiscountType(ReceiptItemDiscountType.AMOUNT);
        item.setDiscountValue(BigDecimal.valueOf(2, 1));
        item.setAgentItemType(ReceiptAgentType.NONE_AGENT);
        item.setSupplierInn("234234234");
        item.setSupplierName("ООО Поставщик");
        item.setSupplierPhoneNumber(List.of("+70001234567"));
        item.setNomenclatureCode("239472394");
        item.setCountryCode("32487234");
        item.setDeclarationNumber("23947234");
        item.setExciseAmount("3224");
        item.setAdditionalDetail("my additional detail");
        item.setUnitValue("213");
        item.setMeasurementUnit(ReceiptItemMeasurementUnit.KILOGRAM);
        item.setMarkingCodeStatus(ReceiptItemMarkingCodeStatus.PIECES_SOLD);
        item.setMarkingFractionalQuantity("1/3");
        item.setCisValues(List.of("cisciscis"));
        item.setCisFullValues(List.of("cisFull\\u001DcisFull\\u001DcisFull"));
        return item;
    }

    private static ReceiptDataItem emptyItem(String name) {
        var item = new ReceiptDataItem();
        item.setName(name);
        item.setPrice(BigDecimal.valueOf(1231293, 3));
        item.setQuantity(BigDecimal.valueOf(123));
        return item;
    }

    private static ReceiptServiceClient filledClient() {
        var client = new ReceiptServiceClient("soft_logic", "23439824", ReceiptProcessorType.TEST);
        client.setEmail("client-email@email.com");
        client.setName("my client name");
        client.setPhone("+7-(client)-phone");
        client.setUrl("http://www.client.url");
        return client;
    }

    private static ReceiptServiceClient emptyClient() {
        return new ReceiptServiceClient("soft_logic", "23439824", ReceiptProcessorType.TEST);
    }

}
