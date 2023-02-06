package ru.yandex.market.partner.mvc.validation;

import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.springframework.validation.Errors;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDocumentForm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link PrepayRequestDocumentUploadValidator}.
 *
 * @author avetokhin 27/03/17.
 */
public class PrepayRequestDocumentUploadValidatorTest {

    private static final long MAX_SIZE = 5000;

    private static final Set<String> ALLOWED_TYPES = Set.of("image/JPEG", "image/png", "application/pdf");

    private final PrepayRequestDocumentUploadValidator validator =
            new PrepayRequestDocumentUploadValidator(MAX_SIZE, ALLOWED_TYPES);

    private static PrepayRequestDocumentForm dummyDoc(final String fileName, final long fileSize, @Nonnull final String mimeType) {
        final MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(fileName);
        when(file.getSize()).thenReturn(fileSize);
        when(file.getContentType()).thenReturn(mimeType);

        return new PrepayRequestDocumentForm(null, file);
    }

    @Test
    public void testValid() {
        final Errors errors = mockErrors();

        validator.validate(dummyDoc("TEST.JPG", MAX_SIZE, "image/jpeg"), errors);
        validator.validate(dummyDoc("test.jpg", MAX_SIZE - 1, "image/jpeg"), errors);
        validator.validate(dummyDoc("test.jpe", MAX_SIZE - 1, "image/jpeg"), errors);
        validator.validate(dummyDoc("test.123", MAX_SIZE - 1, "image/jpeg"), errors);
        validator.validate(dummyDoc("test.png", MAX_SIZE - 100, "image/png"), errors);
        assertThat(errors.hasErrors(), equalTo(false));
    }

    @Test
    public void testInvalidSize() {
        testError((errors -> validator.validate(dummyDoc("test.pdf", MAX_SIZE + 1, "application/pdf"), errors)));
        testError((errors -> validator.validate(dummyDoc("TEST.JPG", MAX_SIZE + 1, "image/jpeg"), errors)));
        testError((errors -> validator.validate(dummyDoc("test.png", MAX_SIZE + 1, "application/pdf"), errors)));
        testError((errors -> validator.validate(dummyDoc("test.jpg", MAX_SIZE + 100, "application/pdf"), errors)));
    }

    @Test
    public void testInvalidType() {
        testError((errors -> validator.validate(dummyDoc("TEST.xlsx", MAX_SIZE - 1, "application/xls"), errors)));
        testError((errors -> validator.validate(dummyDoc("TEST.txt", MAX_SIZE - 1, "text/html"), errors)));
        testError((errors -> validator.validate(dummyDoc("TEST.png", MAX_SIZE - 1, "text/javascript"), errors)));
        testError((errors -> validator.validate(dummyDoc("TEST.abc", MAX_SIZE - 1, "abc/cde"), errors)));
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
