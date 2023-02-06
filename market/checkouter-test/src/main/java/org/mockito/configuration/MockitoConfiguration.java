package org.mockito.configuration;

import org.mockito.stubbing.Answer;

public class MockitoConfiguration extends DefaultMockitoConfiguration {

    private static Answer<Object> defaultAnswer;

    @Override
    public Answer<Object> getDefaultAnswer() {
        return defaultAnswer != null
                ? defaultAnswer
                : super.getDefaultAnswer();
    }

    public static void setDefaultAnswer(Answer<Object> answer) {
        defaultAnswer = answer;
    }
}
