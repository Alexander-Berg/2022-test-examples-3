package ru.yandex.calendar.logic.sending.real;

import javax.xml.transform.Source;

import org.jdom.Document;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.ics.iv5j.ical.IcsCalendar;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.logic.sending.param.MessageParameters;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.util.rr.CalendarRandomValueGenerator;
import ru.yandex.calendar.util.xml.XslUtils;
import ru.yandex.commune.mail.ContentType;
import ru.yandex.commune.mail.DefaultContent;
import ru.yandex.commune.mail.MailMessage;
import ru.yandex.commune.mail.MimeMessagePart;
import ru.yandex.commune.mail.Multipart;
import ru.yandex.misc.io.file.File2;
import ru.yandex.misc.reflection.ClassX;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.jdom.JdomUtils;
import ru.yandex.misc.xml.support.XmlWriteFormat;

/**
 * @author Stepan Koltsov
 */
public class MailSenderSampleMessageTest {

    public static <T> T sampleValue(ClassX<T> type, final MailType mailType) {
        class CustomizedRun extends CalendarRandomValueGenerator {
            @Override
            protected Object randomValueImpl(ClassX<Object> type) {
                if (type.sameAs(MailType.class)) {
                    return mailType;
                } else if (type.sameAs(Language.class)) {
                    ListF<Language> tankered = mailType.getXslName().getSupportedLangs();
                    return super.randomValue(type.asType(), Option.of(tankered.map(Enum::name).toArray(String.class)));
                } else if (type.sameAs(ResourceInfo.class)) {
                    return new ResourceInfo(super.randomValue(Resource.class), super.randomValue(Office.class));
                } else if (type.sameAs(IcsCalendar.class)) {
                    return new IcsCalendar();
                } else {
                    return super.randomValueImpl(type);
                }
            }
        }
        return new CustomizedRun().randomValue(type);
    }

    @Test
    public void generateAll() {
        File2 dir = File2.valueOf("tmp");
        dir.mkdirs();

        for (MailType mailType : MailType.values()) {
            generateMailInner(dir, mailType);
        }

        // XXX: customize InvitationMessageParameters
    }

    @Test
    @Ignore
    public void generateMail() {
        File2 dir = File2.valueOf("tmp");
        dir.mkdirs();

        generateMailInner(dir, MailType.EVENT_INVITATION);
    }

    private void generateMailInner(File2 dir, MailType mailType) {
        generateMailInner(dir.child(mailType.name().toLowerCase()), mailType, mailType.getXslName().getSS());

        mailType.getOutlookerXslName().forEach(name ->
                generateMailInner(dir.child(mailType.name().toLowerCase() + ".outlooker"), mailType, name.getSS()));
    }

    private void generateMailInner(File2 basename, MailType mailType, Source source) {
        ClassX<? extends MessageParameters> clazz = mailType.getDataClass();
        MessageParameters sampleValue = sampleValue(clazz, mailType);
        JdomUtils.I.writeElement(sampleValue.toOldStyleXml(), basename.addSuffixToName(".0.xml").asOutputStreamTool(), XmlWriteFormat.prettyPrintFormat());
        Document rXml = XslUtils.applyXslt(sampleValue.toOldStyleXml(), source);
        JdomUtils.I.writeDocument(rXml, basename.addSuffixToName(".1.xml").asOutputStreamTool(), XmlWriteFormat.prettyPrintFormat());

        MailMessage message = MessageXml.parseMessage(rXml);

        basename.addSuffixToName(".eml").write(message.serializeToBytes());

        Option<String> html = Option.empty();
        Option<String> text = Option.empty();

        Function<MimeMessagePart, String> contentValueF = cnt -> (String) ((DefaultContent) cnt.getContent()).getValue();

        if (message.getContent() instanceof DefaultContent) {
            if (message.getContentType().get().getTypeSubtype().equals(ContentType.TEXT_HTML.getTypeSubtype())) {
                html = Option.of(contentValueF.apply(message));
            } else if (message.getContentType().get().getTypeSubtype().equals(ContentType.TEXT_PLAIN.getTypeSubtype())) {
                text = Option.of(contentValueF.apply(message));
            } else {
                Assert.fail("Unexpected single part content type");
            }
        } else if (message.getContent() instanceof Multipart) {
            html = message.getBottomHtmlParts().singleO().map(contentValueF);
            text = message.getBottomPlainTextParts().singleO().map(contentValueF);
        } else {
            Assert.fail("Unexpected mail message content");
        }

        if (html.isPresent()) {
            basename.addSuffixToName(".html").write(html.get());
        }
        if (text.isPresent()) {
            basename.addSuffixToName(".txt").write(text.get());
        }
    }

} //~
