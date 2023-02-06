package ru.yandex.market.mboc.common.services.mbi;

import javax.xml.bind.JAXBException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.notifications.model.data.suppliers.ProcessedOffersData;
import ru.yandex.market.mboc.common.test.YamlTestUtil;

import static ru.yandex.market.mboc.common.services.mbi.MessageBuilder.constructProcessedOffersMessageToSupplier;

public class MessageBuilderTest {

    @Test
    public void constructMessageToSupplierMarshall() throws JAXBException {
        String data = constructProcessedOffersMessageToSupplier(YamlTestUtil
            .readFromResources("mbi/supplier-processed-offers-data.yml", ProcessedOffersData.class));

        Assertions.assertThat(data).isEqualTo(YamlTestUtil
            .readFromResources("mbi/supplier-processed-offers-xml.yml", String.class));
    }
}
