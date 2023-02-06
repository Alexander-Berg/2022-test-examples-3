package ru.yandex.market.replenishment.autoorder.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.EmailNotification;
import ru.yandex.market.replenishment.autoorder.repository.postgres.EmailNotificationRepository;

public class EmailNotificationRepositoryTest extends FunctionalTest {

    @Autowired
    private EmailNotificationRepository emailNotificationRepository;

    @Test
    @DbUnitDataSet(after = "EmailNotificationRepositoryTest.Insert.after.csv")
    public void insertTest() {
        final List<EmailNotification> toInsert = List.of(
            new EmailNotification(
                null,
                "test1@yandex-team.ru",
                "test1",
                "test1",
                false,
                null
            ),
            new EmailNotification(
                null,
                "test2@yandex-team.ru",
                "test2",
                "test2",
                false,
                LocalDateTime.of(2021, 5, 25, 0, 0)
            )
        );
        toInsert.forEach(emailNotificationRepository::insert);
    }

    @Test
    public void insertFailTest() {
        Assertions.assertThrows(DataIntegrityViolationException.class,
            () -> emailNotificationRepository.insert(
                new EmailNotification(
                    null,
                    null,
                    "test3",
                    "test3",
                    false,
                    null
                )
            )
        );
    }
}
