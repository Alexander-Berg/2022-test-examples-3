package ru.yandex.market.load.admin.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.dao.JugglerCheckDao;
import ru.yandex.market.load.admin.entity.JugglerCheck;
import ru.yandex.mj.generated.server.model.CheckUpdateDto;
import ru.yandex.mj.generated.server.model.ChecksUpdateDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by aproskriakov on 3/17/22
 */
public class JugglerApiServiceTest extends AbstractFunctionalTest {

    @MockBean
    protected JugglerCheckDao dao;

    @Autowired
    protected JugglerApiService service;

    @Test
    void jugglerPostTest() {
        CheckUpdateDto check = new CheckUpdateDto();
        check.setServiceName("test");
        check.setHostName("test");
        check.setStatus(CheckUpdateDto.StatusEnum.OK);
        ChecksUpdateDto checks = new ChecksUpdateDto();
        checks.addChecksItem(check);

        Optional<JugglerCheck> jugglerCheck = Optional.of(JugglerCheck.builder().build());
        when(dao.findByHostAndService("test", "test")).thenReturn(jugglerCheck);

        service.jugglerPost(checks);

        verify(dao).findByHostAndService("test", "test");

        verify(dao).updateJugglerCheck(
                argThat(v -> Objects.equals(v, check.getHostName())),
                argThat(v -> Objects.equals(v, check.getServiceName())),
                argThat(v -> Objects.equals(v, check.getStatus().getValue())),
                any(Instant.class));
    }

    @Test
    void jugglerPostUnknownTest() {
        CheckUpdateDto check = new CheckUpdateDto();
        check.setServiceName("test");
        check.setHostName("test");
        check.setStatus(CheckUpdateDto.StatusEnum.OK);
        ChecksUpdateDto checks = new ChecksUpdateDto();
        checks.addChecksItem(check);

        when(dao.findByHostAndService("test", "test")).thenReturn(Optional.empty());

        service.jugglerPost(checks);

        verify(dao).findByHostAndService("test", "test");
        verify(dao, never()).updateJugglerCheck(anyString(), anyString(), anyString(), any(Instant.class));
    }

    @Test
    void jugglerPostFlappingTest() {
        CheckUpdateDto check = new CheckUpdateDto();
        check.setServiceName("test");
        check.setHostName("test");
        check.setStatus(CheckUpdateDto.StatusEnum.OK);
        check.setFlags(List.of("flapping"));
        ChecksUpdateDto checks = new ChecksUpdateDto();
        checks.addChecksItem(check);

        service.jugglerPost(checks);

        verify(dao, times(0)).updateJugglerCheck(anyString(), anyString(),
                anyString(), any(Instant.class));
        verify(dao).touchJugglerCheck(
                argThat(v -> Objects.equals(v, check.getHostName())),
                argThat(v -> Objects.equals(v, check.getServiceName())),
                any(Instant.class));
    }
}
