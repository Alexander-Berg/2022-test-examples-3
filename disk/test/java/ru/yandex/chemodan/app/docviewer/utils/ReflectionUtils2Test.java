package ru.yandex.chemodan.app.docviewer.utils;

import java.util.List;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class ReflectionUtils2Test {

    @Test
    public void depth0() {
        Assert.equals(
                "AA[i1=123, bb1=bb]",
                ReflectionUtils2.reflectionToStringValueObject(new AA(), Option.of(0))
                );
    }

    @Test
    public void depth1() {
        Assert.equals(
                "AA[i1=123, bb1=BB[s2=abc456, d2={1.2, 3.4}, l2=[56, 78], cc2=ru.yandex.chemodan.app.docviewer.utils.CC@ID]]",
                ReflectionUtils2.reflectionToStringValueObject(new AA(), Option.of(1)).replaceFirst("@\\w+", "@ID")
                );
    }

    @Test
    public void depthAllLevels() {
        Assert.equals(
                "AA[i1=123, bb1=BB[s2=abc456, d2={1.2, 3.4}, l2=[56, 78], cc2=CC[s3=def789]]]",
                ReflectionUtils2.reflectionToStringValueObject(new AA(), Option.empty())
                );
    }


}

@SuppressWarnings("unused")
class AA {
    private int i1 = 123;
    private BB bb1 = new BB();

    @Override
    public String toString() {
        return "aa";
    }
}

@SuppressWarnings("unused")
class BB {
    private String s2 = "abc456";
    private double[] d2 = new double[] {1.2, 3.4};
    private List<Long> l2 = Cf.list(56L, 78L);
    private CC cc2 = new CC();

    @Override
    public String toString() {
        return "bb";
    }
}

@SuppressWarnings("unused")
class CC {
    private String s3 = "def789";
}
