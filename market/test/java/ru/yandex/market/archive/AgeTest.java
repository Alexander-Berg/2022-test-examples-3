package ru.yandex.market.archive;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author snoop
 */
public class AgeTest {

    @Test
    public void boundary() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyMMddHH");

        List<Pair<Age, String>> ages = Arrays.asList(
                Pair.of(new Age(1, Age.Unit.HOUR), now.format(fmt)),
                Pair.of(new Age(24, Age.Unit.HOUR), now.format(fmt)),
                Pair.of(new Age(30, Age.Unit.HOUR), now.format(fmt)),
                Pair.of(new Age(1, Age.Unit.DAY), now.format(fmt)),
                Pair.of(new Age(28, Age.Unit.DAY), now.format(fmt)),
                Pair.of(new Age(29, Age.Unit.DAY), now.format(fmt)),
                Pair.of(new Age(30, Age.Unit.DAY), now.format(fmt)),
                Pair.of(new Age(31, Age.Unit.DAY), now.format(fmt)),
                Pair.of(new Age(33, Age.Unit.DAY), now.format(fmt))
        );

        int i = 0;
        for (Pair<Age, String> age : ages) {
            Assertions.assertEquals(
                    LocalDateTime.parse(age.getRight(), fmt).
                            minus(age.getLeft().amount(), age.getLeft().chronoUnit()).
                            truncatedTo(age.getLeft().chronoUnit()),
                    age.getLeft().boundary(now), "Test " + i++);
        }

        ages = Arrays.asList(
                Pair.of(new Age(1, Age.Unit.MONTH), now.format(fmt)),
                Pair.of(new Age(12, Age.Unit.MONTH), now.format(fmt)),
                Pair.of(new Age(13, Age.Unit.MONTH), now.format(fmt))
        );

        for (Pair<Age, String> age : ages) {
            Assertions.assertEquals(
                    LocalDateTime.parse(age.getRight(), fmt).
                            minus(age.getLeft().amount(), age.getLeft().chronoUnit()).
                            withDayOfMonth(1).
                            truncatedTo(ChronoUnit.DAYS),
                    age.getLeft().boundary(now), "Test " + i++);
        }

        ages = Arrays.asList(
                Pair.of(new Age(1, Age.Unit.YEAR), now.format(fmt)),
                Pair.of(new Age(5, Age.Unit.YEAR), now.format(fmt))
        );

        for (Pair<Age, String> age : ages) {
            Assertions.assertEquals(
                    LocalDateTime.parse(age.getRight(), fmt).
                            minus(age.getLeft().amount(), age.getLeft().chronoUnit()).
                            withDayOfYear(1).
                            truncatedTo(ChronoUnit.DAYS),
                    age.getLeft().boundary(now), "Test " + i++);
        }
    }

}
