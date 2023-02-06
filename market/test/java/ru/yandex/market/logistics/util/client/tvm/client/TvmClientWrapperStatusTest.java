package ru.yandex.market.logistics.util.client.tvm.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.passport.tvmauth.TicketStatus;

import static org.assertj.core.api.Assertions.assertThat;

class TvmClientWrapperStatusTest {

    private final TvmClientWrapper wrapper = new TvmClientWrapper(new MockTvmClient());

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = "UNSUPPORTED_VERSION", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Конвертация статусов")
    public void convertStatus(TicketStatus status) {
        assertThat(wrapper.convertStatus(status)).isNotEqualTo(TvmTicketStatus.UNSUPPORTED_VERSION);
    }

}
