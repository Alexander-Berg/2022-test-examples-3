package ru.yandex.market.mbi.helpers

import org.springframework.context.annotation.Conditional

@Conditional(LocalEnvOnlyCondition::class)
annotation class LocalEnvOnly

@Conditional(CiOnlyCondition::class)
annotation class CiOnly
