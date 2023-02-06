package ru.yandex.tours.storage

import org.mockito.Mockito._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import ru.yandex.tours.model.search.SearchResults.Context
import ru.yandex.tours.model.search.OfferSearchRequest
import ru.yandex.tours.testkit.{TestData, BaseSpec}

import scala.concurrent.Future

trait CommonProxyStorageSpec extends BaseSpec with ScalaFutures with TestData with IntegrationPatience {
  def commonTests(createStorage: ToursDao => ToursDao) = {
    val toursStorage = mock[ToursDao]
    val storage = createStorage(toursStorage)
    "proxy getContext call" in {
      val request = data.randomRequest
      val context = Context.getDefaultInstance
      when(toursStorage.getContext(request)).thenReturn(Future.successful(context))

      storage.getContext(request).futureValue shouldBe context

      verify(toursStorage).getContext(request)
    }
    "proxy saveContext call" in {
      val request = data.randomRequest
      val context = Context.getDefaultInstance
      when(toursStorage.saveContext(request, context)).thenReturn(Future.successful(()))

      storage.saveContext(request, context).futureValue

      verify(toursStorage).saveContext(request, context)
    }
    "proxy getHotelSearchResult call" in {
      val request = data.randomRequest
      when(toursStorage.getHotelSearchResult(request)).thenReturn(Future.successful(None))

      storage.getHotelSearchResult(request).futureValue shouldBe None

      verify(toursStorage).getHotelSearchResult(request)
    }
    "proxy getOffersSearchResult call" in {
      val request = OfferSearchRequest(data.randomRequest, 123)
      when(toursStorage.getOffersSearchResult(request)).thenReturn(Future.successful(None))

      storage.getOffersSearchResult(request).futureValue shouldBe None

      verify(toursStorage).getOffersSearchResult(request)
    }
  }
}
