package ru.yandex.market.mbi.helpers

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import ru.yandex.market.mbi.orderservice.common.util.injectedLogger
import java.net.Inet4Address

const val CI_HOST_PREFIX = "distbuild"

/**
 * Условие запуска в локальной (не CI) среде
 */
class LocalEnvOnlyCondition : Condition {
    private val log by injectedLogger()

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val isLocal = !Inet4Address.getLocalHost().hostName.contains(CI_HOST_PREFIX)
        return isLocal.also { log.info(if (isLocal) "Local environment detected" else "CI environment detected") }
    }
}

/**
 * Условие запуска в CI среде
 */
class CiOnlyCondition : Condition {
    private val log by injectedLogger()

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val isCi = Inet4Address.getLocalHost().hostName.contains(CI_HOST_PREFIX)
        return isCi.also { log.info(if (isCi) "CI environment detected" else "Local environment detected") }
    }
}
