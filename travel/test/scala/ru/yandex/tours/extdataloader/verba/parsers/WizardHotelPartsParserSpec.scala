package ru.yandex.tours.extdataloader.verba.parsers

import java.io.InputStream

import org.apache.commons.io.IOUtils
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import ru.yandex.extdata.common.meta.DataType
import ru.yandex.extdata.loader.engine.DataPersistenceManager
import ru.yandex.tours.testkit.{BaseSpec, TestData}

import scala.concurrent.duration._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 28.09.15
 */
class WizardHotelPartsParserSpec extends BaseSpec with TestData {

  val dpm = mock[DataPersistenceManager]
  val parser = new WizardHotelNamePartsIndexer(() => Map.empty, data.hotelsIndex, 1.hour, dpm)

  "should build bytes" in {
    var result: Array[Byte] = null
    when(dpm.checkAndStore(anyObject[DataType], anyObject[InputStream])).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock): AnyRef = {
        val is = invocation.getArguments.apply(1).asInstanceOf[InputStream]
        result = IOUtils.toByteArray(is)
        null
      }
    })

    parser.run()

    result should not be null
    println("Size: " + result.length)
    result should not be empty
  }
}
