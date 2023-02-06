package ru.yandex.tours.filter

import ru.yandex.tours.filter.State.{IntValues, StringValues}
import ru.yandex.tours.model.filter.StringValue
import ru.yandex.tours.model.filter.hotel.StarFilter
import ru.yandex.tours.model.filter.snippet.PriceFilter
import ru.yandex.tours.model.search.SearchType
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.Collections._
import ru.yandex.tours.util.hotels.HotelSnippetUtil

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 07.08.15
 */
class SnippetFiltratorSpec extends BaseSpec with TestData {
  val filtrator = new SnippetFiltrator(data.hotelsIndex, new HiddenFilters(Map.empty))
  val request = data.randomRequest

  "SnippetFiltrator" should {
    "collect allowed state" in {
      pending
      val request = data.randomRequest
      val snippets = data.randomSnippets(request, 30).toSeq
      val rich = HotelSnippetUtil.enrich(snippets, data.hotelsIndex)
      val stars = rich.map(_.hotel.star.id).toSet
      val (minPrice, maxPrice) = rich.map(_.snippet.getPriceMin).minMax

      val states = filtrator.getAllowedState(request, snippets, Seq(
        new PriceFilter(None, None),
        new StarFilter(Seq.empty)
      ), SearchType.TOURS)

      states should contain theSameElementsAs Seq(
        IntValues("price", Seq(minPrice, maxPrice)),
        StringValues("stars", stars.map(_.toString))
      )
    }
    "collect allowed state with filtered out snippets #1" in {
      pending
      val request = data.randomRequest
      val snippets = data.randomSnippets(request, 30).toSeq
      val rich = HotelSnippetUtil.enrich(snippets, data.hotelsIndex)
      val (minPrice, maxPrice) = rich.map(_.snippet.getPriceMin).minMax

      val states = filtrator.getAllowedState(request, snippets, Seq(
        new PriceFilter(Some(1000000), None),
        new StarFilter(Seq.empty)
      ), SearchType.TOURS)

      states should contain theSameElementsAs Seq(
        IntValues("price", Seq(minPrice, maxPrice)),
        StringValues("stars", Set.empty)
      )
    }
    "collect allowed state with filtered out snippets #2" in {
      pending
      val request = data.randomRequest
      val snippets = data.randomSnippets(request, 30).toSeq
      val rich = HotelSnippetUtil.enrich(snippets, data.hotelsIndex)

      val states = filtrator.getAllowedState(request, snippets, Seq(
        new PriceFilter(None, None),
        new StarFilter(Seq(StringValue("not_existed_value")))
      ), SearchType.TOURS)

      val stars = rich.map(_.hotel.star.id.toString).toSet

      states should contain theSameElementsAs Seq(
        IntValues("price", Seq.empty),
        StringValues("stars", stars)
      )
    }
  }
}
