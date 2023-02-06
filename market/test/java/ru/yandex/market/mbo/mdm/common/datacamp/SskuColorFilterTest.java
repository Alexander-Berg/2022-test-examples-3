package ru.yandex.market.mbo.mdm.common.datacamp;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class SskuColorFilterTest extends MdmBaseDbTestClass {

    @Autowired
    private StorageKeyValueService skv;

    @Test
    public void testFilledColorFilterStoredInSkvProperly() {
        SskuColorFilter filter = new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, 1234, 6789);
        skv.putValue(MdmProperties.SSKU_COLOR_FILTER, filter);

        Assertions.assertThat(skv.getValue(MdmProperties.SSKU_COLOR_FILTER, SskuColorFilter.class))
            .isEqualTo(filter);
    }

    @Test
    public void testSimpleColorFilterStoredInSkvProperly() {
        SskuColorFilter filter = new SskuColorFilter(DbsImportMode.BLUE_ONLY, 0, 0);
        skv.putValue(MdmProperties.SSKU_COLOR_FILTER, filter);

        Assertions.assertThat(skv.getValue(MdmProperties.SSKU_COLOR_FILTER, SskuColorFilter.class))
            .isEqualTo(filter);
    }

    @Test
    public void testValidityChecks() {
        Assertions.assertThat(SskuColorFilter.DEFAULT.isValid()).isTrue();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.BLUE_ONLY, -100500, -100600).isValid()).isTrue();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.BLUE_AND_DBS, -100500, -100600).isValid()).isTrue();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.BLUE_ONLY, 0, 0).isValid()).isTrue();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.BLUE_AND_DBS, 0, 0).isValid()).isTrue();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, 0, 1).isValid()).isTrue();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, 10, 20).isValid()).isTrue();

        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, 20, 10).isValid()).isFalse();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, 10, 10).isValid()).isFalse();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, 0, 0).isValid()).isFalse();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, -5, 0).isValid()).isFalse();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, -5, -1).isValid()).isFalse();
        Assertions.assertThat(new SskuColorFilter(DbsImportMode.DBS_IN_BUSINESS_RANGE, -1, -5).isValid()).isFalse();
    }
}
