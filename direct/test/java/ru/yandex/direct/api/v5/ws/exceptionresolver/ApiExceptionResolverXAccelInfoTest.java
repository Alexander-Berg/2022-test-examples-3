package ru.yandex.direct.api.v5.ws.exceptionresolver;

import com.yandex.direct.api.v5.general.ApiExceptionMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.exeptiontranslator.DefaultExceptionTranslator;
import ru.yandex.direct.api.v5.exeptiontranslator.TranslatableExceptionTranslator;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.ws.json.JsonMessage;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.TranslatableException;
import ru.yandex.direct.core.validation.CommonDefectTranslations;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ApiExceptionResolverXAccelInfoTest {

    private static final int TRANSLATABLE_ERROR_CODE = 1000;

    private static final int API_FAULT_ERROR_CODE = 28;

    private static final int EXPECTED_UNKNOWN_ERROR_CODE_WRITTEN_TO_CONTEXT = -1;

    private static final TranslatableException TRANSLATABLE_ERROR = new TranslatableException(
            TRANSLATABLE_ERROR_CODE,
            CommonDefectTranslations.INSTANCE.invalidValueShort(),
            CommonDefectTranslations.INSTANCE.invalidValueShort()) {
    };

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ApiContext apiContext;

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock
    private ApiUnitsService apiUnitsService;

    @Mock
    private AccelInfoHeaderSetter accelInfoHeaderSetter;

    @Mock
    private TranslationService translationService;

    @InjectMocks
    private TranslatableExceptionTranslator translatableExceptionTranslator;

    @InjectMocks
    private DefaultExceptionTranslator defaultExceptionTranslator;

    private ApiExceptionResolver apiExceptionResolver;

    @Mock
    private JsonMessage apiMessage;
    @Mock
    private ApiExceptionMessage apiFault;

    @Before
    public void init() {
        initMocks(this);
        apiExceptionResolver = new ApiExceptionResolver(
                apiContextHolder,
                apiUnitsService,
                asList(translatableExceptionTranslator, defaultExceptionTranslator),
                asList(new JsonFaultResponseCreator(), new SoapFaultResponseCreator()),
                accelInfoHeaderSetter);

        when(translationService.translate(any())).then(a -> a.getArguments()[0].toString());
        when(apiContextHolder.get()).thenReturn(apiContext);
    }

    @Test
    public void shouldCallHeaderSetterIfDoesNotChargeUnitsForError() {
        shouldNotChargeUnits();

        apiExceptionResolver.resolveException(apiMessage, new RuntimeException());

        verify(accelInfoHeaderSetter).setAccelInfoHeaderToHttpResponse();
    }

    @Test
    public void shouldCallHeaderSetterIfShouldChargeUnits_definedErrorCode() {
        shouldChargeUnits();
        when(apiMessage.getApiFault()).thenReturn(apiFault);
        when(apiFault.getErrorCode()).thenReturn(API_FAULT_ERROR_CODE);

        apiExceptionResolver.resolveException(apiMessage, TRANSLATABLE_ERROR);

        verify(accelInfoHeaderSetter).setAccelInfoHeaderToHttpResponse();
        assertThat(apiContext.getAppErrorCode()).isEqualTo(API_FAULT_ERROR_CODE);
    }

    @Test
    public void shouldCallHeaderSetterIfShouldChargeUnits_unknownError() {
        shouldChargeUnits();

        apiExceptionResolver.resolveException(apiMessage, TRANSLATABLE_ERROR);

        verify(accelInfoHeaderSetter).setAccelInfoHeaderToHttpResponse();
        assertThat(apiContext.getAppErrorCode()).isEqualTo(EXPECTED_UNKNOWN_ERROR_CODE_WRITTEN_TO_CONTEXT);
    }

    private void shouldChargeUnits() {
        when(apiContextHolder.get().shouldChargeUnitsForRequest()).thenReturn(true);
    }

    private void shouldNotChargeUnits() {
        when(apiContextHolder.get().shouldChargeUnitsForRequest()).thenReturn(false);
    }

}
