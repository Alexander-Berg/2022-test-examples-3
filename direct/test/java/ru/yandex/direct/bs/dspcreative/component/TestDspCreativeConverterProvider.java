package ru.yandex.direct.bs.dspcreative.component;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.bs.dspcreative.configuration.BsDspCreativeTest;
import ru.yandex.direct.bs.dspcreative.model.DspCreativeExportEntry;

import static org.assertj.core.api.Assertions.assertThat;

@BsDspCreativeTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TestDspCreativeConverterProvider {
    @Autowired
    private DspCreativeConverterProvider dspCreativeConverterProvider;

    @Test
    public void testNotBlankConstructorData() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setConstructorData("constructorData")
                .build();

        assertThat(dspCreativeConverterProvider.get(creativeEntry))
                .isInstanceOf(DspCreativeCanvasConverter.class);
    }

    @Test
    public void testBlankConstructorData() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setConstructorData("")
                .build();

        assertThat(dspCreativeConverterProvider.get(creativeEntry))
                .isInstanceOf(DspCreativeCanvasConverter.class);
    }

    @Test
    public void testNullConstructorData() {
        DspCreativeExportEntry creativeEntry = DspCreativeExportEntry.builder()
                .setConstructorData(null)
                .build();

        assertThat(dspCreativeConverterProvider.get(creativeEntry))
                .isInstanceOf(DspCreativeBannerstorageConverter.class);
    }
}
