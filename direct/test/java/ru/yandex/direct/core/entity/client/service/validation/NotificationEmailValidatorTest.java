package ru.yandex.direct.core.entity.client.service.validation;

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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.client.service.validation.NotificationEmailValidator.MAX_SIZE;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;

@RunWith(Parameterized.class)
public class NotificationEmailValidatorTest {
    private final NotificationEmailValidator validator = new NotificationEmailValidator();

    @Parameterized.Parameter()
    public String email;
    @Parameterized.Parameter(value = 1)
    public List<Defect> expectedDefectTypes;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(
                new Object[]{"", singletonList(notEmptyString())},
                new Object[]{"notify@email.com", emptyList()},
                new Object[]{StringUtils.repeat('a', MAX_SIZE + 1), singletonList(maxStringLength(MAX_SIZE))});
    }

    @Test
    public void testLogin() {
        List<Defect> actualDefectTypes = validator.apply(email).flattenErrors()
                .stream()
                .map(DefectInfo::getDefect)
                .collect(toList());
        assertThat(actualDefectTypes, equalTo(expectedDefectTypes));
    }
}
