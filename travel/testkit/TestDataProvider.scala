package ru.yandex.tours.testkit

import java.io.File

import org.joda.time.LocalDate
import ru.yandex.tours.avia.{Airports, AviaAirportRecommendations, AviaCities}
import ru.yandex.tours.backa.BackaPermalinks
import ru.yandex.tours.direction._
import ru.yandex.tours.geo.base.region.Tree
import ru.yandex.tours.geo.mapping.GeoMappingHolder
import ru.yandex.tours.geo.partners.{PartnerRegionHotelCounts, PartnerRegionsDef}
import ru.yandex.tours.geo.{Departures, Iso2Country}
import ru.yandex.tours.hotels._
import ru.yandex.tours.model.BaseModel._
import ru.yandex.tours.model.hotels.Features.{BooleanFeature, BooleanFeatureValue, EnumFeature, EnumFeatureValue}
import ru.yandex.tours.model.hotels.HotelsHolder.HotelType
import ru.yandex.tours.model.hotels._
import ru.yandex.tours.model.image.ImageProviders
import ru.yandex.tours.model.search.SearchProducts._
import ru.yandex.tours.model.search.SearchResults.{HotelSearchResult, OfferSearchResult, ResultInfo, SearchProgress}
import ru.yandex.tours.model.search.{EmptySearchFilter, HotelSearchRequest, OfferSearchRequest}
import ru.yandex.tours.model.util.proto
import ru.yandex.tours.model.{Image, Languages, LocalizedString, TourOperator}
import ru.yandex.tours.operators.{HotelProviders, TourOperators}
import ru.yandex.tours.ota.OnlineTourAgencies
import ru.yandex.tours.parsing.PansionUnifier
import ru.yandex.tours.resorts.SkiResorts
import ru.yandex.tours.search.DefaultRequestGenerator
import ru.yandex.tours.util.Collections._
import ru.yandex.tours.util.Randoms
import ru.yandex.tours.util.collections.SimpleBitSet
import ru.yandex.tours.util.file._
import ru.yandex.tours.util.lang.Dates._
import shapeless._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.util.Random

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 06.05.15
 */

object TestDataProvider {

  lazy val root = new File("tours-data/data")

  lazy val geoMapping = GeoMappingHolder.parse(
    cityFile = root / "cities.tsv",
    countryFile = root / "countries.tsv",
    departureFile = root / "departures.tsv",
    airportFile = root / "airports.tsv"
  )

  lazy val treeHolder = Tree.fromFile(root / "regions.xml")

  lazy val hotelRatings = HotelRatings.fromFile(root / "hotels_ratings")
  lazy val hotelsIndex = {
    // generated by ru.yandex.tours.tools.hotels.TestIndexBuilderTool
    YoctoHotelsIndex.fromFile(root / "test_hotels_index", hotelRatings)
  }

  lazy val tourOperators = TourOperators.fromFile(root / "operators")
  lazy val hotelProviders = HotelProviders.fromFile(root / "hotel_providers")

  lazy val pansionUnifier = PansionUnifier.fromFile(root / "pansions")

  lazy val onlineAgencies = OnlineTourAgencies.fromFile(root / "online_agencies")

  lazy val departures = Departures.fromFile(root / "departure_stats.tsv", treeHolder :: geoMapping :: HNil)
  lazy val skiResorts = SkiResorts.fromFile(root / "ski_resorts.json")
  lazy val directions = Directions.fromFile(root / "directions", treeHolder :: geoMapping :: skiResorts :: HNil)

  lazy val directionsSimilarity = DirectionsSimilarity.fromFile(root / "geo_similarity.tsv")

  lazy val countryPriorities =
    CountryPriorities.fromFile(new File("util-heavy/data/suggest/new_countries_priorities_for_suggest.tsv"))
  lazy val resortPriorities =
    ResortPriorities.fromFile(new File("util-heavy/data/suggest/new_resorts_priorities_for_suggest.tsv"))

  lazy val directionsStats = DirectionsStats.from(
    countryPriorities ::
    resortPriorities ::
    hotelsIndex :: treeHolder :: geoMapping :: directionsSimilarity :: HNil
  )

  lazy val aviaAirports = Airports.fromFile(
    root / "avia_airports.tsv"
  )

  lazy val aviaCities = AviaCities.fromFile(
    root / "avia_cities.tsv"
  )

  lazy val airportRecommendations = AviaAirportRecommendations.fromFile(
    root / "airport_recommendations.tsv",
    treeHolder :: aviaCities :: HNil
  )


  lazy val hotelsVideo = HotelsVideo.fromFile(root / "hotels_video")
  lazy val hotelsPanoramas = HotelsPanoramas.fromFile(root / "hotels_panoramas.tsv", hotelsIndex :: HNil)
  lazy val backaPermalinks = BackaPermalinks.fromFile(root / "backa_permalinks.tsv")

  lazy val iso2country = Iso2Country.fromFile(root / "iso2country.tsv", treeHolder :: HNil)

  lazy val partnerRegionHotelCounts = PartnerRegionHotelCounts.fromFile(root / "partner_region_hotel_counts.tsv")

  lazy val partnerTrees = PartnerRegionsDef.fromFile(root / "partner_regions_v2.tsv", partnerRegionHotelCounts :: HNil)
}

class TestDataProvider(staticGeoMapping: Option[GeoMappingHolder] = None,
                       staticHotelIndex: Option[MemoryHotelsIndex] = None,
                       staticTree: Option[Tree] = None,
                       staticOperators: Option[TourOperators] = None) extends Randoms {

  override val random = new Random(0)
  lazy val tourOperators = staticOperators.getOrElse(TestDataProvider.tourOperators)
  lazy val geoMapping: GeoMappingHolder = staticGeoMapping.getOrElse(TestDataProvider.geoMapping)
  lazy val regionTree = staticTree.getOrElse(TestDataProvider.treeHolder)
  lazy val hotelsIndex = staticHotelIndex.getOrElse(TestDataProvider.hotelsIndex)
  lazy val hotelRatings = TestDataProvider.hotelRatings
  lazy val departures = TestDataProvider.departures
  lazy val skiResorts = TestDataProvider.skiResorts
  lazy val directions = TestDataProvider.directions
  lazy val pansions = TestDataProvider.pansionUnifier
  lazy val hotelProviders = TestDataProvider.hotelProviders
  lazy val hotelsVideo = TestDataProvider.hotelsVideo
  lazy val hotelsPanoramas = TestDataProvider.hotelsPanoramas
  lazy val backaPermalinks = TestDataProvider.backaPermalinks
  lazy val directionsStats = TestDataProvider.directionsStats
  lazy val aviaAirports = TestDataProvider.aviaAirports
  lazy val aviaCities = TestDataProvider.aviaCities
  lazy val airportRecommendations = TestDataProvider.airportRecommendations
  lazy val iso2country = TestDataProvider.iso2country
  lazy val partnerTrees = TestDataProvider.partnerTrees

  private def regionChain(regionId: Int) = {
    val tree = regionTree
    for {
      region <- tree.region(regionId).toSeq
      r <- tree.pathToRoot(region)
      if geoMapping.isKnownDestination(r.id)
    } yield r.id
  }

  private lazy val hotelGeoMap: Map[Int, List[Hotel]] = {
    for {
      hotel <- hotelsIndex.hotels
      regionId <- regionChain(hotel.geoId)
    } yield regionId -> hotel
  }.toSeq.toMultiMap

  private val operatorWithPansion = {
    for {
      operator <- tourOperators.getAll
      pansion <- Pansion.values().toSeq
    } yield operator -> pansion
  }

  def randomRequest: HotelSearchRequest = {
    HotelSearchRequest(
      from = geoMapping.departuresGeoIds.randomElement,
      to = hotelGeoMap.keys.randomElement,
      nights = (5 until 20).randomElement,
      when = LocalDate.now().plusDays(random.nextInt(365)),
      ages = DefaultRequestGenerator.DEFAULT_AGES,
      flexWhen = false,
      flexNights = false,
      currency = Currency.RUB,
      lang = Languages.ru,
      filter = EmptySearchFilter
    )
  }

  def randomTourRequest: OfferSearchRequest = {
    val hotelRequest = randomRequest
    val hotelId = hotelGeoMap(hotelRequest.to).randomElement.id
    OfferSearchRequest(hotelRequest, hotelId)
  }

  def randomHotel(): Hotel = hotelsIndex.hotels.toSeq.randomElement
  def randomHotel(geoId: Int): Hotel = hotelGeoMap.getOrElse(geoId, List.empty).randomElement

  def randomSnippet: HotelSnippet = randomSnippets(randomRequest, 1).head

  def randomSnippets(request: HotelSearchRequest, count: Int, maxSources: Int = Int.MaxValue): Iterable[HotelSnippet] = {
    val hotels = hotelGeoMap.getOrElse(request.to, List.empty).sample(count)
    val basePrice = random.nextInt(50000) + 10000

    for (hotel <- hotels) yield {
      val prices =
        (for ((operator, pansion) <- operatorWithPansion.sample())
          yield (operator, pansion) -> (basePrice + random.nextInt(30000))).toMap

      val pansions =
        for ((pansion, prices) <- prices.toList.map(kv => kv._1._2 -> kv._2).toMultiMap)
          yield HotelSnippet.PricedPansion.newBuilder().setPansion(pansion).setPrice(prices.min).build

      val sources =
        prices.map(_._1._1).toSet.map({ operator: TourOperator =>
          Source.newBuilder()
            .setPartnerId(Partners.lt.id)
            .setOperatorId(operator.id)
            .build()
        }).take(maxSources)


      val nights = request.nightsRange.sample()
      val dates = request.dateRange

      HotelSnippet.newBuilder()
        .setHotelId(hotel.id)
        .addAllPansions(pansions.asJava)
        .setOfferCount(prices.size)
        .setPriceMin(prices.values.min)
        .setPriceMax(prices.values.max)
        .setNightsMin(nights.min)
        .setNightsMax(nights.max)
        .setDateMin(proto.fromLocalDate(dates.min))
        .setDateMax(proto.fromLocalDate(dates.max))
        .addAllSource(sources.asJava)
        .build
    }
  }

  def randomProgress: SearchProgress = {
    var failedOperators = Set.empty[TourOperator]
    var skippedOperators = Set.empty[TourOperator]
    var successOperators = Set.empty[TourOperator]
    tourOperators.getAll.zip(Stream.continually(random.nextInt(4))).foreach { case (operator, status) => status match {
      case 0 => failedOperators += operator
      case 1 => skippedOperators += operator
      case 2 =>
      case _ => successOperators += operator
    }}
    val complete = (successOperators ++ skippedOperators ++ failedOperators).size
    val all = tourOperators.getAll.size
    SearchProgress.newBuilder()
      .addAllOBSOLETEFailedOperators(asJavaIterable(failedOperators.map(_.id).map(Int.box)))
      .setOperatorFailedSet(SimpleBitSet(failedOperators.map(_.id)).packed)
      .setOperatorCompleteSet(SimpleBitSet((successOperators ++ skippedOperators ++ failedOperators).map(_.id)).packed)
      .setOperatorSkippedSet(SimpleBitSet(skippedOperators.map(_.id)).packed)
      .setOperatorCompleteCount(complete)
      .setOperatorFailedCount(failedOperators.size)
      .setOperatorSkippedCount(skippedOperators.size)
      .setOperatorTotalCount(all)
      .setIsFinished(all == complete)
      .build()
  }

  def randomTourResult(request: OfferSearchRequest): OfferSearchResult = OfferSearchResult.newBuilder()
    .setCreated(System.currentTimeMillis())
    .setHotelId(request.hotelId)
    .setResultInfo(ResultInfo.newBuilder().setIsFromLongCache(false))
    .setProgress(randomProgress)
    .setUpdated(System.currentTimeMillis())
    .addAllOffer(asJavaIterable(randomTours(request.hotelRequest, request.hotelId, random.nextInt(100))))
    .build()

  def randomHotelResult(request: HotelSearchRequest): HotelSearchResult = HotelSearchResult
    .newBuilder()
    .addAllHotelSnippet(asJavaIterable(randomSnippets(request, random.nextInt(100), 10)))
    .setCreated(System.currentTimeMillis())
    .setUpdated(System.currentTimeMillis())
    .setProgress(randomProgress)
    .setResultInfo(ResultInfo.newBuilder().setIsFromLongCache(false))
    .build

  def randomTour: Offer = {
    val request = randomRequest
    randomTours(request, randomHotel(request.to).id, 1).head
  }

  def randomTours(request: HotelSearchRequest, hotelId: Int, count: Int): Iterable[Offer] = {
    val basePrice = random.nextInt(50000) + 10000

    for ((operator, pansion) <- operatorWithPansion.sample(count)) yield {
      val source = Source.newBuilder()
        .setPartnerId(Partners.lt.id)
        .setOperatorId(operator.id)
        .build()

      val roomType = nextString(8)
      Offer.newBuilder()
        .setId(nextString(16))
        .setExternalId(nextString(16))
        .setSource(source)
        .setHotelId(hotelId)
        .setPansion(pansion)
        .setRawPansion(pansion.toString)
        .setRoomType(roomType)
        .setOriginalRoomCode(roomType)
        .setWithTransfer(random.nextBoolean())
        .setWithMedicalInsurance(random.nextBoolean())
        .setDate(proto.fromLocalDate(request.dateRange.randomElement))
        .setNights(request.nightsRange.randomElement)
        .setPrice(basePrice + random.nextInt(30000))
        .build
    }
  }

  def progress(total: Int, finished: Int, failed: Int = 0): SearchProgress = {
    SearchProgress.newBuilder()
      .setIsFinished(finished == total)
      .setOperatorTotalCount(total)
      .setOperatorCompleteCount(finished)
      .setOperatorFailedCount(failed)
      .setOperatorSkippedCount(0)
      .build
  }

  def hotelResult(request: HotelSearchRequest, count: Int, progress: SearchProgress): HotelSearchResult = {
    HotelSearchResult.newBuilder()
      .setCreated(System.currentTimeMillis())
      .setUpdated(System.currentTimeMillis())
      .setProgress(progress)
      .addAllHotelSnippet(randomSnippets(request, count).asJava)
      .setResultInfo(ResultInfo.newBuilder().setIsFromLongCache(false).build())
      .build
  }

  def tourResult(request: OfferSearchRequest, count: Int, progress: SearchProgress): OfferSearchResult = {
    OfferSearchResult.newBuilder()
      .setCreated(System.currentTimeMillis())
      .setUpdated(System.currentTimeMillis())
      .setHotelId(request.hotelId)
      .setProgress(progress)
      .addAllOffer(randomTours(request.hotelRequest, request.hotelId, count).asJava)
      .setResultInfo(ResultInfo.newBuilder().setIsFromLongCache(false).build())
      .build
  }
}
