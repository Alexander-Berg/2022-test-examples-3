package ru.yandex.calendar.frontend.a3.interceptors;

import java.util.Optional;

import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.commune.a3.action.WebRequestMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class CalendarRequestInterceptorTest {
    private static final Long UID = 1120000000000000L;
    private static final Long ACTOR_UID = 1120000000001111L;
    private static final Long TARGET_UID = 1120000000002222L;

    @Test
    public void getEventsAcceptsUidWithoutActorUidAndTargetUid() {
        val parameters = new WebRequestMock()
                .param("uid", UID.toString())
                .getParameters();
        assertThatCode(() -> CalendarRequestInterceptorHelper.checkParameters(parameters))
                .doesNotThrowAnyException();
        assertThat(CalendarRequestInterceptorHelper.getUid(parameters))
                .contains(UID);
    }

    @Test
    public void getEventsAcceptsActorUidWithoutUidAndTargetUid() {
        val params = new WebRequestMock()
                .param("actorUid", ACTOR_UID.toString())
                .getParameters();
        assertThatCode(() -> CalendarRequestInterceptorHelper.checkParameters(params))
                .doesNotThrowAnyException();
        assertThat(CalendarRequestInterceptorHelper.getUid(params))
                .contains(ACTOR_UID);
    }

    @Test
    public void getEventsAcceptsActorUidAndTargetUidWithoutUid() {
        val parameters = new WebRequestMock()
                .param("actorUid", ACTOR_UID.toString())
                .param("targetUid", TARGET_UID.toString())
                .getParameters();
        assertThatCode(() -> CalendarRequestInterceptorHelper.checkParameters(parameters))
                .doesNotThrowAnyException();
        assertThat(CalendarRequestInterceptorHelper.getUid(parameters))
                .contains(ACTOR_UID);
    }

    @Test
    public void getEventsAcceptsNoUid() {
        val parameters = new WebRequestMock().getParameters();
        assertThatCode(() -> CalendarRequestInterceptorHelper.checkParameters(parameters))
                .doesNotThrowAnyException();
        assertThat(CalendarRequestInterceptorHelper.getUid(parameters))
                .isEqualTo(Optional.empty());
    }

    @Test
    public void getEventsThrowsExceptionOnUidWithActorUid() {
        val parameters = new WebRequestMock()
                .param("uid", UID.toString())
                .param("actorUid", ACTOR_UID.toString())
                .getParameters();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalendarRequestInterceptorHelper.checkParameters(parameters));
    }

    @Test
    public void getEventsThrowsExceptionOnUidWithTargetUid() {
        val parameters = new WebRequestMock()
                .param("uid", UID.toString())
                .param("targetUid", TARGET_UID.toString())
                .getParameters();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalendarRequestInterceptorHelper.checkParameters(parameters));
    }

    @Test
    public void getEventsThrowsExceptionOnTargetUidWithoutActorUid() {
        val parameters = new WebRequestMock()
                .param("targetUid", TARGET_UID.toString())
                .getParameters();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalendarRequestInterceptorHelper.checkParameters(parameters));
    }

    @Test
    public void getEventsThrowsExceptionOnUidWithActorUidAndTargetUid() {
        val parameters = new WebRequestMock()
                .param("uid", UID.toString())
                .param("actorUid", ACTOR_UID.toString())
                .param("targetUid", TARGET_UID.toString())
                .getParameters();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalendarRequestInterceptorHelper.checkParameters(parameters));
    }

    @Test
    public void getEventsThrowsExceptionOnTargetUidWithLayerId() {
        val parameters = new WebRequestMock()
                .param("layerId", "12345")
                .param("targetUid", TARGET_UID.toString())
                .getParameters();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CalendarRequestInterceptorHelper.checkParameters(parameters));
    }
}
