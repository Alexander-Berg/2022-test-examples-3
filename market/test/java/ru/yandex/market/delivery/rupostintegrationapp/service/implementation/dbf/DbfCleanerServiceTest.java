package ru.yandex.market.delivery.rupostintegrationapp.service.implementation.dbf;

import java.time.LocalDateTime;
import java.util.List;

import javax.mail.Flags;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.dao.dbf.DbfMailRepository;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.DbfMail;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf.DbfMailer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfCleanerServiceTest {

    @Mock
    private DbfMailRepository dbfMailRepository;

    @Mock
    private DbfMailer dbfMailer;

    private DbfCleanerService dbfCleanerService;

    private static final int TTL = 12;
    private static final LocalDateTime TTL_DATETIME = LocalDateTime.now().minusMonths(TTL);

    @BeforeEach
    void setUp() {
        dbfCleanerService = new DbfCleanerService(dbfMailer, dbfMailRepository, TTL);
    }

    @Test
    void execute() {
        when(dbfMailRepository.findAllByDeletedIsNullAndCreatedLessThan(
            argThat(this::dateTimeMatcher))
        )
            .thenReturn(List.of());
        dbfCleanerService.markOldDbfMailDeleted();
        verify(dbfMailRepository, never()).save(any(DbfMail.class));
    }

    @Test
    void executeMany() {

        var mail = new DbfMail();
        mail.setId(3L);
        mail.setSender("sender3");
        mail.setSubject("subject3");

        when(dbfMailRepository.findAllByDeletedIsNullAndCreatedLessThan(
            argThat(this::dateTimeMatcher))
        )
            .thenReturn(List.of(mail));

        Assertions.assertNull(mail.getDeleted());
        LocalDateTime startDt = LocalDateTime.now();

        dbfCleanerService.markOldDbfMailDeleted();
        verify(dbfMailRepository, times(1)).save(eq(mail));
        verify(dbfMailer, times(1)).findByIdAndMark(eq(mail), eq(Flags.Flag.DELETED), eq(true));

        Assertions.assertTrue(mail.getDeleted().isAfter(startDt));
    }

    boolean dateTimeMatcher(LocalDateTime actual) {
        return actual.toLocalDate().equals(TTL_DATETIME.toLocalDate());
    }
}
