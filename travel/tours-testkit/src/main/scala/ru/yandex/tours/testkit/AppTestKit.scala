package ru.yandex.tours.testkit

import scala.annotation.tailrec
import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.util.control.NonFatal
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.03.15
 */
trait AppTestKit {
  def now: FiniteDuration = System.nanoTime.nanos

  def awaitAssert(a: ⇒ Any, max: Duration = Duration.Undefined, interval: Duration = 800.millis) {
    val _max = max
    val stop = now + _max

    @tailrec
    def poll(t: Duration) {
      val failed =
        try { a; false } catch {
          case NonFatal(e) ⇒
            if ((now + t) >= stop) throw e
            true
        }
      if (failed) {
        Thread.sleep(t.toMillis)
        poll((stop - now) min interval)
      }
    }

    poll(_max min interval)
  }
}
