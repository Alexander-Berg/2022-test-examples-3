package ru.yandex.direct.common.db;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.TemporalUnitOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.common.testing.CommonTest;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNoException;
import static ru.yandex.direct.common.db.PpcPropertyType.BOOLEAN;
import static ru.yandex.direct.common.db.PpcPropertyType.INTEGER;
import static ru.yandex.direct.common.db.PpcPropertyType.INT_LIST;
import static ru.yandex.direct.common.db.PpcPropertyType.LOCAL_DATE;
import static ru.yandex.direct.common.db.PpcPropertyType.LOCAL_DATE_TIME;
import static ru.yandex.direct.common.db.PpcPropertyType.LONG;
import static ru.yandex.direct.common.db.PpcPropertyType.LONG_LIST;
import static ru.yandex.direct.common.db.PpcPropertyType.LONG_SET;
import static ru.yandex.direct.common.db.PpcPropertyType.STRING;
import static ru.yandex.direct.common.db.PpcPropertyType.STRING_TO_STRING_MAP_JSON;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@RunWith(SpringRunner.class)
@CommonTest
public class PpcPropertiesSupportTest {
    private static final int MAX_KEY_LENGTH = 100;
    private static final String KEY1 = "PpcPropertiesSupportTest_test_key1";
    private static final String KEY2 = "PpcPropertiesSupportTest_test_key2";
    private static final String VERY_LONG_INVALID_KEY = StringUtils.repeat('x', MAX_KEY_LENGTH * 10);
    private static final String LONG_VALID_KEY = StringUtils.repeat('x', MAX_KEY_LENGTH);
    private static final String LONG_INVALID_KEY = StringUtils.repeat('x', MAX_KEY_LENGTH + 1);

    private static final PpcPropertyName<List<Integer>>
            INTEGER_LIST_PPC_PROPERTY_NAME =
            new PpcPropertyName<>("PpcPropertiesSupportTest_test_int_list_key", INT_LIST);

    private static final PpcPropertyName<List<Long>> LONG_LIST_PPC_PROPERTY_NAME =
            new PpcPropertyName<>("PpcPropertiesSupportTest_test_long_list_key", LONG_LIST);

    private static final PpcPropertyName<Set<Long>> LONG_SET_PPC_PROPERTY_NAME =
            new PpcPropertyName<>("PpcPropertiesSupportTest_test_long_set_key", LONG_SET);

    private static final PpcPropertyName<String> NAME1 = new PpcPropertyName<>(KEY1, STRING);
    private static final PpcPropertyName<String> LONG_VALID_NAME = new PpcPropertyName<>(LONG_VALID_KEY, STRING);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());
    private static final LocalDateTime TEST_DATE_TIME1 =
            FORMATTER.parse("2018-09-18 15:52:52.123456789", LocalDateTime::from);

    private static final LocalDateTime TEST_DATE_TIME2 =
            FORMATTER.parse("2018-09-20 15:52:52.123456789", LocalDateTime::from);

    private static final LocalDate TEST_DATE1 = ISO_LOCAL_DATE.parse("2018-10-29", LocalDate::from);

    private static final LocalDate TEST_DATE2 = ISO_LOCAL_DATE.parse("2018-10-30", LocalDate::from);

    private static final PpcPropertyName<Integer>
            INTEGER_PPC_PROPERTY_NAME = new PpcPropertyName<>("PpcPropertiesSupportTest_test_int_key", INTEGER);

    private static final PpcPropertyName<Long>
            LONG_PPC_PROPERTY_NAME = new PpcPropertyName<>("PpcPropertiesSupportTest_test_long_key", LONG);

    private static final PpcPropertyName<Boolean>
            BOOLEAN_PPC_PROPERTY_NAME = new PpcPropertyName<>("PpcPropertiesSupportTest_test_bool_key", BOOLEAN);

    private static final PpcPropertyName<LocalDateTime>
            DATE_TIME_PPC_PROPERTY_NAME =
            new PpcPropertyName<>("PpcPropertiesSupportTest_test_date_time_key", LOCAL_DATE_TIME);

    private static final PpcPropertyName<LocalDate>
            DATE_PPC_PROPERTY_NAME = new PpcPropertyName<>("PpcPropetiesSupportTest_test_date_key", LOCAL_DATE);

    private static final PpcPropertyName<Map<String, String>>
            JSON_S2S_MAP_PROPERTY_NAME = new PpcPropertyName<>("PpcPropetiesSupportTest_test_json_s2s_map_key",
            STRING_TO_STRING_MAP_JSON);

    private static final TemporalUnitOffset OFFSET = new TemporalUnitWithinOffset(1, ChronoUnit.MINUTES);
    public static final Map<String, String> S2S_MAP = Map.of("1:2", "on", "2:2", "3:4");
    public static final Map<String, String> S2S_MAP2 = Map.of("1:3", "on", "2:2", "3:5");

    @Autowired
    private PpcPropertiesSupport pps;

    @Before
    public void setup() {
        pps.remove(KEY1);
        pps.set(KEY2, "some data");
    }

    @Test(expected = IllegalArgumentException.class)
    public void createLongPpcPropertyName_throwsException() {
        new PpcPropertyName<>(LONG_INVALID_KEY, STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createIVeryLongPpcPropertyName_throwsException() {
        new PpcPropertyName<>(VERY_LONG_INVALID_KEY, STRING);
    }

    @Test
    public void getForNotExistingValuesProp_ReturnsNull() {
        assertThat(pps.get(NAME1).get()).isNull();
    }

    @Test
    public void getForNotExistingLongValuesProp_ReturnsNull() {
        assertThat(pps.get(LONG_VALID_NAME).get()).isNull();
    }

    @Test
    public void findForNotExistingValuesProp_ReturnsEmptyOptional() {
        PpcProperty<String> prop = pps.get(NAME1);
        Assertions.assertThat(prop.find()).isEmpty();
    }

    @Test
    public void findForNotExistingLongValuesProp_ReturnsEmptyOptional() {
        PpcProperty<String> prop = pps.get(LONG_VALID_NAME);
        Assertions.assertThat(prop.find()).isEmpty();
    }

    @Test
    public void getFullForNotExistingValuesProp_ReturnsNull() {
        assertThat(pps.getFullByNames(Collections.singleton(KEY1)).get(KEY1)).isNull();
    }

    @Test
    public void forExistingName_getReturnsValue() {
        PpcProperty<String> prop = pps.get(NAME1);
        prop.set("x");
        assertThat(prop.get()).isEqualTo("x");
        prop.remove();
    }

    @Test
    public void forExistingLongName_getReturnsValue() {
        PpcProperty<String> prop = pps.get(LONG_VALID_NAME);
        try {
            prop.set("yyy");
        } catch (Exception e) {
            assumeNoException(e);
        }
        assertThat(prop.get()).isEqualTo("yyy");
        prop.remove();
    }

    @Test
    public void forExistingName_findReturnsValue() {
        PpcProperty<String> prop = pps.get(NAME1);
        prop.set("x");
        Assertions.assertThat(prop.find()).contains("x");
        prop.remove();
    }

    @Test
    public void forExistingLongName_findReturnsValue() {
        PpcProperty<String> prop = pps.get(LONG_VALID_NAME);
        try {
            prop.set("zzz");
        } catch (Exception e) {
            assumeNoException(e);
        }
        Assertions.assertThat(prop.find()).contains("zzz");
        prop.remove();
    }

    @Test
    public void forExistingName_removeReturnsTrue() {
        PpcProperty<String> prop = pps.get(NAME1);
        prop.set("xxx");
        assertThat(prop.remove()).isTrue();
    }

    @Test
    public void forExistingName_getFullReturnsLastChange() {
        PpcPropertyData<String> prop = pps.getFullByNames(Collections.singleton(KEY2)).get(KEY2);
        Assertions.assertThat(prop.getLastChange()).isNotNull();
    }

    @Test
    public void forExistingName_getFullReturnsSameValue() {
        PpcProperty<String> prop = pps.get(NAME1);
        prop.set("x");
        String key = NAME1.getName();
        PpcPropertyData<String> fullProp = pps.getFullByNames(Collections.singleton(key)).get(key);
        assertThat(prop.get()).isEqualTo(fullProp.getValue());
        prop.remove();
    }

    @Test
    public void casForNotExistingName_NullAndNotNull_ReturnsValue() {
        PpcProperty<String> prop = pps.get(NAME1);
        prop.cas(null, "x");
        assertThat(prop.get()).isEqualTo("x");
        prop.remove();
    }

    @Test
    public void casForNotExistingName_NotNullAndNotNull_ReturnsFalse() {
        PpcProperty<String> prop = pps.get(NAME1);
        assertThat(prop.cas("x", "x")).isFalse();
        assertThat(prop.get()).isNull();
    }

    @Test
    public void casForNotExistingName_NullAndNull_ReturnsTrue() {
        PpcProperty<String> prop = pps.get(NAME1);
        assertThat(prop.cas(null, null)).isTrue();
        assertThat(prop.get()).isNull();
    }

    @Test
    public void casForNotExistingName_NotNullAndNull_ReturnsTrue() {
        PpcProperty<String> prop = pps.get(NAME1);
        assertThat(prop.cas("x", null)).isFalse();
        assertThat(prop.get()).isNull();
    }

    @Test
    public void casForExistingLongName_Positive() {
        PpcProperty<String> prop = pps.get(LONG_VALID_NAME);
        try {
            prop.set("x");
        } catch (Exception e) {
            assumeNoException(e);
        }
        assertThat(prop.cas("x", "y")).isTrue();
        assertThat(prop.get()).isEqualTo("y");
        prop.remove();
    }

    @Test
    public void casForExistingLongName_Negative() {
        PpcProperty<String> prop = pps.get(LONG_VALID_NAME);
        try {
            prop.set("x");
        } catch (Exception e) {
            assumeNoException(e);
        }
        assertThat(prop.cas("x1", "y")).isFalse();
        assertThat(prop.get()).isEqualTo("x");
        prop.remove();
    }

    @Test
    public void casForExistingName_Positive() {
        PpcProperty<String> prop = pps.get(NAME1);
        prop.set("x");
        assertThat(prop.cas("x", "y")).isTrue();
        assertThat(prop.get()).isEqualTo("y");
    }

    @Test
    public void casForExistingName_Negative() {
        PpcProperty<String> prop = pps.get(NAME1);
        prop.set("x");
        assertThat(prop.cas("x1", "y")).isFalse();
        assertThat(prop.get()).isEqualTo("x");
    }

    @Test
    public void forIntName_setNoException() {
        PpcProperty<Integer> prop = pps.get(INTEGER_PPC_PROPERTY_NAME);
        try {
            prop.set(1);
        } catch (Exception e) {
            assumeNoException(e);
        }
        prop.remove();
    }

    @Test
    public void forIntName_getReturnsValue() {
        PpcProperty<Integer> prop = pps.get(INTEGER_PPC_PROPERTY_NAME);
        prop.set(1);
        assertThat(prop.get()).isEqualTo(1);
        prop.remove();
    }

    @Test
    public void forIntName_findContainsValue() {
        PpcProperty<Integer> prop = pps.get(INTEGER_PPC_PROPERTY_NAME);
        prop.set(1);
        Assertions.assertThat(prop.find()).contains(1);
        prop.remove();
    }

    @Test
    public void casForIntName_Positive() {
        PpcProperty<Integer> prop = pps.get(INTEGER_PPC_PROPERTY_NAME);
        prop.set(1);
        assertThat(prop.cas(1, 2)).isTrue();
        assertThat(prop.get()).isEqualTo(2);
        prop.remove();
    }

    @Test
    public void casForIntName_Negative() {
        PpcProperty<Integer> prop = pps.get(INTEGER_PPC_PROPERTY_NAME);
        prop.set(1);
        assertThat(prop.cas(2, 3)).isFalse();
        assertThat(prop.get()).isEqualTo(1);
        prop.remove();
    }

    @Test
    public void forLongName_setNoException() {
        PpcProperty<Long> prop = pps.get(LONG_PPC_PROPERTY_NAME);
        try {
            prop.set(1L);
        } catch (Exception e) {
            assumeNoException(e);
        }
        prop.remove();
    }

    @Test
    public void forLongName_getReturnsValue() {
        PpcProperty<Long> prop = pps.get(LONG_PPC_PROPERTY_NAME);
        prop.set(1L);
        assertThat(prop.get()).isEqualTo(1L);
        prop.remove();
    }

    @Test
    public void forLongName_findContainsValue() {
        PpcProperty<Long> prop = pps.get(LONG_PPC_PROPERTY_NAME);
        prop.set(1L);
        Assertions.assertThat(prop.find()).contains(1L);
        prop.remove();
    }

    @Test
    public void casForLongName_Positive() {
        PpcProperty<Long> prop = pps.get(LONG_PPC_PROPERTY_NAME);
        prop.set(1L);
        assertThat(prop.cas(1L, 2L)).isTrue();
        assertThat(prop.get()).isEqualTo(2L);
        prop.remove();
    }

    @Test
    public void casForLongName_Negative() {
        PpcProperty<Long> prop = pps.get(LONG_PPC_PROPERTY_NAME);
        prop.set(1L);
        assertThat(prop.cas(2L, 3L)).isFalse();
        assertThat(prop.get()).isEqualTo(1L);
        prop.remove();
    }

    @Test
    public void forBooleanName_setNoException() {
        PpcProperty<Boolean> prop = pps.get(BOOLEAN_PPC_PROPERTY_NAME);
        try {
            prop.set(true);
        } catch (Exception e) {
            assumeNoException(e);
        }
        prop.remove();
    }

    @Test
    public void forBooleanName_getReturnsValue() {
        PpcProperty<Boolean> prop = pps.get(BOOLEAN_PPC_PROPERTY_NAME);
        prop.set(true);
        assertThat(prop.get()).isTrue();
    }

    @Test
    public void forBooleanName_findContainsValue() {
        PpcProperty<Boolean> prop = pps.get(BOOLEAN_PPC_PROPERTY_NAME);
        prop.set(true);
        Assertions.assertThat(prop.find()).contains(true);
        prop.remove();
    }

    @Test
    public void casForBooleanName_Positive() {
        PpcProperty<Boolean> prop = pps.get(BOOLEAN_PPC_PROPERTY_NAME);
        prop.set(true);
        assertThat(prop.cas(true, false)).isTrue();
        assertThat(prop.get()).isFalse();
        prop.remove();
    }

    @Test
    public void casForBooleanName_Negative() {
        PpcProperty<Boolean> prop = pps.get(BOOLEAN_PPC_PROPERTY_NAME);
        prop.set(true);
        assertThat(prop.cas(false, true)).isFalse();
        assertThat(prop.get()).isTrue();
        prop.remove();
    }

    @Test
    public void forDateTimeName_setNoException() {
        PpcProperty<LocalDateTime> prop = pps.get(DATE_TIME_PPC_PROPERTY_NAME);
        try {
            prop.set(TEST_DATE_TIME1);
        } catch (Exception e) {
            assumeNoException(e);
        }
        prop.remove();
    }

    @Test
    public void forDateTimeName_getReturnsValue() {
        PpcProperty<LocalDateTime> prop = pps.get(DATE_TIME_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE_TIME1);
        assertThat(prop.get()).isEqualTo(TEST_DATE_TIME1);
        prop.remove();
    }

    @Test
    public void forDateTimeName_findReturnsValue() {
        PpcProperty<LocalDateTime> prop = pps.get(DATE_TIME_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE_TIME1);
        assertThat(prop.find()).contains(TEST_DATE_TIME1);
        prop.remove();
    }

    @Test
    public void casForDateTimeName_Positive() {
        PpcProperty<LocalDateTime> prop = pps.get(DATE_TIME_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE_TIME1);
        assertThat(prop.cas(TEST_DATE_TIME1, TEST_DATE_TIME2)).isTrue();
        assertThat(prop.get()).isEqualTo(TEST_DATE_TIME2);
        prop.remove();
    }

    @Test
    public void casForDateTimeName_Negative() {
        PpcProperty<LocalDateTime> prop = pps.get(DATE_TIME_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE_TIME1);
        assertThat(prop.cas(TEST_DATE_TIME2, TEST_DATE_TIME1)).isFalse();
        assertThat(prop.get()).isEqualTo(TEST_DATE_TIME1);
        prop.remove();
    }

    @Test
    public void forDateName_getReturnsValue() {
        PpcProperty<LocalDate> prop = pps.get(DATE_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE1);
        assertThat(prop.get()).isEqualTo(TEST_DATE1);
        prop.remove();
    }

    @Test
    public void forDateName_findReturnsValue() {
        PpcProperty<LocalDate> prop = pps.get(DATE_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE1);
        assertThat(prop.find()).contains(TEST_DATE1);
        prop.remove();
    }

    @Test
    public void casForDateName_Positive() {
        PpcProperty<LocalDate> prop = pps.get(DATE_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE1);
        assertThat(prop.cas(TEST_DATE1, TEST_DATE2)).isTrue();
        assertThat(prop.get()).isEqualTo(TEST_DATE2);
        prop.remove();
    }

    @Test
    public void casForDateName_Negative() {
        PpcProperty<LocalDate> prop = pps.get(DATE_PPC_PROPERTY_NAME);
        prop.set(TEST_DATE1);
        assertThat(prop.cas(TEST_DATE2, TEST_DATE1)).isFalse();
        assertThat(prop.get()).isEqualTo(TEST_DATE1);
        prop.remove();
    }

    @Test
    public void forIntListName_getReturnsValue() {
        PpcProperty<List<Integer>> prop = pps.get(INTEGER_LIST_PPC_PROPERTY_NAME);
        List<Integer> list = new ArrayList<>();
        list.add(2);
        list.add(1);
        prop.set(list);
        assertThat(prop.get()).isEqualTo(list);
        prop.remove();
    }

    @Test
    public void forEmptyIntListName_getReturnsValue() {
        PpcProperty<List<Integer>> prop = pps.get(INTEGER_LIST_PPC_PROPERTY_NAME);
        List<Integer> list = Collections.emptyList();
        prop.set(list);
        assertThat(prop.get()).isEqualTo(list);
        prop.remove();
    }

    @Test
    public void forLongListName_getReturnsValue() {
        PpcProperty<List<Long>> prop = pps.get(LONG_LIST_PPC_PROPERTY_NAME);
        List<Long> list = new ArrayList<>();
        list.add(3L);
        list.add(1L);
        prop.set(list);
        assertThat(prop.get()).isEqualTo(list);
        prop.remove();
    }

    @Test
    public void forEmptyLongListName_getReturnsValue() {
        PpcProperty<List<Long>> prop = pps.get(LONG_LIST_PPC_PROPERTY_NAME);
        List<Long> list = Collections.emptyList();
        prop.set(list);
        assertThat(prop.get()).isEqualTo(list);
        prop.remove();
    }

    @Test
    public void forLongSetName_getReturnsValue() {
        PpcProperty<Set<Long>> prop = pps.get(LONG_SET_PPC_PROPERTY_NAME);
        Set<Long> set = new HashSet<>();
        set.add(2L);
        set.add(3L);
        set.add(1L);
        prop.set(set);
        assertThat(prop.get()).isEqualTo(set);
        prop.remove();
    }

    @Test
    public void forEmptyLongSetName_getReturnsValue() {
        PpcProperty<Set<Long>> prop = pps.get(LONG_SET_PPC_PROPERTY_NAME);
        Set<Long> set = Collections.emptySet();
        prop.set(set);
        assertThat(prop.get()).isEqualTo(set);
        prop.remove();
    }

    @Test
    public void forSetName_getFullReturnsCorrectLastChange() {
        var initTime = LocalDateTime.now();
        pps.set(KEY1, "new data");
        PpcPropertyData<String> prop = pps.getFullByNames(Collections.singleton(KEY1)).get(KEY1);
        LocalDateTime propLastChange = prop.getLastChange();
        Assertions.assertThat(propLastChange).isCloseTo(initTime, OFFSET);
        Assertions.assertThat(propLastChange).isCloseTo(LocalDateTime.now(), OFFSET);
        pps.remove(KEY1);
    }

    @Test
    public void getForNotExistingValues_ReturnsNull() {
        assertThat(pps.get(KEY1)).isNull();
    }

    @Test
    public void getForNotExistingLongValues_ReturnsNull() {
        assertThat(pps.get(LONG_VALID_KEY)).isNull();
    }

    @Test
    public void findForNotExistingValues_ReturnsEmptyOptional() {
        Assertions.assertThat(pps.find(KEY1)).isEmpty();
    }

    @Test
    public void findForNotExistingLongValues_ReturnsEmptyOptional() {
        Assertions.assertThat(pps.find(LONG_VALID_KEY)).isEmpty();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getForVeryLongKey_ThrowsException() {
        pps.get(VERY_LONG_INVALID_KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getForInvalidLongKey_ThrowsException() {
        pps.get(LONG_INVALID_KEY);
    }

    @Test
    public void forExistingKey_getReturnsValue() {
        pps.set(KEY1, "x");
        assertThat(pps.get(KEY1)).isEqualTo("x");
    }

    @Test
    public void forExistingLongKey_getReturnsValue() {
        try {
            pps.set(LONG_VALID_KEY, "yyy");
        } catch (Exception e) {
            assumeNoException(e);
        }
        assertThat(pps.get(LONG_VALID_KEY)).isEqualTo("yyy");
        pps.remove(LONG_VALID_KEY);
    }

    @Test
    public void forExistingKey_findReturnsValue() {
        pps.set(KEY1, "x");
        Assertions.assertThat(pps.find(KEY1)).contains("x");
    }

    @Test
    public void forExistingLongKey_findReturnsValue() {
        try {
            pps.set(LONG_VALID_KEY, "zzz");
        } catch (Exception e) {
            assumeNoException(e);
        }
        Assertions.assertThat(pps.find(LONG_VALID_KEY)).contains("zzz");
        pps.remove(LONG_VALID_KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setForVeryLongKey_ThrowsException() {
        pps.set(VERY_LONG_INVALID_KEY, "x");
    }


    @Test(expected = IllegalArgumentException.class)
    public void setForInvalidLongKey_ThrowsException() {
        pps.set(LONG_INVALID_KEY, "x");
    }

    @Test
    public void casForNotExistingValue_NullAndNotNull_ReturnsValue() {
        pps.cas(KEY1, null, "x");
        assertThat(pps.get(KEY1)).isEqualTo("x");
    }

    @Test
    public void casForNotExistingValue_NotNullAndNotNull_ReturnsFalse() {
        assertThat(pps.cas(KEY1, "x", "x")).isFalse();
        assertThat(pps.get(KEY1)).isNull();
    }

    @Test
    public void casForNotExistingValue_NullAndNull_ReturnsTrue() {
        assertThat(pps.cas(KEY1, null, null)).isTrue();
        assertThat(pps.get(KEY1)).isNull();
    }

    @Test
    public void casForNotExistingValue_NotNullAndNull_ReturnsTrue() {
        assertThat(pps.cas(KEY1, "x", null)).isFalse();
        assertThat(pps.get(KEY1)).isNull();
    }

    @Test
    public void casForExistingValue_Positive() {
        pps.set(KEY1, "x");
        assertThat(pps.cas(KEY1, "x", "y")).isTrue();
        assertThat(pps.get(KEY1)).isEqualTo("y");
    }

    @Test
    public void casForExistingLongValue_Positive() {
        try {
            pps.set(LONG_VALID_KEY, "x");
        } catch (Exception e) {
            assumeNoException(e);
        }
        assertThat(pps.cas(LONG_VALID_KEY, "x", "y")).isTrue();
        assertThat(pps.get(LONG_VALID_KEY)).isEqualTo("y");
        pps.remove(LONG_VALID_KEY);
    }

    @Test
    public void casForExistingValue_Negative() {
        pps.set(KEY1, "x");
        assertThat(pps.cas(KEY1, "x1", "y")).isFalse();
        assertThat(pps.get(KEY1)).isEqualTo("x");
    }

    @Test
    public void casForExistingLongValue_Negative() {
        try {
            pps.set(LONG_VALID_KEY, "x");
        } catch (Exception e) {
            assumeNoException(e);
        }
        assertThat(pps.cas(LONG_VALID_KEY, "x1", "y")).isFalse();
        assertThat(pps.get(LONG_VALID_KEY)).isEqualTo("x");
        pps.remove(LONG_VALID_KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void casForVeryLongKey_ThrowsException() {
        pps.cas(VERY_LONG_INVALID_KEY, "x", "y");
    }

    @Test(expected = IllegalArgumentException.class)
    public void casForInvalidLongKey_ThrowsException() {
        pps.cas(LONG_INVALID_KEY, "x", "y");
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeForVeryLongKey_ThrowsException() {
        pps.remove(VERY_LONG_INVALID_KEY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeForInvalidLongKey_ThrowsException() {
        pps.remove(LONG_INVALID_KEY);
    }

    @Test(expected = PpcPropertyParseException.class)
    public void getInvalidDate() {
        try {
            pps.set(DATE_TIME_PPC_PROPERTY_NAME.getName(), "11 12 2018 12:20");
            pps.get(DATE_TIME_PPC_PROPERTY_NAME).get();
            pps.remove(DATE_TIME_PPC_PROPERTY_NAME.getName());
        } finally {
            pps.get(DATE_TIME_PPC_PROPERTY_NAME).remove();
        }
    }

    @Test
    public void forJsonS2SMap_getReturnsValue() {
        PpcProperty<Map<String, String>> prop = pps.get(JSON_S2S_MAP_PROPERTY_NAME);
        prop.set(S2S_MAP);
        assertThat(prop.get()).isEqualTo(S2S_MAP);
        prop.remove();
    }

    @Test
    public void forJsonS2SMap_findReturnsValue() {
        PpcProperty<Map<String, String>> prop = pps.get(JSON_S2S_MAP_PROPERTY_NAME);
        prop.set(S2S_MAP);
        Assertions.assertThat(prop.find()).contains(S2S_MAP);
        prop.remove();
    }

    @Test
    public void casForJsonS2SMap_Positive() {
        PpcProperty<Map<String, String>> prop = pps.get(JSON_S2S_MAP_PROPERTY_NAME);
        prop.set(S2S_MAP);
        assertThat(prop.cas(S2S_MAP, S2S_MAP2)).isTrue();
        assertThat(prop.get()).isEqualTo(S2S_MAP2);
        prop.remove();
    }

    @Test
    public void casForJsonS2SMap_Negative() {
        PpcProperty<Map<String, String>> prop = pps.get(JSON_S2S_MAP_PROPERTY_NAME);
        prop.set(S2S_MAP);
        assertThat(prop.cas(S2S_MAP2, S2S_MAP)).isFalse();
        assertThat(prop.get()).isEqualTo(S2S_MAP);
        prop.remove();
    }

    @Test
    public void serializeForJsonS2SMap_Negative() {
        PpcProperty<Map<String, String>> prop = pps.get(JSON_S2S_MAP_PROPERTY_NAME);
        prop.set(S2S_MAP);
        assertThat(prop.getName().getType().serialize(prop.get())).isEqualTo(toJson(S2S_MAP));
        prop.remove();
    }

    @Test
    public void deserializeForJsonS2SMap_Negative() {
        PpcProperty<Map<String, String>> prop = pps.get(JSON_S2S_MAP_PROPERTY_NAME);
        prop.set(prop.getName().getType().deserialize(toJson(S2S_MAP)));
        assertThat(prop.get()).isEqualTo(S2S_MAP);
        prop.remove();
    }

}
