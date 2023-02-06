package ru.yandex.market.wms.common.spring.service.unit;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.model.enums.CounterName;
import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.enums.ParcelFormat;
import ru.yandex.market.wms.common.spring.service.CounterService;
import ru.yandex.market.wms.common.spring.service.NamedCounterService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.spring.service.NamedCounterService.DEFAULT_WAREHOUSE_PREFIX;
import static ru.yandex.market.wms.common.spring.service.NamedCounterService.FORMAT_7_DIGITS;
import static ru.yandex.market.wms.common.spring.service.NamedCounterService.WAREHOUSE_PREFIX_CONFIG_NAME;
import static ru.yandex.market.wms.common.spring.service.NamedCounterService.YM_ENABLE_WH_PREFIX_CONTAINERS;

public class NamedCounterServiceTest extends BaseTest {

    private NamedCounterService namedCounterService;
    private CounterService counterService;
    private DbConfigService dbConfigService;

    @BeforeEach
    public void setup() {
        super.setup();

        counterService = mock(CounterService.class);
        dbConfigService = mock(DbConfigService.class);
        when(dbConfigService.getConfig(WAREHOUSE_PREFIX_CONFIG_NAME, DEFAULT_WAREHOUSE_PREFIX))
                .thenReturn("01");
        when(dbConfigService.getConfigAsBoolean(YM_ENABLE_WH_PREFIX_CONTAINERS, false))
                .thenReturn(true);

        namedCounterService = new NamedCounterService(counterService, dbConfigService);
    }

    @Test
    public void getNextSerialNumberForLotWithThirdFromTheEndDigitIsZero() {
        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS)).thenReturn("5505393");
        String serialNumber = namedCounterService.getNextSerialNumber("0000012045");
        assertions.assertThat(serialNumber).isEqualTo("010455505393");
    }

    @Test
    public void getNextSerialNumberForLotWithZerosAtLastThreeDigits() {
        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS)).thenReturn("5505393");
        String serialNumber = namedCounterService.getNextSerialNumber("0000012000");
        assertions.assertThat(serialNumber).isEqualTo("010005505393");
    }

    @Test
    public void getNextSerialNumberForLotWithoutZerosAtLastThreeDigits() {
        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS)).thenReturn("5505393");
        String serialNumber = namedCounterService.getNextSerialNumber("0000012345");
        assertions.assertThat(serialNumber).isEqualTo("013455505393");
    }

    @Test
    public void getNextSerialNumberListForLotWithThirdFromTheEndDigitIsZero() {
        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS))
                .thenReturn("5505393")
                .thenReturn("5505394")
                .thenReturn("5505395");
        List<String> serialNumberList = namedCounterService.getNextSerialNumbers("0000012045", 3);
        assertions.assertThat(serialNumberList)
                .isEqualTo(Arrays.asList("010455505393", "010455505394", "010455505395"));
    }

    @Test
    public void getNextSerialNumberListForLotWithZerosAtLastThreeDigits() {
        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS))
                .thenReturn("5505393")
                .thenReturn("5505394")
                .thenReturn("5505395");
        List<String> serialNumber = namedCounterService.getNextSerialNumbers("0000012000", 3);
        assertions.assertThat(serialNumber).isEqualTo(Arrays.asList("010005505393", "010005505394", "010005505395"));
    }

    @Test
    public void getNextSerialNumberListForLotWithoutZerosAtLastThreeDigits() {
        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS))
                .thenReturn("5505393")
                .thenReturn("5505394")
                .thenReturn("5505395");
        List<String> serialNumber = namedCounterService.getNextSerialNumbers("0000012345", 3);
        assertions.assertThat(serialNumber).isEqualTo(Arrays.asList("013455505393", "013455505394", "013455505395"));
    }

    @Test
    public void getNextParcelIdWithLength() {
        when(dbConfigService.getConfigAsBoolean(YM_ENABLE_WH_PREFIX_CONTAINERS, false))
                .thenReturn(false);
        when(counterService.getNextCounterValue(CounterName.PARCEL_DROP_ID, ParcelFormat.TEN.getFormat()))
                .thenReturn("P987654321");
        String result = namedCounterService.getNextParcelId();

        assertions.assertThat(result).isEqualTo("P987654321");
    }

    @Test
    public void getNextParcelIdWithLengthWarehousePrefixEncbled() {
        when(counterService.getNextCounterValue(CounterName.PARCEL_DROP_ID, "P01%011d"))
                .thenReturn("P01987654321");
        String result = namedCounterService.getNextParcelId();

        assertions.assertThat(result).isEqualTo("P01987654321");
    }

    @Test
    public void getNextParcelIdWithCustomFormat() {
        when(counterService.getNextCounterValue(CounterName.PARCEL_DROP_ID, ParcelFormat.TEN.getFormat()))
                .thenReturn("P987654321");
        when(counterService.getNextCounterValue(CounterName.PARCEL_DROP_ID, ParcelFormat.FOURTEEN.getFormat()))
                .thenReturn("P9876543210123");
        when(counterService.getNextCounterValue(CounterName.PARCEL_DROP_ID, ParcelFormat.TWENTY.getFormat()))
                .thenReturn("P9876543210123456789");

        Assertions.assertAll(
                () -> Assertions.assertEquals(String.format(ParcelFormat.TEN.getFormat(), 123),
                        "P000000123"),
                () -> Assertions.assertEquals(String.format(ParcelFormat.FOURTEEN.getFormat(), 12_345_678),
                        "P0000012345678"),
                () -> Assertions.assertEquals(String.format(ParcelFormat.TWENTY.getFormat(), 12_345_678_999L),
                        "P0000000012345678999"),

                () -> Assertions.assertEquals("P987654321",
                        namedCounterService.getNextParcelId(ParcelFormat.TEN)),
                () -> Assertions.assertEquals("P9876543210123",
                        namedCounterService.getNextParcelId(ParcelFormat.FOURTEEN)),
                () -> Assertions.assertEquals("P9876543210123456789",
                        namedCounterService.getNextParcelId(ParcelFormat.TWENTY))
        );
    }

    @Test
    public void getNextSerialNumberListForLotWithThirdFromTheEndDigitIsZeroAndEightSmbNumber() {
        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS))
                .thenReturn("85505393")
                .thenReturn("85505394")
                .thenReturn("85505395");
        List<String> serialNumberList = namedCounterService.getNextSerialNumbers("0000012045", 3);
        assertions.assertThat(serialNumberList)
                .isEqualTo(Arrays.asList("010455505393", "010455505394", "010455505395"));
    }

    @Test
    public void initNamedCounterServiceWithTooLongWarehousePrefix() {
        when(dbConfigService.getConfig(WAREHOUSE_PREFIX_CONFIG_NAME, DEFAULT_WAREHOUSE_PREFIX))
                .thenReturn("012");
        assertions.assertThatThrownBy(
                () -> {
                    NamedCounterService ncs = new NamedCounterService(counterService, dbConfigService);
                    ncs.getNextParcelId();
                }
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void initNamedCounterServiceWithNonDigitalWarehousePrefix() {
        when(dbConfigService.getConfig(WAREHOUSE_PREFIX_CONFIG_NAME, DEFAULT_WAREHOUSE_PREFIX))
                .thenReturn("ab");
        assertions.assertThatThrownBy(
                () -> {
                    NamedCounterService ncs = new NamedCounterService(counterService, dbConfigService);
                    ncs.getNextParcelId();
                }
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void initNamedCounterServiceWhenNoWarehousePrefixProvided() {
        when(dbConfigService.getConfig(WAREHOUSE_PREFIX_CONFIG_NAME, DEFAULT_WAREHOUSE_PREFIX))
                .thenReturn(DEFAULT_WAREHOUSE_PREFIX);
        namedCounterService = new NamedCounterService(counterService, dbConfigService);

        when(counterService.getNextCounterValue(CounterName.SERIAL_TRANS_KEY, FORMAT_7_DIGITS)).thenReturn("5505393");
        String serialNumber = namedCounterService.getNextSerialNumber("0000012045");

        assertions.assertThat(serialNumber).isEqualTo("0455505393");
    }
}
