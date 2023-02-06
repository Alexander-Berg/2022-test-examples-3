package ru.yandex.market.fulfillment.wrap.marschroute.model.converter.util;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschroutePhones;
import ru.yandex.market.logistic.api.model.fulfillment.Phone;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

class MarschroutePhonesFactoryTest extends BaseIntegrationTest {

    private final MarschroutePhonesFactory factory = new MarschroutePhonesFactory();

    @MethodSource("data")
    @ParameterizedTest
    void test(List<Phone> phones, MarschroutePhones expected) {
        MarschroutePhones actual = factory.create(phones);

        softly.assertThat(actual.getMobilePhone())
            .isEqualTo(expected.getMobilePhone());

        softly.assertThat(actual.getAnotherPhone())
            .isEqualTo(expected.getAnotherPhone());
    }

    @Nonnull
    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                Collections.singletonList(new Phone("79234567890", null)),
                new MarschroutePhones("79234567890", null)

            ),
            Arguments.of(
                Collections.singletonList(new Phone("+79234567890", null)),
                new MarschroutePhones("79234567890", null)
            ),
            Arguments.of(
                Collections.singletonList(new Phone("+7(923)4567890", null)),
                new MarschroutePhones("79234567890", null)
            ),
            Arguments.of(
                Collections.singletonList(new Phone("+7(923) 456 78 90", null)),
                new MarschroutePhones("79234567890", null)
            ),
            Arguments.of(
                Collections.singletonList(new Phone("+7(923)-456-78-90", null)),
                new MarschroutePhones("79234567890", null)
            ),
            Arguments.of(
                Collections.singletonList(new Phone("+7(923)-456-78-90", null)),
                new MarschroutePhones("79234567890", null)
            ),
            Arguments.of(
                ImmutableList.of(
                    new Phone("+7(923)-456-78-90", null),
                    new Phone("+7(925)-455-75-95", null)
                ),
                new MarschroutePhones("79234567890", "79254557595")
            ),
            Arguments.of(
                ImmutableList.of(
                    new Phone("+7(923)-456-78-90", null),
                    new Phone("+7(487)-451-73-21", null)
                ),
                new MarschroutePhones("79234567890", "74874517321")
            ),
            Arguments.of(
                ImmutableList.of(
                    new Phone("+7(923)-456-78-90", null),
                    new Phone("+7(487)-451-73-21", "312")
                ),
                new MarschroutePhones("79234567890", "74874517321 доп. 312")
            ),
            Arguments.of(
                Collections.singletonList(new Phone("+7(487)-451-73-21", "312")),
                new MarschroutePhones("74874517321 доп. 312", null)
            ),
            Arguments.of(
                Collections.singletonList(new Phone("+7(495)-123-45-67", null)),
                new MarschroutePhones("74951234567", null)
            ),
            Arguments.of(
                ImmutableList.of(
                    new Phone("+7(495)-123-45-67", null),
                    new Phone("+7(495)-987-65-43", null))
                , new MarschroutePhones("74951234567", "74959876543")
            ),
            Arguments.of(
                ImmutableList.of(
                    new Phone("+7(495)-123-45-67", null),
                    new Phone("+7(495)-987-65-43", null),
                    new Phone("+7(495)-555-55-55", null)
                ),
                new MarschroutePhones("74951234567", "74959876543")
            )
        );
    }
}
