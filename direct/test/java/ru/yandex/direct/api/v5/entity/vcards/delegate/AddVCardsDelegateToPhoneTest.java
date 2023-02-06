package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.vcard.model.Phone;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class AddVCardsDelegateToPhoneTest {
    private static final String COUNTRY_CODE = "+7";
    private static final String CITY_CODE = "495";
    private static final String NUMBER = "1111111";

    @Parameterized.Parameter
    public com.yandex.direct.api.v5.vcards.Phone requestPhone;
    @Parameterized.Parameter(value = 1)
    public Phone expectedPhone;

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{
                        new com.yandex.direct.api.v5.vcards.Phone().withCountryCode(COUNTRY_CODE)
                                .withCityCode(CITY_CODE)
                                .withPhoneNumber(NUMBER),
                        new Phone().withCountryCode(COUNTRY_CODE)
                                .withCityCode(CITY_CODE)
                                .withPhoneNumber(NUMBER)},
                new Object[]{
                        new com.yandex.direct.api.v5.vcards.Phone().withCountryCode(COUNTRY_CODE)
                                .withCityCode(CITY_CODE)
                                .withPhoneNumber(NUMBER)
                                .withExtension("1"),
                        new Phone().withCountryCode(COUNTRY_CODE)
                                .withCityCode(CITY_CODE)
                                .withPhoneNumber(NUMBER)
                                .withExtension("1")});
    }

    @Test
    public void test() {
        Phone actualPhone = AddVCardsDelegate.toVcardPhone(requestPhone);

        assertThat(actualPhone).isEqualTo(expectedPhone);
    }
}
