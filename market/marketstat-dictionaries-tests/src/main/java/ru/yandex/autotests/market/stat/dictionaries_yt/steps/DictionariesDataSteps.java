package ru.yandex.autotests.market.stat.dictionaries_yt.steps;

import ru.yandex.autotests.market.common.attacher.Attacher;
import ru.yandex.autotests.market.common.differ.WithId;
import ru.yandex.autotests.market.stat.attribute.Fields;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictType;
import ru.yandex.autotests.market.stat.util.ReflectionUtils;
import ru.yandex.autotests.market.stat.util.data.IgnoreField;
import ru.yandex.qatools.allure.annotations.Step;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by kateleb on 25.09.17.
 */
public class DictionariesDataSteps<T extends WithId> {

    public static final List<String> NUMERIC_LIST_FIELS = Arrays.asList(Fields.PARENTS, Fields.CHILDREN);
    public static final List<String> CAN_BE_NEGATIVE_FIELDS = Arrays.asList(Fields.MANAGER_ID, Fields.YA_MANAGER, Fields.SUM_OPERATION,
        Fields.SUM_OPERATION, Fields.SUM_OPERATION_DAY, Fields.BALANCE);
    public static final List<String> POSITIVE_NUMERIC_FIELDS = Arrays.asList(Fields.FEED_ID, Fields.SHOP_ID,
        Fields.SHOP_COUNTRY, Fields.MODEL_ID, Fields.ID, Fields.CLIENT_ID,
        Fields.CLUSTER_ID, Fields.CATEGORY_ID, Fields.BID, Fields.SBID,
        Fields.CBID, Fields.MBID, Fields.FEE, Fields.RANDX,
        Fields.FLAGS
    );

    @Step("Check fields format for data of type {1}")
    public void checkDictFields(List<WithId> hiveData, DictType type) {
        List<String> requiredFields = type.getRequiredFields();
        Attacher.attach("data", hiveData);
        List<String> wrongFormatFields = new ArrayList<>();
        Class<org.beanio.annotation.Field> annotation = org.beanio.annotation.Field.class;
        Set<Field> fields = Stream.of(type.getDataClass().getDeclaredFields())
            .filter(it -> !it.isAnnotationPresent(IgnoreField.class)).collect(Collectors.toSet());
        for (WithId data : hiveData) {
            for (Field field : fields) {
                String badValue = null;
                String fieldName = field.getAnnotation(annotation) != null ? field.getAnnotation(annotation).name() : field.getName();
                String value = getValue(data, field);
                if (requiredFields.contains(fieldName) && (value == null || value.isEmpty() || value.equals("null"))) {
                    badValue = "REQUIRED FIELD EMPTY!";
                } else if (NUMERIC_LIST_FIELS.contains(fieldName)) {
                    badValue = checkMatchingRegexp(value, "^\\[[0-9,null]*\\]$|null");
                } else if (POSITIVE_NUMERIC_FIELDS.contains(fieldName)) {
                    badValue = checkMatchingRegexp(value, "^\\d+$");
                } else if (fieldName.startsWith("is_") && !fieldName.endsWith("id")) {
                    badValue = checkMatchingRegexp(value, "true|false|1|0");
                } else if (CAN_BE_NEGATIVE_FIELDS.contains(fieldName)) {
                    badValue = checkMatchingRegexp(value, "^-?\\d+$");
                }
                if (badValue != null) {
                    wrongFormatFields.add(fieldName + "=" + badValue);
                }
            }
        }
        Attacher.attach("Wrong format data", wrongFormatFields);
        //every day one id=-1 present in user roles
        // wrongFormatFields.remove("id=-1");
        if (type.getTableName().equals("shop_user_roles")) {
            wrongFormatFields.remove("id=[-1], Expected matching ^\\d+$");
        }
        assertThat("Wrong format fields detected! See attachment", wrongFormatFields, is(empty()));
    }

    private String checkMatchingRegexp(String value, String regex) {
        if (value != null && !value.isEmpty() && !value.equals("null") && !value.matches(regex)) {
            return "[" + value + "], Expected matching " + regex;
        }
        return null;
    }

    private String getValue(WithId data, Field field) {
        return String.valueOf(ReflectionUtils.getField(field, data));
    }

    public void checkDataExistInDictionary(List<WithId> hiveData, DictType type) {
        Attacher.attach("data", hiveData);
        assertThat("No data found in table " + type.getTableName(), hiveData.size(), greaterThan(0));
    }

}
