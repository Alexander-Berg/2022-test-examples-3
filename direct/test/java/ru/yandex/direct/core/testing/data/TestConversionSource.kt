package ru.yandex.direct.core.testing.data

import ru.yandex.direct.core.entity.conversionsource.model.ConversionAction
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSource
import ru.yandex.direct.core.entity.conversionsource.model.ConversionSourceSettings
import ru.yandex.direct.core.entity.conversionsource.model.Destination
import ru.yandex.direct.core.entity.conversionsource.model.ProcessingInfo
import ru.yandex.direct.core.entity.conversionsource.model.ProcessingStatus
import ru.yandex.direct.core.entity.conversionsource.validation.ACTION_NAME_IN_PROGRESS
import ru.yandex.direct.core.entity.conversionsource.validation.ACTION_NAME_PAID
import ru.yandex.direct.core.entity.conversionsourcetype.model.ConversionSourceTypeCode
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.TestUtils.randomName
import ru.yandex.direct.test.utils.randomPositiveLong
import kotlin.random.Random

fun defaultConversionSource(clientId: ClientId, counterId: Long? = null): ConversionSource =
    defaultConversionSourceMetrika(clientId, counterId)

fun defaultConversionSourceMetrika(clientId: ClientId, counterId: Long? = null): ConversionSource {
    val internalCounterId = counterId ?: randomPositiveLong(100000)
    return ConversionSource(
        id = null,
        typeCode = ConversionSourceTypeCode.METRIKA,
        clientId = clientId,
        name = randomName("test conversion source ", 30),
        settings = ConversionSourceSettings.Metrika(internalCounterId, "example.com"),
        counterId = internalCounterId,
        actions = listOf(
            ConversionAction(
                randomName("test conversion action ", 30),
                Random.nextLong(1000, Goal.METRIKA_GOAL_UPPER_BOUND),
                value = null,
            )
        ),
        updatePeriodHours = 0,
        destination = Destination.CrmApi(
            counterId = internalCounterId,
            accessUid = 333L
        ),
        processingInfo = ProcessingInfo(ProcessingStatus.SUCCESS, null, null)
    )
}

fun defaultConversionSourceLink(clientId: ClientId, counterId: Long? = null): ConversionSource {
    val internalCounterId = counterId ?: randomPositiveLong(100000)
    return ConversionSource(
        id = null,
        typeCode = ConversionSourceTypeCode.LINK,
        clientId = clientId,
        name = randomName("test conversion source ", 30),
        settings = ConversionSourceSettings.Link(url = "https://example.com/some-path-to-file"),
        counterId = internalCounterId,
        actions = listOf(
            ConversionAction(ACTION_NAME_IN_PROGRESS, null, value = null),
            ConversionAction(ACTION_NAME_PAID, null, value = null),
        ),
        updatePeriodHours = 0,
        destination = Destination.CrmApi(
            counterId = internalCounterId,
            accessUid = 333L
        ),
        processingInfo = ProcessingInfo(ProcessingStatus.NEW, null, null)
    )
}
