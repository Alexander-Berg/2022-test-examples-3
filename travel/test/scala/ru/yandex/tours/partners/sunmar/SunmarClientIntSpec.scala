package ru.yandex.tours.partners.sunmar

import org.joda.time.LocalDate
import org.scalatest.Ignore
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.lang.Dates._
import ru.yandex.tours.util.parsing.Tabbed

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 22.05.15
 */
@Ignore
class SunmarClientIntSpec extends BaseSpec with ScalaFutures with TestData {
  val searchService = new SearchService().getBasicHttpEndpoint(
//    new LoggingFeature("<stdout>", "<stdout>", 65535, true)
  )
  val cred = new OTICredential

  cred.setUsername("Yandex")
  cred.setPassword("65536")

  val client = new DefaultSunmarClient(searchService, cred)

  val request = {
    val from = LocalDate.now.plusDays(20).nextSaturday.toXMLGregorianCalendar
    val until = LocalDate.now.plusDays(20).nextSaturday.toXMLGregorianCalendar

    val request = new PackageListRequest
    request.setAdult(2)
    request.setChild(0)
    request.setBeginNight(7)
    request.setEndNight(7)

    request.setFromArea(2671)
    request.setToCountry(12)

    request.setHotelCategoryGroup(-1)
    request.setRoomCategoryGroup(-1)
    request.setMealCategory(-1)

    request.setBeginDate(from)
    request.setEndDate(until)

    request.setPageSize(100)
    request.setStartIndex(0)

    request.setNotShowSTOPSale(true)
    request.setOnlyAvailableFlight(true)

    request.setCurrency(4)
    request
  }

  "SunmarClient" should {
    "filter places" in {
      client.toPlaces(2671, 12).futureValue(Timeout(30.second)).foreach { toPlace =>
        println(Tabbed(
          toPlace.id,
          toPlace.country,
          toPlace.fromArea,
          toPlace.name,
          toPlace.parentID
        ))
      }
    }
    "search packages" in {
      val result = client.packages(request).futureValue(Timeout(30.second))
      println(result.size + " results")
      result.foreach { p =>
        println(Tabbed(
          p.getBeginDate
          , p.getNight
          , p.getArea.getID, p.getArea.getName
          , p.getArea.getRegion.getID, p.getArea.getRegion.getName
          , p.getHotel.getID
          , p.getHotel.getName
          , p.getOrginalPrice
          , p.getTotalPrice
          , p.getRoom.getLname
          , p.getMeal.getName
          , p.getShopLink
        ))
      }
//      result.headOption.foreach(println)
    }

    "filter hotels" in {
      val hotels = client.hotels(12).futureValue(Timeout(30.second))
      println(hotels.size + " hotels")
    }

    "actualize tour" in {
      val response = client.packages(request).futureValue(Timeout(30.second))
      assume(response.nonEmpty)

      val actualized = client.actualize(response.head).futureValue(Timeout(30.second))
      println(actualized.size + " ProductListDayDetail")
      actualized.foreach { a =>

        response.foreach { p =>
          println(Tabbed(
            p.getBeginDate
            , p.getNight
            //          , p.getArea.getID, p.getArea.getName
            //          , p.getArea.getRegion.getID, p.getArea.getRegion.getName
            , p.getHotel.getID
            , p.getHotel.getName
            , p.getOrginalPrice
            , p.getTotalPrice
            , p.getRoom.getLname
            , p.getMeal.getName
            , p.getTransfer
            , p.getInsurance
          ))
          println(p.getShopLink)
        }

        val p = a.getProductList

        println()

        println(p.getShopLink)
        println(p.getAgencyLink)

        val seatClasses = a.getSeatClassDetail.getDayDetailSeatClass.asScala
        println(seatClasses.size + " DayDetailSeatClass")
        seatClasses.foreach { s =>
          println(Tabbed(
            s.getExtraName,
            s.getExtraPrice,
            s.getExtraCurrency,
            s.getTotalPrice,
            s.isSaleStatus,
            s.getSaleID
          ))
        }
      }
    }
  }
}
