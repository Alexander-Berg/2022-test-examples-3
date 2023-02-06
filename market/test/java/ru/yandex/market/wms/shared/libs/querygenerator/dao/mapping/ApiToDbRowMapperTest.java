package ru.yandex.market.wms.shared.libs.querygenerator.dao.mapping;

import org.assertj.core.api.Assertions;
import org.junit.Test;

class ApiToDbRowMapperTest {

    @Test
    void emptyAnnotation() {
        Assertions.assertThat(ApiToDbRowMapper.map("field1", MappedTestPojo.class))
                .isEqualTo("field1");
    }

    @Test
    void emptyAnnotationIgnoreCase() {
        Assertions.assertThat(ApiToDbRowMapper.map("FIELD1", MappedTestPojo.class))
                .isEqualTo("field1");
    }

    @Test
    void filledAnnotation() {
        Assertions.assertThat(ApiToDbRowMapper.map("field2", MappedTestPojo.class))
                .isEqualTo("field_2_row_name");
    }

    @Test
    void annotationNotFound() {
        Assertions.assertThat(ApiToDbRowMapper.map("field4", MappedTestPojo.class))
                .isNull();
    }

    @Test
    void fieldNotFound() {
        Assertions.assertThat(ApiToDbRowMapper.map("field5", MappedTestPojo.class))
                .isNull();
    }
}
