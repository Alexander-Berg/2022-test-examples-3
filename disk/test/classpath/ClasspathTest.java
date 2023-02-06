package ru.yandex.chemodan.test.classpath;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.test.Assert;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public abstract class ClasspathTest {
    private static final Logger logger = LoggerFactory.getLogger(ClasspathTest.class);

    private static final ListF<ComponentMatcher> componentExcludes = Cf.list(
            ComponentMatcher.anyMatch(".+/[^/]*cglib-(nodep-)?[0-9_.]+\\.jar")
    );

    private static final SetF<String> classExcludes = Cf.set(
            "module-info.class",
            "com/mongodb/util/JSONParser.class",
            "org/jetbrains/annotations/PropertyKey.class",
            "org/json/JSONString.class",
            "org/w3c/dom/UserDataHandler.class"
    );

    @Test
    public void classpathHasNoDuplicateClasses() {
        long start = System.currentTimeMillis();
        DuplicateClassFinder duplicateClassFinder = new DuplicateClassFinder()
                .excludeComponents(getExtraIgnores().plus(componentExcludes))
                .excludeClasses(getExtraClassExcludes().plus(classExcludes))
                .excludeClassesWithSameSize();
        logger.info("Duplicate class searching took " + (System.currentTimeMillis() - start) + "ms");

        if (duplicateClassFinder.hasDuplicates()) {
            Assert.fail(duplicateClassFinder.getFailMessage());
        }
    }

    /**
     * Override in subclasses
     *
     * @return extra ignore patterns.
     */
    protected ListF<ComponentMatcher> getExtraIgnores() {
        return Cf.list();
    }


    /**
     * Override in subclasses
     *
     * @return extra exclude class patterns.
     */
    protected ListF<String> getExtraClassExcludes() {
        return Cf.list();
    }
}
