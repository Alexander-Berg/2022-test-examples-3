package ru.yandex.tours.util.collections

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

import org.scalatest.Inspectors
import ru.yandex.tours.testkit.BaseSpec

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 25.01.16
 */
class MappedArraySpec extends BaseSpec with Inspectors {

  "MappedArray" should {
    "write and read int array" in {
      val array = Array(1, 4, 7, 42)
      val baos = new ByteArrayOutputStream()
      MappedArray.writeArray[Int](array, Some(Integer.BYTES), i => ByteBuffer.allocate(4).putInt(i).array(), baos)

      val bytes = baos.toByteArray
      println("length: " + bytes.length)

      val mapped = MappedArray.intArray(ByteBuffer.wrap(bytes))
      mapped.size shouldBe array.length
      mapped.isFixedSizeRecords shouldBe true
      forAll(array.indices) { i =>
        mapped.get(i) shouldBe array(i)
      }
    }

    "write and read string array" in {
      val array = Array("a", "abc", "55", "gsiohouhioh2i423")
      val baos = new ByteArrayOutputStream()
      MappedArray.writeArray[String](array, None, _.getBytes(), baos)

      val bytes = baos.toByteArray
      println("length: " + bytes.length)

      val mapped = new MappedArray[String](ByteBuffer.wrap(bytes),
        { bb => val arr = new Array[Byte](bb.remaining()); bb.get(arr); new String(arr) }
      )
      mapped.size shouldBe array.length
      mapped.isFixedSizeRecords shouldBe false
      forAll(array.indices) { i =>
        mapped.get(i) shouldBe array(i)
      }
    }
  }
}
