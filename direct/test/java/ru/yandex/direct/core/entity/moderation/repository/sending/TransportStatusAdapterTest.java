package ru.yandex.direct.core.entity.moderation.repository.sending;

import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.moderation.model.TransportStatus;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.enums.BannerTurbolandingsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;

import static org.assertj.core.api.Assertions.assertThat;


@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TransportStatusAdapterTest {

    @Test
    public void fromBannerStatusModerateTest() {
        Map<BannersStatusmoderate, TransportStatus> expected = Map.of(
                BannersStatusmoderate.Ready, TransportStatus.Ready,
                BannersStatusmoderate.New, TransportStatus.New,
                BannersStatusmoderate.Yes, TransportStatus.Yes,
                BannersStatusmoderate.No, TransportStatus.No,
                BannersStatusmoderate.Sent, TransportStatus.Sent,
                BannersStatusmoderate.Sending, TransportStatus.Sending
        );

        for (BannersStatusmoderate statusmoderate : BannersStatusmoderate.values()) {
            assertThat(expected).containsKey(statusmoderate);
            assertThat(expected.get(statusmoderate)).isEqualTo(TransportStatusAdapter.fromDb(statusmoderate));
        }
    }

    @Test
    public void fromBannerTurbolandingsStatusmoderateTest() {
        Map<BannerTurbolandingsStatusmoderate, TransportStatus> expected = Map.of(
                BannerTurbolandingsStatusmoderate.Ready, TransportStatus.Ready,
                BannerTurbolandingsStatusmoderate.New, TransportStatus.New,
                BannerTurbolandingsStatusmoderate.Yes, TransportStatus.Yes,
                BannerTurbolandingsStatusmoderate.No, TransportStatus.No,
                BannerTurbolandingsStatusmoderate.Sent, TransportStatus.Sent,
                BannerTurbolandingsStatusmoderate.Sending, TransportStatus.Sending
        );

        for (BannerTurbolandingsStatusmoderate statusmoderate : BannerTurbolandingsStatusmoderate.values()) {
            assertThat(expected).containsKey(statusmoderate);
            assertThat(expected.get(statusmoderate)).isEqualTo(TransportStatusAdapter.fromDb(statusmoderate));
        }
    }

    @Test
    public void toBannerStatusModerate() {
        Map<TransportStatus, BannersStatusmoderate> expected = Map.of(
                TransportStatus.Ready, BannersStatusmoderate.Ready,
                TransportStatus.New, BannersStatusmoderate.New,
                TransportStatus.Yes, BannersStatusmoderate.Yes,
                TransportStatus.No, BannersStatusmoderate.No,
                TransportStatus.Sent, BannersStatusmoderate.Sent,
                TransportStatus.Sending, BannersStatusmoderate.Sending
        );
        for (TransportStatus transportStatus : TransportStatus.values()) {
            assertThat(expected).containsKey(transportStatus);
            assertThat(expected.get(transportStatus)).isEqualTo(TransportStatusAdapter.toBannerStatusModerate(transportStatus));
        }
    }

    @Test
    public void toBannerTurbolandingsStatusmoderate() {
        Map<TransportStatus, BannerTurbolandingsStatusmoderate> expected = Map.of(
                TransportStatus.Ready, BannerTurbolandingsStatusmoderate.Ready,
                TransportStatus.New, BannerTurbolandingsStatusmoderate.New,
                TransportStatus.Yes, BannerTurbolandingsStatusmoderate.Yes,
                TransportStatus.No, BannerTurbolandingsStatusmoderate.No,
                TransportStatus.Sent, BannerTurbolandingsStatusmoderate.Sent,
                TransportStatus.Sending, BannerTurbolandingsStatusmoderate.Sending
        );

        for (TransportStatus transportStatus : TransportStatus.values()) {
            assertThat(expected).containsKey(transportStatus);
            assertThat(expected.get(transportStatus)).isEqualTo(TransportStatusAdapter.toBannerTurbolandingsStatusmoderate(transportStatus));
        }

    }
}
