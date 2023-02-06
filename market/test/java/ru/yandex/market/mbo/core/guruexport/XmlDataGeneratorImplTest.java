package ru.yandex.market.mbo.core.guruexport;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.mbo.core.notebook.guru.XmlGenerator;

import java.util.Objects;

public class XmlDataGeneratorImplTest {

    @Test
    public void testInnerExceptionAreThrown() {
        XmlDataGeneratorImpl xmlDataGenerator = new XmlDataGeneratorImpl(xml -> {
            throw new XmlDataGeneratorImplException();
        });

        Assertions.assertThatThrownBy(() -> {
            xmlDataGenerator.generate(new SuccessXmlGenerator());
        })
            .hasCauseInstanceOf(XmlDataGeneratorImplException.class);
    }

    @Test
    public void testExceptionsOnStartAreThrown() {
        XmlDataGeneratorImpl xmlDataGenerator = new XmlDataGeneratorImpl(xml -> { });

        Assertions.assertThatThrownBy(() -> {
            xmlDataGenerator.generate(new FailOnStartXmlGenerator());
        })
            .hasCauseInstanceOf(XmlGeneratorException.class);
    }

    @Test
    public void testExceptionsOnFinishAreThrown() {
        XmlDataGeneratorImpl xmlDataGenerator = new XmlDataGeneratorImpl(xml -> { });

        Assertions.assertThatThrownBy(() -> {
            xmlDataGenerator.generate(new FailOnFinishXmlGenerator());
        })
            .hasCauseInstanceOf(XmlGeneratorException.class);
    }

    @Test
    public void testExceptionsOnFinishAndInnerAreThrown() {
        XmlDataGeneratorImpl xmlDataGenerator = new XmlDataGeneratorImpl(xml -> {
            throw new XmlDataGeneratorImplException();
        });

        Assertions.assertThatThrownBy(() -> {
            xmlDataGenerator.generate(new FailOnFinishXmlGenerator());
        })
            .hasCauseInstanceOf(XmlDataGeneratorImplException.class)
            .hasSuppressedException(new XmlGeneratorException());
    }

    private class XmlDataGeneratorImplException extends RuntimeException {
        private String message;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            XmlDataGeneratorImplException that = (XmlDataGeneratorImplException) o;
            return Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }
    private class XmlGeneratorException extends RuntimeException {
        private String message;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            XmlGeneratorException that = (XmlGeneratorException) o;
            return Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(message);
        }
    }

    private class SuccessXmlGenerator implements XmlGenerator {

        @Override
        public void putElement(String tag, String... attributes) {

        }

        @Override
        public void startElement(String tag) {

        }

        @Override
        public void startElement(String tag, String... attributes) {

        }

        @Override
        public void putTagWithBody(String tag, String value) {

        }

        @Override
        public void endElement(String tag) {

        }

        @Override
        public void addText(String value) {

        }

        @Override
        public void addComment(String comment) {

        }

        @Override
        public void start() throws Exception {

        }

        @Override
        public void finish() throws Exception {

        }
    }
    private class FailOnStartXmlGenerator implements XmlGenerator {

        @Override
        public void putElement(String tag, String... attributes) {

        }

        @Override
        public void startElement(String tag) {
        }

        @Override
        public void startElement(String tag, String... attributes) {

        }

        @Override
        public void putTagWithBody(String tag, String value) {

        }

        @Override
        public void endElement(String tag) {

        }

        @Override
        public void addText(String value) {

        }

        @Override
        public void addComment(String comment) {

        }

        @Override
        public void start() throws Exception {
            throw new XmlGeneratorException();
        }

        @Override
        public void finish() throws Exception {

        }
    }
    private class FailOnFinishXmlGenerator implements XmlGenerator {

        @Override
        public void putElement(String tag, String... attributes) {

        }

        @Override
        public void startElement(String tag) {
        }

        @Override
        public void startElement(String tag, String... attributes) {

        }

        @Override
        public void putTagWithBody(String tag, String value) {

        }

        @Override
        public void endElement(String tag) {

        }

        @Override
        public void addText(String value) {

        }

        @Override
        public void addComment(String comment) {

        }

        @Override
        public void start() throws Exception {
        }

        @Override
        public void finish() throws Exception {
            throw new XmlGeneratorException();
        }
    }
}
