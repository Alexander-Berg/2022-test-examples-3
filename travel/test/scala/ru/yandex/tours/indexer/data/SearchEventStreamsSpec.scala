package ru.yandex.tours.indexer.data

import akka.actor.ActorSystem
import akka.stream.testkit.scaladsl.TestSink
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.events.SearchEvent
import ru.yandex.tours.events.SearchEvent.FoundSnippets
import ru.yandex.tours.testkit.{BaseSpec, TemporaryDirectory, TestData}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.01.16
 */
class SearchEventStreamsSpec extends BaseSpec with TestData with TemporaryDirectory with ScalaFutures {

  implicit val actorSystem = ActorSystem("search-event-spec", ConfigFactory.empty())

  lazy val streams = new SearchEventStreams(actorSystem, tempDir.getRoot)

  private def newSink(name: String) = streams.register(name, TestSink.probe[Seq[SearchEvent]])

  "SearchEventStreams" should {
    "register" in {
      val sub = newSink("test1")

      sub.request(1)
      sub.expectNoMsg()
    }
    "push and receive" in {
      val name = "push_and_pull"
      val sub = newSink(name)

      val req = data.randomRequest
      val event = FoundSnippets(DateTime.now, req, data.randomHotelResult(req))

      streams.push(name, Seq(event))

      sub.request(10)
      sub.expectNext(Seq(event))
    }
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }
}
