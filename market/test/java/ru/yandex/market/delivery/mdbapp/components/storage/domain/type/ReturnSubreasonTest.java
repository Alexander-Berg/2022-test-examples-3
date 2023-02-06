package ru.yandex.market.delivery.mdbapp.components.storage.domain.type;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.integration.converter.EnumConverter;

class ReturnSubreasonTest extends AbstractTest {

    private final EnumConverter enumConverter = new EnumConverter();

    @ParameterizedTest
    @EnumSource(
        value = ru.yandex.market.checkout.checkouter.returns.ReturnSubreason.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    void checkouterToMdb(ru.yandex.market.checkout.checkouter.returns.ReturnSubreason source) {
        softly.assertThat(enumConverter.convert(source, ReturnSubreason.class))
            .isNotNull()
            .isNotEqualTo(ReturnSubreason.UNKNOWN);
    }

    @ParameterizedTest
    @EnumSource(
        value = ReturnSubreason.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = "UNKNOWN"
    )
    void mdbToLrm(ReturnSubreason source) {
        softly.assertThat(enumConverter.convert(
            source,
            ru.yandex.market.logistics.mdb.lrm.client.model.ReturnSubreason.class
        ))
            .isNotNull()
            .isNotEqualTo(ReturnSubreason.UNKNOWN);
    }

}
