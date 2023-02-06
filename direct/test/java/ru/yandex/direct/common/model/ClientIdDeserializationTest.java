package ru.yandex.direct.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.Diff;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.differ.AbstractDiffer;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.utils.JsonUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class ClientIdDeserializationTest {
    private String testCase;
    private Object expected;

    private static class NullSafeSimpleTypeDiffer extends AbstractDiffer {
        @Override
        public List<Diff> compare(Object actual, Object expected) {
            List<Diff> result = new ArrayList<>();
            if ((actual == null && expected != null) || (actual != null && !actual.equals(expected))) {
                result.add(Diff.changed(getField(), actual, expected));
            }
            return result;
        }

    }
    private static final DefaultCompareStrategy STRATEGY = DefaultCompareStrategies.allFields()
            .forClasses(ClientId.class).useDiffer(new NullSafeSimpleTypeDiffer());


    private static <T> BeanDifferMatcher<T> beanDiffer(T object) {
        return BeanDifferMatcher.beanDiffer(object).useCompareStrategy(STRATEGY);
    }

    @Test
    public void checkBeanDiffer() {
        // beanDiffer сравнивает поля по публичным геттерам, и только если понимает как их сравнивать.
        ClientId clientId1 = ClientId.fromLong(1);
        ClientId clientId2 = ClientId.fromLong(2);
        ObjectTestCase oCase1 = new ObjectTestCase(ClientId.fromLong(3));
        ObjectTestCase oCase2 = new ObjectTestCase(ClientId.fromLong(4));
        ListTestCase lCase1 = new ListTestCase(ClientId.fromLong(5));
        ListTestCase lCase2 = new ListTestCase(ClientId.fromLong(6));

        // положительные сценарии
        assertThat(clientId1, beanDiffer(ClientId.fromLong(1)));
        assertThat(oCase1, beanDiffer(new ObjectTestCase(ClientId.fromLong(3))));
        assertThat(lCase1, beanDiffer(new ListTestCase(ClientId.fromLong(5))));

        // негативные сценарии
        assertFalse(beanDiffer(clientId2).matches(clientId1));
        assertFalse(beanDiffer(oCase2).matches(oCase1));
        assertFalse(beanDiffer(lCase2).matches(lCase1));
    }

    public static class ObjectTestCase {
        @JsonProperty
        private ClientId clientId;

        @SuppressWarnings("unused")
            // используется при создании объекта из json
        ObjectTestCase() {
        }

        ObjectTestCase(ClientId clientId) {
            this.clientId = clientId;
        }

        @SuppressWarnings("unused") // используется beanDiffer'ом
        public ClientId getClientId() {
            return clientId;
        }
    }

    public static class ListTestCase {
        @JsonProperty
        private List<ClientId> clientIds;

        ListTestCase() {
            this.clientIds = new ArrayList<>();
        }

        ListTestCase(ClientId... clientIds) {
            this.clientIds = Arrays.asList(clientIds);
        }

        @SuppressWarnings("unused") // используется beanDiffer'ом
        public List<ClientId> getClientIds() {
            return clientIds;
        }
    }

    @Test
    public void DeserializeValueFromObject() {
        testCase = "{\"clientId\":1}";
        expected = new ObjectTestCase(ClientId.fromLong(1));
        assertThat(JsonUtils.fromJson(testCase, ObjectTestCase.class), beanDiffer(expected));
    }

    @Test
    public void DeserializeNullFromObject() {
        testCase = "{\"clientId\":null}";
        expected = new ObjectTestCase(null);
        assertThat(JsonUtils.fromJson(testCase, ObjectTestCase.class), beanDiffer(expected));
    }

    @Test
    public void DeserializeEmptyList() {
        testCase = "{\"clientIds\":[]}";
        expected = new ListTestCase();
        assertThat(JsonUtils.fromJson(testCase, ListTestCase.class), beanDiffer(expected));
    }

    @Test
    public void DeserializeValueFromList() {
        testCase = "{\"clientIds\":[2]}";
        expected = new ListTestCase(ClientId.fromLong(2));
        assertThat(JsonUtils.fromJson(testCase, ListTestCase.class), beanDiffer(expected));
    }

    @Test
    public void DeserializeNullFromList() {
        testCase = "{\"clientIds\":[null]}";
        expected = new ListTestCase(ClientId.fromNullableLong(null));
        assertThat(JsonUtils.fromJson(testCase, ListTestCase.class), beanDiffer(expected));
    }

    @Test
    public void DeserializeSeveralValuesFromList() {
        testCase = "{\"clientIds\":[3,4]}";
        expected = new ListTestCase(ClientId.fromLong(3), ClientId.fromLong(4));
        assertThat(JsonUtils.fromJson(testCase, ListTestCase.class), beanDiffer(expected));
    }

    @Test
    public void DeserializeMixedValueAndNullFromList() {
        checkBeanDiffer();
        testCase = "{\"clientIds\":[5,null,6]}";
        expected = new ListTestCase(ClientId.fromLong(5), ClientId.fromNullableLong(null), ClientId.fromLong(6));
        assertThat(JsonUtils.fromJson(testCase, ListTestCase.class), beanDiffer(expected));
    }
}
