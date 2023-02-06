package ru.yandex.tours.partners.leveltravel

import org.junit.Assert._
import org.junit.Test
import ru.yandex.tours.model.RequestStatus
import ru.yandex.tours.operators.TourOperators
import ru.yandex.tours.testkit.BaseSpec

import scala.util.{Failure, Success}

/* @author berkut@yandex-team.ru */

class EnqueueResponseTest extends BaseSpec {
  val tourOperators = TourOperators.fromFile(root / "operators")

  @Test
  def testParse(): Unit = {
    val parsed = EnqueueResponse.parse("{\"partner\":\"Ya\",\"request_id\":\"803879fd-49a9-4d66-a633-d8548901d0e2\",\"status\":{\"1\":\"pending\",\"2\":\"cached\"},\"search_params\":{\"query\":\"Moscow-RU-to-Any-EG-departure-14.11.2014-for-7-nights-2-adults-0-kids-1..5-stars\",\"from\":{\"city\":{\"id\":2,\"name_ru\":\"\\u041c\\u043e\\u0441\\u043a\\u0432\\u0430\",\"name_en\":\"Moscow\"},\"country\":{\"id\":2,\"name_ru\":\"\\u0420\\u043e\\u0441\\u0441\\u0438\\u044f\",\"name_en\":\"Russia\",\"iso2\":\"RU\"}},\"to\":{\"city\":{\"id\":0,\"name_ru\":\"\\u041b\\u044e\\u0431\\u043e\\u0439 \\u0433\\u043e\\u0440\\u043e\\u0434\",\"name_en\":\"Any\"},\"country\":{\"id\":3,\"name_ru\":\"\\u0415\\u0433\\u0438\\u043f\\u0435\\u0442\",\"name_en\":\"Egypt\",\"iso2\":\"EG\",\"wb_code\":\"800260\"}},\"start_date\":\"2014-11-14\",\"nights\":{\"from\":7,\"to\":7},\"adults\":2,\"kids\":{\"count\":0,\"ages\":[]},\"stars\":{\"from\":1,\"to\":5},\"options\":{\"from_price\":0,\"wide_search\":false,\"flex_dates\":false}}}", tourOperators)
    parsed match {
      case Failure(e) =>
        fail("Success is expected" + e)
      case Success(response) =>
        assertEquals("803879fd-49a9-4d66-a633-d8548901d0e2", response.requestId)
        val operators = response.operators
        assertEquals(2, operators.size)
        assertEquals(RequestStatus.PENDING, operators(tourOperators.getById(1).value))
        assertEquals(RequestStatus.CACHED, operators(tourOperators.getById(2).value))
    }
  }

  @Test
  def testRequestIsMissing(): Unit = {
    val parsed = EnqueueResponse.parse("{\"partner\":\"Ya\",\"status\":{\"1\":\"pending\",\"2\":\"pending\"},\"search_params\":{\"query\":\"Moscow-RU-to-Any-EG-departure-14.11.2014-for-7-nights-2-adults-0-kids-1..5-stars\",\"from\":{\"city\":{\"id\":2,\"name_ru\":\"\\u041c\\u043e\\u0441\\u043a\\u0432\\u0430\",\"name_en\":\"Moscow\"},\"country\":{\"id\":2,\"name_ru\":\"\\u0420\\u043e\\u0441\\u0441\\u0438\\u044f\",\"name_en\":\"Russia\",\"iso2\":\"RU\"}},\"to\":{\"city\":{\"id\":0,\"name_ru\":\"\\u041b\\u044e\\u0431\\u043e\\u0439 \\u0433\\u043e\\u0440\\u043e\\u0434\",\"name_en\":\"Any\"},\"country\":{\"id\":3,\"name_ru\":\"\\u0415\\u0433\\u0438\\u043f\\u0435\\u0442\",\"name_en\":\"Egypt\",\"iso2\":\"EG\",\"wb_code\":\"800260\"}},\"start_date\":\"2014-11-14\",\"nights\":{\"from\":7,\"to\":7},\"adults\":2,\"kids\":{\"count\":0,\"ages\":[]},\"stars\":{\"from\":1,\"to\":5},\"options\":{\"from_price\":0,\"wide_search\":false,\"flex_dates\":false}}}", tourOperators)
    parsed match {
      case Failure(e) =>
      case Success(response) => fail("Failure is expected")
    }
  }

  @Test
  def testStatusIsMissing(): Unit = {
    val parsed = EnqueueResponse.parse("{\"partner\":\"Ya\",\"request_id\":\"803879fd-49a9-4d66-a633-d8548901d0e2\",\"some_stuff\":{\"1\":\"pending\",\"2\":\"pending\"},\"search_params\":{\"query\":\"Moscow-RU-to-Any-EG-departure-14.11.2014-for-7-nights-2-adults-0-kids-1..5-stars\",\"from\":{\"city\":{\"id\":2,\"name_ru\":\"\\u041c\\u043e\\u0441\\u043a\\u0432\\u0430\",\"name_en\":\"Moscow\"},\"country\":{\"id\":2,\"name_ru\":\"\\u0420\\u043e\\u0441\\u0441\\u0438\\u044f\",\"name_en\":\"Russia\",\"iso2\":\"RU\"}},\"to\":{\"city\":{\"id\":0,\"name_ru\":\"\\u041b\\u044e\\u0431\\u043e\\u0439 \\u0433\\u043e\\u0440\\u043e\\u0434\",\"name_en\":\"Any\"},\"country\":{\"id\":3,\"name_ru\":\"\\u0415\\u0433\\u0438\\u043f\\u0435\\u0442\",\"name_en\":\"Egypt\",\"iso2\":\"EG\",\"wb_code\":\"800260\"}},\"start_date\":\"2014-11-14\",\"nights\":{\"from\":7,\"to\":7},\"adults\":2,\"kids\":{\"count\":0,\"ages\":[]},\"stars\":{\"from\":1,\"to\":5},\"options\":{\"from_price\":0,\"wide_search\":false,\"flex_dates\":false}}}", tourOperators)
    parsed match {
      case Failure(e) =>
      case Success(response) => fail("Failure is expected")
    }
  }

  @Test
  def testEmptyStatus(): Unit = {
    val parsed = EnqueueResponse.parse("{\"partner\":\"Ya\",\"request_id\":\"803879fd-49a9-4d66-a633-d8548901d0e2\",\"status\": {},\"search_params\":{\"query\":\"Moscow-RU-to-Any-EG-departure-14.11.2014-for-7-nights-2-adults-0-kids-1..5-stars\",\"from\":{\"city\":{\"id\":2,\"name_ru\":\"\\u041c\\u043e\\u0441\\u043a\\u0432\\u0430\",\"name_en\":\"Moscow\"},\"country\":{\"id\":2,\"name_ru\":\"\\u0420\\u043e\\u0441\\u0441\\u0438\\u044f\",\"name_en\":\"Russia\",\"iso2\":\"RU\"}},\"to\":{\"city\":{\"id\":0,\"name_ru\":\"\\u041b\\u044e\\u0431\\u043e\\u0439 \\u0433\\u043e\\u0440\\u043e\\u0434\",\"name_en\":\"Any\"},\"country\":{\"id\":3,\"name_ru\":\"\\u0415\\u0433\\u0438\\u043f\\u0435\\u0442\",\"name_en\":\"Egypt\",\"iso2\":\"EG\",\"wb_code\":\"800260\"}},\"start_date\":\"2014-11-14\",\"nights\":{\"from\":7,\"to\":7},\"adults\":2,\"kids\":{\"count\":0,\"ages\":[]},\"stars\":{\"from\":1,\"to\":5},\"options\":{\"from_price\":0,\"wide_search\":false,\"flex_dates\":false}}}", tourOperators)
    parsed match {
      case Failure(e) => assertEquals("assertion failed: Response should contains at least 1 operator", e.getMessage)
      case Success(response) => fail("Failure is expected")
    }
  }

  @Test
  def testAnother(): Unit = {
    val raw = """{
                  "partner"       : "ya",
                  "request_id"    : "111",
                  "status"        : {
                    1: "pending",
                    2: "performing",
                  },
              }"""
    val parsed = EnqueueResponse.parse(raw, tourOperators).get
    assertEquals("111", parsed.requestId)
    val statuses = parsed.operators.toSet
    assertEquals(2, statuses.size)
    assertTrue(statuses.contains(tourOperators.getById(1).value -> RequestStatus.PENDING))
    assertTrue(statuses.contains(tourOperators.getById(2).value -> RequestStatus.PERFORMING))
  }
}
