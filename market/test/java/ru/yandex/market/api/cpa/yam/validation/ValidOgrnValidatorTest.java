package ru.yandex.market.api.cpa.yam.validation;

import org.junit.Test;

import ru.yandex.market.api.cpa.yam.dto.OrganizationInfoDTO;
import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link ValidOgrnValidator}.
 *
 * @author avetokhin 16/05/17.
 */
public class ValidOgrnValidatorTest {

    private static final String TOO_SHORT = "12345";
    private static final String TOO_LONG = "01234567891112233";
    private static final String INVALID_SYMBOL_IP = "01234567891112A";
    private static final String INVALID_SYMBOL_ORG = "012345678911C";
    private static final String VALID_IP = "012345678911122";
    private static final String VALID_ORG = "0123456789111";
    private static final String INVALID_ZEROS_ORG = "0000000000000";
    private static final String INVALID_ZEROS_IP = "000000000000000";

    private final ValidOgrnValidator validator = new ValidOgrnValidator();

    @Test
    public void test() {
        // Пустые значения.
        assertThat(validate(null, null), equalTo(true));
        assertThat(validate(OrganizationType.NONE, ""), equalTo(true));
        assertThat(validate(OrganizationType.OOO, null), equalTo(true));
        assertThat(validate(OrganizationType.ZAO, ""), equalTo(true));
        assertThat(validate(OrganizationType.OTHER, null), equalTo(true));
        assertThat(validate(OrganizationType.IP, ""), equalTo(true));
        assertThat(validate(OrganizationType.CHP, ""), equalTo(true));

        // Валидный кейз для организаций.
        assertThat(validate(null, VALID_ORG), equalTo(true));
        assertThat(validate(OrganizationType.NONE, VALID_ORG), equalTo(true));
        assertThat(validate(OrganizationType.OOO, VALID_ORG), equalTo(true));
        assertThat(validate(OrganizationType.ZAO, VALID_ORG), equalTo(true));
        assertThat(validate(OrganizationType.AO, VALID_ORG), equalTo(true));
        assertThat(validate(OrganizationType.OTHER, VALID_ORG), equalTo(true));

        // Валидный кейз для ИП.
        assertThat(validate(OrganizationType.IP, VALID_IP), equalTo(true));
        assertThat(validate(OrganizationType.CHP, VALID_IP), equalTo(true));

        // Невалидный кейз для организаций.
        assertThat(validate(null, TOO_SHORT), equalTo(false));
        assertThat(validate(OrganizationType.NONE, TOO_LONG), equalTo(false));
        assertThat(validate(OrganizationType.OOO, INVALID_SYMBOL_ORG), equalTo(false));
        assertThat(validate(OrganizationType.ZAO, VALID_IP), equalTo(false));
        assertThat(validate(OrganizationType.AO, VALID_IP), equalTo(false));
        assertThat(validate(OrganizationType.OTHER, INVALID_SYMBOL_IP), equalTo(false));
        assertThat(validate(OrganizationType.AO, VALID_IP), equalTo(false));
        assertThat(validate(OrganizationType.AO, INVALID_ZEROS_ORG), equalTo(false));
        assertThat(validate(OrganizationType.AO, INVALID_ZEROS_IP), equalTo(false));

        // Невалидный кейз для ИП.
        assertThat(validate(OrganizationType.IP, TOO_SHORT), equalTo(false));
        assertThat(validate(OrganizationType.CHP, INVALID_SYMBOL_IP), equalTo(false));
        assertThat(validate(OrganizationType.IP, VALID_ORG), equalTo(false));
        assertThat(validate(OrganizationType.IP, INVALID_ZEROS_IP), equalTo(false));
        assertThat(validate(OrganizationType.IP, INVALID_ZEROS_ORG), equalTo(false));
    }

    private boolean validate(final OrganizationType type, final String ogrn) {
        final OrganizationInfoDTO organizationInfo = OrganizationInfoDTO.builder()
                .type(type)
                .ogrn(ogrn)
                .build();

        return validator.isValid(organizationInfo, null);
    }

}
