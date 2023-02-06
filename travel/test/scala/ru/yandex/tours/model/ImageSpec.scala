package ru.yandex.tours.model

import ru.yandex.tours.model.image.{ImageFormats, ImageProviders}
import ru.yandex.tours.testkit.BaseSpec

import scala.collection.JavaConverters._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 12.04.16
 */
class ImageSpec extends BaseSpec {
  private val image1 = Image("http://avatars.yandex.net", 2, ImageProviders.ya, "abc_name", None, None, None, None)
  private val image2 = Image("avatars.yandex.net", 2, ImageProviders.ya, "abc_name", None, None, None, None)
  private val image3 = Image("//avatars.yandex.net", 2, ImageProviders.ya, "abc_name", None, None, None, None)

  "Image" should {
    "build valid base url" in {
      image1.baseUrl shouldBe "https://avatars.yandex.net/get-tours/2/abc_name"
      image2.baseUrl shouldBe "https://avatars.yandex.net/get-tours/2/abc_name"
      image3.baseUrl shouldBe "https://avatars.yandex.net/get-tours/2/abc_name"
    }
    "build valid url in format" in {
      image1.inFormat(ImageFormats.small) shouldBe "https://avatars.yandex.net/get-tours/2/abc_name/small"
      image2.inFormat(ImageFormats.small) shouldBe "https://avatars.yandex.net/get-tours/2/abc_name/small"
      image3.inFormat(ImageFormats.small) shouldBe "https://avatars.yandex.net/get-tours/2/abc_name/small"
    }
    "build valid url without protocol" in {
      image1.baseUrlWithoutProtocol shouldBe "//avatars.yandex.net/get-tours/2/abc_name"
      image2.baseUrlWithoutProtocol shouldBe "//avatars.yandex.net/get-tours/2/abc_name"
      image3.baseUrlWithoutProtocol shouldBe "//avatars.yandex.net/get-tours/2/abc_name"
    }
    "build valid url in format without protocol" in {
      image1.inFormatWithoutProtocol(ImageFormats.small) shouldBe "//avatars.yandex.net/get-tours/2/abc_name/small"
      image2.inFormatWithoutProtocol(ImageFormats.small) shouldBe "//avatars.yandex.net/get-tours/2/abc_name/small"
      image3.inFormatWithoutProtocol(ImageFormats.small) shouldBe "//avatars.yandex.net/get-tours/2/abc_name/small"
    }

    "construct from proto" in {
      val proto = image1.toProto
      Image.fromProto(proto).baseUrl shouldBe image1.baseUrl
    }
    "construct from proto with nn features" in {
      val proto = image1.toProto.toBuilder
        .addAllNNetFeatures(List(0.06526f, -0.05068f, -0.02999f, 0.16585f).map(Float.box).asJava)
        .build

      Image.fromProto(proto).nnFeatures.value.toSeq shouldBe Seq(0.06526f, -0.05068f, -0.02999f, 0.16585f)
    }
    "construct from proto with raw nn features" in {
      val proto = image1.toProto.toBuilder
        .setNNetFeaturesRaw("0.06526 -0.05068 -0.02999 0.16585")
        .build

      Image.fromProto(proto).nnFeatures.value.toList shouldBe List(0.06526f, -0.05068f, -0.02999f, 0.16585f)
    }
  }
}
