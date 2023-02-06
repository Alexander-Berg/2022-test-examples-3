package ru.yandex.market.ff.model.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.dto.ShopRequestDocumentDTO;
import ru.yandex.market.ff.model.entity.ShopRequestDocument;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author kotovdv 10/08/2017.
 */
class ShopRequestDocumentConverterTest extends BaseConverterTest {

    private ShopRequestDocumentConverter converter = new ShopRequestDocumentConverter();

    @Test
    void testNullConversion() {
        ShopRequestDocumentDTO dto = converter.convert(null);
        assertThat(dto, nullValue());
    }

    @Test
    void testConversion() {
        ShopRequestDocument source = filledDocument();
        ShopRequestDocumentDTO result = converter.convert(source);
        assertRequestDocDTO(source, result);
    }
}
