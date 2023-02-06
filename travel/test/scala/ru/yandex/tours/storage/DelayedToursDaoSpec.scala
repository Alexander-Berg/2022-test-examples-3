package ru.yandex.tours.storage

import akka.actor.ActorSystem
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import ru.yandex.tours.model.search.{OfferSearchRequest, HotelSearchRequest}
import ru.yandex.tours.model.search.SearchResults.{OfferSearchResult, HotelSearchResult}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

class DelayedToursDaoSpec extends CommonProxyStorageSpec with Eventually {
  implicit val as = ActorSystem()
  implicit val ec = as.dispatcher
  private val delay = 500
  private val times = 5

  "Delayed tours dao" should {
    def delaySaveTest[Req, Res](name: String, request: Req, generateResponse: => Res, generateDao: mutable.Buffer[Res] => ToursDao, save: (ToursDao, Req, Res) => Future[Unit], get: (ToursDao, Req) => Future[Option[Res]]) = {
      s"delay save $name" in {
        val savedResults = mutable.Buffer.empty[Res]
        val dao = generateDao(savedResults)

        val delayedDao = new DelayedToursDao(dao, delay.millis)
        val start = System.currentTimeMillis()
        var lastResult = generateResponse
        var lastSave = Future.successful(())
        while (System.currentTimeMillis() < start + delay * times) {
          val currentResult = generateResponse
          lastResult = currentResult
          Thread.sleep(delay / 5)
          lastSave = save(delayedDao, request, currentResult)
        }
        lastSave.futureValue(Timeout(delay.millis * 30))
        delayedDao shouldBe empty
        get(delayedDao, request).futureValue shouldBe Some(lastResult)
        savedResults.size shouldBe <=(times)
      }
    }

    val hotelRequest = data.randomRequest

    delaySaveTest[HotelSearchRequest, HotelSearchResult]("hotels", hotelRequest, data.randomHotelResult(hotelRequest), savedResults => new MockToursDao() {
      override def saveHotelSearchResult(request: HotelSearchRequest, response: HotelSearchResult): Future[Unit] = {
        savedResults += response
        super.saveHotelSearchResult(request, response)
      }
    }, (dao, req, res) => {
      dao.saveHotelSearchResult(req, res)
    }, (dao, req) => {
      dao.getHotelSearchResult(req)
    })

    val tourRequest = OfferSearchRequest(hotelRequest, data.randomHotel(hotelRequest.to).id)
    delaySaveTest[OfferSearchRequest, OfferSearchResult]("offers", tourRequest, data.randomTourResult(tourRequest), savedResults => new MockToursDao() {
      override def saveOffersSearchResult(request: OfferSearchRequest, response: OfferSearchResult): Future[Unit] = {
        savedResults += response
        super.saveOffersSearchResult(request, response)
      }
    }, (dao, req, res) => {
      dao.saveOffersSearchResult(req, res)
    }, (dao, req) => {
      dao.getOffersSearchResult(req)
    })



    commonTests(mock => new DelayedToursDao(mock, delay.millis))
  }
}
