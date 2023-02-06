package ru.yandex.market.partner.mvc.controller.offer.mapping.shopsku;

import java.io.StringWriter;
import java.time.DayOfWeek;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.offer.dto.TimePeriodDTO;
import ru.yandex.market.core.offer.dto.TimeUnitDTO;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.mvc.controller.misc.DayOfWeekDTO;

/**
 * Тест на корректность генерации xml
 */
class MasterDataDTOTest {

    @Test
    void testPeriodsXmlGeneration() throws Exception {
        TimePeriodDTO shelfLife = new TimePeriodDTO(5, TimeUnitDTO.DAY);
        shelfLife.setComment("shelfLifeComment");
        TimePeriodDTO lifeTime = new TimePeriodDTO(5, TimeUnitDTO.DAY);
        lifeTime.setComment("lifeTimeComment");
        TimePeriodDTO guaranteePeriod = new TimePeriodDTO(5, TimeUnitDTO.DAY);
        guaranteePeriod.setComment("guaranteePeriod");
        MasterDataDTO masterDataDTO = new MasterDataDTO.Builder()
                .setSupplyScheduleDays(Collections.singletonList(DayOfWeekDTO.fromDayOfWeek(DayOfWeek.MONDAY)))
                .setShelfLife(shelfLife)
                .setLifeTime(lifeTime)
                .setGuaranteePeriod(guaranteePeriod)
                .build();

        StringWriter stringWriter = new StringWriter();
        new ApiObjectMapperFactory().createXmlMapper().writeValue(stringWriter, masterDataDTO);

        String actual = stringWriter.toString();
        String expectedXml = StringTestUtil.getString(
                this.getClass(),
                "expected.xml"
        );
        MbiAsserts.assertXmlEquals(expectedXml, actual);
    }
}
