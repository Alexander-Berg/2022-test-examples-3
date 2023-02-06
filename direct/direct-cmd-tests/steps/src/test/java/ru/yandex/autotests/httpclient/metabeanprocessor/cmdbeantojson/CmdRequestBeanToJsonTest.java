package ru.yandex.autotests.httpclient.metabeanprocessor.cmdbeantojson;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.direct.httpclient.util.requestbeantojson.RequestBeanToJsonProcessor.toJson;
import static ru.yandex.autotests.direct.utils.matchers.BeanEquals.beanEquals;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;

/**
 * Created by shmykov on 16.04.15.
 */
public class CmdRequestBeanToJsonTest {


    @Test
    public void test() {
        final String expectedJsonString = "{\"inner-bean\":{\"inner\":{\"property\":true}},\"normal_property\":\"normalProperty\",\"first\":{\"property\":\"string\"},\"third\":{\"array\":{\"property\":[3,4,5]}},\"inner\":{\"bean\":{\"inner\":{\"property\":true}},\"boolean\":0.1},\"second\":{\"list\":{\"property\":[1,2,3]}}}";

        FirstTestBean testBean = new FirstTestBean();
        testBean.setFirstProperty("string");
        testBean.setSecondListProperty(Arrays.asList(1, 2, 3));
        testBean.setThirdArrayProperty(new int[]{3, 4, 5});
        testBean.setNormalProperty("normalProperty");
        testBean.setFieldToBeIgnored("this field should be ignored");
        testBean.setFieldOnInnerLevel(0.1f);
        testBean.setResponseFieldToBeIgnored("this field should be ignored");
        InnerBean innerBean = new InnerBean();
        innerBean.setInnerProperty(true);
        testBean.setInnerBeanWithSlashedPath(innerBean);
        testBean.setInnerBeanWithNormalPath(innerBean);

        String json = toJson(testBean);
        JsonParser parser = new JsonParser();
        JsonElement expectedJson = parser.parse(expectedJsonString);
        JsonElement actualJson = parser.parse(json);
        assertThat("json строка, полученная из бина, соответствует ожиданиям", actualJson, beanEquals(expectedJson));
    }
}
