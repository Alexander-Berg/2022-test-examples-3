package ru.yandex.market.yt.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.yt.binding.YTAttributes.TrackingAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YTAttributesTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(YTAttributesTest.class);

    @ParameterizedTest
    @MethodSource("testJson")
    void testPivotIntKeysFromJson(String filename, Collection<Integer> expectKeys) {
        var actual = testImpl(filename);
        LOGGER.info("{}", actual);

        var expect = new YTAttributes.AttributesBuilder().pivotKeys(expectKeys).build();
        var pivotKeys = expect.get(TrackingAttributes.PIVOT_KEYS);
        if (pivotKeys == null) {
            assertEquals(List.of(), expectKeys);
        } else {

            var copy = new ArrayList<>(new LinkedHashSet<>(expectKeys));
            Collections.sort(copy);
            assertEquals(copy, expectKeys);

            var expectKeysCopy = new ArrayList<>(expectKeys);
            expectKeysCopy.add(0, null);
            assertEquals(expectKeysCopy, pivotKeys.asList().stream()
                    .map(YTreeNode::asList)
                    .map(list -> list.isEmpty() ? YTree.entityNode() : list.get(0))
                    .map(node -> node.isEntityNode() ? null : node.intValue())
                    .collect(Collectors.toList()));
        }

        assertEquals(expect, actual);
    }

    private static Map<String, YTreeNode> testImpl(String filename) {
        return new YTAttributes.AttributesBuilder().pivotIntKeysFromJson(filename).build();
    }

    static Object[] testJson() {
        return new Object[][]{
                {"", List.of()},
                {"classpath:yt/offers-keys-prestable.json", List.of()},
                {"classpath:yt/offers-keys-production.json", List.of(723, 6102, 18710, 28494, 34098, 48002,
                        58669, 65510, 79354, 85294, 88724, 92611, 104113, 110127, 111415, 121642, 124310, 139491,
                        162553, 176975, 184290, 195024, 195737, 201560, 221378, 234350, 242473, 247152, 257942,
                        272643, 275432, 281503, 287036, 296746, 304033, 314422, 325203, 336145, 342602, 352273,
                        359584, 368577, 376376, 388647, 391493, 403409, 412468, 418721, 422750, 426465, 426687,
                        426750, 431782, 435090, 442308, 445147, 450551, 459811, 464522, 470556, 476023, 481473,
                        485003, 531940, 531952, 531979, 535048, 536412, 538886, 540361, 544087, 548623, 551744,
                        554217, 557491, 562317, 565960, 568876, 572437, 575882, 578645, 582260, 586325, 588432,
                        593244, 594900, 598104, 604295, 610027, 610622, 613908)},
                {"classpath:yt/offers-keys-testing.json", List.of(493, 3858, 6039, 13161, 22848, 28484, 37245,
                        49398, 54550, 62403, 74778, 88316, 93477, 104711, 110107, 116875, 123397, 132992, 153879,
                        163421, 176852, 191413, 202597, 217906, 230623, 234561, 241079, 246951, 253104, 260918,
                        263873, 275175, 276429, 281491, 282921, 287321, 296131, 302746, 314283, 320590, 323814,
                        338794, 348151, 353088, 359584, 363087, 372545, 381793, 389196, 392726, 400185, 407302,
                        412468, 420474, 424313, 426675, 430930, 434253, 437119, 442404, 445080, 450130, 453245,
                        461243, 467763, 470444, 470765, 475706, 479179, 484472, 531483, 531941, 531955, 532897,
                        534938, 535447, 536108, 538277, 540331, 543198, 548623, 554344, 556700, 559228, 564681,
                        568775, 572420, 573496, 578142, 581752, 585384, 590201, 596402)}};
    }

}
