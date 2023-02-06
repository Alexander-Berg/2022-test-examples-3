package ru.yandex.tours.utils

import ru.yandex.tours.geocoder.{GeocoderResponse, Point}

import scala.io.Source
import scala.util.Try

/* @author berkut@yandex-team.ru */

object CachedGeocoderClient {

  val cache = Source.fromFile("/Users/berkut/tmp/geocoder_test.tsv").getLines().map(line => Try {
    val parts = line.split("\t")
    val query = parts(0).substring(4, parts(0).size - 1)
    val country = parts(1)
    val locality = parts(2)
    val address = parts(3)
    val geoId = parts(4).toInt
    val accuracy = parts(5).toDouble
    val precision = parts(6)
    val point = Point(parts(7).toDouble, parts(8).toDouble)
    query -> GeocoderResponse(country, locality, address, geoId, point, precision, accuracy)
  }.toOption).flatten.toMap

  def request(query: String): Try[Seq[GeocoderResponse]] = Try {
    cache.get(query) match {
      case Some(result) => Seq(result)
      case None => throw new Exception("Unknown address")
    }
  }


//  case class GeocoderResponse(country: String,
//                              locality: String,
//                              address: String,
//                              geoId: Int,
//                              point: Point,
//                              precision: String,
//                              accuracy: Double)

}
