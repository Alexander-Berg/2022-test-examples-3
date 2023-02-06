package ru.yandex.market.crm.util;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MirrorOutputStreamTest {

    @Test
    public void checkClose() throws IOException {
        OutputStream os1 = Mockito.mock(OutputStream.class);
        MirrorOutputStream os = new MirrorOutputStream(os1);
        os.close();

        // Проверяем, что вызвали метод close у делегируемого OutputStream
        Mockito.verify(os1).close();
    }

    @Test
    public void checkFlush() throws IOException {
        OutputStream os1 = Mockito.mock(OutputStream.class);
        MirrorOutputStream os = new MirrorOutputStream(os1);
        os.flush();

        // Проверяем, что вызвали метод flush у делегируемого OutputStream
        Mockito.verify(os1).flush();
    }

    @Test
    public void checkWriteBytea() throws IOException {
        byte[] value = randomByteArray();

        OutputStream os1 = Mockito.mock(OutputStream.class);
        MirrorOutputStream os = new MirrorOutputStream(os1);
        os.write(value);

        // Проверяем, что вызвали метод write у делегируемого OutputStream
        Mockito.verify(os1).write(Mockito.eq(value));
    }

    @Test
    public void checkWriteByteaWithOffset() throws IOException {
        byte[] value = randomByteArray();
        int off = 2;
        int len = 3;

        OutputStream os1 = Mockito.mock(OutputStream.class);
        MirrorOutputStream os = new MirrorOutputStream(os1);
        os.write(value, off, len);

        // Проверяем, что вызвали метод write у делегируемого OutputStream
        Mockito.verify(os1).write(Mockito.eq(value), Mockito.eq(off), Mockito.eq(len));
    }

    @Test
    public void checkWriteInt() throws IOException {
        int value = ThreadLocalRandom.current().nextInt();

        OutputStream os1 = Mockito.mock(OutputStream.class);
        MirrorOutputStream os = new MirrorOutputStream(os1);
        os.write(value);

        // Проверяем, что вызвали метод write у делегируемого OutputStream
        Mockito.verify(os1).write(Mockito.eq(value));
    }

    @Test
    public void write_double() throws IOException {
        byte[] value = randomByteArray();

        ByteArrayOutputStream os1 = new ByteArrayOutputStream();
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        MirrorOutputStream os = new MirrorOutputStream(os1, os2);
        os.write(value);

        Assertions.assertArrayEquals(
                value, os1.toByteArray(), "Данные записанные в декорированный поток должны совпадать с записанными в " +
                        "MirrorOutputStream");
        Assertions.assertArrayEquals(
                value, os2.toByteArray(), "Данные записанные в декорированный поток должны совпадать с записанными в " +
                        "MirrorOutputStream");
    }

    @Test
    public void write_single() throws IOException {
        byte[] value = randomByteArray();

        ByteArrayOutputStream os1 = new ByteArrayOutputStream();
        MirrorOutputStream os = new MirrorOutputStream(os1);
        os.write(value);

        Assertions.assertArrayEquals(
                value, os1.toByteArray(), "Данные записанные в декорированный поток должны совпадать с записанными в " +
                        "MirrorOutputStream");
    }

    private byte[] randomByteArray() {
        return CrmStrings.getBytes(UUID.randomUUID().toString());
    }

}
