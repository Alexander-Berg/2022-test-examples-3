package ru.yandex.direct.common.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientIdComparableTest {
    private static final ClientId CLIENT_ID1 = ClientId.fromLong(1);
    private static final ClientId CLIENT_ID3 = ClientId.fromLong(3);
    private static final ClientId CLIENT_ID10 = ClientId.fromLong(10);
    private static final ClientId CLIENT_ID22 = ClientId.fromLong(22);

    private ClientId[] test;
    private ClientId[] expected = {CLIENT_ID1, CLIENT_ID3, CLIENT_ID10, CLIENT_ID22};

    @Test
    public void test() {
        test = new ClientId[]{CLIENT_ID10, CLIENT_ID22, CLIENT_ID3, CLIENT_ID1};
        List<ClientId> sorted = Arrays.stream(test).sorted().collect(Collectors.toList());
        assertThat(sorted).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testReversedData() {
        test = new ClientId[]{CLIENT_ID22, CLIENT_ID10, CLIENT_ID3, CLIENT_ID1};
        List<ClientId> sorted = Arrays.stream(test).sorted().collect(Collectors.toList());
        assertThat(sorted).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testSortedData() {
        test = expected;
        List<ClientId> sorted = Arrays.stream(test).sorted().collect(Collectors.toList());
        assertThat(sorted).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void testDuplicates() {
        test = new ClientId[]{CLIENT_ID1, CLIENT_ID10, CLIENT_ID22, CLIENT_ID3, CLIENT_ID1, CLIENT_ID22};
        expected = new ClientId[]{CLIENT_ID1, CLIENT_ID1, CLIENT_ID3, CLIENT_ID10, CLIENT_ID22, CLIENT_ID22};
        List<ClientId> sorted = Arrays.stream(test).sorted().collect(Collectors.toList());
        assertThat(sorted).containsExactlyInAnyOrder(expected);
    }
}
