package ru.yandex.canvas.service.video;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.canvas.model.video.addition.Options;
import ru.yandex.canvas.model.video.addition.options.OptionName;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class MethodByOptionNameTest {
    @Parameterized.Parameter
    public OptionName optionName;

    @Parameterized.Parameters(name = "OptionName {0}")
    public static Collection<OptionName> optionNameValues() {
        return Arrays.stream(OptionName.values()).filter(o -> !o.toValue().contains("_"))
                .collect(Collectors.toList());
    }

    @Test
    public void fieldWithPublicAccessorForOptionNameExists() {
        String apiFieldName = optionName.toValue();
        Field field = FieldUtils.getField(Options.class, optionName.toFieldName(), true);
        assumeTrue("field " + apiFieldName + " without camelCase exists as field", field != null);
        Method method = null;
        try {
            method = Options.class.getMethod("get" + StringUtils.capitalize(optionName.toFieldName()));
        } catch (NoSuchMethodException e) {
            //
        }
        assumeTrue("and accessor method exists", method != null);
        assertTrue("and accessor method is public", Modifier.isPublic(method.getModifiers()));
    }
}
