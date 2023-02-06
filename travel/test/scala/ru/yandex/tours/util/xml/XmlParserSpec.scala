package ru.yandex.tours.util.xml

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.tours.model.Languages
import ru.yandex.tours.model.Languages.Lang

class XmlParserSpec extends WordSpec with Matchers {
  "Xml parser" should {
    "be created from string" in {
      val parser = XmlParser.create("""<objects><object></object><object/></objects>""")
      var count = 0
      parser.processTag("objects", {
        case "object" => count += 1
      })
      count shouldBe 2
    }

    "parse text with entities" in {
      val parser = XmlParser.create("""<objects><object>A&amp;T</object></objects>""")
      var text: String = "fail"
      parser.processTag("objects", {
        case "object" => text = parser.getText
      })
      text shouldBe "A&T"
    }

    "parse langed text" in {
      val parser = XmlParser.create("""<object><name lang="ru">Привет!</name></object>""")
      var text: (Lang, String) = null
      parser.processTag("object", {
        case "name" => text = parser.getLangText
      })
      text shouldBe (Languages.ru, "Привет!")
    }

    "parse attributes" in {
      val parser = XmlParser.create("""<object><point lon="123.123" lat="32.123"></point></object>""")
      var visited = false
      parser.processTag("object", {
        case "point" =>
          visited = true
          parser.getAttribute("lon") shouldBe "123.123"
          parser.getAttribute("lat") shouldBe "32.123"
          parser.getOptAttribute("asd") shouldBe None
      })
      visited shouldBe true
    }

    "skip unknown tags" in {
      val parser = XmlParser.create("""<object><point lon="123.123" lat="32.123"></point><text>text</text></object>""")
      var visited = false
      parser.processTag("object", {
        case "text" => visited = true
      })
      visited shouldBe true
    }
  }
}
