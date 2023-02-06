package ru.yandex.tours.index.composite

import java.nio.ByteBuffer

import org.apache.commons.io.output.ByteArrayOutputStream
import org.joda.time.LocalDate
import org.scalatest.WordSpecLike
import org.scalatest.Matchers._
import ru.yandex.tours.util.lang.Dates._
import ru.yandex.tours.index.{WizardIndexItem, IndexWriter}
import ru.yandex.tours.index.shard.IndexShard
import ru.yandex.tours.model.BaseModel.Pansion

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.03.15
 */
class CompositeIndexSpec extends WordSpecLike {
  val emptyIndex = CompositeIndex.optimized(Vector.empty)

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

  val now = LocalDate.now

  val item1 = WizardIndexItem(0, 213, 9999, now, 7, 777, 10000, Pansion.AI.getNumber)
  val item2 = WizardIndexItem(0, 213, 9999, now, 7, 779, 10000, Pansion.AI.getNumber)
  val item3 = WizardIndexItem(2, 213, 996, now, 7, 779, 10000, Pansion.AI.getNumber)

  "empty CompositeIndex" should {
    "produce empty iterator" in {
      emptyIndex.iterator shouldBe empty
    }
    "find empty iterator" in {
      emptyIndex.find(0, 213, 1111, LocalDate.now.minusDays(10), LocalDate.now) shouldBe empty
    }
  }

  "optimized CompositeIndex" should {
    "handle empty shards" in {
      val index = CompositeIndex.optimized(Vector(shard(), shard()))
      index.iterator.toList shouldBe List()
    }
    "preserve order" in {
      val index = CompositeIndex.optimized(Vector(shard(item1, item2), shard(item3)))
      index.iterator.toList shouldBe List(item1, item2, item3)
    }
    "omit duplicate elements" in {
      val index = CompositeIndex.optimized(Vector(shard(item1, item2), shard(item1, item3)))
      index.iterator.toList shouldBe List(item1, item2, item3)
    }
    "omit duplicate elements on end" in {
      val index = CompositeIndex.optimized(Vector(shard(item2, item3), shard(item1, item3)))
      index.iterator.toList shouldBe List(item1, item2, item3)
    }
    "omit duplicate elements #2" in {
      val index = CompositeIndex.optimized(Vector(shard(), shard(item1, item1, item1)))
      index.iterator.toList shouldBe List(item1)
    }
    "omit duplicate elements #3" in {
      val index = CompositeIndex.optimized(Vector(shard(item1, item1, item1), shard()))
      index.iterator.toList shouldBe List(item1)
    }
    "omit duplicate elements #4" in {
      val index = CompositeIndex.optimized(Vector(shard(item1, item1, item1), shard(item2)))
      index.iterator.toList shouldBe List(item1, item2)
    }
    "merge sort" in {
      val index = CompositeIndex.optimized(Vector(shard(item1, item3), shard(item2)))
      index.iterator.toList shouldBe List(item1, item2, item3)
    }
//    "find item" in {
//      val index = CompositeIndex.optimized(Vector(shard(item1, item2), shard(item3)))
//      index.find(item1.operatorId, item1.from, item1.to, item1.when.toLocalDate, item1.when.toLocalDate.plusDays(1)).toList shouldBe List(item1)
//    }
    "find several items" in {
      val index = CompositeIndex.optimized(Vector(shard(item1, item2), shard(item3)))
      index.find(item1.operatorId, item1.from, item1.to, item1.when.toLocalDate, item1.when.toLocalDate.plusDays(1)).toList shouldBe List(item1, item2)
    }
  }
}
