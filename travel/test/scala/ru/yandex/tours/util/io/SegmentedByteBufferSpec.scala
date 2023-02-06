package ru.yandex.tours.util.io

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.io.SegmentedByteBuffer.SegmentedOutputStream

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.01.16
 */
class SegmentedByteBufferSpec extends BaseSpec {

  "SegmentedByteBuffer" should {
    "read and write" in {
      val baos = new ByteArrayOutputStream()
      val sos = new SegmentedOutputStream(baos)

      sos.writeSegment("seg 1", Array.empty)
      sos.writeSegment("seg 2", Array[Byte](1, 3, 2))
      sos.writeSegment("seg 3", Array[Byte](3, 2))
      sos.close()


      val buffer = SegmentedByteBuffer(ByteBuffer.wrap(baos.toByteArray))
      println(buffer)
      buffer.count shouldBe 3
      buffer.names shouldBe Seq("seg 1", "seg 2", "seg 3")
      buffer.getBuffer("seg 1").remaining() shouldBe 0
      buffer.getBuffer("seg 2").remaining() shouldBe 3
      buffer.getBuffer("seg 3").remaining() shouldBe 2

      val seg2 = buffer.getBuffer("seg 2")
      val seg2Bytes = new Array[Byte](3)
      seg2.get(seg2Bytes)
      seg2Bytes.toVector shouldBe Vector[Byte](1, 3, 2)
    }
  }
}
