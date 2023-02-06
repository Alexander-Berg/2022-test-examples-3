package ru.yandex.market.tsum.clients.sandbox;

import java.util.HashMap;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class TaskSearchRequestTest {

    @Test
    public void whenParameterIsNotSetShouldReturnEmptyJSONObject() {
        TaskSearchRequest taskSearchRequest = new TaskSearchRequest();

        Assert.assertEquals("Empty input parameters should return empty object",
            "{}",
            taskSearchRequest.formatInputParametersAsQueryParam()
        );
    }

    @Test
    public void givenAnEmptyParameterShouldReturnEmptyJSONObjectString() {
        TaskSearchRequest taskSearchRequest = new TaskSearchRequest();
        HashMap<String, String> inputParameters = new HashMap<>();

        taskSearchRequest.setInputParameters(inputParameters);

        Assert.assertEquals("{}",
            taskSearchRequest.formatInputParametersAsQueryParam()
        );
    }

    @Test
    public void givenNonEmptyParameterShouldReturnJSONObjectString() {
        TaskSearchRequest taskSearchRequest = new TaskSearchRequest();
        HashMap<String, String> inputParameters = new HashMap<>();
        inputParameters.put("key1", "value1");
        inputParameters.put("key2", "value2");

        taskSearchRequest.setInputParameters(inputParameters);

        Assert.assertThat(
            taskSearchRequest.formatInputParametersAsQueryParam(),
            Matchers.anyOf(
                Matchers.is("{\"key1\":\"value1\",\"key2\":\"value2\"}"),
                Matchers.is("{\"key2\":\"value2\",\"key1\":\"value1\"}")
            )
        );
    }
}
