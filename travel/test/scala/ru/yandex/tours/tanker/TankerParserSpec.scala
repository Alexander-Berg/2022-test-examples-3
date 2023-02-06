package ru.yandex.tours.tanker

import java.io.ByteArrayInputStream

import org.scalatest.Matchers._
import org.scalatest.WordSpecLike
import ru.yandex.tours.model.Languages

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 18.03.15
 */
class TankerParserSpec extends WordSpecLike {

  "TankerParser" should {
    "parse tanker's json" in {
      val json =
        """
          |{
          |  "ru": {
          |    "keySet": {
          |      "key": "value"
          |    }
          |  }
          |}
        """.stripMargin
      val result = TankerParser.parse(new ByteArrayInputStream(json.getBytes))
      result.isEmpty shouldBe false
      result shouldBe new Translations(Map(("keySet", "key", Languages.ru) -> "value"))
    }
  }
}
