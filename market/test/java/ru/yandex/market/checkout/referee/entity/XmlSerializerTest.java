package ru.yandex.market.checkout.referee.entity;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import org.custommonkey.xmlunit.HTMLDocumentBuilder;
import org.custommonkey.xmlunit.TolerantSaxDocumentBuilder;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.common.xml.SimpleXmlWriter;
import ru.yandex.market.checkout.entity.Attachment;
import ru.yandex.market.checkout.entity.AttachmentGroup;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.Message;
import ru.yandex.market.checkout.entity.Note;
import ru.yandex.market.checkout.entity.xml.AttachmentGroupXmlDeserializer;
import ru.yandex.market.checkout.entity.xml.AttachmentGroupXmlSerializer;
import ru.yandex.market.checkout.entity.xml.AttachmentXmlDeserializer;
import ru.yandex.market.checkout.entity.xml.AttachmentXmlSerializer;
import ru.yandex.market.checkout.entity.xml.ConversationXmlDeserializer;
import ru.yandex.market.checkout.entity.xml.ConversationXmlSerializer;
import ru.yandex.market.checkout.entity.xml.MessageXmlDeserializer;
import ru.yandex.market.checkout.entity.xml.MessageXmlSerializer;
import ru.yandex.market.checkout.entity.xml.NoteXmlDeserializer;
import ru.yandex.market.checkout.entity.xml.NoteXmlSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration({
        "classpath:checkout-referee-test.xml"
})
public class XmlSerializerTest {

    // serializers
    @Autowired
    private ConversationXmlSerializer conversationXmlSerializer;
    @Autowired
    private MessageXmlSerializer messageXmlSerializer;
    @Autowired
    private AttachmentGroupXmlSerializer attachmentGroupXmlSerializer;
    @Autowired
    private NoteXmlSerializer noteXmlSerializer;
    @Autowired
    private AttachmentXmlSerializer attachmentXmlSerializer;

    // deserializers
    @Autowired
    private ConversationXmlDeserializer conversationXmlDeserializer;
    @Autowired
    private MessageXmlDeserializer messageXmlDeserializer;
    @Autowired
    private AttachmentGroupXmlDeserializer attachmentGroupXmlDeserializer;
    @Autowired
    private NoteXmlDeserializer noteXmlDeserializer;
    @Autowired
    private AttachmentXmlDeserializer attachmentXmlDeserializer;

    @BeforeEach
    public void onLoad() {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }

    public static void assertHTMLEquals(String expectedHTML, String actualHTML) throws Exception {
        TolerantSaxDocumentBuilder tolerantSaxDocumentBuilder = new TolerantSaxDocumentBuilder(XMLUnit.newTestParser());
        HTMLDocumentBuilder htmlDocumentBuilder = new HTMLDocumentBuilder(tolerantSaxDocumentBuilder);
        XMLAssert.assertXMLEqual(htmlDocumentBuilder.parse(expectedHTML), htmlDocumentBuilder.parse(actualHTML));
    }

    @Test
    public void testConversation() throws Exception {
        Conversation expectedObj = CheckoutRefereeHelper.getConversation();
        String objectString = CheckoutRefereeHelper.getXmlConversation();
        final StringWriter stringWriter = new StringWriter();
        conversationXmlSerializer.serializeXml(expectedObj, new SimpleXmlWriter(stringWriter));
        assertHTMLEquals(objectString, stringWriter.toString());

        conversationXmlDeserializer.parseXmlStream(new ByteArrayInputStream(objectString.getBytes()));
        Conversation obj = conversationXmlDeserializer.getParsed();
        assertEquals(obj, expectedObj);
        assertEquals(2, obj.getUpdatedMessages().size());
    }

    @Test
    public void testMessage() throws Exception {
        Message expectedObj = CheckoutRefereeHelper.getMessage();
        String objectString = CheckoutRefereeHelper.getXmlMessage();
        final StringWriter stringWriter = new StringWriter();
        messageXmlSerializer.serializeXml(expectedObj, new SimpleXmlWriter(stringWriter));
        String newObjectString = stringWriter.toString();
        assertHTMLEquals(objectString, newObjectString);

        messageXmlDeserializer.parseXmlStream(new ByteArrayInputStream(objectString.getBytes()));
        Message obj = messageXmlDeserializer.getParsed();

        assertEquals(obj.getText(), expectedObj.getText());
        assertEquals(obj.getMessageTs(), expectedObj.getMessageTs());
    }

    @Test
    public void testNote() throws Exception {
        Note expectedObj = CheckoutRefereeHelper.getNote();
        String objectString = CheckoutRefereeHelper.getXmlNote();
        final StringWriter stringWriter = new StringWriter();
        noteXmlSerializer.serializeXml(expectedObj, new SimpleXmlWriter(stringWriter));
        assertHTMLEquals(objectString, stringWriter.toString());

        noteXmlDeserializer.parseXmlStream(new ByteArrayInputStream(objectString.getBytes()));
        Note obj = noteXmlDeserializer.getParsed();

        assertEquals(obj.getConversationId(), expectedObj.getConversationId());
        assertEquals(obj.getConvStatusAfter(), expectedObj.getConvStatusAfter());
    }

    @Test
    public void testAttachmentGroup() throws Exception {
        AttachmentGroup expectedObj = CheckoutRefereeHelper.getAttachmentGroup();
        String objectString = CheckoutRefereeHelper.getXmlAttachmentGroup();
        final StringWriter stringWriter = new StringWriter();
        attachmentGroupXmlSerializer.serializeXml(expectedObj, new SimpleXmlWriter(stringWriter));
        assertHTMLEquals(objectString, stringWriter.toString());

        attachmentGroupXmlDeserializer.parseXmlStream(new ByteArrayInputStream(objectString.getBytes()));
        AttachmentGroup obj = attachmentGroupXmlDeserializer.getParsed();

        assertEquals(obj.getConversationId(), expectedObj.getConversationId());
        assertEquals(obj.getCreatedTs(), expectedObj.getCreatedTs());
        assertEquals(obj.getMessageId(), expectedObj.getMessageId());
    }

    @Test
    public void testAttachment() throws Exception {
        Attachment expectedObj = CheckoutRefereeHelper.getAttachment();
        String objectString = CheckoutRefereeHelper.getXmlAttachment();
        final StringWriter stringWriter = new StringWriter();
        attachmentXmlSerializer.serializeXml(expectedObj, new SimpleXmlWriter(stringWriter));
        assertHTMLEquals(objectString, stringWriter.toString());

        attachmentXmlDeserializer.parseXmlStream(new ByteArrayInputStream(objectString.getBytes()));
        Attachment obj = attachmentXmlDeserializer.getParsed();

        assertEquals(expectedObj, obj);
    }

}
