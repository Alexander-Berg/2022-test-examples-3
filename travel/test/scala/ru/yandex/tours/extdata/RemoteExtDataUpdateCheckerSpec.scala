package ru.yandex.tours.extdata

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.Matchers._
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.scalatest.mock.MockitoSugar
import ru.yandex.extdata.common.meta.{DataInstance, MetaDataInstance, DataType}
import ru.yandex.extdata.provider.HttpExtDataClient
import ru.yandex.extdata.provider.cache.LocalFSDataCache

import scala.concurrent.duration._
import scala.collection.JavaConverters._


/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 08.03.15
 */
class RemoteExtDataUpdateCheckerSpec extends TestKit(ActorSystem("remote-ext-data-update-checker-spec"))
with WordSpecLike with MockitoSugar with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val interval = 1.minute
  val dataType = new DataType("test_data", 2)

  def meta(instances: DataInstance*) = new MetaDataInstance(dataType.getName, instances.asJava)
  def instance(format: Int, version: Int) = new DataInstance(dataType.getName, "hash123", version, format)
  val emptyMeta = meta()

  trait Env {
    val extDataClient = mock[HttpExtDataClient]
    val cache = mock[LocalFSDataCache]
    val checker = new RemoteExtDataUpdateChecker(extDataClient, cache, interval)(system)
  }

  "ExtDataUpdateChecker" should {
    "checkNewVersion == NOP after start" in new Env {
      checker.checkNewVersions()
      verify(extDataClient, never()).getMetaData(anyObject())
    }
    "subscribe for dataType" in new Env {
      var c = false
      checker.subscribe(dataType) {c = true}
      c shouldNot beCalled
    }
    "not call callback on empty meta" in new Env {
      when(extDataClient.getMetaData(dataType)).thenReturn(emptyMeta)
      var c = false

      checker.subscribe(dataType) {c = true}
      checker.checkNewVersions()

      c shouldNot beCalled
      verify(extDataClient).getMetaData(dataType)
    }
    "not call callback if no data with current format" in new Env {
      when(extDataClient.getMetaData(dataType)).thenReturn(meta(instance(1, 1), instance(3, 1)))
      var c = false

      checker.subscribe(dataType) {c = true}
      checker.checkNewVersions()

      c shouldNot beCalled
      verify(extDataClient).getMetaData(dataType)
    }
    "call callback if meta have data with current format" in new Env {
      when(extDataClient.getMetaData(dataType)).thenReturn(meta(instance(2, 1)))
      var c = false

      checker.subscribe(dataType) {c = true}
      checker.checkNewVersions()

      c should beCalled
      verify(extDataClient).getMetaData(dataType)
    }
    "call callback only once for one version" in new Env {
      when(extDataClient.getMetaData(dataType)).thenReturn(meta(instance(2, 1)))
      var c = 0

      checker.subscribe(dataType) {c += 1}
      checker.checkNewVersions()
      checker.checkNewVersions()

      c shouldBe 1
      verify(extDataClient, times(2)).getMetaData(dataType)
    }

    "call callback if version changed" in new Env {
      when(extDataClient.getMetaData(dataType)).thenReturn(meta(instance(2, 1)), meta(instance(2, 2)))
      var c = 0

      checker.subscribe(dataType) {c += 1}

      checker.checkNewVersions()
      c shouldBe 1

      checker.checkNewVersions()
      c shouldBe 2

      verify(extDataClient, times(2)).getMetaData(dataType)
    }

    "return version for provided data type" in new Env {
      when(extDataClient.getMetaData(dataType)).thenReturn(meta(instance(2, 1)))

      checker.getVersion(dataType) shouldBe 1

      verify(extDataClient).getMetaData(dataType)
    }

    "return cached version for provided data type" in new Env {
      when(extDataClient.getMetaData(dataType)).thenReturn(meta(instance(2, 1)))

      checker.subscribe(dataType) {}
      checker.checkNewVersions()
      verify(extDataClient).getMetaData(dataType)

      checker.getVersion(dataType) shouldBe 1
      verifyNoMoreInteractions(extDataClient)
    }
  }

  val beCalled = new Matcher[Boolean] {
    override def apply(left: Boolean): MatchResult = {
      MatchResult(
        left,
        "Method is not called",
        "Method is called"
      )
    }
  }
}
