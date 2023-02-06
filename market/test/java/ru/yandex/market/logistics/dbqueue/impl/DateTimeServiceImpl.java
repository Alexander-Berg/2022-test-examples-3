package ru.yandex.market.logistics.dbqueue.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import ru.yandex.market.logistics.dbqueue.time.DateTimeServiceInterface;

@Service
public class DateTimeServiceImpl implements DateTimeServiceInterface {
    @Override
    public LocalDateTime getDateTime() {
        return LocalDateTime.parse("2020-01-01T15:00");
    }
}
