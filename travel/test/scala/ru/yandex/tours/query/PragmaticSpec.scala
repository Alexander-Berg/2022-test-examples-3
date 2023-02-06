package ru.yandex.tours.query

import java.nio.ByteBuffer

import com.google.common.hash.{BloomFilter, Funnels}
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import org.joda.time.MonthDay
import org.scalatest.Inspectors
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 26.01.16
 */
class PragmaticSpec extends BaseSpec with Inspectors {
  private def check(pragmatic: Pragmatic) = {
    val bytes = Pragmatic.write(pragmatic)
    val read = Pragmatic.read(ByteBuffer.wrap(bytes))
    read shouldBe pragmatic
    println(s"Serialized $pragmatic to ${bytes.length} bytes")
  }

  private def checkSingleton(pragmatic: Pragmatic) = {
    val bytes = Pragmatic.write(pragmatic)
    val read = Pragmatic.read(ByteBuffer.wrap(bytes))
    read shouldBe theSameInstanceAs (pragmatic)
    println(s"Serialized singleton $pragmatic to ${bytes.length} bytes")
  }

  "Pragmatic `write` and `read` methods" should {
    "work with Unknown" in { checkSingleton(Unknown) }
    "work with StopWord" in { checkSingleton(StopWord) }
    "work with Stars" in { check(Stars(3)) }
    "work with DateInterval" in {
      check(DateInterval(MonthDay.parse("--01-12"), MonthDay.parse("--01-31")))
    }
    "work with HotelMarker" in {
      forAll(QueryHotelType.values().toSeq) { ht =>
        check(HotelMarker(ht))
      }
    }
    "work with empty HotelNamePartMap" in { check(HotelNamePartMap(new Long2IntOpenHashMap())) }
    "work with non-empty HotelNamePartMap" in {
      check(HotelNamePartMap(new Long2IntOpenHashMap(Array(0L, Long.MaxValue), Array(Int.MaxValue, 3))))
    }
    "work with empty HotelNamePartBloom" in {
      val filter = BloomFilter.create[java.lang.Long](Funnels.longFunnel(), 10)
      filter.put(1L)
      filter.put(3L)
      check(HotelNamePartBloom(filter))
    }
  }
}
