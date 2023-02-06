package ru.yandex.direct.mysql;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class MySQLGtidTest {
    @Test
    public void parsePositive() throws Exception {
        assertThat(MySQLGtid.of("5507efdf-ac17-f35e-5cad-26645da19341:2218046860"),
                equalTo(new MySQLGtid("5507efdf-ac17-f35e-5cad-26645da19341", 2218046860L))
        );
    }
}
