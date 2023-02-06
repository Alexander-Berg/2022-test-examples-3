package ru.yandex.direct.core.entity.user.validator;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.entity.user.validator.LoginValidator.MAX_SIZE;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;

@RunWith(Parameterized.class)
public class LoginValidatorTest {
    private final LoginValidator validator = new LoginValidator();

    @Parameterized.Parameter()
    public String login;
    @Parameterized.Parameter(value = 1)
    public List<Defect> expectedDefectTypes;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(
                new Object[]{"", singletonList(notEmptyString())},
                new Object[]{"a-b-a", emptyList()},
                new Object[]{"a-b-0", emptyList()},
                new Object[]{"A.b.A", emptyList()},
                new Object[]{"A.b.0", emptyList()},
                new Object[]{"0", singletonList(invalidValue())},
                new Object[]{"a#0", singletonList(invalidValue())},
                new Object[]{"a--0", singletonList(invalidValue())},
                new Object[]{"a..0", singletonList(invalidValue())},
                new Object[]{StringUtils.repeat('a', MAX_SIZE + 1), singletonList(maxStringLength(MAX_SIZE))});
    }

    @Test
    public void testLogin() {
        List<Defect> actualDefectTypes = validator.apply(login).flattenErrors()
                .stream()
                .map(DefectInfo::getDefect)
                .collect(toList());

        assertEquals(expectedDefectTypes, actualDefectTypes);
    }
}
