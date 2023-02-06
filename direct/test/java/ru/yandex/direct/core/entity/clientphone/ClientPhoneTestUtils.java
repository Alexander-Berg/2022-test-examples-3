package ru.yandex.direct.core.entity.clientphone;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkState;

public class ClientPhoneTestUtils {
    private static final AtomicLong COUNTER = new AtomicLong();

    private ClientPhoneTestUtils() {
    }

    /**
     * Возвращает валидные номера, гарантируя их уникальность
     */
    public static String getUniqPhone() {
        // валидны только российские номера. Не все номера с +7 российские (DIRECT-120863)
        String prefix = "+79";
        int digitsLeft = 9;
        String nextNumber = Long.toString(COUNTER.incrementAndGet());
        checkState(nextNumber.length() < digitsLeft);
        return prefix + StringUtils.leftPad(nextNumber, digitsLeft, '0');
    }
}
