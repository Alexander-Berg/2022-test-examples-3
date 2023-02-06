package ru.yandex.common.util.commandline;

import junit.framework.TestCase;
import ru.yandex.common.util.ApplicationUtil;

/**
 * Date: Mar 16, 2009
 *
 * @author Alexander Astakhov (leftie@yandex-team.ru)
 */
public class CommandLineToolsTest extends TestCase {

    public void testHostName() {
        final String hostName = ApplicationUtil.getHostName();
        assertEquals(hostName, CommandLineTools.executeInShell("hostname").trim());
    }
}
