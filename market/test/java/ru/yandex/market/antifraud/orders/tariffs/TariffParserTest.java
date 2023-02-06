package ru.yandex.market.antifraud.orders.tariffs;

import java.io.File;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.storage.entity.ue.Tariff;


/**
 * @author dzvyagin
 */
public class TariffParserTest {

    @Test
    public void parseTariffFile() throws Exception {
        String filePath = getClass().getClassLoader().getResource("4317").getFile();
        File tariffXml = new File(filePath);
        TariffParser parser = new TariffParser();
        List<Tariff> tariffs = parser.parseTariffXml(tariffXml);
        Assertions.assertThat(tariffs).isNotEmpty();
        tariffs.forEach(System.out::println);
    }

}
