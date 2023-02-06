package ru.yandex.market.partner.mvc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.xml.sax.SAXException;

import ru.yandex.common.framework.core.DefaultServantInfo;
import ru.yandex.common.framework.core.ServantInfo;
import ru.yandex.common.framework.core.SimpleErrorInfo;
import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.common.test.spring.MVCSerializationTest;
import ru.yandex.market.core.error.VerboseExceptionErrorInfo;
import ru.yandex.market.mbi.web.MvcConfig;
import ru.yandex.market.mbi.web.converter.MbiHttpMessageConverter;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class ResponseWrapperSerializeTest extends MVCSerializationTest {

    private MbiHttpMessageConverter json;
    private MbiHttpMessageConverter xml;
    private SerializationChecker checker;

    @Before
    public void setUp() {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        new MvcConfig().configureMessageConverters(converters);
        json = (MbiHttpMessageConverter) converters.get(0);
        xml = (MbiHttpMessageConverter) converters.get(1);
        checker = new SerializationChecker(
                obj -> out(json, obj),
                null,
                obj -> out(xml, obj),
                null
        );
    }

    @Test
    public void testEmptyResponse() throws JSONException, SAXException, IOException {
        ResponseWrapper wrapper = new ResponseWrapper(createServantInfo(), 1000L, "testAction");

        checker.testSerialization(
                wrapper,
                "{'servant':'testServ','host':'testHost','version':'1','executingTime':'[1000]','actions':'[testAction]'}",
                "<data servant='testServ' version='1' host='testHost' executing-time='[1000]' actions='[testAction]'/>"
        );
    }

    @Test
    public void testOneError() throws JSONException, SAXException, IOException {
        ResponseWrapper wrapper = new ResponseWrapper(createServantInfo(), 1000L, "testAction");
        wrapper.addError(new SimpleErrorInfo("asdf"));
        checker.testSerialization(
                wrapper,
                "{'errors':[{'messageCode':'asdf'}],'servant':'testServ'," +
                        "'host':'testHost','version':'1','executingTime':'[1000]','actions':'[testAction]'}",

                "<data servant='testServ' host='testHost' version='1' actions='[testAction]' executing-time='[1000]'>" +
                        "<errors><error message-code='asdf'/></errors></data>"
        );
    }

    @Test
    public void testTwoError() throws JSONException, SAXException, IOException {
        ResponseWrapper wrapper = new ResponseWrapper(createServantInfo(), 1000L, "testAction");
        wrapper.addError(new SimpleErrorInfo("asdf"));
        wrapper.addError(new VerboseExceptionErrorInfo(new HttpRequestMethodNotSupportedException("GET", new String[]{"POST"})) {
            @Override
            public String getDetails() {
                return null;
            }

            @Override
            public String getCause() {
                return null;
            }
        });
        checker.testSerialization(
                wrapper,
                "{'errors':[{'messageCode':'asdf'},{'object':{'method':'GET','supported':['POST']}," +
                        "'name':'HttpRequestMethodNotSupportedException'," +
                        "'message':'Request method \\'GET\\' not supported'}],'servant':'testServ'," +
                        "'host':'testHost','version':'1','executingTime':'[1000]','actions':'[testAction]'}",

                "<data servant='testServ' host='testHost' version='1' actions='[testAction]' executing-time='[1000]'>" +
                        "<errors><error message-code='asdf'/><error name='HttpRequestMethodNotSupportedException' " +
                        "message=\"Request method 'GET' not supported\"><object><method>GET</method>" +
                        "<supported>POST</supported></object></error></errors></data>"
        );
    }

    protected ServantInfo createServantInfo() {
        DefaultServantInfo servantInfo = new DefaultServantInfo() {
            @Override
            public String getVersion() {
                return "1";
            }

            @Override
            public String getHostName() {
                return "testHost";
            }
        };
        servantInfo.setName("testServ");
        return servantInfo;
    }

}
