package ru.yandex.tours.testkit

import com.google.common.base.Ticker

import scala.concurrent.duration.FiniteDuration

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 22.12.15
 */
class TestTicker(var now: Long = System.nanoTime()) extends Ticker {
  override def read(): Long = now

  def advance(interval: FiniteDuration): Unit = {
    now += interval.toNanos
  }
}
