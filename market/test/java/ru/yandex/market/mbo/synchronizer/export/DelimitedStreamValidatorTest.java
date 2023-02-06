package ru.yandex.market.mbo.synchronizer.export;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.market.Magics;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.synchronizer.export.vendor.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 05.03.2018
 */
public class DelimitedStreamValidatorTest {

    private DelimitedStreamValidator<MboParameters.Word> validator;

    @Rule
    public ExpectedException failure = ExpectedException.none();

    @Before
    public void before() {
        validator = new DelimitedStreamValidator<MboParameters.Word>(MboParameters.Word.class) {
            @Override
            protected void checkMessage(MboParameters.Word message) throws ValidationException {
                require(MboParameters.Word.NAME_FIELD_NUMBER);
                require(MboParameters.Word.LANG_ID_FIELD_NUMBER);
                uniq(MboParameters.Word.NAME_FIELD_NUMBER);

                if (!message.getName().trim().equals(message.getName())) {
                    throw new ValidationException("name has spaces");
                }

                if (message.getLangId() != Word.DEFAULT_LANG_ID) {
                    throw new ValidationException("only russian words are excepted");
                }
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    public void validationCalled() throws IOException, ExportFileValidationException {
        Set<String> names = new HashSet<>();
        DelimitedStreamValidator<MboParameters.Word> vallidatorCollectsNames
            = new DelimitedStreamValidator<MboParameters.Word>(MboParameters.Word.class, Magics.MagicConstants.MBOC) {
            @Override
            public void checkMessage(MboParameters.Word message) {
                names.add(message.getName());
            }
        };

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MboParameters.Word.Builder builder = MboParameters.Word.newBuilder()
            .setLangId(Word.DEFAULT_LANG_ID);

        ExporterUtils.writeMagic(Magics.MagicConstants.MBOC, output);

        builder.setName("red").build().writeDelimitedTo(output);
        builder.setName("green").build().writeDelimitedTo(output);
        builder.setName("blue").build().writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        vallidatorCollectsNames.validateStream(input);

        assertThat(names).containsExactly("red", "green", "blue");
    }

    @Test
    public void validationOk() throws ExportFileValidationException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MboParameters.Word.Builder builder = MboParameters.Word.newBuilder()
            .setLangId(Word.DEFAULT_LANG_ID)
            .setName("text-name");

        builder.build().writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        assertThat(validator.validateStream(input)).isTrue();
    }

    @Test
    public void noRequiredField() throws IOException, ExportFileValidationException {
        failure.expect(ExportFileValidationException.class);
        failure.expectMessage("message [1] is invalid: missed required field 'name'");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MboParameters.Word.Builder builder = MboParameters.Word.newBuilder()
            .setLangId(Word.DEFAULT_LANG_ID);

        builder.setName("has-name").build().writeDelimitedTo(output);
        builder.clearName().build().writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        validator.validateStream(input);
    }

    @Test
    public void duplicationField() throws IOException, ExportFileValidationException {
        failure.expect(ExportFileValidationException.class);
        failure.expectMessage("message [2] is invalid: name has duplicate value 'first-name'");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MboParameters.Word.Builder builder = MboParameters.Word.newBuilder()
            .setLangId(Word.DEFAULT_LANG_ID);

        builder.setName("first-name").build().writeDelimitedTo(output);
        builder.setName("second-name").build().writeDelimitedTo(output);
        builder.setName("first-name").build().writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        validator.validateStream(input);
    }

    @Test
    public void genericValidationFailed() throws IOException, ExportFileValidationException {
        failure.expect(ExportFileValidationException.class);
        failure.expectMessage("message [1] is invalid: only russian words are excepted");

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MboParameters.Word.Builder builder = MboParameters.Word.newBuilder()
            .setLangId(Word.DEFAULT_LANG_ID);

        builder.setName("red").build().writeDelimitedTo(output);
        builder.setName("green").setLangId(Word.DEFAULT_LANG_ID + 1).build().writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        validator.validateStream(input);
    }

    @Test
    public void customDescribe() throws IOException, ExportFileValidationException {
        failure.expect(ExportFileValidationException.class);
        failure.expectMessage("message [0] { name='red' } is invalid: wrong message");

        DelimitedStreamValidator<MboParameters.Word> validatorWithCustomDescribe
            = new DelimitedStreamValidator<MboParameters.Word>(MboParameters.Word.class) {
            @Override
            public void checkMessage(MboParameters.Word message) throws ValidationException {
                throw new ValidationException("wrong message");
            }

            @Override
            protected String description(MboParameters.Word word) {
                return "name='" + word.getName() + "'";
            }
        };

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MboParameters.Word.newBuilder()
            .setLangId(Word.DEFAULT_LANG_ID)
            .setName("red")
            .build()
            .writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        validatorWithCustomDescribe.validateStream(input);
    }

    @Test
    public void failedOnDescription() throws IOException, ExportFileValidationException {
        failure.expect(ExportFileValidationException.class);
        failure.expectMessage("message [0] is invalid: wrong message");

        DelimitedStreamValidator<MboParameters.Word> validatorWithCustomDescribe
            = new DelimitedStreamValidator<MboParameters.Word>(MboParameters.Word.class) {
            @Override
            public void checkMessage(MboParameters.Word message) throws ValidationException {
                throw new ValidationException("wrong message");
            }

            @Override
            protected String description(MboParameters.Word word) {
                throw new RuntimeException("failed describe");
            }
        };

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        MboParameters.Word.newBuilder()
            .setLangId(Word.DEFAULT_LANG_ID)
            .build()
            .writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        validatorWithCustomDescribe.validateStream(input);
    }

    @Test
    public void checkMagicFailed() throws IOException, ExportFileValidationException {
        failure.expect(ExportFileValidationException.class);
        failure.expectMessage("expected magic MBOC, got MBOM");

        DelimitedStreamValidator<MboParameters.Word> validatorWithMagic
            = new DelimitedStreamValidator<MboParameters.Word>(MboParameters.Word.class, Magics.MagicConstants.MBOC) {
            @Override
            public void checkMessage(MboParameters.Word message) {
                // pass
            }
        };

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ExporterUtils.writeMagic(Magics.MagicConstants.MBOM, output);

        MboParameters.Word.newBuilder()
            .build()
            .writeDelimitedTo(output);

        InputStream input = new ByteArrayInputStream(output.toByteArray());

        validatorWithMagic.validateStream(input);
    }
}
