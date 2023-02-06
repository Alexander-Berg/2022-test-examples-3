package ru.yandex.market.tpl.courier.data.feature

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import ru.yandex.market.tpl.courier.arch.common.Duration
import ru.yandex.market.tpl.courier.arch.common.millis
import ru.yandex.market.tpl.courier.arch.coroutine.DataDispatchers
import ru.yandex.market.tpl.courier.arch.coroutine.delay
import ru.yandex.market.tpl.courier.arch.coroutine.retry
import ru.yandex.market.tpl.courier.arch.ext.firstInstanceOfOrNull
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.arch.kotlin.LOCALE_RU
import ru.yandex.market.tpl.courier.arch.logs.e
import ru.yandex.market.tpl.courier.data.feature.batch.BatchDto
import ru.yandex.market.tpl.courier.data.feature.shift.ShiftUseCases
import ru.yandex.market.tpl.courier.data.feature.time.DateTimeProvider
import ru.yandex.market.tpl.courier.data.feature.xml.XmlDataGenerator
import ru.yandex.market.tpl.courier.data.remote.point.GetRoutePointsUseCase
import ru.yandex.market.tpl.courier.domain.account.AccountCredentials
import ru.yandex.market.tpl.courier.domain.feature.auth.Uid
import ru.yandex.market.tpl.courier.domain.feature.location.Location
import ru.yandex.market.tpl.courier.domain.feature.point.ArriveAtRoutePointUseCase
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePoint
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointId
import ru.yandex.market.tpl.courier.domain.feature.point.RoutePointSwitchReason
import ru.yandex.market.tpl.courier.domain.feature.shift.CheckInShiftUseCase
import ru.yandex.market.tpl.courier.domain.feature.shift.CourierShift
import ru.yandex.market.tpl.courier.domain.feature.shift.CourierShiftId
import ru.yandex.market.tpl.courier.domain.feature.shift.GetCurrentShiftUseCase
import ru.yandex.market.tpl.courier.domain.feature.task.ClientReturnTask
import ru.yandex.market.tpl.courier.domain.feature.task.TaskId
import ru.yandex.market.tpl.courier.domain.feature.task.delivery.PaymentType
import ru.yandex.market.tpl.courier.domain.feature.task.partialReturn.GetPartialReturnParamsUseCase
import ru.yandex.market.tpl.courier.domain.feature.user.GetCurrentUserUseCase
import ru.yandex.market.tpl.courier.domain.task.CancelType
import ru.yandex.market.tpl.courier.domain.user.UserProperties
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestDataRepository @Inject constructor(
    private val dispatchers: DataDispatchers,
    private val manualDataCreationRepository: ManualDataCreationRepository,
    private val dbsDataRepository: DbsDataRepository,
    private val internalRepository: InternalRepository,
    private val checkInShiftUseCase: CheckInShiftUseCase,
    private val getPointsUseCase: GetRoutePointsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val arriveAtRoutePointUseCase: ArriveAtRoutePointUseCase,
    private val getCurrentShiftUseCase: GetCurrentShiftUseCase,
    private val generatorData: GeneratorData,
    private val shiftUseCases: ShiftUseCases,
    private val partialReturnParamsUseCase: GetPartialReturnParamsUseCase,
    private val xmlDataGenerator: XmlDataGenerator,
    private val dateTimeProvider: DateTimeProvider,
) {
    private val dateTimeToDigitsFormatter by lazy {
        DateTimeFormatter.ofPattern("dMyyyyHms", LOCALE_RU)
    }

    private var shift: CourierShift? = null

    fun createShift(uid: Uid, sortingCenterId: Long? = null): CourierShift {
        val shift = withRetry(
            message = "не удалось создать смену",
            maxRetryCount = 1
        ) {
            manualDataCreationRepository.createShift(uid, sortingCenterId ?: SORTING_CENTER_ID)
                .orThrow()
        }
        this.shift = shift
        return shift
    }

    fun createRoutePointWithDeliveryTask(
        uid: Uid,
        latitude: Double = 55.80455,
        longitude: Double = 37.599555,
        phone: String = "+79000000000",
        orderPlaceCount: Int = 1,
        type: String = "DELIVERY",
        pickupPointType: String = "",
        paymentType: PaymentType = PaymentType.Cash,
        isPaid: Boolean = false,
        isFashion: Boolean = false,
        isRatingR: Boolean = false,
        recipientNotes: String? = null,
        itemsPrice: Long? = null,
        isBusinessDelivery: Boolean = false,
        verificationCode: String? = null,
        addMinutesToCurrentTime: Long? = null,
    ): RoutePoint {
        val shiftId = checkNotNull(shift?.id)

        val expectedDate = if (addMinutesToCurrentTime != null) {
            val currentDate =
                dateTimeProvider.getCurrentDateTime().withZoneSameInstant(ZoneOffset.UTC)

            currentDate.plusMinutes(addMinutesToCurrentTime)
            currentDate
        } else {
            null
        }

        return withRetry("не удалось создать задание на доставку") {
            manualDataCreationRepository.createRoutePointWithDeliveryTask(
                uid,
                shiftId,
                Location(latitude, longitude),
                phone,
                orderPlaceCount = orderPlaceCount,
                type = type,
                pickupPointType = pickupPointType,
                paymentType = paymentType,
                isPaid = isPaid,
                fashion = isFashion,
                isRatingR = isRatingR,
                itemsPrice = itemsPrice,
                recipientNotes = recipientNotes,
                isBusinessDelivery = isBusinessDelivery,
                verificationCode = verificationCode,
                expectedDate = expectedDate,
            ).orThrow()
        }
    }

    fun checkIn() {
        val shiftId = checkNotNull(shift?.id)
        withRetry {
            checkInShiftUseCase.checkInShift(shiftId, locationMoscow).orThrow()
        }
    }

    fun getFirstDeliveryRoutePoint(accountCredentials: AccountCredentials): RoutePoint {
        return withRetry {
            val user =
                manualDataCreationRepository.findUserByEmail("${accountCredentials.login}@yandex.ru")
                    .orThrow()
            val routePoints = getPointsUseCase.getDeliveryRoutePoints(user.id).orThrow()
            if (routePoints.isEmpty()) {
                throw IllegalStateException("Пустой список рутпоинтов на доставку")
            } else {
                getPointsUseCase.getRoutePoint(routePoints.first().id, user.id).orThrow()
            }
        }
    }

    fun arriveAtRoutePoint(routePointId: RoutePointId) {
        withRetry("не удалось \"прибыть\" на точку $routePointId") {
            arriveAtRoutePointUseCase.arriveAtRoutePoint(routePointId, locationMoscow).orThrow()
        }
    }

    fun arriveAtCurrentRoutePoint() {
        getCurrentShift()
        val currentRoutePointId = checkNotNull(shift?.currentRoutePointId)
        arriveAtRoutePoint(currentRoutePointId)
    }

    fun setUserProperties(properties: Map<String, Any>) {
        withRetry("не удалось установить флаги $properties") {
            val user = getCurrentUserUseCase.getCurrentUser().orThrow()
            manualDataCreationRepository.setUserProperties(
                id = user.id,
                properties = properties
            ).orThrow()
        }
    }

    fun setUserProperties(credentials: AccountCredentials, properties: Map<String, Any>) {
        withRetry("не удалось установить флаги $properties") {
            val user =
                manualDataCreationRepository.findUserByEmail("${credentials.login}@yandex.ru")
                    .orThrow()
            manualDataCreationRepository.setUserProperties(
                id = user.id,
                properties = properties
            ).orThrow()
        }
    }

    fun setUserSoftMode() {
        withRetry {
            val user = getCurrentUserUseCase.getCurrentUser().orThrow()
            manualDataCreationRepository.setUserProperties(
                id = user.id,
                properties = mapOf(USER_MODE to SOFT_MODE)
            ).orThrow()
        }
    }

    fun createRoutePointWithDropshipTask(uid: Uid): RoutePoint {
        val shiftId = checkNotNull(shift?.id)
        return withRetry("не удалось создать дропшип") {
            manualDataCreationRepository.createRoutePointWithDropshipTask(
                uid = uid,
                userShiftId = shiftId,
            ).orThrow()
        }
    }

    fun createDbsOrder(externalOrderId: String): String {
        val xmlDataAsString = generatorData.prepareXmlDbsOrderData(externalOrderId)

        return runBlocking(dispatchers.io) {
            dbsDataRepository.createDbsOrder(xmlDataAsString).orThrow()
        }
    }

    fun createOrderFromXml(externalOrderId: String, filename: String): String {
        val xml = xmlDataGenerator.getCreateOrderXml(filename, externalOrderId)

        return withRetry {
            internalRepository.createOrder(xml).orThrow()?.orderId?.deliveryId ?: ""
        }
    }

    fun updateItemInstances(externalOrderId: String, filename: String) {
        val xml = xmlDataGenerator.getUpdateOrderItemsXml(filename, externalOrderId)

        return withRetry {
            internalRepository.updateItemsInstances(xml).orThrow()
        }
    }

    fun reassignDbsOrder(orderId: String) {
        // TODO!!!
//        val orderIds = listOf(orderId.toLong())
//        val courierTo = testConfiguration.dbsCredentials.id
//            ?: throw IllegalArgumentException("У текущего курьера нет id: ${testConfiguration.credentials}")
//
//        return runBlocking(dispatchers.io) {
//            dbsDataRepository.reassignOrders(orderIds, courierTo).orThrow()
//        }
    }

    fun reassignOrder(orderId: String, credentials: AccountCredentials) {
        val orderIds = listOf(orderId.toLong())
        val user =
            withRetry {
                manualDataCreationRepository.findUserByEmail("${credentials.login}@yandex.ru")
                    .orThrow()
            }
        val courierTo = user.id

        return withRetry {
            internalRepository.reassignOrders(orderIds, courierTo)
        }
    }

    fun reassignOrders(orders: List<String>, credentials: AccountCredentials) {
        val orderIds = orders.map(String::toLong)
        val user =
            withRetry {
                manualDataCreationRepository.findUserByEmail("${credentials.login}@yandex.ru")
                    .orThrow()
            }
        val courierTo = user.id

        return withRetry {
            internalRepository.reassignOrders(orderIds, courierTo)
        }
    }

    fun generateExternalOrderId(): String {
        return generatorData.generateExternalOrderId()
    }

    fun getCurrentShift() {
        return withRetry("не удалось получить текущую смену") {
            shift = getCurrentShiftUseCase.getCurrentShift().orThrow()
        }
    }

    fun finishShift() {
        val shiftId = checkNotNull(shift?.id)
        return runBlocking(dispatchers.io) {
            manualDataCreationRepository.finishUserShift(shiftId).orThrow()
        }
    }

    fun getDropshipSwitchReasons(): Set<RoutePointSwitchReason> {
        val shiftId = checkNotNull(shift?.id)
        return runBlocking {
            shiftUseCases.getRoutePointSwitchReasons(shiftId, COLLECT_DROPSHIP)
        }
    }

    fun manualSwitchRoutePoint(routePointId: RoutePointId, uid: Uid) {
        return withRetry("не удалось переключится на точку $routePointId") {
            manualDataCreationRepository.manualSwitchRoutePoint(
                routePointId = routePointId,
                uid = uid,
            ).orThrow()
        }
    }

    fun manualFinishPickup(uid: Uid) {
        return withRetry("не удалось завершить приемку") {
            manualDataCreationRepository.manualFinishPickup(uid).orThrow()
        }
    }

    fun getSavePackageBoxBarcode(): String {
        return withRetry("не удалось получить маску баркода") {
            val boxBarcodeMask =
                partialReturnParamsUseCase.getCachedOrLoadBoxBarcodeMask().orThrow()
            boxBarcodeMask.masks.first().unwrap()
        }
    }

    fun createBatch(batch: BatchDto) {
        return withRetry("не удалось создать батч") {
            manualDataCreationRepository.createBatch(batch).orThrow()
        }
    }

    fun transferActSign(uid: Uid) = runBlocking {
        manualDataCreationRepository.transferActSign(uid)
    }

    fun switchUserFlags(
        credentials: AccountCredentials,
        flags: Set<UserProperties>,
        value: Boolean
    ) = runBlocking {
        val userFlags = mutableMapOf<String, Boolean>()

        flags.forEach {
            userFlags[it.value] = value
        }

        setUserProperties(credentials, userFlags)
    }

    fun cancelTask(taskId: TaskId, type: CancelType, uid: Uid) = runBlocking {
        manualDataCreationRepository.cancelTask(taskId, type, uid)
    }

    fun createRoutePointWithClientReturnTask(
        uid: Uid,
        shiftId: CourierShiftId,
    ): RoutePoint {
        return withRetry("Не удалось создать задание на клиентский возврат") {
            val arriveTime = dateTimeProvider.getCurrentDateTime()
                .plusHours(2)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
            manualDataCreationRepository.generateClientReturnTaskAndAssign(
                uid = uid,
                shiftId = shiftId,
                expectedArriveTime = arriveTime,
                location = Location(55.751772, 37.618970),
            )
                .orThrow()
        }
    }

    private fun addDeliveryTaskToRoutePoint(
        uid: Uid,
        shiftId: CourierShiftId,
        routePointId: RoutePointId,
    ): RoutePoint {
        return withRetry {
            manualDataCreationRepository.addDeliveryTaskToRoutePoint(
                uid = uid,
                shiftId = shiftId,
                routePointId = routePointId,
                isPaid = false,
                paymentType = PaymentType.CreditCard,
            )
                .orThrow()
        }
    }

    private fun <T> withRetry(
        message: String? = "ошибка при запросе",
        retryDelay: Duration = MANUAL_RETRY_DELAY.millis,
        maxRetryCount: Int = MANUAL_MAX_RETRY,
        block: suspend CoroutineScope.() -> T
    ): T {
        return runBlocking(dispatchers.io) {
            retry {
                if (it != null) {
                    if (it.number > maxRetryCount) {
                        it.reject()
                    }

                    e(
                        "TestDataRepository: $message",
                        it.previousError
                    )
                    delay(retryDelay)
                }

                block()
            }
        }
    }

    fun updateCache() = runBlocking {
        manualDataCreationRepository.updateCache()
    }

    fun getClientReturnBarcode(): String {
        val now = dateTimeProvider.getCurrentDateTime()
        return "VOZ_MK_${dateTimeToDigitsFormatter.format(now)}"
    }

    fun createShiftWithClientReturnTask(
        uid: Uid,
        sortingCenterId: Long? = null
    ): ClientReturnTask {
        val shift = createShift(uid, sortingCenterId ?: SORTING_CENTER_ID)
        val routePoint = createRoutePointWithClientReturnTask(
            uid = uid,
            shiftId = shift.id,
        )
        addDeliveryTaskToRoutePoint(
            uid = uid,
            shiftId = shift.id,
            routePointId = routePoint.id
        )

        val task = checkNotNull(routePoint.tasks.firstInstanceOfOrNull<ClientReturnTask>())

        val routePointId = routePoint.id
        checkIn()
        manualFinishPickup(uid)
        manualSwitchRoutePoint(routePointId, uid)
        arriveAtRoutePoint(routePointId)
        return task
    }

    private companion object {
        private val locationMoscow = Location(latitude = 55.755819, longitude = 37.617644)
        private const val USER_MODE = "user_mode"
        private const val SOFT_MODE = "SOFT_MODE"
        private const val COLLECT_DROPSHIP = "COLLECT_DROPSHIP"
        private const val MANUAL_MAX_RETRY = 3
        private const val MANUAL_RETRY_DELAY = 250
        private const val SORTING_CENTER_ID = 50595L
    }
}
