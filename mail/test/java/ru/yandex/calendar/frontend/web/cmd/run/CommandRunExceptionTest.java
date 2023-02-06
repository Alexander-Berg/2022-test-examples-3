package ru.yandex.calendar.frontend.web.cmd.run;

import org.jdom.Element;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class CommandRunExceptionTest {
    @Test
    public void toXml() {
        CommandRunException e = CommandRunException.createSituation("wtf", Situation.BUSY_OVERLAP);

        Element xml = e.getRootElement();

        Assert.A.equals("command-run-error", xml.getName());

        Assert.assertTrue(xml.getChild("reason").getText().endsWith("wtf"));

        Assert.A.equals(String.valueOf(Situation.BUSY_OVERLAP.getCode()), xml.getAttribute("situation-code").getValue());
    }

} //~
