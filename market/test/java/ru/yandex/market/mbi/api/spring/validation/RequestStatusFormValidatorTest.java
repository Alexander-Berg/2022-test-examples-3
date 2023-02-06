package ru.yandex.market.mbi.api.spring.validation;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.validation.Errors;

import ru.yandex.market.api.cpa.yam.dto.RequestStatusForm;
import ru.yandex.market.core.application.PartnerApplicationStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit тесты для {@link RequestStatusFormValidator}.
 *
 * @author avetokhin 22/03/17.
 */
public class RequestStatusFormValidatorTest {

    private final RequestStatusFormValidator validator = new RequestStatusFormValidator();

    private static RequestStatusForm status(final PartnerApplicationStatus status, final String comment, final Long dsId) {
        return new RequestStatusForm(status, comment, dsId);
    }

    @Test
    public void testValid() {
        final Errors errors = mockErrors();

        validator.validate(status(PartnerApplicationStatus.IN_PROGRESS, null, null), errors);
        validator.validate(status(PartnerApplicationStatus.COMPLETED, null, null), errors);
        validator.validate(status(PartnerApplicationStatus.FROZEN, null, 1L), errors);
        validator.validate(status(PartnerApplicationStatus.CLOSED, null, 1L), errors);
        validator.validate(status(PartnerApplicationStatus.CANCELLED, null, 1L), errors);
        validator.validate(new RequestStatusForm(PartnerApplicationStatus.FROZEN, null, singletonList(1L)), errors);
        validator.validate(new RequestStatusForm(PartnerApplicationStatus.CLOSED, null, singletonList(1L)), errors);
        validator.validate(new RequestStatusForm(PartnerApplicationStatus.CANCELLED, null, singletonList(1L)), errors);


        validator.validate(status(PartnerApplicationStatus.DECLINED, "COMMENT", null), errors);
        validator.validate(status(PartnerApplicationStatus.NEED_INFO, "COMMENT", null), errors);

        validator.validate(new RequestStatusForm(PartnerApplicationStatus.FROZEN, null, asList(1L, 2L)), errors);
        validator.validate(new RequestStatusForm(PartnerApplicationStatus.CLOSED, null, asList(1L, 2L)), errors);
        validator.validate(new RequestStatusForm(PartnerApplicationStatus.CANCELLED, null, asList(1L, 2L)), errors);

        assertThat(errors.hasErrors(), equalTo(false));
    }

    @Test
    public void testInvalid() {
        testError((errors -> validator.validate(status(PartnerApplicationStatus.NEW, null, null), errors)));
        testError((errors -> validator.validate(status(PartnerApplicationStatus.INIT, null, null), errors)));
        testError((errors -> validator.validate(status(PartnerApplicationStatus.INTERNAL_CLOSED, null, null), errors)));
        testError((errors -> validator.validate(status(PartnerApplicationStatus.CANCELLED, null, null), errors)));

        testError((errors -> validator.validate(status(PartnerApplicationStatus.DECLINED, null, null), errors)));
        testError((errors -> validator.validate(status(PartnerApplicationStatus.NEED_INFO, null, null), errors)));

        testError((errors -> validator.validate(status(PartnerApplicationStatus.COMPLETED, null, 1L), errors)));
        testError((errors -> validator.validate(status(PartnerApplicationStatus.IN_PROGRESS, null, 1L), errors)));
        testError((errors -> validator.validate(status(PartnerApplicationStatus.DECLINED, null, 1L), errors)));
        testError((errors -> validator.validate(status(PartnerApplicationStatus.NEED_INFO, null, 1L), errors)));

        testError((errors -> validator.validate(new RequestStatusForm(PartnerApplicationStatus.COMPLETED, null, singletonList(1L)), errors)));
        testError((errors -> validator.validate(new RequestStatusForm(PartnerApplicationStatus.IN_PROGRESS, null, singletonList(1L)), errors)));
        testError((errors -> validator.validate(new RequestStatusForm(PartnerApplicationStatus.DECLINED, null, singletonList(1L)), errors)));
        testError((errors -> validator.validate(new RequestStatusForm(PartnerApplicationStatus.NEED_INFO, null, singletonList(1L)), errors)));

    }


    private Errors mockErrors() {
        return mock(Errors.class);
    }

    private void testError(Consumer<Errors> function) {
        final Errors errors = mockErrors();
        function.accept(errors);
        verify(errors).reject(anyString(), anyString());
    }

}
