package ru.yandex.market.core.outlet.legal;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.orginfo.exception.IncompleteFieldsException;
import ru.yandex.market.core.orginfo.exception.InvalidRegNumException;
import ru.yandex.market.core.orginfo.model.OrganizationType;
import ru.yandex.market.core.outlet.OutletLegalInfo;

/**
 * Тесты для {@link OutletLegalInfoValidator}.
 */
class OutletLegalInfoValidatorTest extends FunctionalTest {

    @Autowired
    private OutletLegalInfoValidator outletLegalInfoValidator;

    private static Stream<PartnerId> partnerIds() {
        return Stream.of(PartnerId.datasourceId(1L), PartnerId.fmcgId(1L));
    }

    @Test
    @DbUnitDataSet(before = "dropshipNotAllRequiredFieldsTest.before.csv")
    void dropshipNotAllRequiredFieldsTest() {
        OutletLegalInfo outletLegalInfo = new OutletLegalInfo.Builder()
                .setOutletId(1L)
                .build();
        IncompleteFieldsException exception = Assertions.assertThrows(
                IncompleteFieldsException.class,
                () -> outletLegalInfoValidator.validateFields(PartnerId.datasourceId(1L), outletLegalInfo)
        );
        Assertions.assertEquals("Following fields should be specified in outlet legal info: " +
                        "[organization type, organization name, registration number, juridical address, fact address]",
                exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("partnerIds")
    void notAllRequiredFieldsTest(final PartnerId partnerId) {
        OutletLegalInfo outletLegalInfo = new OutletLegalInfo.Builder()
                .setOutletId(1L)
                .build();
        IncompleteFieldsException exception = Assertions.assertThrows(
                IncompleteFieldsException.class,
                () -> outletLegalInfoValidator.validateFields(partnerId, outletLegalInfo)
        );
        Assertions.assertEquals("Following fields should be specified in outlet legal info: " +
                        "[organization type, organization name, registration number, juridical address]",
                exception.getMessage());
    }

    @Test
    void incorrectRegistrationNumber() {
        OutletLegalInfo outletLegalInfo = new OutletLegalInfo.Builder()
                .setOutletId(1L)
                .setOrganizationType(OrganizationType.OOO)
                .setOrganizationName("Name")
                .setRegistrationNumber("123")
                .setJuridicalAddress("Addr1")
                .setFactAddress("Addr2")
                .build();
        InvalidRegNumException exception = Assertions.assertThrows(
                InvalidRegNumException.class,
                () -> outletLegalInfoValidator.validateFields(PartnerId.datasourceId(1L), outletLegalInfo)
        );
        Assertions.assertEquals("Invalid registration number: 123", exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("partnerIds")
    void fullCorrectInfo(final PartnerId partnerId) {
        OutletLegalInfo outletLegalInfo = new OutletLegalInfo.Builder()
                .setOutletId(1L)
                .setOrganizationType(OrganizationType.OOO)
                .setOrganizationName("Name")
                .setRegistrationNumber("5068946887312")
                .setJuridicalAddress("Addr1")
                .setFactAddress("Addr2")
                .build();
        outletLegalInfoValidator.validateFields(partnerId, outletLegalInfo);
    }

}