package ru.yandex.tours.backend.search

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import ru.yandex.tours.model.TourOperator
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.model.search.SearchResults.Context
import ru.yandex.tours.operators.SearchSourceAvailability
import ru.yandex.tours.partners.PartnerProtocol
import ru.yandex.tours.partners.PartnerProtocol._
import ru.yandex.tours.storage.MockToursDao
import ru.yandex.tours.testkit.{BaseSpec, TestConfig, TestData}
import ru.yandex.tours.util.collections.SimpleBitSet

import scala.collection.JavaConversions._
import scala.concurrent.duration._

class HotelRequestHolderSpec extends TestKit(ActorSystem("HotelRequestHolderSpec", TestConfig.config))
  with BaseSpec with BeforeAndAfterAll with TestData {

  import system.dispatcher

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true
  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  val searchSourceAvailability = mock[SearchSourceAvailability[TourOperator]]
  val toursDao = new MockToursDao
  val operators = data.tourOperators

  "Hotel request holder" should {

    "update context on UpdateContext received" in {
      val request = data.randomRequest
      val partnerActor = TestProbe()
      val hotelRequestHolder = TestActorRef(
        HotelRequestHolder.props[TourOperator](toursDao, request,
          data.hotelsIndex, data.regionTree,
          Map(Partners.lt -> partnerActor.ref),
          operators, searchSourceAvailability)
      )

      val context = Context.newBuilder().setLtRequestId("some_request_id").build
      hotelRequestHolder ! UpdateContext(context)
      toursDao.getContext(request).onSuccess { case ctx => ctx shouldBe context }
    }

    "gather results from searchers" in {
      pending
      val request = data.randomRequest
      val partnerActor = TestProbe()
      val startContext = Context.newBuilder().setLtRequestId("start_request").build
      toursDao.saveContext(request, startContext)
      val hotelRequestHolder = TestActorRef(
        HotelRequestHolder.props[TourOperator](toursDao, request,
          data.hotelsIndex, data.regionTree,
          Map(Partners.lt -> partnerActor.ref),
          operators, searchSourceAvailability))

      val watcher = TestProbe()
      watcher watch hotelRequestHolder

      val waitSources = operators.getForPartners(Set(Partners.lt)).toSet
      partnerActor.expectMsg(100.millis, PartnerProtocol.SearchHotels(request, waitSources, startContext))

      hotelRequestHolder ! SourcesToWait(operators.getAll.map(_ -> 1).toMap)
      var failed = Set.empty[TourOperator]
      var skipped = Set.empty[TourOperator]
      var success = Set.empty[TourOperator]

      waitSources.foreach { operator =>
        var partial = false
        if (data.random.nextDouble() < 0.2) {
          val snippets = data.randomSnippets(request, data.random.nextInt(10))
          partial = true
          hotelRequestHolder ! SnippetsResult(operator, Partial(snippets), 0)
        }
        val previousResult = toursDao.getHotelSearchResult(request).futureValue
        var snippetCount = 0
        data.random.nextDouble() match {
          case x if x < 0.1 =>
            if (partial) {
              success += operator
            } else {
              skipped += operator
            }
            hotelRequestHolder ! SnippetsResult(operator, Skipped, 0)
          case x if x < 0.4 =>
            if (partial) {
              success += operator
            } else {
              failed += operator
            }
            hotelRequestHolder ! SnippetsResult(operator, Failed(new Exception("Some fail")), 0)
          case x =>
            success += operator
            val snippets = data.randomSnippets(request, data.random.nextInt(10))
            snippetCount = snippets.size
            hotelRequestHolder ! SnippetsResult(operator, Successful(snippets), 0)
        }

        val expectedSnippetsCount = previousResult.map(_.getHotelSnippetCount).getOrElse(0) + snippetCount

        toursDao.getHotelSearchResult(request).futureValue match {
          case None => fail("Some result expected")
          case Some(result) =>
            val progress = result.getProgress
            progress.getOperatorCompleteCount shouldBe (skipped.size + success.size + failed.size)
            progress.getOperatorFailedCount shouldBe failed.size
            progress.getOperatorSkippedCount shouldBe skipped.size
            progress.getOperatorTotalCount shouldBe waitSources.size
            progress.getIsFinished shouldBe (waitSources.size == skipped.size + success.size + failed.size)
            progress.getOperatorCompleteSet shouldBe SimpleBitSet((skipped ++ failed ++ success).map(_.id)).packed
            progress.getOperatorSkippedSet shouldBe SimpleBitSet(skipped.map(_.id)).packed
            progress.getOperatorFailedSet shouldBe SimpleBitSet(failed.map(_.id)).packed
            progress.getOBSOLETEFailedOperatorsList.toSet shouldBe failed.map(_.id)
            result.getHotelSnippetCount shouldBe expectedSnippetsCount
        }
      }
      watcher.expectTerminated(hotelRequestHolder)
    }
  }
}
