package ru.yandex.tours.storage

import org.mockito.ArgumentMatcher
import org.mockito.Matchers.{eq => eqObj, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import ru.yandex.tours.model.search.SearchResults._
import ru.yandex.tours.model.search.{HotelSearchRequest, OfferSearchRequest, SearchType}
import ru.yandex.tours.services.CalendarPushService
import ru.yandex.tours.storage.minprice.MinPriceStorage
import ru.yandex.tours.storage.serpcache.SerpCacheDao

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 19.06.15
 */
class ProxyToursStorageSpec extends CommonProxyStorageSpec with BeforeAndAfter {

  val toursStorage = mock[ToursDao]
  val toursCache = mock[ToursDao]
  val minPriceStorage = mock[MinPriceStorage]
  val calendarPushService = mock[CalendarPushService]
  val serpCacheDao = mock[SerpCacheDao]

  val storage = new ProxyToursStorage(toursStorage, toursCache, minPriceStorage, SearchType.TOURS, calendarPushService,
    serpCacheDao)

  after {
    reset(toursStorage, toursCache, minPriceStorage, calendarPushService)
  }

  val completed = data.progress(1, 1)
  val notCompleted = data.progress(1, 0)
  val failed = data.progress(1, 1, failed = 1)

  "ProxyToursStorage" should {
    commonTests(mock => new ProxyToursStorage(mock, toursCache, minPriceStorage, SearchType.TOURS, calendarPushService,
      serpCacheDao))

    def checkSave(request: HotelSearchRequest, result: HotelSearchResult, expectUpdate: Boolean): Unit = {
      when(toursStorage.saveHotelSearchResult(request, result)).thenReturn(Future.successful(()))
      when(minPriceStorage.update(any(), any())).thenReturn(Future.successful(()))
      when(calendarPushService.saveTourSnippets(any(), any())).thenReturn(Future.successful(()))

      storage.saveHotelSearchResult(request, result).futureValue

      if (expectUpdate) {
        verify(toursStorage).saveHotelSearchResult(request, result)
      }
      verifyNoMoreInteractions(toursStorage)
    }
    "always proxy saveHotelSearchResult call" in {
      val request = data.randomRequest
      def result(count: Int, progress: SearchProgress) = data.hotelResult(request, count, progress)
      checkSave(request, result(count = 3, completed), expectUpdate = true)
      checkSave(request, result(count = 3, notCompleted), expectUpdate = true)
      checkSave(request, result(count = 0, completed), expectUpdate = true)
      checkSave(request, result(count = 0, failed), expectUpdate = true)
    }

    def checkCache(request: HotelSearchRequest, result: HotelSearchResult, expectUpdate: Boolean): Unit = {
      when(toursStorage.saveHotelSearchResult(request, result)).thenReturn(Future.successful(()))
      when(minPriceStorage.update(any(), any())).thenReturn(Future.successful(()))
      when(calendarPushService.saveTourSnippets(any(), any())).thenReturn(Future.successful(()))

      storage.saveHotelSearchResult(request, result).futureValue

      def sameResultWithFlag() = argThat(new ArgumentMatcher[HotelSearchResult] {
        override def matches(argument: scala.Any): Boolean = argument match {
          case hsr: HotelSearchResult =>
            hsr.getCreated == result.getCreated &&
              hsr.getUpdated == result.getUpdated &&
              hsr.getHotelSnippetList == result.getHotelSnippetList &&
              hsr.getProgress == result.getProgress &&
              hsr.getResultInfo.getIsFromLongCache
          case _ => false
        }
      })

      if (expectUpdate) {
        verify(toursCache).saveHotelSearchResult(eqObj(request), sameResultWithFlag())
      }
      verifyNoMoreInteractions(toursCache)
    }

    "save hotels to cache with `fromCache` flag" in {
      val request = data.randomRequest
      val result = data.hotelResult(request, 3, completed)
      checkCache(request, result, expectUpdate = true)
    }
    "save hotels to cache if result is empty" in {
      val request = data.randomRequest
      val result = data.hotelResult(request, 0, completed)
      checkCache(request, result, expectUpdate = true)
    }
    "not save hotels to cache if result is not complete" in {
      val request = data.randomRequest
      val result = data.hotelResult(request, 3, notCompleted)
      checkCache(request, result, expectUpdate = false)
    }
    "not save hotels to cache if result is failed and empty" in {
      val request = data.randomRequest
      checkCache(request, data.hotelResult(request, 0, failed), expectUpdate = false)
      checkCache(request, data.hotelResult(request, 3, failed), expectUpdate = true)
    }

    def checkSaveTours(request: OfferSearchRequest, result: OfferSearchResult, expectUpdate: Boolean): Unit = {
      when(toursStorage.saveOffersSearchResult(request, result)).thenReturn(Future.successful(()))
      when(calendarPushService.saveTourOffers(any(), any())).thenReturn(Future.successful(()))

      storage.saveOffersSearchResult(request, result).futureValue

      if (expectUpdate) {
        verify(toursStorage).saveOffersSearchResult(request, result)
      }
      verifyNoMoreInteractions(toursStorage)
    }
    "always proxy saveToursSearchResult call" in {
      val request = OfferSearchRequest(data.randomRequest, data.randomHotel().id)
      def result(count: Int, progress: SearchProgress) = data.tourResult(request, count, progress)
      checkSaveTours(request, result(count = 3, completed), expectUpdate = true)
      checkSaveTours(request, result(count = 3, notCompleted), expectUpdate = true)
      checkSaveTours(request, result(count = 0, completed), expectUpdate = true)
      checkSaveTours(request, result(count = 0, failed), expectUpdate = true)
    }

    def checkToursCache(request: OfferSearchRequest, result: OfferSearchResult, expectUpdate: Boolean): Unit = {
      when(toursStorage.saveOffersSearchResult(request, result)).thenReturn(Future.successful(()))
      when(calendarPushService.saveTourOffers(any(), any())).thenReturn(Future.successful(()))

      storage.saveOffersSearchResult(request, result).futureValue

      def sameResultWithFlag() = argThat(new ArgumentMatcher[OfferSearchResult] {
        override def matches(argument: scala.Any): Boolean = argument match {
          case tsr: OfferSearchResult =>
            tsr.getCreated == result.getCreated &&
              tsr.getUpdated == result.getUpdated &&
              tsr.getOfferList == result.getOfferList &&
              tsr.getProgress == result.getProgress &&
              tsr.getResultInfo.getIsFromLongCache
          case _ => false
        }
      })

      if (expectUpdate) {
        verify(toursCache).saveOffersSearchResult(eqObj(request), sameResultWithFlag())
      }
      verifyNoMoreInteractions(toursCache)
    }
    "save tours in cache" in {
      val request = OfferSearchRequest(data.randomRequest, data.randomHotel().id)
      checkToursCache(request, data.tourResult(request, count = 3, progress = completed), expectUpdate = true)
      checkToursCache(request, data.tourResult(request, count = 0, progress = completed), expectUpdate = true)
    }
    "not save tours in cache if result is not complete" in {
      val request = OfferSearchRequest(data.randomRequest, data.randomHotel().id)
      checkToursCache(request, data.tourResult(request, 3, notCompleted), expectUpdate = false)
      checkToursCache(request, data.tourResult(request, 0, notCompleted), expectUpdate = false)
    }
    "not save tours in cache if result is failed and empty" in {
      val request = OfferSearchRequest(data.randomRequest, data.randomHotel().id)
      checkToursCache(request, data.tourResult(request, 3, failed), expectUpdate = true)
      checkToursCache(request, data.tourResult(request, 0, failed), expectUpdate = false)
    }

    def checkMinPrices(request: HotelSearchRequest, result: HotelSearchResult, expectUpdate: Boolean): Unit = {
      when(toursStorage.saveHotelSearchResult(request, result)).thenReturn(Future.successful(()))
      when(minPriceStorage.update(any(), any())).thenReturn(Future.successful(()))
      when(calendarPushService.saveTourSnippets(any(), any())).thenReturn(Future.successful(()))

      storage.saveHotelSearchResult(request, result).futureValue

      if (expectUpdate) {
        verify(minPriceStorage).update(request, result)
      }
      verifyNoMoreInteractions(minPriceStorage)
    }
    "save min prices only if result is complete" in {
      val request = data.randomRequest
      def result(count: Int, progress: SearchProgress) = data.hotelResult(request, count, progress)
      checkMinPrices(request, result(count = 3, progress = completed), expectUpdate = true)
      checkMinPrices(request, result(count = 0, progress = completed), expectUpdate = true)
      checkMinPrices(request, result(count = 3, progress = failed), expectUpdate = true)
      checkMinPrices(request, result(count = 0, progress = failed), expectUpdate = true)
      checkMinPrices(request, result(count = 3, progress = notCompleted), expectUpdate = false)
      checkMinPrices(request, result(count = 0, progress = notCompleted), expectUpdate = false)
    }

    def checkPushSnippets(request: HotelSearchRequest, result: HotelSearchResult, expectUpdate: Boolean): Unit = {
      when(toursStorage.saveHotelSearchResult(request, result)).thenReturn(Future.successful(()))
      when(minPriceStorage.update(any(), any())).thenReturn(Future.successful(()))
      when(calendarPushService.saveTourSnippets(any(), any())).thenReturn(Future.successful(()))

      storage.saveHotelSearchResult(request, result).futureValue

      if (expectUpdate) {
        verify(calendarPushService).saveTourSnippets(request, result)
      }
      verifyNoMoreInteractions(calendarPushService)
    }
    "push snippets to calendar only if result is complete" in {
      val request = data.randomRequest
      def result(count: Int, progress: SearchProgress) = data.hotelResult(request, count, progress)
      checkPushSnippets(request, result(count = 3, progress = completed), expectUpdate = true)
      checkPushSnippets(request, result(count = 0, progress = completed), expectUpdate = true)
      checkPushSnippets(request, result(count = 3, progress = failed), expectUpdate = true)
      checkPushSnippets(request, result(count = 0, progress = failed), expectUpdate = true)
      checkPushSnippets(request, result(count = 3, progress = notCompleted), expectUpdate = false)
      checkPushSnippets(request, result(count = 0, progress = notCompleted), expectUpdate = false)
    }
    def checkPushOffers(request: OfferSearchRequest, result: OfferSearchResult, expectUpdate: Boolean): Unit = {
      when(toursStorage.saveOffersSearchResult(request, result)).thenReturn(Future.successful(()))
      when(calendarPushService.saveTourOffers(any(), any())).thenReturn(Future.successful(()))

      storage.saveOffersSearchResult(request, result).futureValue

      if (expectUpdate) {
        verify(calendarPushService).saveTourOffers(request, result)
      }
      verifyNoMoreInteractions(calendarPushService)
    }
    "push offers to calendar only if result is complete" in {
      val request = data.randomTourRequest
      def result(count: Int, progress: SearchProgress) = data.tourResult(request, count, progress)
      checkPushOffers(request, result(count = 3, progress = completed), expectUpdate = true)
      checkPushOffers(request, result(count = 0, progress = completed), expectUpdate = true)
      checkPushOffers(request, result(count = 3, progress = failed), expectUpdate = true)
      checkPushOffers(request, result(count = 0, progress = failed), expectUpdate = true)
      checkPushOffers(request, result(count = 3, progress = notCompleted), expectUpdate = false)
      checkPushOffers(request, result(count = 0, progress = notCompleted), expectUpdate = false)
    }
  }
}
