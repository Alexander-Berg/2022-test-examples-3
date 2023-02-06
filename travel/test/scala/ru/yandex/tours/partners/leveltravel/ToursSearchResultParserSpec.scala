package ru.yandex.tours.partners.leveltravel

import org.junit.runner.RunWith
import org.scalatest.WordSpecLike
import org.scalatest.junit.JUnitRunner

/* @author berkut@yandex-team.ru */

@RunWith(classOf[JUnitRunner])
//TODO ADD TESTS!!!
class ToursSearchResultParserSpec extends WordSpecLike {
//  "Tours Search Result Parser" must {
//    "parse valid partner response" in {
//      val hotelsIndex = new HotelsIndex(List(Hotel(256, 0, 0, 0, List((Partner.partners(5), "40429")), LocalizedString(), LocalizedString(), LocalizedString(), LocalizedString(), None, Star.stars(1), Iterable.empty, 0, 0, Iterable.empty)))
//      val parser = new HotelSnippetParser(hotelsIndex)
//      val json = scala.io.Source.fromInputStream(getClass.getResource("/partner_response.json").openStream())(Codec.UTF8).getLines().mkString("\n")
//      parser.parse(json, "6") match {
//        case Success(parsed) =>
//          assertEquals(1, parsed.tours.size)
//          val tour = parsed.tours(0)
//          assertEquals(256, tour.hotel.id)
//          assertEquals(32988, tour.priceFrom)
//          assertEquals(32990, tour.priceTo)
//          val pansions = tour.pansions
//          assertEquals(3, pansions.size)
//          assertEquals(Set(Pansions.HB, Pansions.BB, Pansions.AI), pansions)
//          assertEquals(7, tour.nightsFrom)
//          assertEquals(7, tour.nightsTo)
//          assertEquals("14.11.2014", tour.dateFrom.toString("dd.MM.yyyy"))
//          assertEquals("14.11.2014", tour.dateTo.toString("dd.MM.yyyy"))
//          assertEquals(1, tour.offersCount)
//        case Failure(e) =>
//          fail("Partner response expected to be parsed. Exception: " + e.getMessage)
//      }
//    }
//  }
}
