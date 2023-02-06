package ru.yandex.direct.jobs.bannersystem.export.util;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.mobilecontent.model.AgeLabel;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;

import static ru.yandex.direct.jobs.bannersystem.export.util.BsExportMobileContentUtil.getAgeLabelRepresentation;
import static ru.yandex.direct.jobs.bannersystem.export.util.BsExportMobileContentUtil.getOsTypeStringRepresentation;

class BsExportMobileContentUtilTest {
    @Test
    void testGetOsTypeStringRepresentation() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(getOsTypeStringRepresentation(OsType.IOS)).isEqualTo("iOS");
        soft.assertThat(getOsTypeStringRepresentation(OsType.ANDROID)).isEqualTo("Android");

        soft.assertAll();
    }

    @Test
    void testGetAgeLabelRepresentation() {
        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(getAgeLabelRepresentation(AgeLabel._0_2B)).isEqualTo("0");
        soft.assertThat(getAgeLabelRepresentation(AgeLabel._6_2B)).isEqualTo("6");
        soft.assertThat(getAgeLabelRepresentation(AgeLabel._12_2B)).isEqualTo("12");
        soft.assertThat(getAgeLabelRepresentation(AgeLabel._16_2B)).isEqualTo("16");
        soft.assertThat(getAgeLabelRepresentation(AgeLabel._18_2B)).isEqualTo("18");

        soft.assertAll();
    }
}
