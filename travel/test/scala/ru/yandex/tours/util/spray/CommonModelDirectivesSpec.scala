package ru.yandex.tours.util.spray

import ru.yandex.tours.model.Languages
import ru.yandex.tours.testkit.BaseSpec
import spray.http.Uri.Query

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 01.03.16
 */
class CommonModelDirectivesSpec extends BaseSpec {

  "CommonModelDirectives" should {
    "parse lang in request" in {
      extract(Query("lang=en"), CommonModelDirectives.lang) shouldBe Languages.en
      extract(Query("lang=tr"), CommonModelDirectives.lang) shouldBe Languages.tr
    }
    "fallback to empty lang" in {
      extract(Query(""), CommonModelDirectives.lang) shouldBe Languages.ru
    }
  }
}
