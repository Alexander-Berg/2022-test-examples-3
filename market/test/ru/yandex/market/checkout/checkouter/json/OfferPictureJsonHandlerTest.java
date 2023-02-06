package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.common.report.model.OfferPicture;

public class OfferPictureJsonHandlerTest extends AbstractJsonHandlerTestBase {

    private InputStream inputStream;

    @BeforeEach
    public void setUp() throws IOException {
        inputStream = OfferPictureJsonHandlerTest.class.getResourceAsStream("offer_picture.json");
    }

    @Test
    public void shouldParse() throws IOException {
        OfferPicture offerPicture = read(OfferPicture.class, inputStream);
        Assertions.assertEquals("//avatars.mds.yandex.net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw" +
                "/50x50", offerPicture.getUrl());
        Assertions.assertEquals(12, offerPicture.getWidth().longValue());
        Assertions.assertEquals(34, offerPicture.getHeight().longValue());
        Assertions.assertEquals(56, offerPicture.getContainerWidth().longValue());
        Assertions.assertEquals(78, offerPicture.getContainerHeight().longValue());
    }

    @Test
    public void shouldWrite() throws IOException, ParseException {
        OfferPicture offerPicture = EntityHelper.getOfferPicture();

        String json = write(offerPicture);
        checkJson(json, "$.url", EntityHelper.URL);
        checkJson(json, "$.width", EntityHelper.WIDTH);
        checkJson(json, "$.height", EntityHelper.HEIGHT);
        checkJson(json, "$.containerHeight", EntityHelper.CONTAINER_HEIGHT);
        checkJson(json, "$.containerWidth", EntityHelper.CONTAINER_WIDTH);
    }

}
