package ru.yandex.tours.util.collections

import java.io.File

import ru.yandex.tours.testkit.{TemporaryDirectory, BaseSpec}
import ru.yandex.vertis.curator.recipes.map.StringValueSerializer

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 11.01.16
 */
class FileStashSpec extends BaseSpec with TemporaryDirectory {

  def newStash(file: File) = new FileStash(file, StringValueSerializer)
  def newStash(name: String) = new FileStash(tempDir.newFile(name), StringValueSerializer)

  "FileStash" should {
    "stash provided value" in {
      val stash = newStash("simple_push")
      stash.push("abc")
      stash.pull() shouldBe Some("abc")
    }
    "return None if pulled from empty stash" in {
      val stash = newStash("empty")
      stash.pull() shouldBe None
    }
    "return None after all elements was pulled" in {
      val stash = newStash("pull_none")
      stash.push("1")
      stash.pull() shouldBe Some("1")
      stash.pull() shouldBe None
    }
    "return elements in same order as pushed" in {
      val stash = newStash("order")
      stash.push("1")
      stash.push("3")
      stash.push("2")
      stash.pull() shouldBe Some("1")
      stash.pull() shouldBe Some("3")
      stash.pull() shouldBe Some("2")
    }
    "read from existing stash file" in {
      val file = tempDir.newFile("persist")

      val out = newStash(file)
      out.push("123")
      val stash = newStash(file)
      stash.pull() shouldBe Some("123")
    }
    "throw exception on push after flip" in {
      val stash = newStash("exception")
      stash.push("1")
      stash.pull() shouldBe Some("1")

      an[Exception] shouldBe thrownBy {
        stash.push("2")
      }
    }
  }

}
