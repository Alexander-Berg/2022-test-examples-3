package ru.yandex.autotests.innerpochta.tests.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.tests.pq.PqData.MailBoxPqTable;
import ru.yandex.autotests.innerpochta.tests.pq.PqData.MailLabelsPqTable;

import static ch.lambdaj.Lambda.*;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.innerpochta.tests.pq.PqData.MailLabelsPqTable.mailLabelsTableInfoFromPq;
import ch.ethz.ssh2.Connection;
/**
 * User: alex89
 * Date: 21.03.14
 */


public class PqLabelMatcher extends TypeSafeMatcher<MailBoxPqTable> {
    private String errorMessage;

    private String labelName;
    private Matcher labelTypeMatcher;
    private Matcher labelColorMatcher;

    public PqLabelMatcher(String labelName, Matcher labelTypeMatcher, Matcher labelColorMatcher) {
        this.labelName = labelName;
        this.labelTypeMatcher = labelTypeMatcher;
        this.labelColorMatcher = labelColorMatcher;

    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean matchesSafely(MailBoxPqTable mailBoxPqTable) {
        if (select(mailLabelsTableInfoFromPq(mailBoxPqTable.getConnection(), mailBoxPqTable.getUid(), labelName),
                allOf(having(on(MailLabelsPqTable.class).getName(), equalTo(labelName)),
                        having(on(MailLabelsPqTable.class).getType(), (Matcher<String>) labelTypeMatcher))).size()
                == 0) {
            errorMessage = format("В таблице mail.labels не нашли искомую метку. " +
                    "У письма в таблице mail.box есть такие метки:%s", mailBoxPqTable.getLids());
            return false;
        }

        MailLabelsPqTable labelDataInMailLabelsTable = select(
                mailLabelsTableInfoFromPq(mailBoxPqTable.getConnection(), mailBoxPqTable.getUid(), labelName),
                allOf(having(on(MailLabelsPqTable.class).getName(), equalTo(labelName)),
                        having(on(MailLabelsPqTable.class).getType(), (Matcher<String>) labelTypeMatcher))
        ).get(0);

        if (!hasItem(labelDataInMailLabelsTable.getLid()).matches(mailBoxPqTable.getLidsList())) {
            errorMessage = format("В таблице mail.labels нашли искомую метку, но она к письму не добавилась!" +
                    "У письма в таблице mail.box есть такие метки:%s", mailBoxPqTable.getLids());
            return false;
        }

        if (hasItem(labelDataInMailLabelsTable.getLid()).matches(mailBoxPqTable.getLidsList()) &&
                labelTypeMatcher.matches(labelDataInMailLabelsTable.getType())
                && labelColorMatcher.matches(labelDataInMailLabelsTable.getColor())) {
            return true;
        }

        errorMessage = String.format("В таблице mail.labels нашли искомую метку, к письму она добавилась " +
                        "c неверными свойствами: type=%s, color=%s.",
                labelDataInMailLabelsTable.getType(), labelDataInMailLabelsTable.getColor());

        return false;
    }

    @Override
    protected void describeMismatchSafely(MailBoxPqTable mailBoxPqTable, Description description) {
        description.appendText(errorMessage);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(format("Ожидаем увидеть на письме метку с именем %s, type =%s, color=%s.",
                labelName, labelTypeMatcher, labelColorMatcher));
    }

    public static PqLabelMatcher hasLabelWithProperties(String labelName, Matcher<String> labelTypeMatcher,
                                                        Matcher labelColorMatcher) {
        return new PqLabelMatcher(labelName, labelTypeMatcher, labelColorMatcher);
    }

    public static PqLabelMatcher hasLabelWithProperties(int labelName, Matcher<String> labelTypeMatcher,
                                                        Matcher labelColorMatcher) {
        return new PqLabelMatcher(String.format("%d",labelName), labelTypeMatcher, labelColorMatcher);
    }
}
