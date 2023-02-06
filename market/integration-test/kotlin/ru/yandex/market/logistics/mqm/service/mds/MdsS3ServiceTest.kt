package ru.yandex.market.logistics.mqm.service.mds

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.net.URL
import javax.persistence.EntityManager
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.invocation.InvocationOnMock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory
import ru.yandex.market.common.mds.s3.spring.content.provider.MultipartFileContentProvider
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.mds.MdsFile

internal class MdsS3ServiceTest: AbstractContextualTest() {

    @Autowired
    private lateinit var mdsS3Client: MdsS3Client

    @Autowired
    private lateinit var resourceLocationFactory: ResourceLocationFactory

    @Autowired
    private lateinit var mdsFileService: MdsFileService

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun init() {
        whenever(mdsS3Client.getUrl(any())).thenReturn(TEST_URL)
        whenever(resourceLocationFactory.createLocation(eq(FILE_KEY.toString()))).thenReturn(RESOURCE_LOCATION)
    }

    @AfterEach
    fun resetBdSeq() {
        resetSequenceIdGeneratorCache(MdsFile::class.java, entityManager)
    }

    @Test
    @DisplayName("Проверка загрузки файла в Mds хранилище")
    @ExpectedDatabase(
        value = "/service/mds/single_mds_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testFileUpload() {
        val actualFileKey = mdsFileService.uploadFile(createMultipartFile())
        val captor = argumentCaptor<MultipartFileContentProvider>()
        assertSoftly {
            actualFileKey shouldBe FILE_KEY
            verify(mdsS3Client).upload(eq(RESOURCE_LOCATION), captor.capture())
            verify(mdsS3Client).getUrl(RESOURCE_LOCATION)
            verifyNoMoreInteractions(mdsS3Client)
            val content = IOUtils.toByteArray(captor.firstValue.inputStream)
            content shouldBe TEST_FILE_CONTENT
        }
    }

    @Test
    @DisplayName("Ошибка во время загрузки файла в Mds хранилище")
    @ExpectedDatabase(
        value = "/service/mds/no_mds_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testUploadInvalidFile() {
        doThrow(IllegalArgumentException("mock error during upload"))
            .whenever(mdsS3Client)
            .upload(any(), any())
        val exception = assertThrows<RuntimeException> { mdsFileService.uploadFile(createMultipartFile()) }
        exception.cause?.javaClass shouldBe IllegalArgumentException::class.java
    }

    @Test
    @DatabaseSetup("/service/mds/single_mds_file.xml")
    @ExpectedDatabase(
        value = "/service/mds/downloaded_mds_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testDownloadFile() {
        whenever(mdsS3Client.download(eq(RESOURCE_LOCATION), any<StreamCopyContentConsumer<OutputStream>>()))
            .thenAnswer { invocation: InvocationOnMock ->
                val argument = invocation.getArgument<StreamCopyContentConsumer<OutputStream>>(1)
                return@thenAnswer argument.consume(ByteArrayInputStream(TEST_FILE_CONTENT))
            }
        val downloadFileStream = mdsFileService.downloadFile(FILE_KEY)
        downloadFileStream.toByteArray() shouldBe TEST_FILE_CONTENT
    }

    private fun createMultipartFile(): MockMultipartFile {
        return MockMultipartFile(
            "test.txt",
            "original-test.txt",
            "text/plain",
            TEST_FILE_CONTENT
        )
    }

    companion object {
        private const val BUCKET_NAME = "test-bucket-name"
        private const val FILE_KEY = 1L
        private val TEST_FILE_CONTENT = "Some test".toByteArray()
        private val TEST_URL = URL("http://localhost:8080/$BUCKET_NAME-$FILE_KEY.xml")
        private val RESOURCE_LOCATION = ResourceLocation.create(BUCKET_NAME, FILE_KEY.toString())
    }
}
