package ru.yandex.travel.hotels.common.partners.bronevik

import com.google.common.base.Preconditions
import ru.yandex.travel.hotels.common.partners.base.CallContext
import ru.yandex.travel.hotels.common.partners.base.CallContext.CallPhase.OFFER_VALIDATION
import ru.yandex.travel.hotels.common.partners.base.CallContext.CallPhase.ORDER_CANCELLATION
import ru.yandex.travel.hotels.common.partners.base.CallContext.CallPhase.ORDER_CONFIRMATION
import ru.yandex.travel.hotels.common.partners.base.CallContext.CallPhase.ORDER_CREATION
import ru.yandex.travel.hotels.common.partners.base.CallContext.CallPhase.ORDER_REFUND
import ru.yandex.travel.hotels.common.partners.base.CallContext.CallPhase.ORDER_RESERVATION
import ru.yandex.travel.hotels.common.partners.bronevik.RateType as BronevikRateType
import ru.yandex.travel.hotels.common.partners.bronevik.model.FaultCode
import ru.yandex.travel.hotels.common.partners.bronevik.model.GuestNotificationType
import ru.yandex.travel.hotels.common.partners.bronevik.model.MealsDevelopment
import ru.yandex.travel.hotels.common.partners.bronevik.model.OrderStatus
import ru.yandex.travel.hotels.common.partners.bronevik.model.OrderStatus.AWAITING_CANCELLATION
import ru.yandex.travel.hotels.common.partners.bronevik.model.OrderStatus.AWAITING_CONFIRMATION
import ru.yandex.travel.hotels.common.partners.bronevik.model.OrderStatus.CANCELLED_WITHOUT_PENALTY
import ru.yandex.travel.hotels.common.partners.bronevik.model.OrderStatus.CANCELLED_WITH_PENALTY
import ru.yandex.travel.hotels.common.partners.bronevik.model.OrderStatus.CONFIRMED
import ru.yandex.travel.hotels.common.partners.bronevik.model.RateType
import ru.yandex.travel.hotels.common.partners.bronevik.model.TaxType
import ru.yandex.travel.hotels.common.partners.bronevik.utils.MockUtils
import ru.yandex.travel.hotels.proto.EHotelCancellation.CR_CUSTOM
import ru.yandex.travel.hotels.proto.EHotelCancellation.CR_FULLY_REFUNDABLE
import ru.yandex.travel.hotels.proto.EHotelCancellation.CR_NON_REFUNDABLE
import ru.yandex.travel.hotels.proto.EHotelCancellation.CR_PARTIALLY_REFUNDABLE
import ru.yandex.travel.hotels.proto.EHotelConfirmationOutcome
import ru.yandex.travel.hotels.proto.EHotelConfirmationOutcome.CO_NOT_FOUND
import ru.yandex.travel.hotels.proto.EHotelConfirmationOutcome.CO_PRICE_MISMATCH
import ru.yandex.travel.hotels.proto.EHotelConfirmationOutcome.CO_SUCCESS
import ru.yandex.travel.hotels.proto.EHotelConfirmationOutcome.CO_WAITLIST_ALWAYS
import ru.yandex.travel.hotels.proto.EHotelDataLookupOutcome.HO_MOCKED
import ru.yandex.travel.hotels.proto.EHotelDataLookupOutcome.HO_REAL
import ru.yandex.travel.hotels.proto.EHotelOfferOutcome.OO_DISCONNECTED
import ru.yandex.travel.hotels.proto.EHotelOfferOutcome.OO_PRICE_MISMATCH
import ru.yandex.travel.hotels.proto.EHotelOfferOutcome.OO_SOLD_OUT
import ru.yandex.travel.hotels.proto.EHotelOfferOutcome.OO_SUCCESS
import ru.yandex.travel.hotels.proto.EHotelOfferOutcome.UNRECOGNIZED
import ru.yandex.travel.hotels.proto.EHotelRefundOutcome.RF_SUCCESS
import ru.yandex.travel.hotels.proto.EHotelRefundOutcome.RF_UNABLE_TO_REFUND
import ru.yandex.travel.hotels.proto.EHotelRefundOutcome.RF_UNEXPECTED_PENALTY
import ru.yandex.travel.hotels.proto.EHotelReservationOutcome
import ru.yandex.travel.hotels.proto.EHotelReservationOutcome.RO_PRICE_MISMATCH
import ru.yandex.travel.hotels.proto.EHotelReservationOutcome.RO_SOLD_OUT
import ru.yandex.travel.hotels.proto.EHotelReservationOutcome.RO_SUCCESS
import ru.yandex.travel.hotels.proto.EPansionType.PT_AI
import ru.yandex.travel.hotels.proto.EPansionType.PT_BB
import ru.yandex.travel.hotels.proto.EPansionType.PT_BD
import ru.yandex.travel.hotels.proto.EPansionType.PT_FB
import ru.yandex.travel.hotels.proto.EPansionType.PT_HB
import ru.yandex.travel.hotels.proto.EPansionType.PT_LAI
import ru.yandex.travel.hotels.proto.EPansionType.PT_RO
import ru.yandex.travel.hotels.proto.EPansionType.PT_UAI
import ru.yandex.travel.hotels.proto.TBronevikOffer
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import javax.xml.datatype.DatatypeFactory
import kotlin.math.roundToInt

class TestContextBronevikClient(
    private val callContext: CallContext,
    private val baseClient: BronevikClient
) : BronevikClient {

    private val testContext = callContext.testContext
    private val withTestContext = testContext != null

    override fun ping(): CompletableFuture<PingResponse> {
        if (withTestContext) {
            return CompletableFuture.completedFuture(PingResponse())
        }

        return baseClient.ping()
    }

    override fun getHotelsInfo(hotelIds: List<Int>, requestId: String): CompletableFuture<HotelsWithInfo> {
        if (!withTestContext || !testContext!!.forceAvailability) {
            return baseClient.getHotelsInfo(hotelIds, requestId)
        }

        val hotelsWithInfo = HotelsWithInfo()
        hotelsWithInfo.setHotel(hotelIds.map { hotelId -> getHotelInfoSync(hotelId, requestId) })

        return CompletableFuture.completedFuture(hotelsWithInfo)
    }

    override fun getHotelInfo(hotelId: Int, requestId: String): CompletableFuture<HotelWithInfo> {
        if (!withTestContext) {
            return baseClient.getHotelInfo(hotelId, requestId)
        }

        return when (testContext!!.hotelDataLookupOutcome) {
            HO_MOCKED -> CompletableFuture.completedFuture(getHotelInfo(hotelId))
            HO_REAL -> {
                return if (testContext.forceAvailability) {
                    baseClient.getHotelInfo(hotelId, requestId).thenApply {
                        addMockedDataToRealHotelInfo(it)
                    }
                } else {
                    baseClient.getHotelInfo(hotelId, requestId)
                }
            }
            else -> CompletableFuture.failedFuture(IllegalArgumentException("Invalid HotelDataLookupOutcome"))
        }
    }

    override fun getMeals(requestId: String): CompletableFuture<GetMealsResponse> {
        if (!withTestContext) {
            return baseClient.getMeals(requestId)
        }

        val getMealsResponse = GetMealsResponse()
        val meals = Meals()

        meals.setMeal(MealsDevelopment.values().map { value ->
            val meal = Meal()
            meal.setId(value.value)

            meal
        })

        getMealsResponse.setMeals(meals)

        return CompletableFuture.completedFuture(getMealsResponse)
    }

    override fun searchHotelOffers(
        adults: Int, children: List<Int>, hotelIds: List<Int>,
        checkIn: String, checkOut: String,
        currency: String, requestId: String
    ): CompletableFuture<SearchHotelOffersResponse> {
        if (!withTestContext || !testContext!!.forceAvailability) {
            return baseClient.searchHotelOffers(adults, children, hotelIds, checkIn, checkOut, currency, requestId)
        }

        val searchHotelOffersResponse = SearchHotelOffersResponse()
        val hotels = Hotels()

        hotels.setHotel(hotelIds.map { hotelId: Int ->
            val hotel = HotelWithOffers()
            hotel.setName("Тестовый отель")
            hotel.setAddress("ул Льва Толстого, 16")
            hotel.setDescriptionDetails(MockUtils.mockDescriptionDetails())
            hotel.setCategory(5)
            hotel.setId(hotelId)

            hotel.checkinTime = DatatypeFactory.newInstance().newXMLGregorianCalendarTime(14, 0, 0, 3)
            hotel.checkoutTime = DatatypeFactory.newInstance().newXMLGregorianCalendarTime(14, 0, 0, 3)

            val informationForGuest = InformationForGuest()
            val guestNotification = GuestNotification()
            guestNotification.type = GuestNotificationType.CHECKIN_POLICIES.value
            guestNotification.value = "Тестовая нотификация в Броневике"
            informationForGuest.notification = listOf(guestNotification)
            hotel.informationForGuest = informationForGuest

            var freeRooms = 2
            var offerPrice = testContext.priceAmount.toDouble()

            if (callContext.phase == OFFER_VALIDATION || callContext.phase == ORDER_CREATION) {
                val offerOutcome = if (callContext.phase == ORDER_CREATION) testContext.createOrderOutcome else testContext.getOfferOutcome

                when (offerOutcome) {
                    OO_SOLD_OUT -> freeRooms = 0
                    OO_PRICE_MISMATCH -> offerPrice *= testContext.priceMismatchRate
                    OO_SUCCESS, OO_DISCONNECTED, UNRECOGNIZED -> {}
                }
            }

            val hotelOffers = HotelOffers()
            val hotelOffer = HotelOffer()
            hotelOffer.code = UUID.randomUUID().toString()
            hotelOffer.name = "Тестовый оффер"
            hotelOffer.roomId = 1001
            hotelOffer.freeRooms = freeRooms
            hotelOffer.rateType = buildRateType()
            hotelOffer.cancellationPolicies = buildCancellationPolicies()
            hotelOffer.meals = buildAvailableMeals()

            val priceDetails = PriceDetails()
            priceDetails.client = MockUtils.mockClientPriceDetails(offerPrice)
            hotelOffer.priceDetails = priceDetails

            val dailyPrices = DailyPrices()
            val days = Duration.between(LocalDateTime.parse(checkOut), LocalDateTime.parse(checkIn)).toDays()

            dailyPrices.dailyPrice = IntArray(days.toInt()).map {
                val dailyPrice = DailyPrice()
                val priceValue = (offerPrice / days.toInt() * 100).roundToInt() / 100
                dailyPrice.rate = MockUtils.mockClientPriceDetails(priceValue.toDouble())

                dailyPrice
            }
            hotelOffer.dailyPrices = dailyPrices

            val taxes = Taxes()
            val tax = Tax()
            tax.type = TaxType.DEPOSIT.value
            tax.amount = 100f
            tax.included = true
            tax.currency = "RUB"
            taxes.tax = listOf(tax)
            hotelOffer.taxes = taxes

            hotelOffers.offer = listOf(hotelOffer)
            hotel.offers = hotelOffers

            hotel
        })

        searchHotelOffersResponse.setHotels(hotels)

        return CompletableFuture.completedFuture(searchHotelOffersResponse)
    }

    override fun getHotelOfferPricing(
        offerCode: String,
        guests: List<Guest>,
        meals: List<Int>,
        currency: String,
        requestId: String,
        children: MutableList<Child>
    ): CompletableFuture<GetHotelOfferPricingResponse> {
        if (!withTestContext || !testContext!!.forceAvailability) {
            return baseClient.getHotelOfferPricing(offerCode, guests, meals, currency, requestId, children)
        }

        return when (callContext.phase) {
            ORDER_RESERVATION -> mockGetHotelOfferPricing(testContext!!.reservationOutcome)
            ORDER_CONFIRMATION -> mockGetHotelOfferPricing(testContext!!.confirmationOutcome)
            else -> throw IllegalStateException()
        }
    }

    override fun createOrder(
        offerCode: String, guests: List<Guest>,
        meals: List<Int>, currency: String, referenceId: String, requestId: String,
        children: List<Child>
    ): CompletableFuture<CreateOrderResponse> {
        if (!withTestContext) {
            return baseClient.createOrder(offerCode, guests, meals, currency, referenceId, requestId, children)
        }

        return when (testContext!!.confirmationOutcome) {
            CO_SUCCESS -> mockCreateOrder(CONFIRMED)
            CO_NOT_FOUND -> CompletableFuture.failedFuture(MockUtils.mockCreateOrderFault(FaultCode.SOLD_OUT.value))
            CO_WAITLIST_ALWAYS -> mockCreateOrder(AWAITING_CONFIRMATION)
            else -> throw IllegalStateException()
        }
    }

    override fun getOrder(orderId: Int, requestId: String): CompletableFuture<GetOrderResponse> {
        if (!withTestContext) {
            return baseClient.getOrder(orderId, requestId)
        }

        return when (callContext.phase) {
            ORDER_CONFIRMATION -> mockGetOrder(CONFIRMED)
            ORDER_CANCELLATION, ORDER_REFUND -> {
                when (testContext!!.refundOutcome) {
                    RF_SUCCESS -> mockGetOrder(if (testContext.cancellation == CR_FULLY_REFUNDABLE) CANCELLED_WITHOUT_PENALTY else CANCELLED_WITH_PENALTY)
                    RF_UNEXPECTED_PENALTY -> mockGetOrder(CANCELLED_WITH_PENALTY)
                    RF_UNABLE_TO_REFUND -> mockGetOrder(AWAITING_CANCELLATION)
                    else -> throw IllegalStateException()
                }
            }
            else -> throw IllegalStateException()
        }
    }

    override fun cancelOrder(orderId: Int, requestId: String): CompletableFuture<CancelOrderResponse> {
        if (!withTestContext) {
            return baseClient.cancelOrder(orderId, requestId)
        }

        return CompletableFuture.completedFuture(MockUtils.mockCancelOrderResponse())
    }

    override fun searchOrdersByReferenceId(referenceId: String, requestId: String): CompletableFuture<SearchOrdersResponse> {
        if (!withTestContext) {
            return baseClient.searchOrdersByReferenceId(referenceId, requestId)
        }

        val offer = getOffer()

        return CompletableFuture.completedFuture(MockUtils.mockSearchOrdersResponse(offer.price.toFloat(), CONFIRMED))
    }

    override fun getAmenities(requestId: String): CompletableFuture<GetAmenitiesResponse> {
        return baseClient.getAmenities(requestId)
    }

    private fun mockGetOrder(status: OrderStatus): CompletableFuture<GetOrderResponse> {
        val offer = getOffer()

        return CompletableFuture.completedFuture(MockUtils.mockGetOrderResponse(offer.price.toFloat(), status))
    }

    private fun mockCreateOrder(status: OrderStatus): CompletableFuture<CreateOrderResponse> {
        val offer = getOffer()

        return CompletableFuture.completedFuture(MockUtils.mockCreateOrderResponse(offer.price.toFloat(), status))
    }

    private fun mockGetHotelOfferPricing(outcome: EHotelReservationOutcome): CompletableFuture<GetHotelOfferPricingResponse> {
        val offer = getOffer()

        if (outcome == RO_SOLD_OUT) {
            return CompletableFuture.failedFuture(MockUtils.mockGetHotelOfferPricingFault(FaultCode.SOLD_OUT.value))
        }

        val price = when (outcome) {
            RO_SUCCESS -> offer.price.toDouble()
            RO_PRICE_MISMATCH -> offer.price * testContext!!.priceMismatchRate
            else -> throw IllegalStateException()
        }

        return CompletableFuture.completedFuture(MockUtils.mockGetHotelOfferPricingResponse(price.toFloat()))
    }

    private fun mockGetHotelOfferPricing(outcome: EHotelConfirmationOutcome): CompletableFuture<GetHotelOfferPricingResponse> {
        val offer = getOffer()

        if (outcome == CO_NOT_FOUND) {
            return CompletableFuture.failedFuture(MockUtils.mockGetHotelOfferPricingFault(FaultCode.SOLD_OUT.value))
        }

        val price = when (outcome) {
            CO_SUCCESS -> offer.price.toDouble()
            CO_PRICE_MISMATCH -> offer.price * testContext!!.priceMismatchRate
            else -> throw IllegalStateException()
        }

        return CompletableFuture.completedFuture(MockUtils.mockGetHotelOfferPricingResponse(price.toFloat()))
    }

    private fun getOffer(): TBronevikOffer {
        Preconditions.checkState(callContext.offerData != null, "Offer data is not passed to call context")
        Preconditions.checkState(callContext.offerData is TBronevikOffer, "Invalid offer type")

        return callContext.offerData as TBronevikOffer
    }

    private fun getHotelInfo(hotelId: Int): HotelWithInfo {
        val hotelWithInfo = HotelWithInfo()

        hotelWithInfo.setName("Тестовый отель")
        hotelWithInfo.setAddress("ул Льва Толстого, 16")
        hotelWithInfo.setDescriptionDetails(MockUtils.mockDescriptionDetails())
        hotelWithInfo.setRooms(MockUtils.mockHotelRooms())
        hotelWithInfo.setCategory(5)
        hotelWithInfo.setId(hotelId)

        return hotelWithInfo
    }

    private fun buildRateType(): BronevikRateType {
        val rateType = BronevikRateType()
        rateType.rateName = when (testContext!!.cancellation) {
            CR_FULLY_REFUNDABLE -> RateType.REFUNDABLE_RATE.value
            CR_PARTIALLY_REFUNDABLE -> RateType.REFUNDABLE_RATE.value
            CR_NON_REFUNDABLE -> RateType.NON_REFUNDABLE_RATE.value
            CR_CUSTOM -> RateType.NON_REFUNDABLE_SPECIAL_BRONEVIK_RATE.value
            else -> throw IllegalStateException("Unknown cancellation type ${testContext.cancellation}")
        }

        return rateType
    }

    private fun buildCancellationPolicies(): List<HotelOfferCancellationPolicy> {
        return when (testContext!!.cancellation) {
            CR_FULLY_REFUNDABLE -> listOf()
            CR_NON_REFUNDABLE -> {
                val policy = HotelOfferCancellationPolicy()
                policy.penaltyDateTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(Instant.now().toString())
                policy.setPenaltyPriceDetails(MockUtils.mockClientPriceDetails(testContext.priceAmount.toDouble()))

                listOf(policy)
            }
            CR_PARTIALLY_REFUNDABLE -> {
                val policy = HotelOfferCancellationPolicy()
                policy.penaltyDateTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(Instant.now().toString())
                policy.setPenaltyPriceDetails(MockUtils.mockClientPriceDetails(testContext.priceAmount * testContext.partiallyRefundableRate))

                listOf(policy)
            }
            CR_CUSTOM -> {
                val border1 = Instant.now().plusSeconds((testContext.partiallyRefundableInMinutes * 60).toLong())
                val border2 = Instant.now().plusSeconds((testContext.nonRefundableInMinutes * 60).toLong())

                listOf(
                    MockUtils.mockHotelOfferCancellationPolicy(border1, (testContext.priceAmount * testContext.partiallyRefundableRate).toFloat()),
                    MockUtils.mockHotelOfferCancellationPolicy(border2, testContext.priceAmount.toFloat())
                )
            }
            else -> throw IllegalStateException()
        }
    }

    private fun buildAvailableMeals(): AvailableMeals {
        val meals = AvailableMeals()

        meals.meal = when (testContext!!.pansionType) {
            // Bronevik has not all inclusive pansion type
            PT_AI, PT_UAI, PT_LAI, PT_FB -> {
                val meal = AvailableMeal()
                meal.id = MealsDevelopment.FULL_BOARD.value

                listOf(meal)
            }
            PT_BB -> {
                val meal = AvailableMeal()
                meal.id = MealsDevelopment.CONTINENTAL_BREAKFAST.value

                listOf(meal)
            }
            PT_HB -> {
                val meal = AvailableMeal()
                meal.id = MealsDevelopment.HALF_BOARD.value

                listOf(meal)
            }
            PT_RO -> listOf()
            PT_BD -> {
                val meal = AvailableMeal()
                meal.id = MealsDevelopment.DINNER.value

                listOf(meal)
            }
            else -> throw IllegalStateException("Unknown pansion type")
        }

        return meals
    }

    private fun addMockedDataToRealHotelInfo(hotelWithInfo: HotelWithInfo): HotelWithInfo {
        hotelWithInfo.setDescriptionDetails(MockUtils.mockDescriptionDetails())
        hotelWithInfo.setRooms(MockUtils.mockHotelRooms())

        return hotelWithInfo
    }
}
