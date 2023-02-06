package ru.yandex.market.api.cpa.yam.validation;

import org.junit.Test;

import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.SignatoryDocType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link ValidSignatoryValidator}.
 *
 * @author avetokhin 02/04/17.
 */
public class ValidSignatoryValidatorTest {

    private final ValidSignatoryValidator validator = new ValidSignatoryValidator();

    private static PrepayRequest request(final OrganizationType organizationType,
                                         final SignatoryDocType signatoryDocType,
                                         final String signatoryDocInfo,
                                         final String signatoryPosition) {
        final PrepayRequest request = new PrepayRequest(1L, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.INIT, 3L);
        request.setOrganizationType(organizationType);
        request.setSignatoryDocType(signatoryDocType);
        request.setSignatoryDocInfo(signatoryDocInfo);
        request.setSignatoryPosition(signatoryPosition);
        return request;
    }

    private static PrepayRequest request(final OrganizationType organizationType,
                                         final SignatoryDocType signatoryDocType) {
        return request(organizationType, signatoryDocType, null, "Director");
    }

    @Test
    public void testValid() {
        assertThat(validator.isValid(request(OrganizationType.OOO, SignatoryDocType.POA), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.ZAO, SignatoryDocType.POA), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.AO, SignatoryDocType.POA), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.IP, SignatoryDocType.POA), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.CHP, SignatoryDocType.POA), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.OTHER, SignatoryDocType.POA), null), equalTo(true));

        assertThat(validator.isValid(request(OrganizationType.OOO, SignatoryDocType.OTHER, "test", "test"), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.ZAO, SignatoryDocType.OTHER, "test", "test"), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.AO, SignatoryDocType.OTHER, "test", "test"), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.OTHER, SignatoryDocType.OTHER, "test", "test"), null), equalTo(true));

        assertThat(validator.isValid(request(OrganizationType.OOO, SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.ZAO, SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.AO, SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.OTHER, SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.IP, SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.CHP, SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(true));

        assertThat(validator.isValid(request(OrganizationType.IP, SignatoryDocType.OTHER, "test", "test"), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.CHP, SignatoryDocType.OTHER, "test", "test"), null), equalTo(true));
        assertThat(validator.isValid(request(OrganizationType.OOO, SignatoryDocType.ORDER), null), equalTo(true));
    }

    @Test
    public void testNotValid() {
        assertThat(validator.isValid(null, null), equalTo(false));
        assertThat(validator.isValid(request(null, null), null), equalTo(false));

        assertThat(validator.isValid(request(OrganizationType.NONE, SignatoryDocType.OTHER, "test", "test"), null), equalTo(false));
        assertThat(validator.isValid(request(OrganizationType.NONE, SignatoryDocType.AOA_OR_ENTREPRENEUR, "test", "test"), null), equalTo(false));
        assertThat(validator.isValid(request(OrganizationType.NONE, SignatoryDocType.POA, "test", "test"), null), equalTo(false));

        assertThat(validator.isValid(request(OrganizationType.OOO, SignatoryDocType.OTHER), null), equalTo(false));
        assertThat(validator.isValid(request(OrganizationType.ZAO, SignatoryDocType.OTHER), null), equalTo(false));
        assertThat(validator.isValid(request(OrganizationType.OTHER, SignatoryDocType.OTHER), null), equalTo(false));
        assertThat(validator.isValid(request(OrganizationType.IP, SignatoryDocType.ORDER), null), equalTo(false));

    }

}
