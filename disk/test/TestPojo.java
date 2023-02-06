package ru.yandex.chemodan.videostreaming.framework.util.threadlocal.test;

import lombok.Value;
import lombok.experimental.NonFinal;

import ru.yandex.misc.lang.StringUtils;

/**
 * MUST NOT be in the same package as TlOverrideDynamicProxyTest!
 *
 * @author Dmitriy Amelin (lemeh)
 */
@Value
@NonFinal
public class TestPojo {
    String value;

    public void publicMethod(TestPojo other) {
        doSomeUselessWork();
        other.protectedMethod();
    }

    protected void protectedMethod() {
        doSomeUselessWork();
    }

    private void doSomeUselessWork() {
        StringUtils.notBlankO("");
    }
}
