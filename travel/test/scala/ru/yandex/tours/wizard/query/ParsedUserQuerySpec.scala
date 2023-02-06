package ru.yandex.tours.wizard.query

import org.scalatest.WordSpecLike
import org.scalatest.Matchers._
import ru.yandex.tours.query.{DepartureRegion, SomeRegion, TourMarker, Unknown}
import ru.yandex.tours.wizard.query.ParsedUserQuery.QueryPart

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 03.02.15
 */
class ParsedUserQuerySpec extends WordSpecLike {

  val userRequest = "туры из санкт-петербурга"
  val parsedQuery = {
    ParsedUserQuery(
      userRequest = userRequest,
      Vector(
        QueryPart(userRequest, 0, 4, TourMarker),
        QueryPart(userRequest, 5, 24, DepartureRegion(2)),
        QueryPart(userRequest, 8, 24, SomeRegion(2))
      )
    )
  }

  val bestMatch = {
    parsedQuery.copy(queryParts =
      Vector(
        QueryPart(userRequest, 0, 4, TourMarker),
        QueryPart(userRequest, 5, 24, DepartureRegion(2))
      )
    )
  }

  val withoutHoles = {
    parsedQuery.copy(queryParts =
      Vector(
        QueryPart(userRequest, 0, 4, TourMarker),
        QueryPart(userRequest, 4, 5, Unknown),
        QueryPart(userRequest, 5, 24, DepartureRegion(2))
      )
    )
  }

  "ParsedUserQuery" should {
    "be empty" in {
      ParsedUserQuery("abc", Vector.empty).isEmpty shouldBe true
      ParsedUserQuery("abc", Vector.empty).nonEmpty shouldBe false
    }
    "be nonEmpty" in {
      ParsedUserQuery("abc", Vector(QueryPart("abc", 0, 4, Unknown))).isEmpty shouldBe false
      ParsedUserQuery("abc", Vector(QueryPart("abc", 0, 4, Unknown))).nonEmpty shouldBe true
    }
    "select best match" in {
      parsedQuery.selectBestMatch shouldBe bestMatch
    }
    "check for holes" in {
      val empty = ParsedUserQuery("abc", Vector.empty)
      empty.containHoles shouldBe true
      bestMatch.containHoles shouldBe true
      withoutHoles.containHoles shouldBe false
    }
    "fill holes" in {
      bestMatch.fillHoles shouldBe withoutHoles
    }
    "fill holes if nothing parsed" in {
      val emptyQuery = ParsedUserQuery(userRequest, Vector.empty)
      val expected = ParsedUserQuery(userRequest, Vector(QueryPart(userRequest, 0, 24, Unknown)))
      emptyQuery.fillHoles shouldBe expected
    }
  }

  "QueryPart" should {
    "calculate score from length" in {
      val part = QueryPart(userRequest, 8, 24, SomeRegion(2))
      part.score shouldBe 15
    }
    "use boost to calculate score" in {
      val part = QueryPart(userRequest, 8, 24, SomeRegion(2))
      part.boost = 0.9d
      part.score shouldBe 13.5
      part.boost = 1.1d
      part.score shouldBe 16.5
    }
    "copy boost" in {
      val part = QueryPart(userRequest, 8, 24, SomeRegion(2))
      part.boost = 3d
      val copy = part.copy(startPosition = 7)
      copy.boost shouldBe 3d
    }
  }
}
