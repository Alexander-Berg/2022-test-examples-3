package ru.yandex.market.mbo.storage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class StorageKeyValueServiceTest {

    private static final String VALID_KEY = "valid_key_it_is_exactly_50_chars_long_hmm_hello_wo";
    private static final String INVALID_KEY_EMPTY = "";

    private StorageKeyValueRepositoryMock repository;
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() {
        repository = new StorageKeyValueRepositoryMock();
        storageKeyValueService = new StorageKeyValueServiceImpl(repository, null);
    }

    @Test
    public void testCheckKey() {
        Assertions.assertThatThrownBy(() -> storageKeyValueService.getValue(null, Object.class))
                .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> storageKeyValueService.getValue(INVALID_KEY_EMPTY, Object.class))
                .isInstanceOf(IllegalArgumentException.class);

        Object testObject = new Object();
        Assertions.assertThatThrownBy(() -> storageKeyValueService.putValue(null, testObject))
                .isInstanceOf(IllegalArgumentException.class);
        Assertions.assertThatThrownBy(() -> storageKeyValueService.putValue(INVALID_KEY_EMPTY, testObject))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void assertGetPutValue() {
        Assertions.assertThat(storageKeyValueService.getValue(VALID_KEY, Object.class)).isNull();

        Long testLong = 5L;
        storageKeyValueService.putValue(VALID_KEY, testLong);
        Assertions.assertThat(storageKeyValueService.getValue(VALID_KEY, Long.class)).isEqualTo(testLong);

        String testStirng = "hello world";
        storageKeyValueService.putValue(VALID_KEY, testStirng);
        Assertions.assertThat(storageKeyValueService.getValue(VALID_KEY, String.class)).isEqualTo(testStirng);

        LocalDateTime testLocalDateTime = LocalDateTime.now();
        storageKeyValueService.putValue(VALID_KEY, testLocalDateTime);
        Assertions.assertThat(storageKeyValueService.getValue(VALID_KEY, LocalDateTime.class))
                .isEqualTo(testLocalDateTime);

        Instant instant = Instant.parse("2007-12-03T10:15:30.00Z");
        storageKeyValueService.putValue(VALID_KEY, instant);
        Assertions.assertThat(storageKeyValueService.getInstant(VALID_KEY, null))
                .isEqualTo(instant);
    }

    @Test
    public void testGetPutList() {
        Assertions.assertThat(storageKeyValueService.getList(VALID_KEY, Object.class)).isEmpty();

        List<String> strings = Arrays.asList("a", "b");
        storageKeyValueService.putValue(VALID_KEY, strings);
        Assertions.assertThat(storageKeyValueService.getList(VALID_KEY, String.class))
                .containsExactly("a", "b");

        List<Integer> integers = Arrays.asList(1, 2);
        storageKeyValueService.putValue(VALID_KEY, integers);
        Assertions.assertThat(storageKeyValueService.getList(VALID_KEY, Integer.class))
                .containsExactly(1, 2);

        int[] array = new int[]{1, 2};
        storageKeyValueService.putValue(VALID_KEY, array);
        Assertions.assertThat(storageKeyValueService.getList(VALID_KEY, Integer.class))
                .containsExactly(1, 2);
    }

    @Test
    public void testParseDateFormats() {
        // instant
        repository.insertOrUpdate(VALID_KEY, "1597072127.133779000");
        Assertions.assertThat(storageKeyValueService.getInstant(VALID_KEY, null))
                .isEqualTo(Instant.parse("2020-08-10T15:08:47.133779Z"));

        repository.insertOrUpdate(VALID_KEY, "\"2020-08-10T15:08:47.133779Z\"");
        Assertions.assertThat(storageKeyValueService.getInstant(VALID_KEY, null))
                .isEqualTo(Instant.parse("2020-08-10T15:08:47.133779Z"));

        // localDate
        repository.insertOrUpdate(VALID_KEY, "[2020,8,10]");
        Assertions.assertThat(storageKeyValueService.getLocalDate(VALID_KEY, null))
                .isEqualTo(LocalDate.parse("2020-08-10"));

        repository.insertOrUpdate(VALID_KEY, "\"2020-08-10\"");
        Assertions.assertThat(storageKeyValueService.getLocalDate(VALID_KEY, null))
                .isEqualTo(LocalDate.parse("2020-08-10"));


        // localDateTime
        repository.insertOrUpdate(VALID_KEY, "[2020,8,5,16,31,22,539736000]");
        Assertions.assertThat(storageKeyValueService.getValue(VALID_KEY, LocalDateTime.class))
                .isEqualTo(LocalDateTime.parse("2020-08-05T16:31:22.539736000"));

        repository.insertOrUpdate(VALID_KEY, "\"2020-08-05T16:31:22.539736000\"");
        Assertions.assertThat(storageKeyValueService.getValue(VALID_KEY, LocalDateTime.class))
                .isEqualTo(LocalDateTime.parse("2020-08-05T16:31:22.539736000"));
    }
}
