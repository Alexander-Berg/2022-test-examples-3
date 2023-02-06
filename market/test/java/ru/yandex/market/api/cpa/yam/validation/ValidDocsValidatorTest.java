package ru.yandex.market.api.cpa.yam.validation;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.cpa.yam.entity.PartnerApplicationDocumentType;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequest;
import ru.yandex.market.api.cpa.yam.entity.PrepayRequestDocument;
import ru.yandex.market.api.cpa.yam.entity.SignatoryDocType;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.core.partner.PartnerService;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Unit тесты для {@link ValidDocsValidator}.
 *
 * @author avetokhin 02/04/17.
 */
public class ValidDocsValidatorTest {

    private static final PrepayRequestDocument OTHER_DOC =
            new PrepayRequestDocument(1L, 2L, PartnerApplicationDocumentType.OTHER, "test", 1231L, "test");

    private static final PrepayRequestDocument FORM_DOC =
            new PrepayRequestDocument(1L, 2L, PartnerApplicationDocumentType.SIGNED_APP_FORM, "test", 1231L, "test");

    private static final PrepayRequestDocument SIGNATORY_DOC =
            new PrepayRequestDocument(1L, 2L, PartnerApplicationDocumentType.SIGNATORY_DOC, "test", 1231L, "test");

    private static ValidDocsValidator validator;

    @BeforeAll
    static void init() {
        validator = new ValidDocsValidator();
        validator.setPartnerService(Mockito.mock(PartnerService.class));
    }


    private static PrepayRequest requestWithDocs(final List<PrepayRequestDocument> docs,
                                                 final SignatoryDocType signatoryDocType) {
        final PrepayRequest request = new PrepayRequest(1L, PrepayType.YANDEX_MARKET, PartnerApplicationStatus.INIT,
                3L);
        request.setDocuments(docs);
        request.setSignatoryDocType(signatoryDocType);
        return request;
    }

    @Test
    public void testValid() {
        assertThat(validator.isValid(requestWithDocs(singletonList(FORM_DOC), SignatoryDocType.AOA_OR_ENTREPRENEUR),
                null), equalTo(true));
        assertThat(validator.isValid(requestWithDocs(asList(FORM_DOC, OTHER_DOC),
                SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(true));
        assertThat(validator.isValid(requestWithDocs(asList(FORM_DOC, SIGNATORY_DOC), SignatoryDocType.POA), null),
                equalTo(true));
        assertThat(validator.isValid(requestWithDocs(asList(FORM_DOC, SIGNATORY_DOC), SignatoryDocType.OTHER), null),
                equalTo(true));
    }

    @Test
    public void testNotValid() {
        assertThat(validator.isValid(null, null), equalTo(false));

        assertThat(validator.isValid(requestWithDocs(new ArrayList<>(), SignatoryDocType.POA), null), equalTo(false));
        assertThat(validator.isValid(requestWithDocs(new ArrayList<>(), SignatoryDocType.AOA_OR_ENTREPRENEUR), null),
                equalTo(false));
        assertThat(validator.isValid(requestWithDocs(new ArrayList<>(), SignatoryDocType.OTHER), null), equalTo(false));

        assertThat(validator.isValid(requestWithDocs(singletonList(OTHER_DOC), SignatoryDocType.POA), null),
                equalTo(false));
        assertThat(validator.isValid(requestWithDocs(singletonList(OTHER_DOC), SignatoryDocType.AOA_OR_ENTREPRENEUR),
                null), equalTo(false));
        assertThat(validator.isValid(requestWithDocs(singletonList(OTHER_DOC), SignatoryDocType.OTHER), null),
                equalTo(false));

        assertThat(validator.isValid(requestWithDocs(singletonList(FORM_DOC), SignatoryDocType.POA), null),
                equalTo(false));
        assertThat(validator.isValid(requestWithDocs(singletonList(FORM_DOC), SignatoryDocType.OTHER), null),
                equalTo(false));

        assertThat(validator.isValid(requestWithDocs(singletonList(SIGNATORY_DOC), SignatoryDocType.POA), null),
                equalTo(false));
        assertThat(validator.isValid(requestWithDocs(singletonList(SIGNATORY_DOC),
                SignatoryDocType.AOA_OR_ENTREPRENEUR), null), equalTo(false));
        assertThat(validator.isValid(requestWithDocs(singletonList(SIGNATORY_DOC), SignatoryDocType.OTHER), null),
                equalTo(false));
    }

}
