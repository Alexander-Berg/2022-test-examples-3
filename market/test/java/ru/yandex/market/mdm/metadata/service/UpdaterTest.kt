package ru.yandex.market.mdm.metadata.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmEvent
import ru.yandex.market.mdm.lib.model.mdm.MdmValidationError
import ru.yandex.market.mdm.metadata.repository.MdmVersionedRepository
import ru.yandex.market.mdm.metadata.service.validation.MdmValidator
import ru.yandex.market.mdm.metadata.service.validation.ValidationsHelper.Companion.CONCURRENT_UPDATE_CODE

class UpdaterTest {

    val validator: MdmValidator<MdmAttribute> = mock()
    val repository: MdmVersionedRepository<MdmAttribute, Long> = mock()
    val auditor: Auditor = mock()
    val updater: Updater<MdmAttribute> = Updater(repository, validator, auditor)

    @Test
    fun `should return update result when successful`() {
        // given
        val storedAttribute = mdmAttribute()
        val toUpdate = storedAttribute.copy(
            ruTitle = "new Title"
        )

        given(auditor.updateWithAudit(eq(listOf(toUpdate)), any(), any())).willReturn(listOf(toUpdate))
        given(repository.findLatestByIds(listOf(toUpdate.mdmId))).willReturn(listOf(storedAttribute))
        given(validator.validate(any())).willReturn(listOf())

        // when
        val result = updater.updateWithAudit(listOf(toUpdate), MdmEvent())

        // then
        assertSoftly {
            result.errors shouldHaveSize 0
            result.results shouldContain toUpdate
        }
    }

    @Test
    fun `should return error when validation error`() {
        // given
        val storedAttribute = mdmAttribute()
        val toUpdate = storedAttribute.copy(
            ruTitle = "new Title"
        )
        val error = MdmValidationError("test", "test")
        given(repository.findLatestByIds(listOf(toUpdate.mdmId))).willReturn(listOf(storedAttribute))
        given(validator.validate(any())).willReturn(listOf(error))

        // when
        val result = updater.updateWithAudit(listOf(toUpdate), MdmEvent())

        // then
        assertSoftly {
            result.errors shouldContain error
            result.results shouldHaveSize 0
        }
        // and
        verify(repository, never()).insertOrUpdateBatch(any())
    }

    @Test
    fun `should return concurrent modification error when concurrent modifacation`() {
        // given
        val storedAttribute = mdmAttribute()
        val toUpdate = storedAttribute.copy(
            ruTitle = "new Title"
        )
        given(repository.findLatestByIds(listOf(toUpdate.mdmId))).willReturn(listOf(storedAttribute))
        given(auditor.updateWithAudit(eq(listOf(toUpdate)), any(), any())).willThrow(
            ConcurrentModificationException("error"))
        given(validator.validate(any())).willReturn(listOf())

        // when
        val result = updater.updateWithAudit(listOf(toUpdate), MdmEvent())

        // then
        assertSoftly {
            result.errors shouldHaveSize 1
            result.errors.first().code shouldBe CONCURRENT_UPDATE_CODE
            result.errors.first().message shouldBe "error"
            result.results shouldHaveSize 0
        }
    }
}
