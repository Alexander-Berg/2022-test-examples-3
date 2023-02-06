package ru.yandex.tours.util.file

import java.io.File

import org.scalatest.WordSpecLike
import org.scalatest.Matchers._

import scala.io.Source

/**
 * Author: Vladislav Dolbilov (darl@child-team.ru)
 * Created: 22.01.15
 */
class RichFileSpec extends WordSpecLike {

  "RichFile" should {
    "provide method `/`" in {
      val file = new File("/etc")
      file / "child" shouldBe new File(file, "child")
      file / "/child" shouldBe new File(file, "child")
      file / "child/" shouldBe new File(file, "child")
      file / "child/" / "grandchild" shouldBe new File(new File(file, "child"), "grandchild")
    }
    "provide method writeLines" in {
      val file = File.createTempFile("abc", "defg")
      file.writeLines(
        "abc",
        "def",
        "qwerty"
      )
      val content = Source.fromFile(file).mkString
      content shouldBe
        """abc
          |def
          |qwerty
          |""".stripMargin
      file.delete()
    }
  }
}
