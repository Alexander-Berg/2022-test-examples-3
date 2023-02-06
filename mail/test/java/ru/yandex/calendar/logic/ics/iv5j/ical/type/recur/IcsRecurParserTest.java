package ru.yandex.calendar.logic.ics.iv5j.ical.type.recur;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function;
import ru.yandex.bolts.function.Function2;
import ru.yandex.calendar.util.exception.ExceptionUtils;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
@RunWith(Parameterized.class)
public class IcsRecurParserTest {
    private static final Logger logger = LoggerFactory.getLogger(IcsRecurParserTest.class);

    @Parameters
    public static ListF<Object[]> inputs() {
        ListF<String> correct = Cf.list(
                "FREQ=WEEKLY;COUNT=5",
                "FREQ=MONTHLY;UNTIL=20111111;BYMONTHDAY=7,13,21",
                "FREQ=WEEKLY;UNTIL=20111111T111111Z;BYWEEKNO=1,13,53;INTERVAL=3;",
                "FREQ=YEARLY;INTERVAL=2;BYMONTH=6;BYDAY=MO,22TU,-37FR;BYHOUR=8,9,11,23,15;BYMINUTE=30"
        );
        ListF<String> incorrect = Cf.list(
                "COUNT=7", // freq is required
                "INTERVAL=3,X=25", // unknown property
                "FREQ=WEEKLY; COUNT=5", // spaces should not occur
                "FREQ=DAILY;BYWEEKNO=59", // value out of range
                "FREQ=DAILY;BYHOUR=13,-7", // value should be positive
                "FREQ=DAILY;BYDAY=MO;BYDAY=TU", // duplicate property name
                "FREQ=DAILY;UNTIL=20111111T111111X", // invalid time format
                "FREQ=DAILY;UNTIL=20111111;COUNT=3" // both until and count occur
        );
        return correct.zipWith(Function.constF(true))
                .plus(incorrect.zipWith(Function.constF(false)))
                .map(new Function2<String, Boolean, Object[]>() {
                    public Object[] apply(String input, Boolean correct) {
                        return new Object[] { input, correct };
                    }
                });
    }

    private final String input;
    private final boolean correct;

    public IcsRecurParserTest(String input, boolean correct) {
        this.input = input;
        this.correct = correct;
    }

    @Test
    public void parse() {
        IcsRecur icsRecur;
        try {
            icsRecur = IcsRecurParser.P.parseRecur(input);
        } catch (RuntimeException e) {
            Throwable unwrapped = ExceptionUtils.unwrapReflection(e);
            if (correct ||
                !(unwrapped instanceof NumberFormatException) &&
                !(unwrapped instanceof IllegalArgumentException) &&
                !(unwrapped instanceof NoSuchElementException))
            {
                logger.warn("Unexpected exception", e);
                Assert.fail("Unexpected exception was thrown while parse " + input);
            }
            return;
        }
        Assert.isTrue(correct, "Expected exception was not thrown while parse " + input);

        ListF<IcsRecurRulePart> parts = icsRecur.getParts();
        Assert.sameSize(input.split(";"), parts);
        Assert.equals(Cf.set(input.split(";")), Cf.toSet(parts.map(Function.toStringF())));
    }

} //~
