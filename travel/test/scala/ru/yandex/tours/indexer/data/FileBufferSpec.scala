package ru.yandex.tours.indexer.data

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.{ActorMaterializerSettings, Attributes, ActorMaterializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{IntegrationPatience, Eventually, ScalaFutures}
import ru.yandex.tours.testkit.{BaseSpec, TemporaryDirectory}
import ru.yandex.tours.util.zoo.IntSerializer

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.01.16
 */
class FileBufferSpec extends BaseSpec with TemporaryDirectory with Eventually
  with ScalaFutures with IntegrationPatience {

  implicit val actorSystem = ActorSystem("file-buffer-spec", ConfigFactory.empty())
  implicit val actorMaterializer = ActorMaterializer()
  implicit val ec = actorSystem.dispatcher


  override protected def afterAll(): Unit = {
    super.afterAll()
    actorMaterializer.shutdown()
    actorSystem.terminate()
  }

  def createBuffer(maxMemorySize: Int = 5, maxSize: Int = 10000000) = {
    val buffer = new FileBuffer[Int](tempDir.newFolder(), IntSerializer, maxMemorySize, maxSize)
    val flow = Flow[Int].transform(() => buffer)
    val (pub, sub) = TestSource.probe[Int]
      .via(flow)
      .toMat(TestSink.probe[Int])(Keep.both)
      .run()

    (pub, sub, buffer)
  }

  "FileBuffer" should {
    "pass elements in same order" in {
      val (pub, sub, _) = createBuffer()

      pub.sendNext(3)
      pub.sendNext(2)
      pub.sendNext(1)
      pub.sendComplete()

      sub.request(3)
      sub.expectNext(3, 2, 1)

      sub.expectComplete()
    }
    "buffer passed elements" in {
      val (pub, sub, buffer) = createBuffer()

      pub.sendNext(3)
      eventually { buffer.buffer should have size 1 }
      pub.sendNext(2)
      eventually { buffer.buffer should have size 2 }
      pub.sendNext(1)
      eventually { buffer.buffer should have size 3 }
      pub.sendComplete()

      sub.request(3)
      eventually { buffer.buffer should have size 0 }

      sub.expectNext(3, 2, 1)
      sub.expectComplete()
    }
    "dump element after threshold" in {
      val (pub, sub, buffer) = createBuffer(maxMemorySize = 5)

      for (i <- 0 until 5) pub.sendNext(i)
      eventually { buffer.buffer should have size 5 }
      buffer.buffer.inMemorySize() shouldBe 5

      pub.sendNext(5)
      eventually { buffer.buffer should have size 6 }
      buffer.buffer.inMemorySize() shouldBe 0 //dump after 6th element

      for (i <- 6 until 50) pub.sendNext(i)
      eventually { buffer.buffer should have size 50 }
      buffer.buffer.inMemorySize() shouldBe 0
      sub.request(50)
      sub.expectNextN(0 until 50)
    }
    "backpressure on maxSize reached" in {
      val (pub, sub, buffer) = createBuffer(maxMemorySize = 5, maxSize = 10)

      for (i <- 0 until 20) pub.sendNext(i)
      eventually { buffer.buffer should have size 10 }

      sub.request(10)
      sub.expectNextN(0 until 10)

      eventually { buffer.buffer should have size 10 }
      sub.request(10)
      sub.expectNextN(10 until 20)
    }
    "pass all elements if requested before pushed" in {
      val (pub, sub, _) = createBuffer(maxMemorySize = 5, maxSize = 10)

      sub.request(50)
      for (i <- 0 until 50) pub.sendNext(i)

      sub.expectNextN(0 until 50)
    }
    "construct with valid input buffer size" in {
      val buffer = FileBuffer(tempDir.newFolder(), IntSerializer, 50, 100)
      val (pub, sub) = TestSource.probe[Int]
        .via(buffer)
        .toMat(TestSink.probe[Int])(Keep.both)
        .run()

      pub.sendNext(123)
      pub.pending shouldBe 31
      pub.sendComplete()

      sub.request(1)
      sub.expectNext(123)
      sub.expectComplete()
    }
  }
}
