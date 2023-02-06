package ru.yandex.market.pvz.core.domain.configuration.legal_partner;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.configuration.ConfigurationProviderSource;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.tpl.common.util.configuration.ConfigurationProvider;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ConfigurationLegalPartnerCommandServiceTest {

    private static final Long ID = 12345L;
    private static final String KEY = "KEY";
    private static final String VALUE = "54321";

    private final TestLegalPartnerFactory testLegalPartnerFactory;

    private final ConfigurationProviderSource configurationProviderSource;
    private final ConfigurationLegalPartnerCommandService configurationLegalPartnerCommandService;

    @Test
    void testNonExistent() {
        assertThat(configurationProviderSource.getForLegalPartner(ID).isBooleanEnabled(KEY)).isFalse();
    }

    @Test
    void testSetBooleanTrue() {
        LegalPartner partner = testLegalPartnerFactory.createLegalPartner();

        configurationLegalPartnerCommandService.setValue(partner.getId(), KEY, true);

        assertThat(configurationProviderSource.getForLegalPartner(partner.getId()).isBooleanEnabled(KEY)).isTrue();
    }

    @Test
    void testSetAndThenReset() {
        LegalPartner partner = testLegalPartnerFactory.createLegalPartner();

        configurationLegalPartnerCommandService.setValue(partner.getId(), KEY, true);
        configurationLegalPartnerCommandService.setValue(partner.getId(), KEY, false);

        assertThat(configurationProviderSource.getForLegalPartner(partner.getId()).isBooleanEnabled(KEY)).isFalse();
    }

    @Test
    void testSetOnlyForOnePartner() {
        LegalPartner partner1 = testLegalPartnerFactory.createLegalPartner();
        LegalPartner partner2 = testLegalPartnerFactory.createLegalPartner();

        configurationLegalPartnerCommandService.setValue(partner1.getId(), KEY, VALUE);

        assertThat(configurationProviderSource.getForLegalPartner(partner1.getId()).getValue(KEY)).hasValue(VALUE);
        assertThat(configurationProviderSource.getForLegalPartner(partner2.getId()).getValue(KEY)).isEmpty();
    }

    @Test
    void testSetTheSameValuesForDifferentPartners() {
        LegalPartner partner1 = testLegalPartnerFactory.createLegalPartner();
        LegalPartner partner2 = testLegalPartnerFactory.createLegalPartner();

        configurationLegalPartnerCommandService.setValue(partner1.getId(), KEY, VALUE);
        configurationLegalPartnerCommandService.setValue(partner2.getId(), KEY, VALUE + 1);

        assertThat(configurationProviderSource.getForLegalPartner(partner1.getId()).getValue(KEY)).hasValue(VALUE);
        assertThat(configurationProviderSource.getForLegalPartner(partner2.getId()).getValue(KEY)).hasValue(VALUE + 1);
    }

    @Test
    void testSetDifferentValuesForOnePartner() {
        LegalPartner partner = testLegalPartnerFactory.createLegalPartner();

        configurationLegalPartnerCommandService.setValue(partner.getId(), KEY, VALUE);
        configurationLegalPartnerCommandService.setValue(partner.getId(), KEY + "1", VALUE + "1");

        assertThat(configurationProviderSource.getForLegalPartner(partner.getId()).getValue(KEY)).hasValue(VALUE);
        assertThat(configurationProviderSource.getForLegalPartner(partner.getId()).getValue(KEY + "1")).hasValue(VALUE + "1");
        assertThat(configurationProviderSource.getForLegalPartner(partner.getId()).getValue(KEY + "2")).isEmpty();
    }

    @Test
    void testGetValueInDifferentFormats() {
        LegalPartner partner = testLegalPartnerFactory.createLegalPartner();

        configurationLegalPartnerCommandService.setValue(partner.getId(), KEY, "12345");

        ConfigurationProvider configurationProvider = configurationProviderSource.getForLegalPartner(partner.getId());
        assertThat(configurationProvider.getValue(KEY)).hasValue("12345");
        assertThat(configurationProvider.getValueAsInteger(KEY)).hasValue(12345);
        assertThat(configurationProvider.getValueAsDouble(KEY)).hasValue(12345.0);
        assertThat(configurationProvider.getValueAsLong(KEY)).hasValue(12345L);
    }
}
