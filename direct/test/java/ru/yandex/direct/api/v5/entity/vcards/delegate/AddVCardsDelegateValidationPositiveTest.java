package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.util.List;

import com.yandex.direct.api.v5.vcards.VCardAddItem;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AddVCardsDelegateValidationPositiveTest {
    private AddVCardsDelegate addDelegate;

    @Before
    public void setUp() {
        addDelegate = new AddVCardsDelegate(
                mock(ApiAuthenticationSource.class),
                mock(VcardService.class),
                mock(ResultConverter.class));
    }

    @Test
    public void test() {
        ValidationResult<List<VCardAddItem>, DefectType> vr = addDelegate.validate(
                singletonList(new VCardAddItem()));
        assertThat(vr.hasAnyErrors()).isEqualTo(false);
    }
}
