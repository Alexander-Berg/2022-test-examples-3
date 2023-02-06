package ru.yandex.market.stat.dicts.records;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Created by kateleb on 27.04.17.
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class TestDictionaryModified implements DictionaryRecord {
    private String test_id;
    private Integer test_number;
    private BigDecimal experimental_field;
    private String yet_another;

    public TestDictionaryModified(String test_id, Integer test_number, BigDecimal experimental_field, String yet_another) {
        this.test_id = test_id;
        this.test_number = test_number;
        this.experimental_field = experimental_field;
        this.yet_another = yet_another;
    }

    public TestDictionaryModified() {

    }

    public void setExperimental_field(String value){
        this.experimental_field = new BigDecimal(value);
    }
}
