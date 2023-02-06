package ru.yandex.market.logistic.gateway.service.util;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.personal.enums.PersonalDataType;

public class PersonalUtilsTest extends BaseTest {

    private static final String PHONE_ID = "phoneId";
    private static final String PHONE_VALUE = "phoneValue";

    @Test
    public void successGetValue() {
        String value = PersonalUtils.getValue(createIdToValueMap(), PHONE_ID, PersonalDataType.PHONE, String.class);
        assertions.assertThat(value).isEqualTo(PHONE_VALUE);
    }

    @Test
    public void successGetValueSubType() {
        Object value = PersonalUtils.getValue(createIdToValueMap(), PHONE_ID, PersonalDataType.PHONE, Object.class);
        assertions.assertThat(value).isEqualTo(PHONE_VALUE);
    }

    @Test
    public void errorTryToGetWrongType() {
        assertions.assertThatThrownBy(() -> {
                PersonalUtils.getValue(createIdToValueMap(), PHONE_ID, PersonalDataType.PHONE, Integer.class);
            })
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Can not get class Integer from personalDataType PHONE");
    }

    private Map<Pair<String, PersonalDataType>, Object> createIdToValueMap() {
        return Map.ofEntries(
            Map.entry(Pair.of(PHONE_ID, PersonalDataType.PHONE), PHONE_VALUE)
        );
    }
}
