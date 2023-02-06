package ru.yandex.tours

import java.io.File

import ru.yandex.extdata.common.service.ExtDataService
import ru.yandex.tours.avatars.AvatarClient
import ru.yandex.tours.backa.BackaPermalinks
import ru.yandex.tours.direction.Directions
import ru.yandex.tours.extdataloader.verba.Verba
import ru.yandex.tours.geo.base.region
import ru.yandex.tours.geo.mapping.GeoMappingHolder
import ru.yandex.tours.hotels.HotelsIndex
import ru.yandex.tours.testkit.{BaseSpec, TestConfig}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 06.06.15
 */
class IndexerDataLoadersSpec extends BaseSpec {

  "IndexerDataLoaders" should {
    "initialize" in {
      val loaders = new IndexerDataLoaders(
        new File("."),
        TestConfig.config,
        mock[AvatarClient],
        mock[Verba],
        mock[HotelsIndex],
        mock[BackaPermalinks],
        mock[GeoMappingHolder],
        mock[region.Tree],
        mock[Directions],
        mock[ExtDataService]
      )
      loaders.loaders should not be empty
      loaders.specs should not be empty
      loaders.loadableSpecs should not be empty
      loaders.validators should not be empty
    }
  }
}
