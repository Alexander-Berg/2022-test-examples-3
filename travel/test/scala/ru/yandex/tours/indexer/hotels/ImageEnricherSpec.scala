package ru.yandex.tours.indexer.hotels

import java.io.{FileInputStream, InputStream}

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

import org.mockito.Matchers._
import org.mockito.Mockito._
import ru.yandex.tours.avatars.AvatarClient
import ru.yandex.tours.model.Image
import ru.yandex.tours.model.hotels.HotelsHolder.PartnerHotel
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.model.image.ImageProviders
import ru.yandex.tours.model.image.ImageProviders.ImageProvider
import ru.yandex.tours.testkit.BaseSpec
import ru.yandex.tours.util.collections.RafBasedMap
import ru.yandex.tours.util.{IO, ProtoIO}
import spray.http.{StatusCode, StatusCodes}

import scala.concurrent.Future

class ImageEnricherSpec extends BaseSpec with BeforeAndAfterAll with ScalaFutures with IntegrationPatience {
  val as = ActorSystem("test-as", ConfigFactory.empty)
  val hotel = readHead(getClass.getResourceAsStream("/single_hotel.proto"))
  val map = new RafBasedMap[Int, PartnerHotel](_.toByteArray, PartnerHotel.parseFrom)
  map += hotel.getId -> hotel

  "Image enricher" should {
    "Enrich hotel with photos" in {
      val avatarClient = new AvatarClient {
        override def put(url: String, provider: ImageProvider): Future[Image] =
          Future.successful(Image("host",
            1000,
            provider,
            Image.name(url),
            None,
            size = None,
            pHash = None,
            nnFeatures = None)
          )

        override def putRaw(url: String, imageName: String, provider: ImageProvider): Future[(StatusCode, String)] = {
          Future.successful((StatusCodes.OK, "Ok"))
        }

        override def recover(image: Image): Future[Image] = Future.successful(image)
      }
      val imageEnricher = new ImageEnricher(avatarClient)(as.dispatcher, as)
      val input = IO.usingTmp("spec") { os =>
        hotel.toBuilder.clearImages().build.writeDelimitedTo(os)
      }
      val result = imageEnricher.enrich(input, Partners.oktogo, ImageProviders.oktogo, map).futureValue
      val enrichedHotel = readHead(new FileInputStream(result))
      enrichedHotel.getImagesCount shouldBe hotel.getRawHotel.getRawImagesCount
      IO.deleteFile(result)
      IO.deleteFile(input)
    }

    "Fallback to old photos in case of avatar fail" in {
      val failingAvatar = mock[AvatarClient]
      when(failingAvatar.put(anyString(), anyObject())).thenReturn(Future.failed(new Exception("failed")))
      val imageEnricher = new ImageEnricher(failingAvatar)(as.dispatcher, as)
      val input = IO.usingTmp("spec") { os =>
        val builder: PartnerHotel.Builder = hotel.toBuilder.clearImages()
        builder.addImagesBuilder().setGroup(1).setHost("").setName("").setProviderId(1)
        builder.build.writeDelimitedTo(os)
      }
      val result = imageEnricher.enrich(input, Partners.oktogo, ImageProviders.oktogo, map).futureValue
      val enrichedHotel = readHead(new FileInputStream(result))
      enrichedHotel.getImagesCount shouldBe 1
      IO.deleteFile(result)
      IO.deleteFile(input)
    }
  }

  private def readHead(is: InputStream) = {
    ProtoIO.loadFromStream(is, PartnerHotel.PARSER).next()
  }

  override protected def afterAll(): Unit = {
    as.terminate()
    map.close()
  }
}
