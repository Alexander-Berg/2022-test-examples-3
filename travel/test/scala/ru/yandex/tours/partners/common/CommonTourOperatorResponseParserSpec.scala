package ru.yandex.tours.partners.common

import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.{TestData, BaseSpec}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 15.03.16
 */
class CommonTourOperatorResponseParserSpec extends BaseSpec with TestData {

  val parser = new CommonTourOperatorResponseParser(data.hotelsIndex, data.pansions,
    data.geoMapping, data.aviaAirports, Partners.sodis, data.regionTree)

  val operator = data.tourOperators.getById(36).get

  "CommonTourOperatorResponseParser" should {
    "parse actualization" in {
      val json = getTestData("/sodis_actualization.json", getClass)

      val offer = data.randomTour
      val actualizedOffer = parser.parseActualization(offer, operator)(json).get
      val actualization = actualizedOffer.getActualizationInfo

      actualization.getPrice shouldBe 194169
      actualization.getInfantPrice shouldBe 0
      actualization.getVisaPrice shouldBe 0
      actualization.getFuelCharge shouldBe 0
      // using transfer info from original offer
      actualization.getWithTransfer shouldBe offer.getWithTransfer

      actualization.getFlightsCount shouldBe 1

      val flight = actualization.getFlights(0)
      flight.getAdditionalPrice shouldBe 0
      flight.getTo.getIsDirect shouldBe true
      flight.getTo.getPointCount shouldBe 2
      flight.getTo.getPoint(0).getGeoId shouldBe 213
      flight.getTo.getPoint(0).getCompany shouldBe "Aeroflot"
      flight.getTo.getPoint(0).getAirport shouldBe "Шереметьево"
      flight.getTo.getPoint(0).getAirportCode shouldBe "SVO"
      flight.getTo.getPoint(0).getFlightNumber shouldBe "SU 270"
      flight.getTo.getPoint(0).getDeparture shouldBe "2016-05-14T19:20:00.000+03:00"
      flight.getTo.getPoint(0).hasArrival shouldBe false
      flight.getTo.getPoint(1).getGeoId shouldBe 10620
      flight.getTo.getPoint(1).getCompany shouldBe "Aeroflot"
      flight.getTo.getPoint(1).getAirport shouldBe "Суварнабхуми"
      flight.getTo.getPoint(1).getAirportCode shouldBe "BKK"
      flight.getTo.getPoint(1).getFlightNumber shouldBe "SU 270"
      flight.getTo.getPoint(1).hasDeparture shouldBe false
      flight.getTo.getPoint(1).getArrival shouldBe "2016-05-15T08:20:00.000+07:00"

      flight.getBack.getIsDirect shouldBe true
      flight.getBack.getPointCount shouldBe 2
      flight.getBack.getPoint(0).getGeoId shouldBe 10620
      flight.getBack.getPoint(0).getCompany shouldBe "Aeroflot"
      flight.getBack.getPoint(0).getAirport shouldBe "Суварнабхуми"
      flight.getBack.getPoint(0).getAirportCode shouldBe "BKK"
      flight.getBack.getPoint(0).getFlightNumber shouldBe "SU 271"
      flight.getBack.getPoint(0).getDeparture shouldBe "2016-05-28T10:40:00.000+07:00"
      flight.getBack.getPoint(0).hasArrival shouldBe false
      flight.getBack.getPoint(1).getGeoId shouldBe 213
      flight.getBack.getPoint(1).getCompany shouldBe "Aeroflot"
      flight.getBack.getPoint(1).getAirport shouldBe "Шереметьево"
      flight.getBack.getPoint(1).getAirportCode shouldBe "SVO"
      flight.getBack.getPoint(1).getFlightNumber shouldBe "SU 271"
      flight.getBack.getPoint(1).hasDeparture shouldBe false
      flight.getBack.getPoint(1).getArrival shouldBe "2016-05-28T16:35:00.000+03:00"
    }
  }
}
