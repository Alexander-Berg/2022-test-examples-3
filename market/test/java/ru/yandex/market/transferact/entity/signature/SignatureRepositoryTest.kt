package ru.yandex.market.transferact.entity.signature

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.utils.TestOperationHelper

const val signerId = "signerId"
const val signerName = "signerName"
const val signatureData = "signatureData"

class SignatureRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Autowired
    lateinit var signatureRepository: SignatureRepository

    @Test
    fun `When getById then return signature`() {
        val operation = testOperationHelper.createOperation()
        val signature = SignatureEntity(signerId = signerId, signerName = signerName, signatureData = signatureData)
        signature.operation = operation
        signatureRepository.save(signature)

        val found = signatureRepository.getById(signature.getId())

        Assertions.assertThat(found).isNotNull
        Assertions.assertThat(found?.signerId).isEqualTo(signerId)
        Assertions.assertThat(found?.signerName).isEqualTo(signerName)
        Assertions.assertThat(found?.signatureData).isEqualTo(signatureData)
        Assertions.assertThat(found?.operation).isEqualTo(operation)
    }

}
