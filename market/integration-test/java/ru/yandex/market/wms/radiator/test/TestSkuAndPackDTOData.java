package ru.yandex.market.wms.radiator.test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import ru.yandex.market.wms.radiator.dto.MiniPackDTO;
import ru.yandex.market.wms.radiator.dto.SkuAndMiniPackDTO;
import ru.yandex.market.wms.radiator.dto.SkuDTO;
import ru.yandex.market.wms.radiator.dto.SkuMiniPackIdentitiesDto;

// data correspond to stocks.xml
public interface TestSkuAndPackDTOData {

    static SkuMiniPackIdentitiesDto mSkuAutoGetStocksTest() {
        var dto = new SkuMiniPackIdentitiesDto();
        populate_mSkuAutoGetStocksTest(dto);

        var pack = new MiniPackDTO();
        pack.setPackkey("P_ROV0000000000000000331");
        pack.setHeightuom3(34d);
        pack.setLengthuom3(12d);
        pack.setWidthuom3(23d);
        dto.setPack(pack);

        return dto;
    }

    static SkuMiniPackIdentitiesDto mSkuIdentifiedTest() {
        var dto = new SkuMiniPackIdentitiesDto();
        populate_mSkuIdentified(dto);

        var pack = new MiniPackDTO();
        pack.setPackkey("P_ROV0000000000000000531");
        pack.setHeightuom3(34d);
        pack.setLengthuom3(12d);
        pack.setWidthuom3(23d);
        dto.setPack(pack);

        return dto;
    }

    private static void populate_mSkuAutoGetStocksTest(SkuDTO dto) {
        dto.setPackkey("P_ROV0000000000000000331");
        dto.setSerialkey(1);
        dto.setSku("ROV0000000000000000331");
        dto.setManufacturersku("AUTO_GET_STOCKS_TEST");
        dto.setStorerkey("1559");
        dto.setStdgrosswgt(new BigDecimal("3.00000"));
        dto.setStdnetwgt(new BigDecimal("3.00000"));
        dto.setTare(new BigDecimal("0.00000"));
        dto.setShelflifeindicator("Y");
        dto.setToexpiredays(365);
        dto.setDescr("AUTO_GET_STOCKS_TEST");
        dto.setSusr1(null);
        dto.setSusr4("90");
        dto.setSusr5("180");
        dto.setShelflifeonreceivingPercentage(50);
        dto.setShelflifePercentage(25);
//        dto.setShelflifeEditDate(OffsetDateTime.parse("2007-12-01T10:15:30+03:00"));
    }

    private static void populate_mSkuIdentified(SkuDTO dto) {
        dto.setPackkey("P_ROV0000000000000000531");
        dto.setSerialkey(5);
        dto.setSku("ROV0000000000000000531");
        dto.setManufacturersku("REF_IDENTITES");
        dto.setStorerkey("1559");
        dto.setStdgrosswgt(new BigDecimal("3.00000"));
        dto.setStdnetwgt(new BigDecimal("3.00000"));
        dto.setTare(new BigDecimal("0.00000"));
        dto.setShelflifeindicator("Y");
        dto.setToexpiredays(365);
        dto.setDescr("REF_IDENTITES");
        dto.setSusr1(null);
        dto.setSusr4("90");
        dto.setSusr5("180");
        dto.setShelflifeonreceivingPercentage(50);
        dto.setShelflifePercentage(25);
//        dto.setShelflifeEditDate(OffsetDateTime.parse("2007-12-01T10:15:30+03:00"));
    }


    static SkuMiniPackIdentitiesDto mSkuRefMultibox() {
        var dto = new SkuMiniPackIdentitiesDto();
        populate_mSkuRefMultibox(dto);

        var pack = new MiniPackDTO();
        pack.setPackkey("P_ROV0000000000000000421");
        pack.setHeightuom3(30d);
        pack.setLengthuom3(10d);
        pack.setWidthuom3(20d);
        dto.setPack(pack);

        return dto;
    }

    private static void populate_mSkuRefMultibox(SkuDTO dto) {
        dto.setPackkey("P_ROV0000000000000000421");
        dto.setSerialkey(2);
        dto.setSku("ROV0000000000000000421");
        dto.setManufacturersku("REF_MULTIBOX");
        dto.setStorerkey("1559");
        dto.setStdgrosswgt(new BigDecimal("4.4000000000"));
        dto.setStdnetwgt(new BigDecimal("4.4000000000"));
        dto.setTare(new BigDecimal("0E-10"));
        dto.setShelflifeindicator("N");
        dto.setToexpiredays(0);
        dto.setDescr(null);
        dto.setSusr1("2");
        dto.setSusr4(null);
        dto.setSusr5(null);
        dto.setShelflifeonreceivingPercentage(null);
        dto.setShelflifePercentage(null);
        dto.setShelflifeEditDate(null);
    }
}
