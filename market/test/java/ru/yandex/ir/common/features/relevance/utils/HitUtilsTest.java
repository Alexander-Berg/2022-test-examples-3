package ru.yandex.ir.common.features.relevance.utils;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.ir.common.features.relevance.base.TFullPosition;
import ru.yandex.ir.common.features.relevance.base.TFullPositionEx;
import ru.yandex.ir.common.features.relevance.base.TSignedPosting;
import ru.yandex.ir.common.string.SimpleStringProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HitUtilsTest {

    @Test
    void fillInvertedIndexContainingCapitalIWithDot() {
        String title = "Раковина-столешница 100.3 см Creavit ANTİK AN100";
        String body = "Установка\nуниверсальная";

        SimpleStringProcessor stringProcessor = new SimpleStringProcessor();
        Map<String, List<TSignedPosting>> invertedIndex = new HashMap<>();
        IntList sentenceLengths = new IntArrayList();

        // ---

        HitUtils.fillInvertedIndex(title, body, stringProcessor, invertedIndex, sentenceLengths);

        // ---

        Assertions.assertEquals(14, invertedIndex.size());
        Assertions.assertEquals(5, sentenceLengths.size());
    }

    @Test
    void getHitPositions() {
        SimpleStringProcessor stringProcessor = new SimpleStringProcessor();

        String query = "Набор Юный Физик Intellectico \"Магнитный лабиринт\"";

        String title = "Набор Intellectico Юный физик. Магнитный лабиринт (211)";
        String body = "наука: физика, опыты с магнетизмом изучение магнитных свойств опыты с магнитами Наборы для " +
                "исследований Детские наборы для опытов Intelectico Арс Джениус Юный физик. Магнитный лабиринт (211) " +
                "Магнитный лабиринт";

        // ---

        Map<String, List<TSignedPosting>> reverseIndex = new HashMap<>();
        IntList senLengths = new IntArrayList();
        HitUtils.fillInvertedIndex(
                title,
                body,
                stringProcessor,
                reverseIndex,
                senLengths
        );

        List<TFullPositionEx> positions = HitUtils.getHitPositions(
                query,
                reverseIndex,
                stringProcessor
        );

        // ---

        TFullPositionEx[] actualPositions = positions.toArray(new TFullPositionEx[0]);
        int[] actualSenLengths = senLengths.toIntArray();

        TFullPositionEx[] expectedPositions = {
                buildFullPositionEx(4208, 4208, 0),
                buildFullPositionEx(4272, 4272, 3),
                buildFullPositionEx(4336, 4336, 1),
                buildFullPositionEx(4400, 4400, 2),
                buildFullPositionEx(8304, 8304, 4),
                buildFullPositionEx(8368, 8368, 5),
                buildFullPositionEx(12433, 12433, 2),
                buildFullPositionEx(13073, 13073, 0),
                buildFullPositionEx(13329, 13329, 0),
                buildFullPositionEx(13712, 13712, 1),
                buildFullPositionEx(13776, 13776, 2),
                buildFullPositionEx(16464, 16464, 4),
                buildFullPositionEx(16528, 16528, 5),
                buildFullPositionEx(16656, 16656, 4),
                buildFullPositionEx(16720, 16720, 5)
        };
        int[] expectedSenLengths = {0, 4, 3, 23, 5};

        Assertions.assertArrayEquals(expectedPositions, actualPositions);
        Assertions.assertArrayEquals(expectedSenLengths, actualSenLengths);
    }

    @NotNull
    private TFullPositionEx buildFullPositionEx(int beg, int end, int wordIdx) {
        return new TFullPositionEx(new TFullPosition(TSignedPosting.Unpack(beg), TSignedPosting.Unpack(end)), wordIdx);
    }
}
