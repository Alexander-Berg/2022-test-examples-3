package ru.yandex.tours.index

import org.joda.time.DateTime
import org.junit.rules.TemporaryFolder
import org.scalatest.BeforeAndAfter
import ru.yandex.tours.testkit.BaseSpec
import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 27.05.15
 */
class IndexCleanerSpec extends BaseSpec with BeforeAndAfter {
  val dir = new TemporaryFolder()

  before {
    dir.create()
  }
  after {
    dir.delete()
  }

  private def name(createdAt: DateTime) = {
    createdAt.getMillis + "_foo.index"
  }

  "IndexCleaner" should {
    "delete outdated file" in {
      val file = dir.newFile(name(DateTime.now.minusMinutes(10)))
      file should exist
      IndexCleaner.cleanDirectory(dir.getRoot, ".index", 5.minutes)
      file shouldNot exist
    }
    "keep fresh file" in {
      val file = dir.newFile(name(DateTime.now.minusMinutes(4)))
      file should exist
      IndexCleaner.cleanDirectory(dir.getRoot, ".index", 5.minutes)
      file should exist
    }
    "skip file without .index suffix" in {
      val file = dir.newFile("12441_foo")
      file should exist
      IndexCleaner.cleanDirectory(dir.getRoot, ".index", 5.minutes)
      file should exist
    }
    "delete file with invalid name" in {
      val file = dir.newFile("12411.index")
      file should exist
      IndexCleaner.cleanDirectory(dir.getRoot, ".index", 5.minutes)
      file shouldNot exist
    }
  }
}
