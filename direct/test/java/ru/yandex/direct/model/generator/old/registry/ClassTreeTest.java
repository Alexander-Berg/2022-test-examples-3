package ru.yandex.direct.model.generator.old.registry;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Sets;
import org.junit.Test;

import ru.yandex.direct.model.generator.old.conf.UpperLevelModelConf;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class ClassTreeTest {

    private static final String INTERFACE_A = "InterfaceA";
    private static final String INTERFACE_B = "InterfaceB";
    private static final String INTERFACE_C = "InterfaceC";

    private static final String CLASS_A = "ClassA";
    private static final String CLASS_B = "ClassB";
    private static final String CLASS_C = "ClassC";

    private UpperLevelModelConf interfaceA = conf(INTERFACE_A);
    private UpperLevelModelConf interfaceB = conf(INTERFACE_B);
    private UpperLevelModelConf interfaceC = conf(INTERFACE_C, INTERFACE_B);

    private UpperLevelModelConf classA = conf(CLASS_A, INTERFACE_A);
    private UpperLevelModelConf classB = conf(CLASS_B, CLASS_A);
    private UpperLevelModelConf classC = conf(CLASS_C, CLASS_A, INTERFACE_C);

    private ClassTree classTree = new ClassTree(Sets.newHashSet(
            interfaceA, interfaceB, interfaceC,
            classA, classB, classC
    ));

    @Test
    public void allDescendantsOfInterfaceA() {
        assertThat(descendants(interfaceA)).containsOnlyElementsOf(asList(
                classA, classB, classC
        ));
    }

    @Test
    public void allDescendantsOfInterfaceB() {
        assertThat(descendants(interfaceB)).containsOnlyElementsOf(asList(
                interfaceC, classC
        ));
    }

    @Test
    public void allAncestorsOfClassC() {
        assertThat(ancestors(classC)).containsOnlyElementsOf(asList(
                classA, interfaceC, interfaceA, interfaceB
        ));
    }

    @Test
    public void allAncestorsOfClassB() {
        assertThat(ancestors(classB)).containsOnlyElementsOf(asList(
                interfaceA, classA
        ));
    }

    @Test
    public void allDescendantsOfClassB() {
        assertThat(descendants(classB)).isEmpty();
    }

    @Test
    public void allAncestorsOfClassA() {
        assertThat(ancestors(interfaceA)).isEmpty();
    }

    @Test
    public void allLeaves() {
        assertThat(classTree.getAllLeaves()).containsOnlyElementsOf(asList(
                classB, classC
        ));
    }

    @Test
    public void testFilterUpperClasses_Cls_ABC() {
        assertThat(classTree.filterUpperClassesAmong(asList(classA, classB, classC))).containsOnlyElementsOf(
                singletonList(classA));
    }

    @Test
    public void testFilterUpperClasses_Cls_BC() {
        assertThat(classTree.filterUpperClassesAmong(asList(classB, classC))).containsOnlyElementsOf(asList(
                classB, classC
        ));
    }

    @Test
    public void testFilterUpperClasses_Cls_BC_Int_C() {
        assertThat(classTree.filterUpperClassesAmong(asList(classB, classC, interfaceC))).containsOnlyElementsOf(asList(
                classB, interfaceC
        ));
    }

    @Test
    public void testFilterUpperClasses_Cls_BC_Int_B() {
        assertThat(classTree.filterUpperClassesAmong(asList(classB, classC, interfaceB))).containsOnlyElementsOf(asList(
                classB, classC, interfaceB
        ));
    }

    private Set<UpperLevelModelConf> descendants(UpperLevelModelConf conf) {
        return classTree.getAllDescendants(conf);
    }

    private Set<UpperLevelModelConf> ancestors(UpperLevelModelConf conf) {
        return classTree.getAllAncestors(conf);
    }

    private static UpperLevelModelConf conf(String fullName, String... supersFullNames) {
        UpperLevelModelConf conf = mock(UpperLevelModelConf.class);
        when(conf.getFullName()).thenReturn(fullName);
        when(conf.getExtendsAndImplementsFullNames()).thenReturn(Stream.of(supersFullNames).collect(toList()));
        return conf;
    }

}
