package ru.yandex.direct.internaltools.core.input;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.internaltools.core.annotations.input.Input;
import ru.yandex.direct.internaltools.core.container.InternalToolParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.core.input.InternalToolInputTestUtil.getInput;

public class InternalToolInputPreprocessingTest {
    private static class TestPreProcessorBase implements InternalToolInputPreProcessor<String> {
        boolean createCalled = false;
        boolean sendCalled = false;
        boolean receiveCalled = false;

        @Override
        public <T extends InternalToolParameter> InternalToolInput.Builder<T, String> preCreate(
                InternalToolInput.Builder<T, String> inputBuilder) {
            createCalled = true;
            return inputBuilder;
        }

        @Override
        public <T extends InternalToolParameter> InternalToolInput.Builder<T, String> preSend(
                InternalToolInput.Builder<T, String> inputBuilder) {
            sendCalled = true;
            return inputBuilder;
        }

        @Override
        public <T extends InternalToolParameter> InternalToolInput.Builder<T, String> preReceive(
                InternalToolInput.Builder<T, String> inputBuilder) {
            receiveCalled = true;
            return inputBuilder;
        }

        public void reset() {
            createCalled = false;
            sendCalled = false;
            receiveCalled = false;
        }
    }

    private static class TestPreprocessorOne extends TestPreProcessorBase {
    }

    private static class TestPreprocessorTwo extends TestPreProcessorBase {
    }

    private static class TestClass extends InternalToolParameter {
        @Input(label = "one", processors = TestPreprocessorOne.class)
        public String one;

        @Input(label = "two", processors = TestPreprocessorTwo.class)
        public String two;

        @Input(label = "three")
        public String three;

        @Input(label = "four", processors = {TestPreprocessorOne.class, TestPreprocessorTwo.class})
        public String four;
    }

    private TestPreprocessorOne preprocessorOne;
    private TestPreprocessorTwo preprocessorTwo;
    private List<InternalToolInputPreProcessor<?>> preProcessors;

    @Before
    public void before() {
        preprocessorOne = new TestPreprocessorOne();
        preprocessorTwo = new TestPreprocessorTwo();
        preProcessors = Arrays.asList(preprocessorOne, preprocessorTwo);
    }

    @Test
    public void testPreProcessorIsRegistered() throws NoSuchFieldException {
        //noinspection unchecked
        InternalToolInput<TestClass, String> input =
                (InternalToolInput<TestClass, String>) getInput(TestClass.class, preProcessors, "one");

        assertThat(input)
                .isNotNull();
        assertThat(input.getPreProcessors())
                .containsExactly(preprocessorOne);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(preprocessorOne.createCalled)
                .isTrue();
        soft.assertThat(preprocessorOne.sendCalled)
                .isFalse();
        soft.assertThat(preprocessorOne.receiveCalled)
                .isFalse();
        soft.assertAll();
    }

    @Test
    public void testAnotherPreProcessorIsRegistered() throws NoSuchFieldException {
        //noinspection unchecked
        InternalToolInput<TestClass, String> input =
                (InternalToolInput<TestClass, String>) getInput(TestClass.class, preProcessors, "two");

        assertThat(input)
                .isNotNull();
        assertThat(input.getPreProcessors())
                .containsExactly(preprocessorTwo);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(preprocessorTwo.createCalled)
                .isTrue();
        soft.assertThat(preprocessorTwo.sendCalled)
                .isFalse();
        soft.assertThat(preprocessorTwo.receiveCalled)
                .isFalse();
        soft.assertAll();
    }

    @Test
    public void testNoPreProcessorIsRegistered() throws NoSuchFieldException {
        //noinspection unchecked
        InternalToolInput<TestClass, String> input =
                (InternalToolInput<TestClass, String>) getInput(TestClass.class, preProcessors, "three");

        assertThat(input)
                .isNotNull();
        assertThat(input.getPreProcessors())
                .isEmpty();
    }

    @Test
    public void testBothPreProcessorsAreRegistered() throws NoSuchFieldException {
        //noinspection unchecked
        InternalToolInput<TestClass, String> input =
                (InternalToolInput<TestClass, String>) getInput(TestClass.class, preProcessors, "four");

        assertThat(input)
                .isNotNull();
        assertThat(input.getPreProcessors())
                .containsExactly(preprocessorOne, preprocessorTwo);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(preprocessorOne.createCalled)
                .isTrue();
        soft.assertThat(preprocessorOne.sendCalled)
                .isFalse();
        soft.assertThat(preprocessorOne.receiveCalled)
                .isFalse();
        soft.assertThat(preprocessorTwo.createCalled)
                .isTrue();
        soft.assertThat(preprocessorTwo.sendCalled)
                .isFalse();
        soft.assertThat(preprocessorTwo.receiveCalled)
                .isFalse();
        soft.assertAll();
    }

    @Test
    public void testPreProcessorSendIsCalled() throws NoSuchFieldException {
        //noinspection unchecked
        InternalToolInput<TestClass, String> input =
                (InternalToolInput<TestClass, String>) getInput(TestClass.class, preProcessors, "one");

        assertThat(input)
                .isNotNull();
        assertThat(input.getPreProcessors())
                .containsExactly(preprocessorOne);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(preprocessorOne.createCalled)
                .isTrue();
        soft.assertThat(preprocessorOne.sendCalled)
                .isFalse();
        soft.assertThat(preprocessorOne.receiveCalled)
                .isFalse();
        soft.assertAll();

        preprocessorOne.reset();
        input.applyPreProcessors();

        SoftAssertions softTwo = new SoftAssertions();
        softTwo.assertThat(preprocessorOne.createCalled)
                .isFalse();
        softTwo.assertThat(preprocessorOne.sendCalled)
                .isTrue();
        softTwo.assertThat(preprocessorOne.receiveCalled)
                .isFalse();
        softTwo.assertAll();

        preprocessorOne.reset();
        input.applyPreProcessors(false);

        SoftAssertions softThree = new SoftAssertions();
        softThree.assertThat(preprocessorOne.createCalled)
                .isFalse();
        softThree.assertThat(preprocessorOne.sendCalled)
                .isFalse();
        softThree.assertThat(preprocessorOne.receiveCalled)
                .isTrue();
        softThree.assertAll();
    }
}
