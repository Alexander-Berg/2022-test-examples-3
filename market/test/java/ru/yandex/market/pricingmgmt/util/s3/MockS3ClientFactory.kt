package ru.yandex.market.pricingmgmt.util.s3

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import org.mockito.Mockito

class MockS3ClientFactory : S3ClientFactory {

    override val s3Client: AmazonS3 = Mockito.mock(AmazonS3Client::class.java)
}
