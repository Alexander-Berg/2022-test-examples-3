package ru.yandex.canvas.service.multitype;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.canvas.model.CreativeWithId;
import ru.yandex.canvas.model.html5.Creative;

import static org.junit.Assert.assertTrue;

public class CreativeTypeTest {
    @Test
    public void getCreativeClass() {
        assertTrue("html5 class resolved", Creative.class.equals(CreativeType.HTML5.getCreativeClass()));
    }

    @Test
    public void fromCreativeClass() {
        assertTrue("enum value for html5 creative class found", CreativeType.HTML5.equals(CreativeType.fromCreativeClass(Creative.class)));
    }

    @Test
    public void CreativeTypesMatchClasses() {
        Set<Class<? extends CreativeWithId>> uniqCreativeClasses
                = Arrays.stream(CreativeType.values()).map(ct -> ct.getCreativeClass()).collect(Collectors.toSet());
        assertTrue("Uniq creative classes amount the same as values in enum",
                uniqCreativeClasses.size() == CreativeType.values().length);
    }
}
