package ru.yandex.market.api.partner.controllers.price.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.api.partner.log.impl.APIRequestDBLogger;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.jaxb.jackson.XmlNamingStrategy;

@ParametersAreNonnullByDefault
class OfferPriceControllerBatchSizeTest {

    @Test
    void smokeTestPaginationXml() throws Exception {
        ObjectMapper xmlMapper = new ApiObjectMapperFactory().createXmlMapper(new XmlNamingStrategy());
        OfferPriceListDTO list = new OfferPriceListDTO();
        for (int i = 0; i < OfferPriceListDTO.MAX_REQUEST_SIZE; i++) {
            OfferPriceDTO offerPrice = new OfferPriceDTO();
            offerPrice.setFeedId(1000000L);
            offerPrice.setId("offer" + i);
            offerPrice.setUpdatedAt(Instant.now().atOffset(ZoneOffset.UTC));
            PriceDTO price = new PriceDTO();
            price.setValue(BigDecimal.valueOf(100000099, 2));
            price.setDiscountBase(BigDecimal.valueOf(200000099, 2));
            price.setCurrencyId("RUR");
            offerPrice.setPrice(price);
            list.getOfferPrices().add(offerPrice);
        }
        CountingOutputStream stream = new CountingOutputStream(new NullOutputStream());
        xmlMapper.writeValue(stream, list);
        Assertions.assertThat(stream.getCount())
                .isLessThan(APIRequestDBLogger.MAX_REQUEST_LENGTH);
    }
}
