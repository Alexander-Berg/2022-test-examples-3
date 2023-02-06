package ru.yandex.direct.api.v5.entity.sitelinks.delegate;

import java.util.List;
import java.util.Random;

import com.yandex.direct.api.v5.sitelinks.DeleteRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.sitelinks.converter.DeleteRequestConverter;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.core.entity.sitelink.service.SitelinkSetService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.PathConverter;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.entity.sitelinks.Constants.MAX_IDS_PER_DELETE;

public class DeleteSitelinksDelegateTest {

    private static Random random = new Random();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiAuthenticationSource authenticationSource;

    @Mock
    private DeleteRequestConverter requestConverter;

    @Mock
    private ResultConverter resultConverter;

    @Mock
    private SitelinkSetService service;

    @InjectMocks
    private DeleteSitelinksDelegate delegate;

    private ClientId clientId = ClientId.fromLong(random.nextLong());

    @Before
    public void init() {
        initMocks(this);

        when(authenticationSource.getChiefSubclient().getClientId()).thenReturn(clientId);
    }

    @Test
    public void shouldCallConverterOnConvertRequest() {
        DeleteRequest request = new DeleteRequest();

        delegate.convertRequest(request);

        verify(requestConverter).convert(same(request));
    }

    @Test
    public void shouldCallServiceDeleteOnProcessRequestIfItIsValid() {
        List<Long> ids = listOfRandomLongs(MAX_IDS_PER_DELETE);

        delegate.processRequest(ids);

        verify(service).deleteSiteLinkSets(eq(clientId), eq(ids));
    }

    @Test
    public void shouldCallResultConverterOnConvertResponse() {
        @SuppressWarnings("unchecked")
        ApiMassResult<Long> result = mock(ApiMassResult.class);

        delegate.convertResponse(result);

        verify(resultConverter).toActionResults(same(result), any(PathConverter.class));
    }

    private static List<Long> listOfRandomLongs(int size) {
        return random.longs(size).boxed().collect(toList());
    }

}

