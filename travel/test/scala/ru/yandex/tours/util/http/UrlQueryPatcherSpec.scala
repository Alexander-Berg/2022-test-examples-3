package ru.yandex.tours.util.http

import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.model.utm.UtmMark
import ru.yandex.tours.personalization.UserIdentifiers
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.LabelBuilder
import spray.http.Uri

class UrlQueryPatcherSpec extends BaseSpec{
  val labelBuilder = new LabelBuilder("testtesttesttest")
  "UrlQueryPatcher" should {
    "patch common urls with redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "https://redirect.url", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, 12345, 4543)
      val url = "https://old.url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      val patchedUri = Uri(patchedUrl)
      patchedUri.authority.toString() should be ("//redirect.url")
      labelBuilder.decrypt(patchedUri.query.toMap("PUrl")) should be (
        "https://old.url?utm_source=yandextravel&param1=value1&param2=value2")
      labelBuilder.decrypt(patchedUri.query.toMap("Label")) should be ("\t\t\t\t\t\t\t12345\t12345\t\t\t4543")
    }

    "patch common incorrect urls with redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "https://redirect.url", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, 12345, 4543)
      val url = "bad url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      val patchedUri = Uri(patchedUrl)
      patchedUri.authority.toString() should be ("//redirect.url")
      labelBuilder.decrypt(patchedUri.query.toMap("PUrl")) should be (
        "bad url?param1=value1&param2=value2&utm_source=yandextravel")
      labelBuilder.decrypt(patchedUri.query.toMap("Label")) should be ("\t\t\t\t\t\t\t12345\t12345\t\t\t4543")
    }

    "patch specific urls with redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "https://redirect.url", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, Partners.ostrovok.id, 4543)
      val url = "https://old.url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      val patchedUri = Uri(patchedUrl)
      patchedUri.authority.toString() should be ("//redirect.url")
      labelBuilder.decrypt(patchedUri.query.toMap("PUrl")) should be (
        "https://old.url?utm_source=yandextravel&param1=value1&param2=value2")
      labelBuilder.decrypt(patchedUri.query.toMap("Label")) should be ("\t\t\t\t\t\t\t12345\t12345\t\t\t4543")
    }

    "patch specific incorrect urls with redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "https://redirect.url", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, Partners.ostrovok.id, 4543)
      val url = "bad url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      val patchedUri = Uri(patchedUrl)
      patchedUri.authority.toString() should be ("//redirect.url")
      labelBuilder.decrypt(patchedUri.query.toMap("PUrl")) should be (
        "bad url?param1=value1&param2=value2&utm_source=yandextravel")
      labelBuilder.decrypt(patchedUri.query.toMap("Label")) should be ("\t\t\t\t\t\t\t12345\t12345\t\t\t4543")
    }

    "patch common urls without redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, 12345, 4543)
      val url = "https://old.url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      val patchedUri = Uri(patchedUrl)
      patchedUri.authority.toString() should be ("//old.url")
      labelBuilder.decrypt(patchedUri.query.toMap("label")) should be ("\t\t\t\t\t\t\t12345\t12345\t\t\t4543")
    }

    "patch common incorrect urls without redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, 12345, 4543)
      val url = "bad url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      patchedUrl.startsWith("bad url?param1=value1&param2=value2&utm_source=yandextravel&label=") should be (true)
    }

    "patch specific urls without redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, Partners.ostrovok.id, 4543)
      val url = "https://old.url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      val patchedUri = Uri(patchedUrl)
      patchedUri.authority.toString() should be ("//old.url")
      labelBuilder.decrypt(patchedUri.query.toMap("utm_term")) should be ("\t\t\t\t\t\t\t12345\t12345\t\t\t4543")
    }

    "patch incorrect specific urls without redirection" in {
      val labeller = UrlLabelerBuilder(UtmMark.empty, UserIdentifiers.empty, "", labelBuilder)
      val patcher = labeller.buildFrom(12345, 12345, Partners.ostrovok.id, 4543)
      val url = "bad url?param1=value1&param2=value2"
      val patchedUrl = patcher(url)
      patchedUrl.startsWith("bad url?param1=value1&param2=value2&utm_source=yandextravel&utm_term=") should be (true)
    }
  }
}
