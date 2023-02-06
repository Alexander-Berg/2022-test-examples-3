package ru.yandex.ir.modelsclusterizer.title;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.ir.modelsclusterizer.be.ClusterizerSettings;
import ru.yandex.ir.modelsclusterizer.be.Token;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.robot.shared.clusterizer.CategorySettings;
import ru.yandex.market.robot.shared.clusterizer.SourceSettings;

import java.util.Arrays;
import java.util.Collections;

public class TitleParserTest extends Assert {
    @Test
    public void testTitleParser() throws Exception {
        CategorySettings categorySettings = new CategorySettings();
        ClusterizerSettings settings = new ClusterizerSettings(categorySettings);
        settings.buildStopWordsMachine();
        SourceSettings sourceSettings = new SourceSettings();
        sourceSettings.setRemoveUpperCase(true);
        sourceSettings.setRemoveCategoryTokens(true);
        categorySettings.getSources().put(0, sourceSettings);

        settings.getRemovingFormalizedParams().addAll(Arrays.asList(1, 2));
        TitleParser parser = new TitleParser(settings);

        assertEquals("ProOne 400 G2", parser.parse(0, "ProOne 400 G2", "", Collections.emptyList()).getTitle());
        assertEquals("ProOne 400 G2", parser.parse(0, "   ProOne 400 G2", "", Collections.emptyList()).getTitle());

        sourceSettings.setSplitNumbersLetters(true);

        assertEquals("ProOne 400 G 2", parser.parse(0, "ProOne 400 G2", "", Collections.emptyList()).getTitle());
        assertEquals("ProOne 400 G 2", parser.parse(0, "   ProOne 400 G2", "", Collections.emptyList()).getTitle());

        assertEquals(
            "Aqua Увлажняющий",
            parser.parse(
                0,
                "ГЕЛЬ-СМАЗКА SICO AQUA УВЛАЖНЯЮЩИЙ 50МЛ",
                "Apteka.RU/Медицинские изделия/Презервативы, гель-смазки",
                Arrays.asList(
                    FormalizerParam.FormalizedParamPosition.newBuilder()
                        .setParamId(1)
                        .addPosition(
                            FormalizerParam.Position.newBuilder()
                                .setSourceIndex(Formalizer.SourceIndex.TITLE_VALUE)
                                .setValueStart(12)
                                .setValueEnd(16)
                                .build()
                        )
                        .build(),
                    FormalizerParam.FormalizedParamPosition.newBuilder()
                        .setParamId(2)
                        .addPosition(
                            FormalizerParam.Position.newBuilder()
                                .setSourceIndex(Formalizer.SourceIndex.TITLE_VALUE)
                                .setValueStart(34)
                                .setValueEnd(38)
                                .build()
                        )
                        .build()
                )
            ).getTitle()
        );
        assertEquals(
            "Samsung Aqua Увлажняющий",
            parser.parse(
                0,
                "ГЕЛЬ-СМАЗКА samsung AQUA УВЛАЖНЯЮЩИЙ",
                "Apteka.RU/Медицинские изделия/Презервативы, гель-смазки",
                Collections.emptyList()
            ).getTitle()
        );
        assertEquals(
            "Бэд Samsung Aqua Увлажняющий",
            parser.parse(
                0,
                "ГЕЛЬ-СМАЗКА Бэд electronix Samsung electronix AQUA УВЛАЖНЯЮЩИЙ",
                "Apteka.RU/Медицинские изделия/Презервативы, гель-смазки",
                Collections.singletonList(
                    FormalizerParam.FormalizedParamPosition.newBuilder()
                        .setParamId(1)
                        .addPosition(
                            FormalizerParam.Position.newBuilder()
                                .setSourceIndex(Formalizer.SourceIndex.TITLE_VALUE)
                                .setValueStart(16)
                                .setValueEnd(25)
                                .build()
                        )
                        .addPosition(
                            FormalizerParam.Position.newBuilder()
                                .setSourceIndex(Formalizer.SourceIndex.TITLE_VALUE)
                                .setValueStart(36)
                                .setValueEnd(45)
                                .build()
                        )
                        .build()
                )
            ).getTitle()
        );
    }

    @Test
    public void hasParamIntersection() {
        assertTrue(TitleParser.hasParamIntersection(
            new Token("test", "test", 0),
            Collections.singleton(new TitleParser.ParamPosition(0, 4))
        ));
        assertFalse(TitleParser.hasParamIntersection(
            new Token(" ", " ", 4),
            Collections.singleton(new TitleParser.ParamPosition(0, 4))
        ));
    }
}