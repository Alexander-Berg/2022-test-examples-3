package ru.yandex.market.transferact.entity.document

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.transfer.TransferEntity
import ru.yandex.market.transferact.entity.transfer.TransferRepository
import ru.yandex.market.transferact.entity.transfer.status

const val link = "yandex.ru"

class DocumentRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Test
    fun `When getById then return document`() {
        var transfer = TransferEntity(status = status)
        transfer = transferRepository.save(transfer)
        var document = DocumentEntity(link = link, type = DocumentType.TRANSFER_ACT)
        document.transfer = transfer
        document = documentRepository.save(document)

        val found = documentRepository.getById(document.getId())

        assertThat(found).isNotNull
        assertThat(found?.link).isEqualTo(link)
        assertThat(found?.transfer).isEqualTo(transfer)
    }

    @Test
    fun `When findAllByTransferId then return documents`() {
        var transfer = TransferEntity(status = status)
        transfer = transferRepository.save(transfer)
        val document = DocumentEntity(link = link, type = DocumentType.TRANSFER_ACT)
        document.transfer = transfer
        documentRepository.save(document)

        val found = documentRepository.findAllByTransferId(transfer.getId())

        assertThat(found).hasSize(1)
        assertThat(found[0].link).isEqualTo(link)
    }
}
