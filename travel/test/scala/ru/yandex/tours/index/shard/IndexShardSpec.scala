package ru.yandex.tours.index.shard

import java.nio.ByteBuffer

import org.apache.commons.io.output.ByteArrayOutputStream
import org.joda.time.LocalDate
import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.index.{IndexWriter, WizardIndexItem}
import ru.yandex.tours.model.BaseModel.Pansion

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 13.03.15
 */
class IndexShardSpec extends WordSpecLike {

  def shard(items: WizardIndexItem*): IndexShard = {
    val os = new ByteArrayOutputStream()
    val writer = new IndexWriter(os)
    writer.writeHeader(items.size)
    for (item <- items.sorted) {
      writer.writeItem(item.operatorId, item.from, item.to, item.when, item.nights, item.hotelId, item.minPrice, Pansion.valueOf(item.pansion))
    }
    writer.close()
    IndexShard.fromBuffer(ByteBuffer.wrap(os.toByteArray))
  }

  val emptyShard = shard()
  val now = LocalDate.now

  val item1 = WizardIndexItem(0, 213, 9999, now, 7, 777, 10000, Pansion.AI.getNumber)
  val item2 = WizardIndexItem(0, 213, 9999, now, 7, 779, 10000, Pansion.AI.getNumber)
  val item3 = WizardIndexItem(2, 213, 996, now, 7, 779, 10000, Pansion.AI.getNumber)

  "emptyShard.find" should {
    "return empty iterator" in {
      val result = emptyShard.find(1, 1, 1, LocalDate.parse("2015-03-13"), LocalDate.parse("2015-03-14"))
      result.hasNext shouldBe false
    }
  }

  "IndexShard.find" should {
    val shard1 = shard(item1)
    val shard2 = shard(item1, item2)
    "find exact element" in {
      val it = shard1.find(0, 213, 9999, now, now.plusDays(1))
      it.toList shouldBe List(item1)
    }
    "find exact element if hotel not defined" in {
      val it = shard1.find(0, 213, 9999, now, now.plusDays(1))
      it.toList shouldBe List(item1)
    }
    "not find element" in {
      shard1.find(1, 213, 9999, now, now.plusDays(1)) shouldBe empty
      shard1.find(0, 212, 9999, now, now.plusDays(1)) shouldBe empty
      shard1.find(0, 213, 9998, now, now.plusDays(1)) shouldBe empty
      shard1.find(0, 213, 9999, now.plusDays(1), now.plusDays(2)) shouldBe empty
      shard1.find(0, 213, 9999, now, now) shouldBe empty
//      shard1.find(0, 213, 9999, now, now.plusDays(1)) shouldBe empty
    }
    "find 2 matched elements if hotel not defined" in {
      val it = shard2.find(0, 213, 9999, now, now.plusDays(1))
      it.toList shouldBe List(item1, item2)
    }
  }
  "IndexShard.iterator" should {
    "return all elements" in {
      val s = shard(item1, item2, item3)
      s.iterator.toList shouldBe List(item1, item2, item3)
    }
  }
}
