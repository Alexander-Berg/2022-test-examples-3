package ru.yandex.market.sberlog.utils;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;

/**
 * @author Strakhov Artem <a href="mailto:dukeartem@yandex-team.ru"></a>
 * @date 16.04.19
 */
@Ignore
public class CheckClientIdTest {

    @Test
    public void checkContainClientIdInAllow() {
        HashSet<String> exampleHashSet = new HashSet<>();
        exampleHashSet.add("1");
        exampleHashSet.add("2");
        exampleHashSet.add("3");


        CheckClientId checkClientId = new CheckClientId(exampleHashSet);

        Assert.assertTrue(checkClientId.checkContainClientIdInAllow(2));
    }
}
