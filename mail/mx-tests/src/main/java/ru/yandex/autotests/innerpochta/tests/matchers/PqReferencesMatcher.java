package ru.yandex.autotests.innerpochta.tests.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import ru.yandex.autotests.innerpochta.tests.pq.PqData.MailMessageReferencesPqTable;

import java.util.List;

import static ch.lambdaj.Lambda.*;
import static java.lang.String.format;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * User: alex89
 * Date: 21.03.14
 */

public class PqReferencesMatcher extends TypeSafeMatcher<List<MailMessageReferencesPqTable>> {
    private String errorMessage;

    private String refValue;
    private String refType;

    public PqReferencesMatcher(String refValue, String refType) {
        this.refValue = refValue;
        this.refType = refType;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean matchesSafely(List<MailMessageReferencesPqTable> referencesInfo) {
        if (select(referencesInfo,
                having(on(MailMessageReferencesPqTable.class).getValue(), equalTo(refValue))).size() == 0) {
            errorMessage = format("В таблице mail.message_references %s не нашли value:%s", referencesInfo, refValue);
            return false;
        }

        List<MailMessageReferencesPqTable> foundRefNotationWithDefiniteValue =
                select(referencesInfo,
                        having(on(MailMessageReferencesPqTable.class).getValue(), equalTo(refValue)));
        int amountOfRefNotationsWithDefiniteValueAndType = select(foundRefNotationWithDefiniteValue,
                having(on(MailMessageReferencesPqTable.class).getType(), equalTo(refType))).size();
        if (amountOfRefNotationsWithDefiniteValueAndType == 0) {
            errorMessage = format("В таблице mail.message_references %s не нашли value:%s c type %s",
                    referencesInfo, refValue, refType);
            return false;
        }

        if (amountOfRefNotationsWithDefiniteValueAndType != 1) {
            errorMessage = format("В таблице mail.message_references %s нашли value:%s c type %s более одной",
                    referencesInfo, refValue, refType);
            return false;
        }
        return true;
    }

    @Override
    protected void describeMismatchSafely(List<MailMessageReferencesPqTable> table, Description description) {
        description.appendText(errorMessage);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(format("Ожидаем увидеть запись о reference с value=%s, type =%s.",
                refValue, refType));
    }

    public static PqReferencesMatcher hasReferenceNotationWithValueAndType(String refValue, String refType) {
        return new PqReferencesMatcher(refValue, refType);
    }
}