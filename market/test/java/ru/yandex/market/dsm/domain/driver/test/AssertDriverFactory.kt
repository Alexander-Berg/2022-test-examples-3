package ru.yandex.market.dsm.domain.driver.test

import org.assertj.core.api.Assertions
import ru.yandex.market.dsm.domain.driver.model.Driver
import ru.yandex.mj.generated.server.model.DriverDto
import ru.yandex.mj.generated.server.model.DriverPersonalDataDto
import ru.yandex.mj.generated.server.model.TagDto

object AssertDriverFactory {

    fun assertEquals(
        driverDto: DriverDto,
        driver: Driver
    ) {
        Assertions.assertThat(driverDto.id).isNotEmpty
        Assertions.assertThat(driver.id).isEqualTo(driverDto.id)
        Assertions.assertThat(driver.uid).isEqualTo(driverDto.uid)
        Assertions.assertThat(driver.employerIds).isEqualTo(driverDto.employerIds)
        Assertions.assertThat(driver.personalData.name).isEqualTo(driverDto.name)
        Assertions.assertThat(driver.personalData.passportData.lastName).isEqualTo(driverDto.lastName)
        Assertions.assertThat(driver.personalData.passportData.firstName).isEqualTo(driverDto.firstName)
        Assertions.assertThat(driver.personalData.passportData.patronymicName).isEqualTo(driverDto.patronymic)
        Assertions.assertThat(driver.personalData.phone).isEqualTo(driverDto.phone)
        Assertions.assertThat(driver.personalData.passportData.birthday).isEqualTo(driverDto.birthday)
        Assertions.assertThat(driver.blackListed).isEqualTo(driverDto.blackListed)

        if (driver.personalData.passportData.passportNumber == null) {
            Assertions.assertThat(driverDto.tags).containsExactlyInAnyOrder(TagDto.NO_PASSPORT)
        } else {
            Assertions.assertThat(driverDto.tags).isEmpty()
        }
    }

    fun assertEquals(
        dto: DriverPersonalDataDto,
        driver: Driver
    ) {
        Assertions.assertThat(dto.id).isEqualTo(driver.personalData.id)
        Assertions.assertThat(dto.email).isEqualTo(driver.personalData.email)
        Assertions.assertThat(dto.name).isEqualTo(driver.personalData.name)
        Assertions.assertThat(dto.lastName).isEqualTo(driver.personalData.passportData.lastName)
        Assertions.assertThat(dto.firstName).isEqualTo(driver.personalData.passportData.firstName)
        Assertions.assertThat(dto.patronymic).isEqualTo(driver.personalData.passportData.patronymicName)
        Assertions.assertThat(dto.passportNumber).isEqualTo(driver.personalData.passportData.passportNumber)
        Assertions.assertThat(dto.nationality).isEqualTo(driver.personalData.passportData.nationality)
        Assertions.assertThat(dto.birthday).isEqualTo(driver.personalData.passportData.birthday)
        Assertions.assertThat(dto.phone).isEqualTo(driver.personalData.phone)
    }

}
