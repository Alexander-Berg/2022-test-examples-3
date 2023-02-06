package ru.yandex.direct.api.v5.ws.exceptionresolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.ApiValidationException;
import ru.yandex.direct.api.v5.exeptiontranslator.DefaultExceptionTranslator;
import ru.yandex.direct.api.v5.exeptiontranslator.TranslatableExceptionTranslator;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.api.v5.validation.ApiDefect;
import ru.yandex.direct.api.v5.validation.DefectTypes;
import ru.yandex.direct.api.v5.ws.ApiMessage;
import ru.yandex.direct.api.v5.ws.json.JsonMessage;
import ru.yandex.direct.api.v5.ws.soap.SoapMessage;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.TranslatableException;
import ru.yandex.direct.core.validation.CommonDefectTranslations;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.PathNode;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(Parameterized.class)
public class ApiExceptionResolverUnitsWithdrawTest {
    private static final int SOME_ERROR_CODE = 1000;
    private static final TranslatableException SOME_TRANSLATABLE_ERROR = new TranslatableException(
            SOME_ERROR_CODE,
            CommonDefectTranslations.INSTANCE.invalidValueShort(),
            CommonDefectTranslations.INSTANCE.invalidValueShort()) {{
    }};
    private static final ApiDefect SOME_DEFECT = new ApiDefect(
            new DefectInfo<>(
                    new Path(singletonList(new PathNode.Field("someField"))),
                    "someInvalidValue",
                    DefectTypes.invalidValue()));

    @Mock
    private ApiContextHolder apiContextHolder;
    @Mock
    private ApiContext apiContext;
    @Mock
    private ApiUnitsService apiUnitsService;
    @Mock
    private AccelInfoHeaderSetter accelInfoHeaderSetter;

    private ApiExceptionResolver apiExceptionResolver;

    private ApiMessage responseMessage;

    public ApiExceptionResolverUnitsWithdrawTest(
            ApiMessage responseMessage, String messageClassName) {
        this.responseMessage = spy(responseMessage);
    }

    private static SoapMessage createSoapMessage() {
        try {
            return new SoapMessage(
                    new SaajSoapMessageFactory(MessageFactory.newInstance()).createWebServiceMessage(),
                    Function.identity(),
                    Function.identity());
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        }
    }

    @Parameterized.Parameters(name = "messageClass: {1}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[]{
                        new JsonMessage(new ObjectMapper(), "service"),
                        JsonMessage.class.getSimpleName()},
                new Object[]{
                        createSoapMessage(),
                        SoapMessage.class.getSimpleName()});
    }

    @Before
    public void setUp() {
        initMocks(this);

        TranslationService translationService = mock(TranslationService.class);
        when(translationService.translate(any()))
                .then(a -> a.getArguments()[0].toString());

        when(apiContext.shouldChargeUnitsForRequest()).thenReturn(true);

        when(apiContextHolder.get()).thenReturn(apiContext);

        apiExceptionResolver = new ApiExceptionResolver(
                apiContextHolder,
                apiUnitsService,
                Arrays.asList(
                        new TranslatableExceptionTranslator(translationService),
                        new DefaultExceptionTranslator(translationService)),
                Arrays.asList(
                        new JsonFaultResponseCreator(),
                        new SoapFaultResponseCreator()),
                accelInfoHeaderSetter);
    }

    @Test
    public void testIgnoreNotTranslatableException() {
        apiExceptionResolver.resolveException(
                responseMessage,
                new RuntimeException());
        verify(apiUnitsService, never()).withdrawForRequestError(any());
    }

    @Test
    public void testApiValidationExceptionWithUnitsSpent() {
        apiExceptionResolver.resolveException(
                responseMessage,
                new ApiValidationException(SOME_DEFECT, false));
        verify(apiUnitsService).withdrawForRequestError(any());
    }

    @Test
    public void testApiValidationExceptionWithoutUnitsSpent() {
        apiExceptionResolver.resolveException(
                responseMessage,
                new ApiValidationException(SOME_DEFECT, true));
        verify(apiUnitsService, never()).withdrawForRequestError(any());
    }

    @Test
    public void testSomeTranslatableException() {
        apiExceptionResolver.resolveException(
                responseMessage, SOME_TRANSLATABLE_ERROR);
        verify(apiUnitsService)
                .withdrawForRequestError(eq(SOME_ERROR_CODE));
    }

    @Test
    public void testIfNotShouldChargeUnitsSetInContext() {
        when(apiContext.shouldChargeUnitsForRequest()).thenReturn(false);
        apiExceptionResolver.resolveException(
                responseMessage,
                SOME_TRANSLATABLE_ERROR);
        verify(apiUnitsService, never()).withdrawForRequestError(any());
    }

    @Test
    public void testCantExtractFaultInfo() throws Exception {
        when(responseMessage.getApiFault()).thenReturn(null);
        apiExceptionResolver.resolveException(responseMessage, SOME_TRANSLATABLE_ERROR);
        verify(apiUnitsService)
                .withdrawForRequestError(eq(null));
    }
}
