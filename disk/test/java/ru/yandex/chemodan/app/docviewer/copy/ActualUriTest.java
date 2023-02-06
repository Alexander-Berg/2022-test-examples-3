package ru.yandex.chemodan.app.docviewer.copy;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class ActualUriTest {

    @Test
    public void testEquals() {
        ActualUri one1 = new ActualUri("http://1.txt");
        ActualUri one2 = new ActualUri("http://1.txt");
        ActualUri two = new ActualUri("http://2.txt");
        Assert.equals(one1, one2);
        Assert.isTrue(one1.equals(one2));
        Assert.isFalse(one1.equals(two));
    }
}
