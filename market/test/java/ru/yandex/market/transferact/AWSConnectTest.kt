package ru.yandex.market.transferact

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.TestPropertySource
import java.io.File

@TestPropertySource(
    properties = [
        "external.market-transfer-act.url=https://s3.mds.yandex.net",
        "external.market-transfer-act.region = ru-central1",
        "external.market-transfer-act.access-key-id = <your-access-key-id>",
        "external.market-transfer-act.secret-key = <your-secret-key>>",
        "external.market-transfer-act.bucket-name = market-transfer-act-production-document",
    ]
)
class AWSConnectTest {

    private val S3_SOCKET_TIMEOUT = 30 * 1000

    private val S3_REQUEST_TIMEOUT = 30 * 1000

    private val S3_CLIENT_EXECUTION_TIMEOUT = 45 * 1000

    @Value("\${external.market-transfer-act.url}")
    private lateinit var s3Endpoint: String

    @Value("\${external.market-transfer-act.region}")
    private lateinit var s3Region: String

    @Value("\${external.market-transfer-act.access-key-id:defaultAccessKey}")
    private lateinit var accessKeyId: String

    @Value("\${external.market-transfer-act.secret-key:defaultSecretKey}")
    private lateinit var secretKey: String

    @Value("\${external.market-transfer-act.bucket-name}")
    private lateinit var bucketName: String

    lateinit var s3Client: AmazonS3

    @BeforeEach
    fun setUp() {
        s3Client = AmazonS3ClientBuilder.standard()
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials(accessKeyId, secretKey)))
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3Endpoint, s3Region))
            .withClientConfiguration(
                ClientConfiguration()
                    .withSocketTimeout(S3_SOCKET_TIMEOUT)
                    .withRequestTimeout(S3_REQUEST_TIMEOUT)
                    .withClientExecutionTimeout(S3_CLIENT_EXECUTION_TIMEOUT)
            ).build()
    }

    @Test
    @Disabled
    fun `Try to upload file to the bucket`() {
        val myFile = File("null")
        myFile.writeText("Test data")
        s3Client.putObject(bucketName, "dev/null", myFile)
    }
}
