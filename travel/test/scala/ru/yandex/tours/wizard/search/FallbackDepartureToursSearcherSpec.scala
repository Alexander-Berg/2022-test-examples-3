package ru.yandex.tours.wizard.search

import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.wizard.domain.{ToursResponse, TourStartInterval, ToursWizardRequest}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 17.07.15
 */
class FallbackDepartureToursSearcherSpec extends BaseSpec with BeforeAndAfter {

  val searcher = mock[WizardToursSearcher]
  val fallbackSearcher = new FallbackDepartureToursSearcher(searcher)

  val originalRequest = ToursWizardRequest("бали", 39, None, Set(), TourStartInterval.default(false), 39, List(2, 213), Some(10572), None, None)
  val fallbackRequest1 = ToursWizardRequest("бали", 39, None, Set(), TourStartInterval.default(false), 2, List(2, 213), Some(10572), None, None)
  val fallbackRequest2 = ToursWizardRequest("бали", 39, None, Set(), TourStartInterval.default(false), 213, List(2, 213), Some(10572), None, None)

  def response(from: Int) = ToursResponse(from, 10572, Seq.empty)
  val originalResponse = response(39)
  val fallbackResponse1 = response(2)
  val fallbackResponse2 = response(213)

  after {
    Mockito.reset(searcher)
  }

  "FallbackDepartureToursSearcher" should {
    "return response if searcher found original request" in {
      when(searcher.search(originalRequest)).thenReturn(Some(originalResponse))

      fallbackSearcher.search(originalRequest) shouldBe Some(originalResponse)

      verify(searcher).search(originalRequest)
      verifyNoMoreInteractions(searcher)
    }
    "fallback to another departures sequentially" in {
      when(searcher.search(originalRequest)).thenReturn(None)
      when(searcher.search(fallbackRequest1)).thenReturn(Some(fallbackResponse1))

      fallbackSearcher.search(originalRequest) shouldBe Some(fallbackResponse1)

      val order = Mockito.inOrder(searcher)
      order.verify(searcher).search(originalRequest)
      order.verify(searcher).search(fallbackRequest1)
      verifyNoMoreInteractions(searcher)
    }
    "fallback to another departures sequentially #2" in {
      when(searcher.search(originalRequest)).thenReturn(None)
      when(searcher.search(fallbackRequest1)).thenReturn(None)
      when(searcher.search(fallbackRequest2)).thenReturn(Some(fallbackResponse2))

      fallbackSearcher.search(originalRequest) shouldBe Some(fallbackResponse2)

      val order = Mockito.inOrder(searcher)
      order.verify(searcher).search(originalRequest)
      order.verify(searcher).search(fallbackRequest1)
      order.verify(searcher).search(fallbackRequest2)
      verifyNoMoreInteractions(searcher)
    }
    "return None if all searches failed" in {
      when(searcher.search(originalRequest)).thenReturn(None)
      when(searcher.search(fallbackRequest1)).thenReturn(None)
      when(searcher.search(fallbackRequest2)).thenReturn(None)

      fallbackSearcher.search(originalRequest) shouldBe None

      val order = Mockito.inOrder(searcher)
      order.verify(searcher).search(originalRequest)
      order.verify(searcher).search(fallbackRequest1)
      order.verify(searcher).search(fallbackRequest2)
      verifyNoMoreInteractions(searcher)
    }
  }
}
