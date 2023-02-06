package ru.yandex.direct.teststeps.service

import org.springframework.stereotype.Service
import ru.yandex.direct.core.testing.steps.EcomDomainsSteps

@Service
class EcomStepsService(
    private val ecomDomainsSteps: EcomDomainsSteps
) {
    fun addEcomDomain(domain: String, offersCount: Long) {
        ecomDomainsSteps.addEcomDomain(domain, offersCount)
    }
}
