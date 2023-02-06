package ru.yandex.direct.api.v5.ws;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.MethodParameter;
import org.springframework.ws.context.MessageContext;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.ws.json.JsonMessage;
import ru.yandex.direct.api.v5.ws.json.ObjectSourceAndResult;
import ru.yandex.direct.api.v5.ws.soap.SoapMessage;
import ru.yandex.direct.api.v5.ws.testbeans.SimpleGetRequest;
import ru.yandex.direct.api.v5.ws.validation.ApiObjectValidator;
import ru.yandex.direct.api.v5.ws.validation.IncorrectRequestApiException;
import ru.yandex.direct.api.v5.ws.validation.InvalidFormatApiException;
import ru.yandex.direct.api.v5.ws.validation.UnknownParameterApiException;
import ru.yandex.direct.api.v5.ws.validation.WsdlValidator;
import ru.yandex.direct.api.v5.ws.validation.WsdlValidatorFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class ApiObjectsArgumentAndReturnValueResolverTest {

    private ApiObjectsArgumentAndReturnValueResolver resolverUnderTest;
    private ObjectMapper jsonMessageObjectReader;
    private ObjectMapper jsonMessageObjectWriter;
    private XmlMapper xmlMessageObjectMapper;
    private ApiObjectValidator apiObjectValidator;
    private MessageContext messageContext;
    private JsonMessage jsonMessage;
    private SoapMessage soapMessage;
    private ObjectSourceAndResult jsonPayload;
    private MethodParameter methodParameter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        jsonMessageObjectReader = mock(ObjectMapper.class);
        jsonMessageObjectWriter = mock(ObjectMapper.class);
        xmlMessageObjectMapper = mock(XmlMapper.class);
        apiObjectValidator = mock(ApiObjectValidator.class);

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());

        WsdlValidator wsdlValidator = mock(WsdlValidator.class);
        WsdlValidatorFactory wsdlValidatorFactory = mock(WsdlValidatorFactory.class);
        when(wsdlValidatorFactory.getValidator(any())).thenReturn(wsdlValidator);

        resolverUnderTest = new ApiObjectsArgumentAndReturnValueResolver(
                jsonMessageObjectReader, jsonMessageObjectWriter, xmlMessageObjectMapper, apiObjectValidator,
                apiContextHolder, wsdlValidatorFactory);

        methodParameter = mock(MethodParameter.class);
        when(methodParameter.getParameterType()).thenReturn((Class) SimpleGetRequest.class);

        jsonPayload = mock(ObjectSourceAndResult.class);
        jsonMessage = mock(JsonMessage.class);
        when(jsonMessage.getPayloadSource()).thenReturn(jsonPayload);

        soapMessage = mock(SoapMessage.class);
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        when(soapMessage.getPayloadSource()).thenReturn(new DOMSource(docBuilder.newDocument()));

        messageContext = mock(MessageContext.class);

    }

    @Test
    public void resolveArgumentJson() throws Exception {
        Map jsonMap = new HashMap();
        SimpleGetRequest expected = new SimpleGetRequest();

        when(jsonPayload.getObject()).thenReturn(jsonMap);
        when(messageContext.getRequest()).thenReturn(jsonMessage);
        when(jsonMessageObjectReader.convertValue(jsonMap, SimpleGetRequest.class)).thenReturn(expected);


        Object argument = resolverUnderTest.resolveArgument(messageContext, methodParameter);

        assertThat(argument, beanDiffer(expected));
        verify(jsonMessage).setApiRequestPayload(same(expected));
        verify(apiObjectValidator).validate(same(expected));
    }

    @Test
    public void resolveArgumentXml() throws Exception {
        SimpleGetRequest expected = new SimpleGetRequest();

        when(messageContext.getRequest()).thenReturn(soapMessage);
        when(xmlMessageObjectMapper.readValue(any(XMLStreamReader.class), same(SimpleGetRequest.class)))
                .thenReturn(expected);

        Object argument = resolverUnderTest.resolveArgument(messageContext, methodParameter);

        assertThat(argument, beanDiffer(expected));
        verify(soapMessage).setApiRequestPayload(same(expected));
        verify(apiObjectValidator).validate(same(expected));
    }

    @Test(expected = UnknownParameterApiException.class)
    public void resolveArgumentXmlUnrecognizedPropertyException() throws Exception {
        when(messageContext.getRequest()).thenReturn(soapMessage);
        when(xmlMessageObjectMapper.readValue(any(XMLStreamReader.class), same(SimpleGetRequest.class))).thenThrow(
                new UnrecognizedPropertyException(
                        null, "xxx", JsonLocation.NA, SimpleGetRequest.class, "zzz", Collections.emptyList()));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test(expected = InvalidFormatApiException.class)
    public void resolveArgumentXmlInvalidFormatException() throws Exception {
        when(messageContext.getRequest()).thenReturn(soapMessage);
        when(xmlMessageObjectMapper.readValue(any(XMLStreamReader.class), same(SimpleGetRequest.class))).thenThrow(
                new InvalidFormatException(null, "xxx", null, SimpleGetRequest.class));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }

    @Test(expected = IncorrectRequestApiException.class)
    public void resolveArgumentXmlMappingException() throws Exception {
        when(messageContext.getRequest()).thenReturn(soapMessage);
        when(xmlMessageObjectMapper.readValue(any(XMLStreamReader.class), same(SimpleGetRequest.class))).thenThrow(
                new JsonMappingException(null, "xxx"));
        resolverUnderTest.resolveArgument(messageContext, methodParameter);
    }
}
