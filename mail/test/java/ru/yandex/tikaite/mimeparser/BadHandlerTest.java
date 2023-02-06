package ru.yandex.tikaite.mimeparser;

import java.io.IOException;

import org.apache.james.mime4j.MimeException;
import org.junit.Test;

public class BadHandlerTest {
    @Test(expected = MimeException.class)
    public void testBody() throws IOException, MimeException {
        new BadHandler().body(null, null);
    }

    @Test(expected = MimeException.class)
    public void testEndBodyPart() throws MimeException {
        new BadHandler().endBodyPart();
    }

    @Test(expected = MimeException.class)
    public void testEndHeader() throws MimeException {
        new BadHandler().endHeader();
    }

    @Test(expected = MimeException.class)
    public void testEndMessage() throws MimeException {
        new BadHandler().endMessage();
    }

    @Test(expected = MimeException.class)
    public void testEndMultipart() throws MimeException {
        new BadHandler().endMultipart();
    }

    @Test(expected = MimeException.class)
    public void testEpilogue() throws IOException, MimeException {
        new BadHandler().epilogue(null);
    }

    @Test(expected = MimeException.class)
    public void testField() throws MimeException {
        new BadHandler().field(null);
    }

    @Test(expected = MimeException.class)
    public void testPreamble() throws IOException, MimeException {
        new BadHandler().preamble(null);
    }

    @Test(expected = MimeException.class)
    public void testRaw() throws IOException, MimeException {
        new BadHandler().raw(null);
    }

    @Test(expected = MimeException.class)
    public void testStartBodyPart() throws MimeException {
        new BadHandler().startBodyPart();
    }

    @Test(expected = MimeException.class)
    public void testStartHeader() throws MimeException {
        new BadHandler().startHeader();
    }

    @Test(expected = MimeException.class)
    public void testStartMessage() throws MimeException {
        new BadHandler().startMessage();
    }

    @Test(expected = MimeException.class)
    public void testStartMultipart() throws MimeException {
        new BadHandler().startMultipart(null);
    }
}

