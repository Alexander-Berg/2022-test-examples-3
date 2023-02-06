package ru.yandex.tours.tools.hotels

import java.io.FileOutputStream

import ru.yandex.tours.hotels.YoctoHotelsIndex
import ru.yandex.tours.tools.{HotelAware, Tool}
import ru.yandex.tours.util.IO
import ru.yandex.tours.util.Randoms._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 16.03.16
 */
object TestIndexBuilderTool extends Tool with HotelAware {

  val sample = shardedHotels.sample(400d / shardedHotelsIndex.size)
  println(s"got ${sample.size} hotels in sample")

  IO.using(new FileOutputStream("tours-data/data/test_hotels_index")) { os =>
    YoctoHotelsIndex.build(sample, data.regionTree, data.hotelRatings, os)
  }
}
