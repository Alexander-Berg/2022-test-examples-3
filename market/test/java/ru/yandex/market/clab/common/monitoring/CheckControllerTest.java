package ru.yandex.market.clab.common.monitoring;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 26.12.2018
 */
public class CheckControllerTest {

    @Test
    public void returnOk() {
        CheckController controller = new CheckController(() -> new ComplexMonitoring.Result(MonitoringStatus.OK, "ok-message"));

        ResponseEntity<String> responseEntity = controller.handle();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo("0;ok-message");
    }

    @Test
    public void returnWarn() {
        CheckController controller = new CheckController(() -> new ComplexMonitoring.Result(MonitoringStatus.WARNING, "warn-message"));

        ResponseEntity<String> responseEntity = controller.handle();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo("1;warn-message");
    }

    @Test
    public void returnCrit() {
        CheckController controller = new CheckController(() -> new ComplexMonitoring.Result(MonitoringStatus.CRITICAL, "crit-message"));

        ResponseEntity<String> responseEntity = controller.handle();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo("2;crit-message");
    }

    @Test
    public void returnCritOnException() {
        CheckController controller = new CheckController(() -> {
            throw new RuntimeException("exception-message");
        });

        ResponseEntity<String> responseEntity = controller.handle();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo("2;Internal error java.lang.RuntimeException: exception-message");
    }

    @Test
    public void returnCritOnNullResponce() {
        CheckController controller = new CheckController(() -> null);

        ResponseEntity<String> responseEntity = controller.handle();
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isEqualTo("2;Internal error java.lang.IllegalStateException: result is null");
    }
}
