package controllers.suggest

import ru.yandex.tours.geo.base.region.Tree
import ru.yandex.tours.geo.partners.{PartnerTree, PartnerTreeUtils}
import ru.yandex.tours.model.hotels.Partners
import ru.yandex.tours.testkit.{BaseSpec, TestData}
import ru.yandex.tours.util.Randoms._
import ru.yandex.tours.util.parsing.IntValue

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 27.11.15
 */
class GeoSuggestSpec extends BaseSpec with TestData {

  "GeoSuggest" should {
    "suggest regions" in {
      val tree = new Tree(data.regionTree.regions.sample(50))
      val suggester = new YandexGeoSuggest(tree, IntValue.parse, data.directionsStats.getPriority)

      def testSuggest(query: String): Unit = {
        val regions = suggester.suggest(query, 10)
        println(query + " = " + regions.map(_.name.ruName).mkString(", "))
      }

      testSuggest("Егип")
      testSuggest("Е")
      testSuggest("а")
      testSuggest("сш")
      testSuggest("сша")
      testSuggest("Ро")
      testSuggest("Р")
      testSuggest("тур")
      testSuggest("ОАЭ")
      testSuggest("эмираты")
      testSuggest("бали")
    }

    "suggest partner regions" in {
      val partner = Partners.booking
      val pTree = data.partnerTrees(partner)
      val regionSample = pTree.extendedRegions.sample(50d / pTree.size)
      val partnerTreeIndex = PartnerTreeUtils.toMap(regionSample)
      val tree = new PartnerTree(partner, partnerTreeIndex)
      val suggester = new PartnerGeoSuggest(tree, identity, _ => 0d)

      def testSuggest(query: String): Unit = {
        val regions = suggester.suggest(query, 10)
        println(query + " = " + regions.map(_.name.ruName).mkString(", "))
      }
      testSuggest("Росс")
    }
  }
}
