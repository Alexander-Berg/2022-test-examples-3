package ru.yandex.market.dsm.test

import org.assertj.core.api.Assertions.assertThat
import ru.yandex.market.dsm.domain.employer.db.Employer
import ru.yandex.market.dsm.domain.employer.db.EmployerDbo
import ru.yandex.mj.generated.server.model.EmployerDto
import ru.yandex.mj.generated.server.model.EmployerUpsertDto

object AssertsEmployerFactory {
    fun asserts(
        entity: EmployerDbo,
        projection: Employer
    ) {
        assertThat(entity).usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(projection);
    }

    fun asserts(
        dto: EmployerDto,
        domain: Employer,
    ) {
        assertThat(dto.id).isNotEmpty
        assertThat(domain.id).isEqualTo(dto.id)
        assertThat(domain.type.name).isEqualTo(dto.type.value)
        assertThat(domain.name).isEqualTo(dto.name)
        assertThat(domain.login).isEqualTo(dto.login)
        assertThat(domain.phoneNumber).isEqualTo(dto.phoneNumber)
        assertThat(domain.taxpayerNumber).isEqualTo(dto.taxpayerNumber)
        assertThat(domain.juridicalAddress).isEqualTo(dto.juridicalAddress)
        assertThat(domain.naturalAddress).isEqualTo(dto.naturalAddress)
        assertThat(domain.ogrn).isEqualTo(dto.ogrn)
        assertThat(domain.legalForm?.name).isEqualTo(dto.legalForm?.value)
        assertThat(domain.companyMbiId).isEqualTo(dto.companyMbiId)
        assertThat(domain.companyCabinetMbiId).isEqualTo(dto.companyCabinetMbiId)
        assertThat(domain.isActive).isEqualTo(dto.active)

        assertThat(domain.emailForDocuments).isEqualTo(dto.employerContactInfo.emailForDocuments)
        assertThat(domain.nds).isEqualTo(dto.nds)
    }

    fun asserts(
        dto: EmployerDto,
        upsertDto: EmployerUpsertDto
    ) {
        assertThat(dto).usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(upsertDto)

        assertThat(dto.id).isNotEmpty;

        if (upsertDto.id != null)
            assertThat(upsertDto.id).isEqualTo(dto.id)
    }

    fun asserts(
        projection: Employer,
        upsertDto: EmployerUpsertDto
    ) {
        assertThat(projection).usingRecursiveComparison()
            .ignoringFields(
                "id", "isActive", "fullNameOfResponsible",
                "phoneNumberOfResponsible",
                "emailOfResponsible",
                "emailForDocuments",
                "contractNumber",
                "contractDate",
                "fullNameOfCeo"
            )
            .isEqualTo(upsertDto)

        assertThat(projection.isActive).isEqualTo(upsertDto.active)

        assertThat(projection.id).isNotEmpty;

        if (upsertDto.id != null)
            assertThat(upsertDto.id).isEqualTo(projection.id)
    }
}
