package ru.yandex.market.mbo.db.modelstorage.description.sanitizer;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.ModelDescriptionPreprocessor;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.utils.MboAssertions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelDescriptionSanitizerTest {
    private static final long INPUT_UID = 12345L;
    private static final long CHANGE_UID = 54321L;
    private static final Date INPUT_DATE = date("2018/01/01");

    private ModelDescriptionPreprocessor descriptionPreprocessor;

    @Before
    public void setUp() throws Exception {
        descriptionPreprocessor = new ModelDescriptionPreprocessor();
    }

    @Test
    public void sanitizeWithoutDescription() {
        CommonModel model = model(null);
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .notExists();
    }

    @Test
    public void sanitizeEmpty() {
        CommonModel model = model("");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modificationUserId(INPUT_UID)
            .modificationDate(INPUT_DATE)
            .values("");
    }

    @Test
    public void sanitizeNormalText() {
        CommonModel model = model("normal text");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("normal text")
            .modificationUserId(INPUT_UID);
    }

    @Test
    public void sanitizeTextWithSingleNewLine() {
        CommonModel model = model("new\nline");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("new<br />line")
            .modificationUserId(CHANGE_UID);
    }

    @Test
    public void sanitizeTextWithDoubleNewLine() {
        CommonModel model = model("new\n \n line");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("new<p>line</p>")
            .modificationUserId(CHANGE_UID);
    }

    @Test
    public void sanitizeTextWithSeveralNewLines() {
        CommonModel model = model("new\n\nline \n  \n\n end of line");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("new<p>line</p><p>end of line</p>")
            .modificationUserId(CHANGE_UID);
    }

    @Test
    public void sanitizeTextWithSeveralDoubleNewLines() {
        CommonModel model = model("hello\n\ndarkness\n\nmy old friend\n\n");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("hello<p>darkness</p><p>my old friend</p>")
            .modificationUserId(CHANGE_UID);
    }

    @Test
    public void sanitizeTextWithNotTrimedLines() {
        CommonModel model = model("  \nnew line\t ");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("new line")
            .modificationUserId(CHANGE_UID);
    }

    @Test
    public void sanitizeTextWithNotAllowedTags() {
        CommonModel model = model("some<i>not</a> allowed text");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("somenot allowed text")
            .modificationUserId(CHANGE_UID);
    }

    @Test
    public void sanitizeWillSetModificationSourceChangeDateAndUserId() {
        CommonModel model = model("test <a>");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("test")
            .modificationUserId(CHANGE_UID);
    }

    @Test
    public void sanitizeAllowedBElement() {
        CommonModel model = model("allowed br tags are: <br />");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("allowed br tags are: <br />")
            .modificationUserId(INPUT_UID);
    }

    @Test
    public void sanitizeAllowedPElements() {
        CommonModel model = model("allowed p tags are: <p>, </p>, <p>paragraph</p>");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("allowed p tags are: <p>, </p>, <p>paragraph</p>")
            .modificationUserId(INPUT_UID);
    }

    @Test
    public void sanitizeComplexText() {
        CommonModel model = model("<p>complex text</p>\n" +
            "First line will be ok<br>" +
            "Second will be with forbidden tag: <a href='http://urls'>picture<a><br>" +
            "Third will be with not closed paragraph <p> and with end of line\n" +
            "Fourth will be the last and with trimming spaces   ");
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("<p>complex text</p>\n" +
                "First line will be ok<br />" +
                "Second will be with forbidden tag: picture<br />" +
                "Third will be with not closed paragraph <p> and with end of line\n" +
                "Fourth will be the last and with trimming spaces</p>");
    }

    @Test
    public void testSomeMnemonicSymbols() {
        // https://ru.wikipedia.org/wiki/Мнемоники_в_HTML
        CommonModel model = model(
            "< &lt; &#60;" +
                "> &gt; &#62;" +
                "& &amp; &#38;" +
                "\" &quot; &#34;" +
                "♠ &spades; &#9824;"
        );
        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values(
                "&lt; &lt; &lt;" +
                "&gt; &gt; &gt;" +
                "&amp; &amp; &amp;" +
                "&#34; &#34; &#34;" +
                "♠ ♠ ♠");
    }

    @Test
    public void testNoSanitizeIfDescriptionDidntChange() {
        CommonModel before = model("Illegal tag: <a>");
        CommonModel after = model("Illegal tag: <a>");

        descriptionPreprocessor.sanitize(before, after, CHANGE_UID);

        MboAssertions.assertThat(after, XslNames.DESCRIPTION)
            .values("Illegal tag: <a>")
            .modificationUserId(INPUT_UID)
            .isEqualTo(before.getParameterValues(XslNames.DESCRIPTION));
    }

    @Test
    public void sanitizeIfDescriptionChanged() {
        CommonModel before = model("Illegal tag: <a>");
        CommonModel after = model("Illegal tag: <a>!");

        descriptionPreprocessor.sanitize(before, after, CHANGE_UID);

        MboAssertions.assertThat(after, XslNames.DESCRIPTION)
            .values("Illegal tag: !")
            .modificationUserId(CHANGE_UID)
            .isNotEqualTo(before.getParameterValues(XslNames.DESCRIPTION));
    }

    @Test // MBO-15387#1534361916000
    public void sanitizeRealText() {
        CommonModel model = model(
            "Мягкие \"ПАВы\" в составе геля деликатно очищают детские принадлежности от засохшей каши" +
                " или сока, не теряя эффективности даже в холодной воде. За счёт своего натурального состава" +
                " средство заботится о маминых руках: не раздражает и не сушит кожу. <p/>\n" +
            "Дозатор на флакончике позволяет выдавить нужное количество средства.<p/>");

        descriptionPreprocessor.sanitize(null, model, CHANGE_UID);

        MboAssertions.assertThat(model, XslNames.DESCRIPTION)
            .values("Мягкие &#34;ПАВы&#34; в составе геля деликатно очищают детские принадлежности от засохшей каши" +
                " или сока, не теряя эффективности даже в холодной воде. За счёт своего натурального состава" +
                " средство заботится о маминых руках: не раздражает и не сушит кожу. <p>\n" +
                "Дозатор на флакончике позволяет выдавить нужное количество средства.</p><p></p>")
            .modificationUserId(CHANGE_UID);
    }

    private CommonModel model(String description) {
        CommonModel model = new CommonModel();
        if (description != null) {
            ParameterValue value = new ParameterValue(123, XslNames.DESCRIPTION, Param.Type.STRING,
                ParameterValue.ValueBuilder.newBuilder().setStringValue(new Word(Word.DEFAULT_LANG_ID, description)));
            ParameterValues parameterValues = ParameterValues.of(value);
            parameterValues.setLastModificationInfo(ModificationSource.OPERATOR_FILLED, INPUT_UID, INPUT_DATE);
            model.putParameterValues(parameterValues);
        }
        return model;
    }

    private static Date date(String date) {
        try {
            return new SimpleDateFormat("yyyy/MM/dd").parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
