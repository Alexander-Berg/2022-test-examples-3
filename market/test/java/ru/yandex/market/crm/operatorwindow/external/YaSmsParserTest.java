package ru.yandex.market.crm.operatorwindow.external;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.operatorwindow.external.yasms.YaSmsParser;
import ru.yandex.market.crm.operatorwindow.external.yasms.YaSmsResult;
import ru.yandex.market.crm.operatorwindow.serialization.CustomXmlDeserializer;
import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;

public class YaSmsParserTest {

    private static final CustomXmlDeserializer deserializer
            = new CustomXmlDeserializer(new ObjectMapperFactory(Optional.empty()));
    private static final YaSmsParser parser = new YaSmsParser(deserializer);

    @Test
    public void invalidSentSms() throws IOException {
        YaSmsResult result =
                parser.parseSentSms(IOUtils.toByteArray(getClass().getResourceAsStream("failure-send-sms-response" +
                        ".xml")));
        Assertions.assertEquals("NOCURRENT", result.getErrorCode());
        Assertions.assertEquals("User does not have an active phone to recieve messages", result.getErrorMessage());
    }

    @Test
    public void validSentSms() throws IOException {
        YaSmsResult result =
                parser.parseSentSms(IOUtils.toByteArray(getClass().getResourceAsStream("success-send-sms-response" +
                        ".xml")));
        Assertions.assertEquals(127000000003456L, result.getMessage().getId());
    }
}
