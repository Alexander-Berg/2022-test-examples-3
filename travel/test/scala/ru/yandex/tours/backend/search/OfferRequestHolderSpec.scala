package ru.yandex.tours.backend.search

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.model.search.OfferSearchRequest
import ru.yandex.tours.model.search.SearchProducts.Offer
import ru.yandex.tours.model.search.SearchResults.Context
import ru.yandex.tours.model.{Source, TourOperator}
import ru.yandex.tours.operators.SearchSourceAvailability
import ru.yandex.tours.partners.PartnerProtocol
import ru.yandex.tours.partners.PartnerProtocol._
import ru.yandex.tours.storage.MockToursDao
import ru.yandex.tours.testkit.{BaseSpec, TestConfig, TestData}

import scala.concurrent.duration._

class OfferRequestHolderSpec extends TestKit(ActorSystem("hotel-req-holder-spec", TestConfig.config)) with BaseSpec with BeforeAndAfterAll with ScalaFutures with TestData {

  override val invokeBeforeAllAndAfterAllEvenIfNoTestsAreExpected: Boolean = true
  override protected def afterAll() = {
    TestKit.shutdownActorSystem(system)
  }

  val searchSourceAvailability = mock[SearchSourceAvailability[TourOperator]]
  val toursDao = new MockToursDao
  val operators = data.tourOperators

  "Offer request holder" should {
    "merge tours" in {
      val request = data.randomTourRequest
      val partnerActor = TestProbe()
      val requestHolder = TestActorRef(OfferRequestHolder.props[TourOperator](toursDao, request,
        Map(Partners.lt -> partnerActor.ref), operators, searchSourceAvailability))

      val watcher = TestProbe()
      watcher watch requestHolder
      val waitSources = operators.getForPartners(Set(Partners.lt)).toSet
      partnerActor.expectMsg(100.millis, PartnerProtocol.SearchOffers(request, waitSources, Context.getDefaultInstance))

      assume(waitSources.size >= 3)
      val waitSourceSeq = waitSources.toSeq
      val failedOperator = waitSourceSeq.head
      val successOperator = waitSourceSeq.tail.head
      val skipped = waitSourceSeq.tail.tail
      requestHolder ! SourcesToWait(Map(failedOperator -> 1, successOperator -> 3) ++ skipped.map(_.asInstanceOf[Source] -> 1).toMap)
      requestHolder ! OffersResult(failedOperator, Failed(new Exception("Some fail")))
      skipped.foreach(source => requestHolder ! OffersResult(source, Skipped))

      val baseTour = data.randomTour
      requestHolder ! OffersResult(successOperator, Successful(Iterable(baseTour)), 1)
      checkResult(request, _ shouldBe baseTour)

      val otherTour = baseTour.toBuilder.setPrice(baseTour.getPrice - 1).build
      requestHolder ! OffersResult(successOperator, Successful(Iterable(otherTour)), 1)
      checkResult(request, _ shouldBe otherTour)

      val mostPriorityTour = baseTour.toBuilder.setPrice(baseTour.getPrice + 1).build
      requestHolder ! OffersResult(successOperator, Successful(Iterable(mostPriorityTour)))
      checkResult(request, _ shouldBe mostPriorityTour)

      val result = toursDao.getOffersSearchResult(request).futureValue
      result shouldBe 'defined
      val progress = result.get.getProgress
      progress.getOperatorSkippedCount shouldBe skipped.size
      progress.getOperatorFailedCount shouldBe 1
      progress.getOperatorCompleteCount shouldBe waitSourceSeq.size
      progress.getIsFinished shouldBe true

      watcher.expectTerminated(requestHolder)
    }
  }

  private def checkResult(request: OfferSearchRequest, check: Offer => Unit): Unit = {
    val result = toursDao.getOffersSearchResult(request).futureValue
    result shouldBe 'defined
    val tours = result.get.getOfferList
    tours should have size 1
    check(tours.get(0))
  }

}
