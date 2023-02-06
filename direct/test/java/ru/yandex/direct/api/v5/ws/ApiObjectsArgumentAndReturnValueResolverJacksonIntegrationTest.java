package ru.yandex.direct.api.v5.ws;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.MethodParameter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.ws.context.MessageContext;

import ru.yandex.direct.api.v5.ApiFaultTranslations;
import ru.yandex.direct.api.v5.configuration.MessageMapperConfiguration;
import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.ws.json.JsonMessage;
import ru.yandex.direct.api.v5.ws.json.ObjectSourceAndResult;
import ru.yandex.direct.api.v5.ws.testbeans.SimpleFieldNamesEnum;
import ru.yandex.direct.api.v5.ws.testbeans.SimpleGetRequest;
import ru.yandex.direct.api.v5.ws.validation.ApiObjectValidator;
import ru.yandex.direct.api.v5.ws.validation.IncorrectRequestApiException;
import ru.yandex.direct.api.v5.ws.validation.InvalidFormatApiException;
import ru.yandex.direct.api.v5.ws.validation.InvalidValueApiException;
import ru.yandex.direct.api.v5.ws.validation.MissedParamsValueApiException;
import ru.yandex.direct.api.v5.ws.validation.UnknownParameterApiException;
import ru.yandex.direct.api.v5.ws.validation.WsdlValidator;
import ru.yandex.direct.api.v5.ws.validation.WsdlValidatorFactory;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.api.v5.ws.WsConstants.JSON_MESSAGE_OBJECT_READER_BEAN_NAME;
import static ru.yandex.direct.api.v5.ws.WsConstants.JSON_MESSAGE_OBJECT_WRITER_BEAN_NAME;
import static ru.yandex.direct.api.v5.ws.WsConstants.SOAP_MESSAGE_OBJECT_MAPPER_BEAN_NAME;
import static ru.yandex.direct.utils.JsonUtils.fromJson;
import static ru.yandex.direct.utils.JsonUtils.toJson;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = MessageMapperConfiguration.class)
public class ApiObjectsArgumentAndReturnValueResolverJacksonIntegrationTest {
    @Autowired
    @Qualifier(JSON_MESSAGE_OBJECT_READER_BEAN_NAME)
    private ObjectMapper jsonMessageObjectReader;

    @Autowired
    @Qualifier(JSON_MESSAGE_OBJECT_WRITER_BEAN_NAME)
    private ObjectMapper jsonMessageObjectWriter;

    @Autowired
    @Qualifier(SOAP_MESSAGE_OBJECT_MAPPER_BEAN_NAME)
    private XmlMapper soapMessageObjectMapper;

    private ApiObjectsArgumentAndReturnValueResolver resolverUnderTest;
    private ApiObjectValidator apiObjectValidator;
    private MethodParameter methodParameter;
    private ApiContext apiContext;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws ExecutionException {
        apiObjectValidator = mock(ApiObjectValidator.class);

        apiContext = new ApiContext();
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(apiContext);

        WsdlValidator wsdlValidator = mock(WsdlValidator.class);
        WsdlValidatorFactory wsdlValidatorFactory = mock(WsdlValidatorFactory.class);
        when(wsdlValidatorFactory.getValidator(any())).thenReturn(wsdlValidator);

        resolverUnderTest = new ApiObjectsArgumentAndReturnValueResolver(
                jsonMessageObjectReader, jsonMessageObjectWriter, soapMessageObjectMapper, apiObjectValidator,
                apiContextHolder, wsdlValidatorFactory);

        methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterType()).thenReturn((Class) SimpleGetRequest.class);
    }

    @Test
    public void resolveArgumentJson() throws Exception {
        ImmutableMap<String, Object> data = ImmutableMap.of(
                "FieldNames", singletonList("Id"),
                "IntValue", 33,
                "LongValue", "22");
        MessageContext messageContext = incomingJsonMessageAsMap(data);

        Object argument = resolverUnderTest.resolveArgument(messageContext, methodParameter);

        SimpleGetRequest expected = new SimpleGetRequest()
                .withFieldNames(singletonList(SimpleFieldNamesEnum.ID))
                .withLongValue(22L)
                .withIntValue(33);

        assertThat(argument, beanDiffer(expected));
        Assertions.assertThat(fromJson(apiContext.getApiLogRecord().getParams()))
                .isEqualTo(fromJson(toJson(data)));
        verify((JsonMessage) messageContext.getRequest()).setApiRequestPayload(argThat(beanDiffer(expected)));
        verify(apiObjectValidator).validate(argThat(beanDiffer(expected)));
    }

    @Test
    public void resolveArgumentJsonWithJaxbNil() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("FieldNames", singletonList("Id"));
        data.put("IntValue", 33);
        data.put("LongValue", "22");
        data.put("JaxbIntValue", null);
        MessageContext messageContext = incomingJsonMessageAsMap(data);

        SimpleGetRequest argument =
                (SimpleGetRequest) resolverUnderTest.resolveArgument(messageContext, methodParameter);

        Assertions.assertThat(argument.getJaxbIntValue()).isNotNull();
        Assertions.assertThat(argument.getJaxbIntValue().isNil()).isTrue();
    }

    @Test
    public void resolveArgumentJsonWithJaxbNotNil() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("FieldNames", singletonList("Id"));
        data.put("IntValue", 33);
        data.put("LongValue", "22");
        data.put("JaxbIntValue", 123);
        MessageContext messageContext = incomingJsonMessageAsMap(data);

        SimpleGetRequest argument =
                (SimpleGetRequest) resolverUnderTest.resolveArgument(messageContext, methodParameter);

        Assertions.assertThat(argument.getJaxbIntValue()).isNotNull();
        Assertions.assertThat(argument.getJaxbIntValue().isNil()).isFalse();
        Assertions.assertThat(argument.getJaxbIntValue().getValue()).isEqualTo(123);
    }

    @Test
    public void logRecordParamsFilledOnException() {
        ImmutableMap<String, Object> data = ImmutableMap.of(
                "FieldNames", singletonList("Id"),
                "IntValue", 33,
                "Incorrect", 33,
                "LongValue", "22");
        MessageContext messageContext = incomingJsonMessageAsMap(data);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThatThrownBy(() -> resolverUnderTest.resolveArgument(messageContext, methodParameter))
                .isInstanceOf(Exception.class);
        softly.assertThat(fromJson(apiContext.getApiLogRecord().getParams()))
                .isEqualTo(fromJson(toJson(data)));
        softly.assertAll();
    }

    @Test(expected = UnknownParameterApiException.class)
    public void resolveArgumentJsonUnrecognizedPropertyException() throws Exception {
        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("zzz", "xxx"));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidFormatExceptionEnum() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidFormatApiException(
                        null,
                        ApiFaultTranslations.INSTANCE.detailedInvalidFormatExpectedEnum(
                                "SingleEnum", "Id, Name"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("SingleEnum", "xxx"));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidFormatExceptionEnumInArray() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidFormatApiException(
                        null,
                        ApiFaultTranslations.INSTANCE.detailedInvalidFormatExpectedEnumInArray(
                                "FieldNames", "Id, Name"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("FieldNames", singletonList("xxx")));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidFormatExceptionLong() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidFormatApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedInvalidFormatExpectedInteger("LongValue"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("LongValue", "xxx"));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidFormatExceptionInteger() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidFormatApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedInvalidFormatExpectedInteger("IntValue"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("IntValue", "xxx"));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test(expected = IncorrectRequestApiException.class)
    public void resolveArgumentJsonMappingException() throws Exception {
        MessageContext messageContext = incomingJsonMessageAsMap("aaaa");
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test(expected = MissedParamsValueApiException.class)
    public void resolveArgumentJsonMissedParamsException() throws Exception {
        MessageContext messageContext = incomingJsonMessageAsMap(null);
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidValueApiException() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedIncorrectValue("InnerBean"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("InnerBean", 22));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidValueApiExceptionExpectedObjectButGotArray() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedIncorrectValueFieldShouldNotBeArray("InnerBean"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("InnerBean", singletonList("xxx")));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidValueApiExceptionExpectedArrayButGotString() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedIncorrectValueFieldShouldBeArray("FieldNames"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("FieldNames", "xxx"));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidValueApiExceptionExpectedArrayButGotObject() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedIncorrectValueFieldShouldBeArray("FieldNames"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("FieldNames", emptyMap()));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidValueApiExceptionExpectedArrayOfArrays() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedIncorrectValueFieldShouldBeArray("ListOfLists"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("ListOfLists", singletonList("xxx")));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidValueApiExceptionExpectedArrayOfLong() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedInvalidFormatExpectedIntegerInArray("ListOfLong"))));

        MessageContext messageContext = incomingJsonMessageAsMap(ImmutableMap.of("ListOfLong", singletonList("xxx")));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test
    public void resolveArgumentJsonInvalidValueApiExceptionExpectedArrayOfLongButGotArrayAsElement() throws Exception {
        thrown.expect(new TranslatableExceptionMatcher(
                new InvalidValueApiException(null,
                        ApiFaultTranslations.INSTANCE.detailedInvalidFormatExpectedIntegerInArray("ListOfLong"))));

        MessageContext messageContext =
                incomingJsonMessageAsMap(ImmutableMap.of("ListOfLong", singletonList(singletonList("xxx"))));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    private MessageContext incomingJsonMessageAsMap(Object objectObjectMap) {
        ObjectSourceAndResult jsonPayload = mock(ObjectSourceAndResult.class);
        when(jsonPayload.getObject()).thenReturn(objectObjectMap);

        JsonMessage jsonMessage = mock(JsonMessage.class);
        when(jsonMessage.getPayloadSource()).thenReturn(jsonPayload);

        MessageContext messageContext = mock(MessageContext.class);
        when(messageContext.getRequest()).thenReturn(jsonMessage);
        return messageContext;
    }
}
