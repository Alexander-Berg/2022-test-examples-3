package ru.yandex.tours.model.utm

import java.net.URLEncoder

import org.scalatest.{Matchers, WordSpecLike}

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 14.05.15
 */
class UtmMarkSpec extends WordSpecLike with Matchers {
  val mark = UtmMark("s", "m", "camp", "c", "t", "q", "w")

  def e(raw: String) = URLEncoder.encode(raw, "UTF-8")

  "UtmMark" should {
    "serialize to url parameters" in {
      val mark = UtmMark("s", "m", "camp", "c", "t", "q", "w")
      mark.urlSafe shouldBe "utm_source=s&utm_medium=m&utm_campaign=camp&utm_content=c&utm_term=t&user_query=q&req_id=w"
    }
    "serialize to url with correct escaping" in {
      val mark = UtmMark("источник", "медиум", "компания", "контент", "терм", "запрос", "реквест")
      val expected = s"utm_source=${e("источник")}&utm_medium=${e("медиум")}&utm_campaign=${e("компания")}&utm_content=${e("контент")}&utm_term=${e("терм")}&user_query=${e("запрос")}&req_id=${e("реквест")}"
      mark.urlSafe shouldBe expected
    }
    "serialize to url with correct escaping #2" in {
      val mark = UtmMark.empty.withSource("источник")
      mark.urlSafe shouldBe "utm_source=%D0%B8%D1%81%D1%82%D0%BE%D1%87%D0%BD%D0%B8%D0%BA"
    }
  }
}
