package ru.yandex.direct.api.v5.entity.sitelinks.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

@ParametersAreNonnullByDefault
public class DeleteRequestConverterNullArgumentTest {

    private DeleteRequestConverter converter = new DeleteRequestConverter();

    @Test(expected = NullPointerException.class)
    public void shouldThrowNpeIfRequestIsNull() {
        converter.convert(null);
    }

}
