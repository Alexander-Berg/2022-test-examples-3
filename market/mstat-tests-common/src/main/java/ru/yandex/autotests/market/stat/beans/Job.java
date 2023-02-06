package ru.yandex.autotests.market.stat.beans;

import java.time.Duration;

/**
 * Created by jkt on 01.07.14.
 */
public interface Job {
    String getName();
    default Duration getMaxDuration() {
      return Duration.ofMinutes(30);
    }
}
