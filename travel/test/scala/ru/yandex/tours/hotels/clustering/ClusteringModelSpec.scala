package ru.yandex.tours.hotels.clustering

import java.io.FileInputStream

import ru.yandex.tours.model.hotels.HotelsHolder.PartnerHotel
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.naming.TfIdfModel
import shapeless.HNil

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 06.05.16
 */
class ClusteringModelSpec extends BaseSpec with TestData {

  val tfIdf = TfIdfModel.parse(new FileInputStream(root / "hotels_tf_idf"))
  val model = ClusteringModel.parse(new FileInputStream(root / "clustering.mx.info"), tfIdf :: HNil)

  "ClusteringModel" should {
    "predict probability for features" in {
      val features = Array(0.227181595559d, 0.0945269614313d, 0.0d, 0.131747206432d, 0.0d, 0.902841357443d, -1.0d,
        0.379810665684d, -1.0d, 0.0d, -1.0d, -1.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 1.0d, -1.0d, 0.0d, 0.0d, 1.0d)

      val res = model.apply(features)
      println("Result = " + res)
      res shouldBe (0.00011160135 +- 0.0001)
    }
    "predict probability for features #2" in {
      val features = Array(0.904418323323d, 0.856860402775d, 1.0d, 0.877302387125d, 1.0d, 0.760438573799d, -1.0d,
        0.114527540901d, 0.6875d, 12.21875d, 0.0d, 0.867045910848d, 16.0312971469d, 0.333333333333d, 0.5d,
        -1.0d, 0.4d, -1.0d, -1.0d, 0.0d, 0.0d, 0.0d)

      val res = model.apply(features)
      println("Result = " + res)
      res shouldBe (0.995505839 +- 0.0001)
    }
    "predict probability for context" in {
      val hotel1 = HotelContext.apply(PartnerHotel.getDefaultInstance, new LocalContext(Iterator.empty))
      val hotel2 = HotelContext.apply(PartnerHotel.getDefaultInstance, new LocalContext(Iterator.empty))
      val ctx = ClusteringContext(hotel1, hotel2)

      val features = model.features(ctx)
      println("Features:")
      model.features.names zip features foreach {
        case (f, v) => println(s"$f = $v")
      }
      val res = model.apply(ctx)
      println("Result = " + res)
      res shouldBe (0.5d +- 0.5d)
    }
  }
}
