package ru.yandex.tours.utils

import ru.yandex.tours.geocoder.{GeocoderResponse, Point}

import scala.io.Source
import scala.util.Try

/* @author berkut@yandex-team.ru */

object GeocoderAnalytics {
  val cache = Source.fromFile("/Users/berkut/tmp/geocoder_analytics.tsv").getLines().map(line => Try {
    val parts = line.split("\t")
    val query = parts(0).substring(4, parts(0).size - 1)
    val country = parts(1)
    val locality = parts(2)
    val address = parts(3)
    val geoId = parts(4).toInt
    val accuracy = parts(5).toDouble
    val precision = parts(6)
    val point = Point(parts(7).toDouble, parts(8).toDouble)
    val hotelPoint = Point(parts(9).toDouble, parts(10).toDouble)
    val distance = parts(11).toDouble
    GeocoderResponse(country, locality, address, geoId, point, precision, accuracy) -> (hotelPoint, distance, line)
  }.toOption).flatten

  def main(args: Array[String]): Unit = {
//    println(cache.count(pair => {
//      val geo = pair._1
//      val (hotelPoint, distance) = pair._2
//      geo.precision == "exact" && geo.accuracy == 1.0 && distance < 0.5
//    }))
    cache.filter(pair => {
      val geo = pair._1
      val (hotelPoint, distance, line) = pair._2
      geo.precision == "exact" && geo.accuracy == 1.0 && distance > 0.5
    }).foreach(t => println(t._2._3))
  }
}
