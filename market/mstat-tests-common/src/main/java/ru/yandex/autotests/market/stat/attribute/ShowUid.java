package ru.yandex.autotests.market.stat.attribute;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import java.time.LocalDateTime;

/**
 * Created by entarrion on 21.01.15.
 */
public class ShowUid {
    public static ImmutableList<Integer> BENFORD_DISTRIBUTION =
            ImmutableList.of(23, 36, 46, 53, 59, 64, 69, 73, 76, 80, 82, 85, 88, 90, 92, 94, 96, 98, 99);

    public static String generate() {
        return generate(BlockId.generate());
    }

    public static String generate(LocalDateTime dateTime) {

        return generate(BlockId.generate(dateTime));
    }

    public static String generate(String blockId) {
        return generate(blockId, String.valueOf(UrlType.getRandomValue()));
    }

    public static String generate(String blockId, String urlType) {
        return generate(blockId, getRandomPosition(), urlType);
    }

    public static String generate(String blockId, String position, String urlType) {
        return blockId + StringUtils.leftPad(urlType, 2, "0") + StringUtils.leftPad(position, 3, "0");
    }

    private static String getRandomPosition() {
        Integer next = RandomUtils.nextInt(100);
        int index;
        for (index = 0; index < BENFORD_DISTRIBUTION.size(); index++) {
            if (BENFORD_DISTRIBUTION.get(index) > next) {
                break;
            }
        }
        return String.valueOf(index + 1);
    }

}
