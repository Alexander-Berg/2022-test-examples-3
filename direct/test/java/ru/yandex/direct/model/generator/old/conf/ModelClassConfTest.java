package ru.yandex.direct.model.generator.old.conf;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;


public class ModelClassConfTest {

    private ModelClassConf.Builder modelBuilder;

    @Before
    public void initTestData() {
        modelBuilder = new ModelClassConf.Builder("test.package", "testClass")
                .withAttrs(Collections.emptyList())
                .withAnnotations(Collections.emptyList());
    }


    @Test
    public void checkSupersFullNames() {
        String superTestClass = "SuperTestClass";
        ModelClassConf modelConf = modelBuilder
                .withExtendsClass(superTestClass)
                .build();

        assertThat(modelConf.getSupersFullNames())
                .containsExactly(format("%s.%s", modelConf.getPackageName(), superTestClass));
    }

    @Test
    public void checkSupersFullNames_whenSetExtendsClassWithFullName() {
        String superTestClassFullName = "another.package.SuperTestClass";
        ModelClassConf modelConf = modelBuilder
                .withExtendsClass(superTestClassFullName)
                .build();

        assertThat(modelConf.getSupersFullNames())
                .containsExactly(superTestClassFullName);
    }
}
