package ru.yandex.market.adv.incut.controllers.file

import io.restassured.RestAssured.given
import net.javacrumbs.jsonunit.JsonAssert
import net.javacrumbs.jsonunit.core.Option
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.adv.incut.AbstractFunctionalTest
import ru.yandex.market.adv.incut.service.file.UuidFactory
import ru.yandex.market.adv.incut.utils.time.toInstantAtUtc3
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.net.URL
import java.time.Clock
import java.time.LocalDateTime
import java.time.Month
import java.util.*

class ApiFileUploadFunctionalTest(
    @Autowired
    private val clock: Clock,
    @Autowired
    private val mdsS3IncutClient: MdsS3Client,
    @Autowired
    private val uuidFactory: UuidFactory
) : AbstractFunctionalTest() {

    @Test
    @DbUnitDataSet(
        after = ["/ru/yandex/market/adv/incut/controllers/file/ApiFileUploadFunctionalTest/uploadImage/after.csv"]
    )
    fun `upload image`() {

        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 0, 0, 0
                ).toInstantAtUtc3()
            )

        Mockito.`when`(
            mdsS3IncutClient.getUrl(Mockito.any(ResourceLocation::class.java))
        ).thenReturn(URL("http://incut-public/autobanner/c5bb8200-a62d-4fb6-9440-c73098e5013b.png"))

        Mockito.`when`(uuidFactory.getUuid())
            .thenReturn(UUID.fromString("c5bb8200-a62d-4fb6-9440-c73098e5013b"))

        val actually: String = given()
            .`when`()
            .param("name", "white_square_600x600.png")
            .param("uid", 1186962236)
            .param("serviceKey", "AUTOBANNER")
            .multiPart("file", getFile("/uploadImage/white_square_600x600.png"))
            .post("${baseUrl()}/api/v1/files/upload/multipart")
            .body
            .asString()

        val expected = getStringResource("/uploadImage/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

    }

    @Test
    fun `test validate file width test` () {
        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 0, 0, 0
                ).toInstantAtUtc3()
            )

        Mockito.`when`(
            mdsS3IncutClient.getUrl(Mockito.any(ResourceLocation::class.java))
        ).thenReturn(URL("http://image.jpg"))

        Mockito.`when`(uuidFactory.getUuid())
            .thenReturn(UUID.fromString("c5bb8200-a62d-4fb6-9440-c73098e5013b"))

        val actually = given()
            .`when`()
            .param("name", "image.png")
            .param("uid", 1186962236)
            .param("serviceKey", "AUTOBANNER")
            .multiPart("file", getFile("image.png"))
            .post("${baseUrl()}/api/v1/files/upload/multipart")
            .body
            .asString()

        val expected = getStringResource("/validateFileWidthTest/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

    }

    @Test
    fun `test upload not image`() {

        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 0, 0, 0
                ).toInstantAtUtc3()
            )

        Mockito.`when`(
            mdsS3IncutClient.getUrl(Mockito.any(ResourceLocation::class.java))
        ).thenReturn(URL("http://c5bb8200-a62d-4fb6-9440-c73098e5013b/test_file.txt"))

        Mockito.`when`(uuidFactory.getUuid())
            .thenReturn(UUID.fromString("c5bb8200-a62d-4fb6-9440-c73098e5013b"))

        val actually = given()
            .`when`()
            .param("name", "test_file.txt")
            .param("uid", 1186962236)
            .param("serviceKey", "AUTOBANNER")
            .multiPart("file", getFile("/testUploadNotImage/test_file.txt"))
            .post("${baseUrl()}/api/v1/files/upload/multipart")
            .body
            .asString()

        val expected = getStringResource("/testUploadNotImage/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }


    @Test
    fun `test autobanner upload large file`() {

        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 0, 0, 0
                ).toInstantAtUtc3()
            )

        Mockito.`when`(
            mdsS3IncutClient.getUrl(Mockito.any(ResourceLocation::class.java))
        ).thenReturn(URL("http://c5bb8200-a62d-4fb6-9440-c73098e5013b/gt_1mb.jpg"))

        Mockito.`when`(uuidFactory.getUuid())
            .thenReturn(UUID.fromString("c5bb8200-a62d-4fb6-9440-c73098e5013b"))

        val actually = given()
            .`when`()
            .param("name", "gt_1mb.jpg")
            .param("uid", 1186962236)
            .param("serviceKey", "AUTOBANNER")
            .multiPart("file", getFile("/testAutobannerUploadLargeFile/gt_1mb.jpg"))
            .post("${baseUrl()}/api/v1/files/upload/multipart")
            .body
            .asString()

        val expected = getStringResource("/testAutobannerUploadLargeFile/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        after = ["/ru/yandex/market/adv/incut/controllers/file/ApiFileUploadFunctionalTest/testTransparentPngImage/after.csv"]
    )
    fun `test transparent png image`() {

        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 0, 0, 0
                ).toInstantAtUtc3()
            )

        val actually = given()
            .`when`()
            .param("name", "transparent_rectangle.png")
            .param("uid", 1)
            .param("serviceKey", "AUTOBANNER")
            .multiPart("file", getFile("/testTransparentPngImage/transparent_rectangle.png"))
            .post("${baseUrl()}/api/v1/files/upload/multipart")
            .body
            .asString()

        val expected = getStringResource("/testTransparentPngImage/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )
    }

    @Test
    @DbUnitDataSet(
        after = ["/ru/yandex/market/adv/incut/controllers/file/ApiFileUploadFunctionalTest/testOpaqueJpegImage/after.csv"]
    )
    fun `test opaque jpeg image`() {

        Mockito.`when`(clock.instant())
            .thenReturn(
                LocalDateTime.of(
                    2021, Month.DECEMBER,
                    17, 0, 0, 0
                ).toInstantAtUtc3()
            )

        Mockito.`when`(
            mdsS3IncutClient.getUrl(Mockito.any(ResourceLocation::class.java))
        ).thenReturn(URL("http://incut-public/autobanner/c5bb8200-a62d-4fb6-9440-c73098e5013b.jpg"))

        Mockito.`when`(uuidFactory.getUuid())
            .thenReturn(UUID.fromString("c5bb8200-a62d-4fb6-9440-c73098e5013b"))

        val actually: String = given()
            .`when`()
            .param("name", "opaque.jpg")
            .param("uid", 1186962236)
            .param("serviceKey", "AUTOBANNER")
            .multiPart("file", getFile("/testOpaqueJpegImage/opaque.jpg"))
            .post("${baseUrl()}/api/v1/files/upload/multipart")
            .body
            .asString()

        val expected = getStringResource("/testOpaqueJpegImage/expected.json")

        JsonAssert.assertJsonEquals(
            expected,
            actually,
            JsonAssert.`when`(Option.IGNORING_ARRAY_ORDER)
        )

    }


}
