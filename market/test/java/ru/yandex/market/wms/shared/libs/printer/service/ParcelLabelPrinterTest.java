package ru.yandex.market.wms.shared.libs.printer.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.wms.common.model.enums.OrderType;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.shared.libs.label.printer.dao.ParcelLabelDataDao;
import ru.yandex.market.wms.shared.libs.label.printer.domain.dto.ParcelLabelPrinterData;
import ru.yandex.market.wms.shared.libs.label.printer.service.printer.DefaultParcelLabelPrinter;
import ru.yandex.market.wms.shared.libs.utils.time.DateTimeUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParcelLabelPrinterTest {

    @Mock
    private DbConfigService configService;

    @InjectMocks
    private DefaultParcelLabelPrinter defaultParcelLabelPrinter;

    @Test
    void getMacrosMapDefault() {
        when(configService.getConfigAsBoolean(eq("PACKING_LABEL_HIDE_CARRIER"), anyBoolean())).thenReturn(false);
        Map<String, String> macrosMap = defaultParcelLabelPrinter.toMap(
                getParcelDataSample(null), "****-123");
        assertEquals(getParcelMacrosMapSample(), macrosMap);
    }

    @Test
    void getMacrosMapHiddenCarrierRegexNotAffect() {
        when(configService.getConfigAsBoolean(eq("PACKING_LABEL_HIDE_CARRIER"), anyBoolean())).thenReturn(true);
        when(configService.getConfig(eq("PACKING_LABEL_HIDE_CARRIER_PAT"), anyString()))
                .thenReturn("(Тарный|Дзержинский)");
        Map<String, String> macrosMap = defaultParcelLabelPrinter.toMap(
                getParcelDataSample(null), "****-123");
        Map<String, String> expectedMacrosMap = getParcelMacrosMapSample();
        assertEquals(expectedMacrosMap, macrosMap);
    }

    @Test
    void getMacrosMapHiddenCarrierAll() {
        Map<String, String> expectedMacrosMap = getParcelMacrosMapSample();
        expectedMacrosMap.put("$@CC_COMP_NAME@$", "");
        expectedMacrosMap.put("$@CC_COMP_NAME_FONT_WIDTH@$", "40");

        when(configService.getConfigAsBoolean(eq("PACKING_LABEL_HIDE_CARRIER"), anyBoolean())).thenReturn(true);
        Map<String, String> macrosMap = defaultParcelLabelPrinter.toMap(
                getParcelDataSample(null), "****-123");
        assertEquals(expectedMacrosMap, macrosMap);
    }

    @Test
    void getMacrosMapHiddenCarrierFirstRegex() {
        Map<String, String> expectedMacrosMap = getParcelMacrosMapSample();
        expectedMacrosMap.put("$@CC_COMP_NAME@$", "");
        expectedMacrosMap.put("$@CC_COMP_NAME_FONT_WIDTH@$", "40");

        when(configService.getConfigAsBoolean(eq("PACKING_LABEL_HIDE_CARRIER"), anyBoolean())).thenReturn(true);
        when(configService.getConfig(eq("PACKING_LABEL_HIDE_CARRIER_PAT"), anyString()))
                .thenReturn("(Тарный|Дзержинский)");

        ParcelLabelPrinterData parcelData = getParcelDataSample("МК Дзержинский КГТ");
        Map<String, String> macrosMap = defaultParcelLabelPrinter.toMap(parcelData, "****-123");
        assertEquals(expectedMacrosMap, macrosMap);
    }

    @Test
    void getMacrosMapHiddenCarrierListSecondRegex() {
        Map<String, String> expectedMacrosMap = getParcelMacrosMapSample();
        expectedMacrosMap.put("$@CC_COMP_NAME@$", "");
        expectedMacrosMap.put("$@CC_COMP_NAME_FONT_WIDTH@$", "40");

        when(configService.getConfigAsBoolean(eq("PACKING_LABEL_HIDE_CARRIER"), anyBoolean())).thenReturn(true);
        when(configService.getConfig(eq("PACKING_LABEL_HIDE_CARRIER_PAT"), anyString()))
                .thenReturn("(Тарный|Дзержинский)");

        ParcelLabelPrinterData parcelData = getParcelDataSample("МК Тарный 2 волна");
        Map<String, String> macrosMap = defaultParcelLabelPrinter.toMap(parcelData, "****-123");
        assertEquals(expectedMacrosMap, macrosMap);
    }


    private Map<String, String> getParcelMacrosMapSample() {
        Map<String, String> out = new HashMap<>();
        out.put("$@EXTERNORDERKEY@$", "3176992");
        out.put("$@C_ADDRESS1@$", "40-летия Победы пр-кт, 340");
        out.put("$@C_ADDRESS2@$", "");
        out.put("$@C_ADDRESS3@$", "");
        out.put("$@C_CITY@$", "Ростов-на-Дону");
        out.put("$@C_COMPANY@$", "Surname Name");
        out.put("$@C_STATE@$", "Ростовская область");
        out.put("$@SCHEDULEDSHIPDATE@$", "11.04.2019");
        out.put("$@CASEID@$", "Q444");
        out.put("$@PDUDF2@$", "");
        out.put("$@TOTAL@$", "");
        out.put("$@LASTBOXMESSAGE@$", "");
        out.put("$@GROSSWGT@$", "0.000");
        out.put("$@TRACE_DATA@$", "****-123");
        out.put("$@CC_COMP_NAME@$", "PickPoint Длинное название перевозчика ");
        out.put("$@CC_COMP_NAME_FONT_WIDTH@$", "14");
        return out;
    }

    /**
     * Пример ParcelLabelPrinterData для исходных данных
     * @return
     */
    private ParcelLabelPrinterData getParcelDataSample(String overrideCarrierName) {
        return ParcelLabelPrinterData.builder()
                .externOrderKey("3176992")
                .orderKey("0000001069")
                .storerName(null)
                .storerKey("1339")
                .address1("40-летия Победы пр-кт, 340")
                .address2(null)
                .address3(null)
                .city("Ростов-на-Дону")
                .zip(null)
                .state("Ростовская область")
                .scheduledshipdate(asLocalDateString(Timestamp.valueOf("2019-04-10 21:00:00.000")))
                .customerName(ParcelLabelDataDao.normalize("Surname Name"))
                .carrierName(overrideCarrierName != null ?
                        overrideCarrierName : "PickPoint Длинное название перевозчика ")
                .grossWgt(Objects.toString(BigDecimal.valueOf(0.000000).setScale(3, RoundingMode.CEILING)))
                .caseId("Q444")
                .boxNumber(null)
                .type(OrderType.asCode("0"))
                .build();
    }

    private static String asLocalDateString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(DateTimeUtils.toNullableInstant(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.uuuu"));
    }

}
