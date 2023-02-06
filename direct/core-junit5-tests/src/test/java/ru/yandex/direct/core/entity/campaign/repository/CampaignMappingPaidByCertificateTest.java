package ru.yandex.direct.core.entity.campaign.repository;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.dbschema.ppc.enums.CampaignsPaidByCertificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class CampaignMappingPaidByCertificateTest {

    static Stream<Arguments> variants() {
        return Stream.of(
                arguments(Boolean.FALSE, CampaignsPaidByCertificate.No),
                arguments(Boolean.TRUE, CampaignsPaidByCertificate.Yes),
                arguments(null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("variants")
    public void testToDbFormat(Boolean modelStatus, CampaignsPaidByCertificate dbStatus) {
        assertThat(CampaignMappings.paidByCertificateToDb(modelStatus))
                .describedAs("Конвертация типа в формат базы должна быть однозначной")
                .isEqualTo(dbStatus);
    }

    @ParameterizedTest
    @MethodSource("variants")
    public void testFromDbFormat(Boolean modelStatus, CampaignsPaidByCertificate dbStatus) {
        assertThat(CampaignMappings.paidByCertificateFromDb(dbStatus))
                .describedAs("Конвертация типа в формат модели должна быть однозначной")
                .isEqualTo(modelStatus);
    }
}
