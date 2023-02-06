package ru.yandex.tours.util.io

import java.io.{ByteArrayInputStream, PrintWriter, ByteArrayOutputStream}

import org.scalatest.{Matchers, WordSpec}

class StringIteratorInputStreamSpec extends WordSpec with Matchers {
  "StringIteratorInputStream" should {
    testWithCapacity(1)
    testWithCapacity(1024 * 1024)
  }

  private def testWithCapacity(capacity: Int) = {
    s"Convert scala source of lines to input stream with internal buffer capacity $capacity" in {
      val lines = Seq("asdfasdf", "123fh554", "234234$$!!", "34 asd\t23")
      val os = new ByteArrayOutputStream()
      val pw = new PrintWriter(os)
      lines.foreach(pw.println)
      pw.close()
      val is = new ByteArrayInputStream(os.toByteArray)
      val linesFromSource = scala.io.Source.fromInputStream(is).getLines()
      val siis = new StringIteratorInputStream(linesFromSource, Some(is), capacity)
      scala.io.Source.fromInputStream(siis).getLines().toSeq shouldBe lines
    }
  }
}
