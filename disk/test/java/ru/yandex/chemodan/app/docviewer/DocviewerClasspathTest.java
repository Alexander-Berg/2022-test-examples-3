package ru.yandex.chemodan.app.docviewer;

import org.junit.Ignore;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.test.classpath.ClasspathTest;
import ru.yandex.chemodan.test.classpath.ComponentMatcher;

/**
 * @author Dmitriy Amelin (lemeh)
 */
@Ignore
public class DocviewerClasspathTest extends ClasspathTest {
    @Override
    protected ListF<ComponentMatcher> getExtraIgnores() {
        return Cf.list(
                ComponentMatcher.anyMatch("/jython-"),
                ComponentMatcher.anyMatch("/bouncycastle/"),
                ComponentMatcher.anyMatch("/xml-apis-")
        );
    }
}
