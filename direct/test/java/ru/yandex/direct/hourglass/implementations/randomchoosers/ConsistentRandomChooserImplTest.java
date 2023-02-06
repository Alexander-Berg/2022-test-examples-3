package ru.yandex.direct.hourglass.implementations.randomchoosers;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.hourglass.implementations.MD5Hash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsistentRandomChooserImplTest {

    @Test
    void chooseIfEntityHashHasNotCollision() {
        MD5Hash md5Hash = mock(MD5Hash.class);
        when(md5Hash.hash(anyString())).thenAnswer(args -> -Long.parseLong(args.getArgument(0)));
        ConsistentRandomChooserImpl<Integer> consistentRandomChooser = new ConsistentRandomChooserImpl<>("8", md5Hash);

        List<Integer> elements = List.of(1, 3, 5, 7, 9, 11, 13, 15);
        List<Integer> choosed = consistentRandomChooser.choose(elements, 5);

        assertThat(choosed).containsExactly(7, 5, 3, 1, 15);
    }

    @Test
    void chooseIfEntityHashHasCollision() {
        MD5Hash md5Hash = mock(MD5Hash.class);
        when(md5Hash.hash(anyString())).thenAnswer(args -> -Long.parseLong(args.getArgument(0)));
        ConsistentRandomChooserImpl<Integer> consistentRandomChooser = new ConsistentRandomChooserImpl<>("9", md5Hash);

        List<Integer> elements = List.of(1, 3, 5, 7, 9, 11, 13, 15);
        List<Integer> choosed = consistentRandomChooser.choose(elements, 5);

        assertThat(choosed).containsExactly(7, 5, 3, 1, 15);
    }

    @Test
    void chooseIfEntityHashIsBig() {
        MD5Hash md5Hash = mock(MD5Hash.class);
        when(md5Hash.hash(anyString())).thenAnswer(args -> -Long.parseLong(args.getArgument(0)));
        ConsistentRandomChooserImpl<Integer> consistentRandomChooser =
                new ConsistentRandomChooserImpl<>(Integer.MAX_VALUE + "", md5Hash);

        List<Integer> elements = List.of(1, 3, 5, 7, 9, 11, 13, 15);
        List<Integer> choosed = consistentRandomChooser.choose(elements, 5);

        assertThat(choosed).containsExactly(15, 13, 11, 9, 7);
    }

    @Test
    void chooseIfEntityHashIsSmall() {
        MD5Hash md5Hash = mock(MD5Hash.class);
        when(md5Hash.hash(anyString())).thenAnswer(args -> -Long.parseLong(args.getArgument(0)));
        ConsistentRandomChooserImpl<Integer> consistentRandomChooser =
                new ConsistentRandomChooserImpl<>(Integer.MIN_VALUE + "", md5Hash);

        List<Integer> elements = List.of(1, 3, 5, 7, 9, 11, 13, 15);
        List<Integer> choosed = consistentRandomChooser.choose(elements, 5);

        assertThat(choosed).containsExactly(15, 13, 11, 9, 7);
    }

}
