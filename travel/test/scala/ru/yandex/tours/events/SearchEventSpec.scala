package ru.yandex.tours.events

import com.google.protobuf.Message
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.output.ByteArrayOutputStream
import org.joda.time.DateTime
import ru.yandex.tours.model.search.SearchProducts.HotelSnippet
import ru.yandex.tours.model.search.SearchResults.{HotelSearchResult, SearchProgress}
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.GZip._
import ru.yandex.tours.util.collections.SimpleBitSet

import scala.collection.JavaConverters._

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 28.12.15
 */
class SearchEventSpec extends BaseSpec with TestData {

  private def writeProto[T <: Message](msg: T): String = {
    val os = new ByteArrayOutputStream()
    msg.writeTo(os)
    Base64.encodeBase64String(compress(os.toByteArray))
  }

  private def writeDelimited[T <: Message](seq: Iterable[T]): String = {
    val os = new ByteArrayOutputStream()
    seq.foreach(_.writeDelimitedTo(os))
    Base64.encodeBase64String(compress(os.toByteArray))
  }

  private def checkProgress(progress: SearchProgress, snippets: Iterable[HotelSnippet], failed: Int) = {
    val successOperators = snippets.flatMap(_.getSourceList.asScala).map(_.getOperatorId).toSet
    progress.getIsFinished shouldBe true
    progress.getOBSOLETEFailedOperatorsList.asScala shouldBe Seq(failed)
    progress.getOperatorTotalCount shouldBe (successOperators + failed).size
    progress.getOperatorCompleteCount shouldBe (successOperators + failed).size
    progress.getOperatorSkippedCount shouldBe 0
    progress.getOperatorFailedCount shouldBe 1

    progress.getOperatorCompleteSet shouldBe SimpleBitSet(successOperators + failed).packed
    progress.getOperatorSkippedSet shouldBe SimpleBitSet(Set.empty).packed
    progress.getOperatorFailedSet shouldBe SimpleBitSet(Set(failed)).packed
  }

  "SearchEvent" should {
    "parse snippets with old hydra format" in {
      val time = DateTime.now
      val req = data.randomRequest
      val snippets = data.randomSnippets(req, 5)
      val failed = data.random.nextInt(64)
      val map = (req.asMap ++ Map(
        "snippets" -> writeDelimited(snippets),
        "failed" -> SimpleBitSet(Set(failed)).packed
      )).mapValues(_.toString)

      val parsed = SearchEvent.parseSnippets(time, map)

      parsed.eventTime shouldBe time
      parsed.request shouldBe req
      checkProgress(parsed.result.getProgress, snippets, failed)
      parsed.result.getHotelSnippetList.asScala shouldBe snippets
    }
    "parse snippets with new hydra format" in {
      val time = DateTime.now
      val req = data.randomRequest
      val snippets = data.randomSnippets(req, 5)
      val builder = HotelSearchResult.newBuilder()
        .setCreated(time.getMillis)
        .setUpdated(time.getMillis + 501)
        .addAllHotelSnippet(snippets.asJava)

      builder.getProgressBuilder
        .setIsFinished(true)
        .setOperatorTotalCount(13)
        .setOperatorCompleteCount(5)
        .setOperatorFailedCount(1)
        .setOperatorSkippedCount(7)

      builder.getResultInfoBuilder.setIsFromLongCache(false)

      val result = builder.build()

      val map = (req.asMap ++ Map(
        "result" -> writeProto(result)
      )).mapValues(_.toString)

      val parsed = SearchEvent.parseSnippets(time, map)

      parsed.eventTime shouldBe time
      parsed.request shouldBe req
      parsed.result.getHotelSnippetList.asScala shouldBe snippets
      parsed.result shouldBe result
    }

    "parse offers with old format" in {
      pending
    }
    "parse offers with new format" in {
      pending
    }
    "parse actualization" in {
      pending
    }
  }
}
