package ru.yandex.calendar.logic.ics.iv5j.ical.meta;

import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import ru.yandex.misc.log.mlf.Logger;
import ru.yandex.misc.log.mlf.LoggerFactory;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public abstract class IcssMetaTestSupport<O, T> {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected abstract String packageSuffix();
    protected abstract Class<T> dataClass();
    protected abstract Class<? extends T> xDataClass();
    protected abstract IcssMeta<O, ?, T, ?, ?> meta();

    @Test
    public void checkAllClassesFound() throws Exception {
        PathMatchingResourcePatternResolver resolve = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolve.getResources("classpath:net/fortuna/ical4j/model/" + packageSuffix() + "/*.class");
        for (Resource res : resources) {
            String className = res.getURI().toString()
                .replaceFirst(".*/net/fortuna/ical4j/", "net/fortuna/ical4j/")
                .replaceFirst("\\.class$", "")
                .replaceAll("/", ".")
                ;

            if (className.contains("$")) {
                logger.info("Skipping inner " + className);
                continue;
            }

            if (className.endsWith("Factory")) {
                logger.info("Skipping factory " + className);
                continue;
            }

            if (className.endsWith("FactoryWrapper")) {
                logger.info("Skipping factory wrapper " + className);
                continue;
            }

            ClassX<T> clazz = ClassX.forName(className).uncheckedCast();
            if (!clazz.isAssignableTo(dataClass())) {
                logger.info("Skipping not assignable " + clazz);
                continue;
            }

            if (clazz.isAbstract()) {
                logger.info("Skipping abstact " + clazz);
                continue;
            }

            if (clazz.getClazz().equals(xDataClass())) {
                logger.info("Skipping x " + clazz);
                continue;
            }

            Assert.A.some(meta().metaByTheirClass(clazz), "not found mapping for class " + clazz);

            logger.info("Good: " + clazz);
        }
    }

} //~
