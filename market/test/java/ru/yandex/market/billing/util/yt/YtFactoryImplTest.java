package ru.yandex.market.billing.util.yt;

import java.util.Map;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.inside.yt.kosher.Yt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class YtFactoryImplTest {

    @Mock
    private Yt ytMock1;

    @Mock
    private Yt ytMock2;

    @Test
    void testGetYt() {
        YtFactory ytFactory = new YtFactoryImpl(
                Map.of(
                        "Hahn", ytMock1,
                        "arnold", ytMock2
                )
        );

        assertThat(ytFactory.getYtByClusterName("HAHN"))
                .isEqualTo(ytMock1);
        assertThat(ytFactory.getYtByClusterName("arnold"))
                .isEqualTo(ytMock2);
    }

    @Test
    void testGetYtNoSuchCluster() {
        YtFactory ytFactory = new YtFactoryImpl(Map.of());

        assertThatCode(() -> ytFactory.getYtByClusterName("Hahn"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("No such cluster 'Hahn'");
    }
}
