package ru.yandex.travel.hotels.tugc.grpc

import io.grpc.Context
import io.grpc.StatusRuntimeException
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.testing.GrpcCleanupRule
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.flywaydb.test.annotation.FlywayTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner

import ru.yandex.travel.credentials.*
import ru.yandex.travel.grpc.interceptors.UserCredentialsClientInterceptor
import ru.yandex.travel.grpc.interceptors.UserCredentialsServerInterceptor
import ru.yandex.travel.hotels.cluster_permalinks.ClusterPermalinkDataProvider
import ru.yandex.travel.hotels.common.Permalink
import ru.yandex.travel.hotels.proto.tugc_service.*
import ru.yandex.travel.hotels.tugc.entities.Favorite
import ru.yandex.travel.hotels.tugc.storage.FavoriteStorage

@AutoConfigureEmbeddedDatabase
@FlywayTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["tugc-app.max-favorite-hotels=6"]
)
@RunWith(value = SpringRunner::class)
@ActiveProfiles("test")
open class FavoriteGrpcServiceTest {
    private var yandexUid = 100L
    private var puid = 1L
    private val DEFAULT_TEST_LOGIN = "login"
    private val DEFAULT_SESSION_KEY = "qwerty"

    @Autowired
    private val favoriteStorage: FavoriteStorage? = null

    @Autowired
    private val favoritesGrpcService: FavoriteGrpcService? = null

    @Autowired
    private val clusterPermalinkDataProvider: ClusterPermalinkDataProvider? = null

    @Rule
    @JvmField
    val cleanupRule = GrpcCleanupRule()

    private var rootTestContext: Context? = null

    @Before
    fun init() {
        rootTestContext = Context.current()
        favoriteStorage?.clear()
    }

    @After
    fun tearDown() {
        Context.current().detach(rootTestContext)
    }

    @Test
    fun `Should return favorite hotels for login user`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorites = listOf(
            favoriteStorage!!.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid),
            favoriteStorage.create(puid = null, yuid = uc.yandexUid)
        )

        mockFindClusterPermalink(favorites)

        val req = TGetFavoriteHotelsReq.newBuilder()
        val rsp = clientStub.getFavoriteHotels(req.build())

        assertThat(rsp.permalinksList).isEqualTo(favorites.asReversed().map { it.permalink })
    }

    @Test
    fun `Should return favorite hotels with cluster permalink`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorite = favoriteStorage!!.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid)
        val clusterPermalink = favorite.permalink + 100

        mockFindClusterPermalink(favorite.permalink, clusterPermalink)

        val req = TGetFavoriteHotelsReq.newBuilder()
        val rsp = clientStub.getFavoriteHotels(req.build())

        assertThat(rsp.permalinksList).isEqualTo(listOf(clusterPermalink))
    }

    @Test
    fun `Should return favorite hotels with actual permalink`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorite = favoriteStorage!!.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid)

        mockFindClusterPermalink(favorite.permalink, null)

        val req = TGetFavoriteHotelsReq.newBuilder()
        val rsp = clientStub.getFavoriteHotels(req.build())

        assertThat(rsp.permalinksList).isEqualTo(emptyList<Int>())
    }

    @Test
    fun `Should return favorite hotels for unknown user`() {
        mockUnknownUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorite = favoriteStorage!!.create(yuid = uc.yandexUid)

        mockFindClusterPermalink(listOf(favorite))

        val req = TGetFavoriteHotelsReq.newBuilder()
        val rsp = clientStub.getFavoriteHotels(req.build())

        assertThat(rsp.permalinksList).isEqualTo(listOf(favorite.permalink))
    }

    @Test
    fun `Should return favorite hotels with filter by geoId`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorite = favoriteStorage!!.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid)

        mockFindClusterPermalink(listOf(favorite))

        val req = TGetFavoriteHotelsReq.newBuilder()
        req.geoId = favorite.geoId
        val rsp = clientStub.getFavoriteHotels(req.build())

        assertThat(rsp.permalinksList).isEqualTo(listOf(favorite.permalink))
    }

    @Test
    fun `Should add favorite hotel for login user`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val permalink = favoriteStorage!!.nextPermalink
        val geoId = favoriteStorage.nextGeoId

        mockFindClusterPermalink(permalink, permalink)

        val req = TAddFavoriteHotelReq.newBuilder()
            .setPermalink(permalink)
            .setGeoId(geoId)
        clientStub.addFavoriteHotel(req.build())

        val favorites = favoriteStorage.findByPermalink(permalink)
        val expectedFavorite = Favorite(uc.parsedPassportIdOrNull, null, permalink, geoId, favorites[0].createdAt)

        assertThat(favorites.size).isEqualTo(1)
        assertThat(favorites[0]).isEqualTo(expectedFavorite)
    }

    @Test
    fun `Should add favorite hotel with cluster permalink`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val permalink = favoriteStorage!!.nextPermalink
        val geoId = favoriteStorage.nextGeoId
        val clusterPermalink = permalink + 100

        mockFindClusterPermalink(permalink, clusterPermalink)

        val req = TAddFavoriteHotelReq.newBuilder()
            .setPermalink(permalink)
            .setGeoId(geoId)
        clientStub.addFavoriteHotel(req.build())

        val favorites = favoriteStorage.findByPermalink(clusterPermalink)
        val expectedFavorite = Favorite(uc.parsedPassportIdOrNull, null, clusterPermalink, geoId, favorites[0].createdAt)

        assertThat(favorites.size).isEqualTo(1)
        assertThat(favorites[0]).isEqualTo(expectedFavorite)
    }

    @Test
    fun `Should add favorite hotel for unknown user`() {
        mockUnknownUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val permalink = favoriteStorage!!.nextPermalink
        val geoId = favoriteStorage.nextGeoId

        mockFindClusterPermalink(permalink, permalink)

        val req = TAddFavoriteHotelReq.newBuilder()
            .setPermalink(permalink)
            .setGeoId(geoId)
        clientStub.addFavoriteHotel(req.build())

        val favorites = favoriteStorage.findByPermalink(permalink)
        val expectedFavorite = Favorite(null, uc.yandexUid, permalink, geoId, favorites[0].createdAt)

        assertThat(favorites.size).isEqualTo(1)
        assertThat(favorites[0]).isEqualTo(expectedFavorite)
    }

    @Test
    fun `Should add favorite hotel idempotently`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val permalink = favoriteStorage!!.nextPermalink
        val geoId = favoriteStorage.nextGeoId

        mockFindClusterPermalink(permalink, permalink)

        val req = TAddFavoriteHotelReq.newBuilder()
            .setPermalink(permalink)
            .setGeoId(geoId)
        clientStub.addFavoriteHotel(req.build())
        clientStub.addFavoriteHotel(req.build())

        val favorites = favoriteStorage.findByPermalink(permalink)
        val expectedFavorite = Favorite(uc.parsedPassportIdOrNull, null, permalink, geoId, favorites[0].createdAt)

        assertThat(favorites.size).isEqualTo(1)
        assertThat(favorites[0]).isEqualTo(expectedFavorite)
    }

    @Test
    fun `Should return error when favorite hotel limit exceeded`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        for (i in 0..3) {
            val favorite = favoriteStorage!!.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid)
            mockFindClusterPermalink(favorite.permalink, favorite.permalink)
        }

        for (i in 0..1) {
            val favorite = favoriteStorage!!.create(puid = null, yuid = uc.yandexUid)
            mockFindClusterPermalink(favorite.permalink, favorite.permalink)
        }

        val permalink = favoriteStorage!!.nextPermalink
        val geoId = favoriteStorage.nextGeoId

        mockFindClusterPermalink(permalink, permalink)

        val req = TAddFavoriteHotelReq.newBuilder()
            .setPermalink(permalink)
            .setGeoId(geoId)

        assertThatThrownBy { clientStub.addFavoriteHotel(req.build()) }
            .isInstanceOf(StatusRuntimeException::class.java)
            .hasMessage("FAILED_PRECONDITION: Favorite hotel limit exceeded for user")
    }

    @Test
    fun `Should remove favorite hotels by permalink for login user`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val permalink = favoriteStorage!!.nextPermalink
        val geoId = favoriteStorage.nextGeoId

        favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoId, permalink = permalink)
        favoriteStorage.create(puid = null, yuid = uc.yandexUid, geoId = geoId, permalink = permalink)

        mockFindAllPermalinksOfCluster(permalink, listOf(permalink))

        val req = TRemoveFavoriteHotelsReq.newBuilder()
            .setPermalink(permalink)
        clientStub.removeFavoriteHotels(req.build())

        val favorites = favoriteStorage.findByPermalink(permalink)

        assertThat(favorites.size).isEqualTo(0)
    }

    @Test
    fun `Should remove favorite hotels by permalink for unknown user`() {
        mockUnknownUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val permalink = favoriteStorage!!.nextPermalink
        val geoId = favoriteStorage.nextGeoId
        val userPuid = puid++

        favoriteStorage.create(puid = userPuid, yuid = uc.yandexUid, geoId = geoId, permalink = permalink)
        favoriteStorage.create(puid = null, yuid = uc.yandexUid, geoId = geoId, permalink = permalink)

        mockFindAllPermalinksOfCluster(permalink, listOf(permalink))

        val req = TRemoveFavoriteHotelsReq.newBuilder()
            .setPermalink(permalink)
        clientStub.removeFavoriteHotels(req.build())

        val favorites = favoriteStorage.findByPermalink(permalink)
        val expectedFavorite = Favorite(userPuid, null, permalink, geoId, favorites[0].createdAt)

        assertThat(favorites.size).isEqualTo(1)
        assertThat(favorites[0]).isEqualTo(expectedFavorite)
    }

    @Test
    fun `Should remove all cluster hotel permalinks by permalink`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val geoId = favoriteStorage!!.nextGeoId

        val favorites = listOf(
            favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoId),
            favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoId)
        )

        mockFindAllPermalinksOfCluster(favorites[0].permalink, favorites.map { it.permalink })

        val req = TRemoveFavoriteHotelsReq.newBuilder()
            .setPermalink(favorites[0].permalink)
        clientStub.removeFavoriteHotels(req.build())

        val actual = favoriteStorage.findByUser(uc.parsedPassportIdOrNull, uc.yandexUid)

        assertThat(actual.size).isEqualTo(0)
    }

    @Test
    fun `Should remove favorite hotels by geoId`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val geoId = favoriteStorage!!.nextGeoId

        favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoId)
        favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoId)

        val req = TRemoveFavoriteHotelsReq.newBuilder()
            .setGeoId(geoId)
        clientStub.removeFavoriteHotels(req.build())

        val favorites = favoriteStorage.findByUser(uc.parsedPassportIdOrNull, uc.yandexUid)

        assertThat(favorites.size).isEqualTo(0)
    }

    @Test
    fun `Should return geo ids for login user`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val geoIds = listOf(favoriteStorage!!.nextGeoId, favoriteStorage.nextGeoId)

        val favorites = listOf(
            favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoIds[0]),
            favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoIds[0]),
            favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoIds[1])
        )

        mockFindClusterPermalink(favorites)

        val req = TGetGeoIdsReq.newBuilder()
        val rsp = clientStub.getGeoIds(req.build())

        assertThat(rsp.geoIdsList).isEqualTo(geoIds.asReversed())
    }

    @Test
    fun `Should return geo ids for unknown user`() {
        mockUnknownUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val geoIds = listOf(favoriteStorage!!.nextGeoId, favoriteStorage.nextGeoId)

        val favorites = listOf(
            favoriteStorage.create(puid = null, yuid = uc.yandexUid, geoId = geoIds[0]),
            favoriteStorage.create(puid = null, yuid = uc.yandexUid, geoId = geoIds[0]),
            favoriteStorage.create(puid = null, yuid = uc.yandexUid, geoId = geoIds[1])
        )

        mockFindClusterPermalink(favorites)

        val req = TGetGeoIdsReq.newBuilder()
        val rsp = clientStub.getGeoIds(req.build())

        assertThat(rsp.geoIdsList).isEqualTo(geoIds.asReversed())
    }

    @Test
    fun `Should return geo ids by actual permalinks`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val geoIds = listOf(favoriteStorage!!.nextGeoId, favoriteStorage.nextGeoId)

        val favorite = favoriteStorage.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid, geoId = geoIds[0])

        mockFindClusterPermalink(favorite.permalink, null)

        val req = TGetGeoIdsReq.newBuilder()
        val rsp = clientStub.getGeoIds(req.build())

        assertThat(rsp.geoIdsList).isEqualTo(emptyList<Int>())
    }

    @Test
    fun `Should return hotel favorite infos for login user`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorite = favoriteStorage!!.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid)
        val notExistFavoritePermalink = favoriteStorage.nextPermalink

        mockFindAllPermalinksOfCluster(favorite, listOf(favorite))
        mockFindAllPermalinksOfCluster(notExistFavoritePermalink, listOf(notExistFavoritePermalink))

        val req = TGetHotelFavoriteInfosReq.newBuilder()
            .addAllPermalinks(listOf(favorite.permalink, notExistFavoritePermalink))
        val rsp = clientStub.getHotelFavoriteInfos(req.build())
        val expected = listOf(
            TGetHotelFavoriteInfosRsp.TPermalinkInfo.newBuilder()
                .setPermalink(favorite.permalink)
                .setIsFavorite(true)
                .build(),
            TGetHotelFavoriteInfosRsp.TPermalinkInfo.newBuilder()
                .setPermalink(notExistFavoritePermalink)
                .setIsFavorite(false)
                .build()
        )

        assertThat(rsp.permalinkInfosList).isEqualTo(expected)
    }

    @Test
    fun `Should return hotel favorite infos by cluster permalinks`() {
        mockDefaultUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorite = favoriteStorage!!.create(puid = uc.parsedPassportIdOrNull, yuid = uc.yandexUid)
        val clusterPermalinks = listOf(favorite.permalink, favorite.permalink + 100)

        mockFindAllPermalinksOfCluster(clusterPermalinks[1], clusterPermalinks)

        val req = TGetHotelFavoriteInfosReq.newBuilder()
            .addAllPermalinks(listOf(clusterPermalinks[1]))
        val rsp = clientStub.getHotelFavoriteInfos(req.build())
        val expected = listOf(
            TGetHotelFavoriteInfosRsp.TPermalinkInfo.newBuilder()
                .setPermalink(clusterPermalinks[1])
                .setIsFavorite(true)
                .build()
        )

        assertThat(rsp.permalinkInfosList).isEqualTo(expected)
    }

    @Test
    fun `Should return hotel favorite infos for unknown user`() {
        mockUnknownUser()
        val uc = UserCredentials.get()
        val clientStub = createServerAndBlockingStub()

        val favorite = favoriteStorage!!.create(puid = null, yuid = uc.yandexUid)
        val notExistFavoritePermalink = favoriteStorage.nextPermalink

        mockFindAllPermalinksOfCluster(favorite, listOf(favorite))
        mockFindAllPermalinksOfCluster(notExistFavoritePermalink, listOf(notExistFavoritePermalink))

        val req = TGetHotelFavoriteInfosReq.newBuilder()
            .addAllPermalinks(listOf(favorite.permalink, notExistFavoritePermalink))
        val rsp = clientStub.getHotelFavoriteInfos(req.build())
        val expected = listOf(
            TGetHotelFavoriteInfosRsp.TPermalinkInfo.newBuilder()
                .setPermalink(favorite.permalink)
                .setIsFavorite(true)
                .build(),
            TGetHotelFavoriteInfosRsp.TPermalinkInfo.newBuilder()
                .setPermalink(notExistFavoritePermalink)
                .setIsFavorite(false)
                .build()
        )

        assertThat(rsp.permalinkInfosList).isEqualTo(expected)
    }

    private fun createServerAndBlockingStub(): FavoriteInterfaceV1Grpc.FavoriteInterfaceV1BlockingStub {
        return try {
            val serverName = InProcessServerBuilder.generateName()
            cleanupRule.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(favoritesGrpcService)
                .intercept(UserCredentialsServerInterceptor(
                    UserCredentialsBuilder(),
                    UserCredentialsPassportExtractorStubImpl(),
                    UserCredentialsValidator(UserCredentialsAuthValidatorStubImpl())
                ))
                .build()
                .start()
            )
            FavoriteInterfaceV1Grpc.newBlockingStub(
                cleanupRule.register(InProcessChannelBuilder.forName(serverName)
                    .intercept(UserCredentialsClientInterceptor())
                    .build()
                )
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun mockFindClusterPermalink(favorites: List<Favorite>) {
        favorites.forEach {
            Mockito.`when`(clusterPermalinkDataProvider!!.findClusterPermalink(Permalink.of(it.permalink))).thenReturn(Permalink.of(it.permalink))
        }
    }

    private fun mockFindClusterPermalink(permalink: Long, clusterPermalink: Long?) {
        Mockito.`when`(clusterPermalinkDataProvider!!.findClusterPermalink(Permalink.of(permalink)))
            .thenReturn(clusterPermalink?.let { Permalink.of(clusterPermalink) })
    }

    private fun mockFindAllPermalinksOfCluster(permalink: Long, clusterPermalinks: List<Long>) {
        Mockito.`when`(clusterPermalinkDataProvider!!.findAllPermalinksOfCluster(Permalink.of(permalink)))
            .thenReturn(clusterPermalinks.map { Permalink.of(it) })
    }

    private fun mockFindAllPermalinksOfCluster(favorite: Favorite, favoritesOfCluster: List<Favorite>) {
        Mockito.`when`(clusterPermalinkDataProvider!!.findAllPermalinksOfCluster(Permalink.of(favorite.permalink)))
            .thenReturn(favoritesOfCluster.map { Permalink.of(it.permalink) })
    }

    private fun mockDefaultUser() {
        bindCredentials(UserCredentials(null, "${yandexUid++}", "${puid++}", DEFAULT_TEST_LOGIN,
            null, "127.0.0.1", false, false))
    }

    private fun mockUnknownUser() {
        bindCredentials(UserCredentials(DEFAULT_SESSION_KEY, "${yandexUid++}", null, null, null, "127.0.0.1", false, false))
    }

    private fun bindCredentials(credentials: UserCredentials) {
        Context.current().withValue(UserCredentials.KEY, credentials).attach()
    }
}
