package ru.yandex.tours.wizard.microoffer

import org.joda.time.LocalDate
import ru.yandex.tours.model.BaseModel.Pansion
import ru.yandex.tours.model.wizard.MicroOffer.{HotelMicroOffersProto, MicroOfferProto}
import ru.yandex.tours.util.lang.Dates.RichLocalDate

import scala.collection.JavaConverters._

/**
  * Created by asoboll on 17.02.17.
  */
trait MicroOfferTestData {
  def offer(price: Int, prov: Int = 13, room: String = "double",
            pansion: Pansion = Pansion.AI, url: String = "yandex.ru"): MicroOfferProto = {
    MicroOfferProto.newBuilder()
      .setOperatorId(prov).setRoomName(room).setPansion(pansion).setPrice(price).setUrl(url).build
  }

  def hotelOffers(hotelId: Int, prices: Int*): HotelMicroOffersProto = {
    val offers = prices.map(offer(_)).sorted(MicroOfferIndex.offerPriceOrdering)
    HotelMicroOffersProto.newBuilder()
      .setHotelId(hotelId)
      .addAllOffers(offers.asJava)
      .setCompactWhen(LocalDate.now.plusDays(7).toCompactInt)
      .setNights(7)
      .build
  }
}
