package ru.yandex.market.robot.utils;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitriy Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 24.10.13
 */
public class EncodingCheckerTest extends Assert {
    @Test
    public void testCheck() throws Exception {
        assertFalse(EncodingChecker.validate(" Moulinex Noumea 1 7L Su Isıtıcıâ€� "));

        assertFalse(EncodingChecker.validate("郋赲訄郅迮郕郋郇邽郕訄|苠, 赲邽迡迮郋 邽"));
    }
}
