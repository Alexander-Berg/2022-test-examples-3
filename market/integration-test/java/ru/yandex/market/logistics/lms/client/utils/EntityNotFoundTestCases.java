package ru.yandex.market.logistics.lms.client.utils;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EntityNotFoundTestCases<T> {
    boolean existsInRedis;

    boolean existsInYt;

    T expectedClientResponse;
}
