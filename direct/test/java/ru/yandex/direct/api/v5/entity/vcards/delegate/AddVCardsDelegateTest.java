package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.util.List;

import com.yandex.direct.api.v5.vcards.VCardAddItem;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AddVCardsDelegateTest {
    private VcardService vcardService;
    private AddVCardsDelegate addDelegate;

    @Before
    public void setUp() {
        ApiAuthenticationSource authenticationSource = mock(ApiAuthenticationSource.class);
        doReturn(mock(ApiUser.class))
                .when(authenticationSource).getOperator();
        doReturn(mock(ApiUser.class))
                .when(authenticationSource).getChiefSubclient();

        vcardService = mock(VcardService.class);

        addDelegate = spy(
                new AddVCardsDelegate(authenticationSource, vcardService, mock(ResultConverter.class)));
    }

    @Test
    public void testIfValidationFail() {
        doReturn(
                new ValidationResult<List<VCardAddItem>, Defect>(
                        null,
                        singleton(CommonDefects.isNull()),
                        emptyList())).when(addDelegate).validate(anyList());

        addDelegate.processRequest(singletonList(new VCardAddItem()));

        verify(vcardService, never()).addVcardsPartial(anyList(), anyLong(), any());
    }

    @Test
    public void testIfValidationSuccess() {
        doReturn(
                new ValidationResult<List<VCardAddItem>, Defect>((List<VCardAddItem>) null))
                .when(addDelegate).validate(anyList());

        addDelegate.processRequest(singletonList(new VCardAddItem()));

        verify(vcardService).addVcardsPartial(anyList(), anyLong(), any());
    }
}
