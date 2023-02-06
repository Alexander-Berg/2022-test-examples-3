package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.yandex.direct.api.v5.vcards.VCardAddItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.vcards.Constants.MAX_IDS_COUNT_PER_ADD_REQUEST;
import static ru.yandex.direct.api.v5.validation.DefectTypes.absentElementInArray;
import static ru.yandex.direct.api.v5.validation.DefectTypes.invalidValue;
import static ru.yandex.direct.api.v5.validation.DefectTypes.maxElementsPerRequest;

@RunWith(Parameterized.class)
public class AddVCardsDelegateValidationNegativeTest {
    @Parameterized.Parameter
    public List<VCardAddItem> vcards;
    @Parameterized.Parameter(value = 1)
    public DefectType expectedDefect;
    private AddVCardsDelegate addDelegate;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{null, invalidValue()},
                new Object[]{Arrays.asList(null, new VCardAddItem()), absentElementInArray()},
                new Object[]{Collections.nCopies(MAX_IDS_COUNT_PER_ADD_REQUEST + 1, new VCardAddItem()),
                        maxElementsPerRequest(MAX_IDS_COUNT_PER_ADD_REQUEST)});
    }

    @Before
    public void setUp() {
        addDelegate = new AddVCardsDelegate(
                mock(ApiAuthenticationSource.class),
                mock(VcardService.class),
                mock(ResultConverter.class));
    }

    @Test
    public void test() {
        ValidationResult<List<VCardAddItem>, DefectType> actualResult = addDelegate.validate(vcards);

        assertThat(actualResult.getErrors()).containsOnly(expectedDefect);
    }
}
