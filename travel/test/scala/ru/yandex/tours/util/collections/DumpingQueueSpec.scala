package ru.yandex.tours.util.collections

import java.io.File

import org.scalatest.concurrent.Conductors
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import ru.yandex.tours.testkit.{BaseSpec, TemporaryDirectory}
import ru.yandex.tours.util.Randoms
import ru.yandex.vertis.curator.recipes.map.StringValueSerializer

import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.01.16
 */
class DumpingQueueSpec extends BaseSpec with TemporaryDirectory with Conductors {

  def newQueue(dir: File, capacity: Int): DumpingQueue[String] =
    DumpingQueue(dir, StringValueSerializer, capacity)

  def newQueue(name: String, capacity: Int): DumpingQueue[String] =
    newQueue(tempDir.newFolder(name), capacity)

  "DumpingQueue" should {
    "push and pull provided value" in {
      val queue = newQueue("simple_store", 10)
      queue.push("123")
      queue.pull() shouldBe Some("123")
    }
    "return values in given order" in {
      val queue = newQueue("order", 10)
      queue.push("1")
      queue.push("3")
      queue.push("2")
      queue.pull() shouldBe Some("1")
      queue.pull() shouldBe Some("3")
      queue.pull() shouldBe Some("2")
    }
    "return None if queue is empty" in {
      val queue = newQueue("empty", 10)
      queue.pull() shouldBe None
    }
    "return None if all values was consumed" in {
      val queue = newQueue("consume", 10)
      queue.push("1")
      queue.pull() shouldBe Some("1")
      queue.pull() shouldBe None
    }
    "flush elements after given threshold" in {
      val dir = tempDir.newFolder("threshold")
      val queue = newQueue(dir, 1)
      assert(dir.listFiles().isEmpty, "Folder for newly created queue should be empty")
      queue.push("1")
      queue.push("2")
      dir.listFiles() should have size 1
    }
    "delete file after all elements consumed" in {
      val dir = tempDir.newFolder("delete")
      val queue = newQueue(dir, 1)
      assert(dir.listFiles().isEmpty, "Folder for newly created queue should be empty")
      queue.push("1")
      queue.push("2")
      dir.listFiles() should have size 1

      while(queue.pull().nonEmpty) {}

      dir.listFiles() should have size 0
    }

    "determine its size" in {
      val queue = newQueue("size", 10)
      queue.size() shouldBe 0

      queue.push("1")
      queue.size() shouldBe 1
      queue.push("2")
      queue.size() shouldBe 2
      queue.pull()
      queue.size shouldBe 1
    }
    "determine its empty or not" in {
      val queue = newQueue("empty_or_not", 10)
      queue.isEmpty shouldBe true

      queue.push("1")
      queue.isEmpty shouldBe false
      queue.push("2")
      queue.isEmpty shouldBe false
      queue.pull()
      queue.isEmpty shouldBe false
      queue.pull()
      queue.isEmpty shouldBe true
    }
    "pass stress test" in {
      val queue = newQueue("stress", 100)
      val items = Iterator.continually(Randoms.nextString(16)).take(15000).toArray
      val conductor = new Conductor
      import conductor._

      thread("queue_producer") {
        for (item <- items) {
          queue.push(item)
//          Thread.sleep(Randoms.random.nextInt(2))
        }
      }
      thread("queue_consumer") {
        var consumed = 0
        while (consumed < items.length) {
          queue.pull() match {
            case None =>
            case Some(e) =>
              e shouldBe items(consumed)
              consumed += 1
          }
        }
      }

      conduct(Timeout(30.seconds))
      queue shouldBe empty
    }
  }
}
