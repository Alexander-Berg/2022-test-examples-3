package ru.yandex.market.common.util.io;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public class InMemoryOrTemporaryFileSendingOutputStreamTest {
    private static InMemoryOrTemporaryFileSendingOutputStream createStream(int maxInMemorySize,
                                                                           IOSupplier<Path> pathSupplier,
                                                                           ResourceSender sender) {
        return new InMemoryOrTemporaryFileSendingOutputStream(maxInMemorySize, pathSupplier, sender);
    }

    private static void testStream(int maxInMemorySize, StreamTester tester) throws IOException {
        IOSupplier<Path> tempFileSupplier = () -> Files.createTempFile("test", ".tmp");
        ResourceSender sender = new StreamIORunnableResourceSender(tester::createAssertValidResultAction);
        try (OutputStream stream = createStream(maxInMemorySize, tempFileSupplier, sender)) {
            IORunnable action = tester.createAction(stream);
            action.run();
        }
    }

    private static StreamTester validateCharacterContents(String data, Charset charset) {
        return new EncodedCharacterStreamTester(charset, new ValidatingStringCharacterStreamTester(data));
    }

    private static StreamTester validateLength(int length) {
        return new ValidatingLengthStreamTester(length);
    }

    @Test
    public void testStreamContents() throws IOException {
        testStream(3, validateCharacterContents("", StandardCharsets.UTF_8));
        testStream(3, validateCharacterContents("a", StandardCharsets.UTF_8));
        testStream(3, validateCharacterContents("ab", StandardCharsets.UTF_8));
        testStream(3, validateCharacterContents("abc", StandardCharsets.UTF_8));
        testStream(3, validateCharacterContents("abcd", StandardCharsets.UTF_8));
        testStream(3, validateCharacterContents("abcde", StandardCharsets.UTF_8));
        testStream(3, validateCharacterContents("abcdef", StandardCharsets.UTF_8));
    }

    @Test
    public void testStreamLength() throws IOException {
        testStream(1024, validateLength(0));
        testStream(1024, validateLength(1));

        testStream(1024, validateLength(1023));
        testStream(1024, validateLength(1024));
        testStream(1024, validateLength(1025));

        testStream(1024, validateLength(2048));
    }

    private static class StreamIORunnableResourceSender implements ResourceSender {
        private final Function<InputStream, IORunnable> actionFactory;

        StreamIORunnableResourceSender(Function<InputStream, IORunnable> actionFactory) {
            this.actionFactory = actionFactory;
        }

        @Override
        public void sendResource(@Nonnull Resource resource) {
            try (InputStream stream = resource.getInputStream()) {
                IORunnable assertValidResultAction = actionFactory.apply(stream);
                assertValidResultAction.run();
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    private static class ValidatingLengthStreamTester implements StreamTester {
        private final long length;

        ValidatingLengthStreamTester(long length) {
            this.length = length;
        }

        @Override
        public IORunnable createAction(OutputStream stream) {
            return () -> {
                for (int i = 0; i < length; i++) {
                    stream.write('a');
                }
            };
        }

        @Override
        public IORunnable createAssertValidResultAction(InputStream stream) {
            return () -> {
                long readLength = 0;
                byte[] buffer = new byte[1024];
                int len;
                while ((len = stream.read(buffer)) >= 0) {
                    readLength += len;
                }
                Assert.assertEquals(length, readLength);
            };
        }
    }

    private interface StreamTester {
        IORunnable createAction(OutputStream stream);
        IORunnable createAssertValidResultAction(InputStream stream);
    }

    private static class ValidatingStringCharacterStreamTester implements CharacterStreamTester {
        private final String data;

        ValidatingStringCharacterStreamTester(String data) {
            this.data = data;
        }

        @Override
        public IORunnable createAction(Writer stream) {
            return () -> stream.write(data);
        }

        @Override
        public IORunnable createAssertValidResultAction(Reader stream) {
            return () -> {
                StringBuilder builder = new StringBuilder();
                char[] buffer = new char[1024];
                int len;
                while ((len = stream.read(buffer)) >= 0) {
                    builder.append(buffer, 0, len);
                }
                Assert.assertEquals(data, builder.toString());
            };
        }
    }

    private interface CharacterStreamTester {
        IORunnable createAction(Writer stream);
        IORunnable createAssertValidResultAction(Reader stream);
    }

    private static class EncodedCharacterStreamTester implements StreamTester {
        private final Charset encoding;
        private final CharacterStreamTester tester;

        EncodedCharacterStreamTester(Charset encoding, CharacterStreamTester tester) {
            this.encoding = encoding;
            this.tester = tester;
        }

        @Override
        public IORunnable createAction(OutputStream stream) {
            return () -> {
                try (Writer writer = new OutputStreamWriter(stream, encoding)) {
                    IORunnable action = tester.createAction(writer);
                    action.run();
                }
            };
        }

        @Override
        public IORunnable createAssertValidResultAction(InputStream stream) {
            return tester.createAssertValidResultAction(new InputStreamReader(stream, encoding));
        }
    }
}
