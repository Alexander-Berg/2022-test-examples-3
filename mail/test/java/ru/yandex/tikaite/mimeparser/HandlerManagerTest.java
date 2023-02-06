package ru.yandex.tikaite.mimeparser;

import java.io.IOException;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.ContentHandler;
import org.junit.Assert;
import org.junit.Test;

public class HandlerManagerTest {
    private int paramCount;

    private ContentHandler createManager() {
        paramCount = -1;
        HandlerManager manager = new HandlerManager();
        manager.push(new ManagedHandler() {
            @Override
            public void fail(final Object... params) {
                paramCount = params.length;
            }
        });
        return manager;
    }

    @Test
    public void testBody() throws IOException, MimeException {
        createManager().body(null, null);
        Assert.assertEquals(2, paramCount);
    }

    @Test
    public void testEndBodyPart() throws MimeException {
        createManager().endBodyPart();
        Assert.assertEquals(0, paramCount);
    }

    @Test
    public void testEndHeader() throws MimeException {
        createManager().endHeader();
        Assert.assertEquals(0, paramCount);
    }

    @Test
    public void testEndMessage() throws MimeException {
        createManager().endMessage();
        Assert.assertEquals(0, paramCount);
    }

    @Test
    public void testEndMultipart() throws MimeException {
        createManager().endMultipart();
        Assert.assertEquals(0, paramCount);
    }

    @Test
    public void testEpilogue() throws IOException, MimeException {
        createManager().epilogue(null);
        Assert.assertEquals(1, paramCount);
    }

    @Test
    public void testField() throws MimeException {
        createManager().field(null);
        Assert.assertEquals(1, paramCount);
    }

    @Test
    public void testPreamble() throws IOException, MimeException {
        createManager().preamble(null);
        Assert.assertEquals(1, paramCount);
    }

    @Test
    public void testRaw() throws IOException, MimeException {
        createManager().raw(null);
        Assert.assertEquals(1, paramCount);
    }

    @Test
    public void testStartBodyPart() throws MimeException {
        createManager().startBodyPart();
        Assert.assertEquals(0, paramCount);
    }

    @Test
    public void testStartHeader() throws MimeException {
        createManager().startHeader();
        Assert.assertEquals(0, paramCount);
    }

    @Test
    public void testStartMessage() throws MimeException {
        createManager().startMessage();
        Assert.assertEquals(0, paramCount);
    }

    @Test
    public void testStartMultipart() throws MimeException {
        createManager().startMultipart(null);
        Assert.assertEquals(1, paramCount);
    }
}

