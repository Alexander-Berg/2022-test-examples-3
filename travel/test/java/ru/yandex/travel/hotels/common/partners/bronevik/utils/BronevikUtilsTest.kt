package ru.yandex.travel.hotels.common.partners.bronevik.utils

import org.assertj.core.api.Assertions
import org.javamoney.moneta.Money
import org.junit.Ignore
import org.junit.Test
import ru.yandex.travel.hotels.common.partners.bronevik.AvailableMeal
import ru.yandex.travel.hotels.common.partners.bronevik.AvailableMeals
import ru.yandex.travel.hotels.common.partners.bronevik.BronevikRefundRulesException
import ru.yandex.travel.hotels.common.partners.bronevik.ClientPriceDetails
import ru.yandex.travel.hotels.common.partners.bronevik.CurrencyPriceDetails
import ru.yandex.travel.hotels.common.partners.bronevik.DetailedPrice
import ru.yandex.travel.hotels.common.partners.bronevik.HotelOfferCancellationPolicy
import ru.yandex.travel.hotels.common.partners.bronevik.PriceDetails
import ru.yandex.travel.hotels.common.partners.bronevik.SOAPType.DEVELOPMENT
import ru.yandex.travel.hotels.common.partners.bronevik.model.MealsDevelopment.FULL_BOARD
import ru.yandex.travel.hotels.common.partners.bronevik.model.MealsDevelopment.BUFFET_BREAKFAST
import ru.yandex.travel.hotels.common.partners.bronevik.model.MealsDevelopment.DINNER
import ru.yandex.travel.hotels.common.partners.bronevik.model.MealsDevelopment.LUNCH
import ru.yandex.travel.hotels.common.partners.bronevik.utils.BronevikUtils.Companion.getNights
import ru.yandex.travel.hotels.common.partners.bronevik.utils.BronevikUtils.Companion.parseChildren
import ru.yandex.travel.hotels.common.partners.bronevik.utils.BronevikUtils.Companion.getTotalPrice
import ru.yandex.travel.hotels.common.partners.bronevik.utils.BronevikUtils.Companion.parseMeals
import ru.yandex.travel.hotels.common.refunds.RefundRules
import ru.yandex.travel.hotels.common.refunds.RefundType.FULLY_REFUNDABLE
import ru.yandex.travel.hotels.common.refunds.RefundType.NON_REFUNDABLE
import ru.yandex.travel.hotels.common.refunds.RefundType.REFUNDABLE_WITH_PENALTY
import ru.yandex.travel.hotels.proto.EPansionType.PT_BD
import ru.yandex.travel.hotels.proto.EPansionType.PT_FB
import ru.yandex.travel.hotels.proto.EPansionType.PT_HB
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Stack
import javax.xml.datatype.DatatypeConfigurationException
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

class BronevikUtilsTest {

    @Test
    fun parseGetNights_Test() {
        val checkin = "2022-08-09"
        val checkout = "2022-08-20"
        Assertions.assertThat(getNights(checkin, checkout)).isEqualTo(11)
    }

    @Test
    fun getTotalPrice_Test() {

        val meals = MockUtils.mockNotIncludedFullBoard()
        val priceDetails = PriceDetails()
        priceDetails.client = MockUtils.mockClientPriceDetails(10000.0)
        priceDetails.isVatApplicable = false
        val totalPrice = getTotalPrice(priceDetails, meals, 2, 2)
        Assertions.assertThat(totalPrice).isEqualTo(11200)
    }

    @Test
    fun getTotalPriceWithoutMeals_Test() {

        val meals = AvailableMeals()
        meals.meal = mutableListOf()
        val priceDetails = PriceDetails()
        priceDetails.client = MockUtils.mockClientPriceDetails(10000.0)
        priceDetails.isVatApplicable = false
        val totalPrice = getTotalPrice(priceDetails, meals, 2, 2)
        Assertions.assertThat(totalPrice).isEqualTo(10000)
    }

    @Test
    fun parseChildren_Test() {
        val children: List<Int> = listOf(1, 2, 1)
        val bronevikChildren = parseChildren(children)
        Assertions.assertThat(bronevikChildren.size).isEqualTo(2)
        val oneYearChildren = bronevikChildren.first { it.age == 1 }
        Assertions.assertThat(oneYearChildren.count).isEqualTo(2)
        Assertions.assertThat(oneYearChildren.age).isEqualTo(1)
        val twoYearChildren = bronevikChildren.first { it.age == 2 }
        Assertions.assertThat(twoYearChildren.count).isEqualTo(1)
        Assertions.assertThat(twoYearChildren.age).isEqualTo(2)
    }

    @Test
    fun parseMeals_developmentMealsDinnerTest() {
        val availableDevelopmentDinnerMeals = AvailableMeals()
        availableDevelopmentDinnerMeals.meal = listOf(
            MockUtils.mockMeal(DINNER, true)
        )
        val soapType = DEVELOPMENT
        val pansionType = parseMeals(availableDevelopmentDinnerMeals, soapType)

        Assertions.assertThat(pansionType).isEqualByComparingTo(PT_BD)
    }

    @Test
    fun parseMeals_developmentMealsHBTest() {
        val availableDevelopmentHBMeals = AvailableMeals()
        availableDevelopmentHBMeals.meal = listOf(
            MockUtils.mockMeal(DINNER, true),
            MockUtils.mockMeal(BUFFET_BREAKFAST, true)
        )
        val soapType = DEVELOPMENT
        val pansionType = parseMeals(availableDevelopmentHBMeals, soapType)

        Assertions.assertThat(pansionType).isEqualByComparingTo(PT_HB)
    }

    @Test
    fun parseMeals_developmentMealsFBTest() {
        val availableDevelopmentFBMeals = AvailableMeals()
        availableDevelopmentFBMeals.meal = listOf(
            MockUtils.mockMeal(DINNER, true),
            MockUtils.mockMeal(BUFFET_BREAKFAST, true),
            MockUtils.mockMeal(LUNCH, true),
        )
        val soapType = DEVELOPMENT
        val pansionType = parseMeals(availableDevelopmentFBMeals, soapType)

        Assertions.assertThat(pansionType).isEqualByComparingTo(PT_FB)
    }

    @Test
    fun parseRefundRules_fullRefundableTest() {

        val total = 10400
        val checkin = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        val firstCancellationPolicy = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS)
        val cancellationPolicies: List<HotelOfferCancellationPolicy> = listOf(
            getCancellationPolicy(firstCancellationPolicy.toString(), 0),
            getCancellationPolicy(checkin.toString(), total)
        )

        val refundRules = callParseRefundRules(cancellationPolicies, total)

        Assertions.assertThat(refundRules.rules.size).isEqualTo(2)
        Assertions.assertThat(refundRules.rules[0].type).isEqualTo(FULLY_REFUNDABLE)
        Assertions.assertThat(refundRules.rules[0].startsAt).isEqualTo(firstCancellationPolicy)
        Assertions.assertThat(refundRules.rules[0].endsAt).isEqualTo(checkin)

        Assertions.assertThat(refundRules.rules[1].type).isEqualTo(NON_REFUNDABLE)
        Assertions.assertThat(refundRules.rules[1].startsAt).isEqualTo(checkin)
        Assertions.assertThat(refundRules.rules[1].endsAt).isNull()
    }

    @Test
    fun parseRefundRules_nonRefundableTest() {

        val total = 10400
        val firstCancellationPolicy = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS)
        val cancellationPolicies: List<HotelOfferCancellationPolicy> = listOf(
            getCancellationPolicy(firstCancellationPolicy.toString(), 10400)
        )

        val refundRules = callParseRefundRules(cancellationPolicies, total)

        Assertions.assertThat(refundRules.rules.size).isEqualTo(1)

        Assertions.assertThat(refundRules.rules[0].type).isEqualTo(NON_REFUNDABLE)
        Assertions.assertThat(refundRules.rules[0].startsAt).isEqualTo(firstCancellationPolicy)
        Assertions.assertThat(refundRules.rules[0].endsAt).isNull()
    }

    @Test
    fun parseRefundRules_refundableWithPenaltyTest() {
        val firstCancellationPolicy = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS)
        val secondCancellationPolicy = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        val checkin = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS)
        val cancellationPolicies = listOf(
            getCancellationPolicy(firstCancellationPolicy.toString(), 0),
            getCancellationPolicy(secondCancellationPolicy.toString(), 5000),
            getCancellationPolicy(checkin.toString(), 10400)
        )
        val total = 10400

        val refundRules = callParseRefundRules(cancellationPolicies, total)
        Assertions.assertThat(refundRules.rules.size).isEqualTo(3)

        Assertions.assertThat(refundRules.rules[0].type).isEqualTo(FULLY_REFUNDABLE)
        Assertions.assertThat(refundRules.rules[0].penalty).isNull()
        Assertions.assertThat(refundRules.rules[0].startsAt).isEqualTo(firstCancellationPolicy)
        Assertions.assertThat(refundRules.rules[0].endsAt).isEqualTo(secondCancellationPolicy)

        Assertions.assertThat(refundRules.rules[1].type).isEqualTo(REFUNDABLE_WITH_PENALTY)
        Assertions.assertThat(refundRules.rules[1].penalty).isEqualTo(Money.of(5000, "RUB"))
        Assertions.assertThat(refundRules.rules[1].startsAt).isEqualTo(secondCancellationPolicy)
        Assertions.assertThat(refundRules.rules[1].endsAt).isEqualTo(checkin)

        Assertions.assertThat(refundRules.rules[2].type).isEqualTo(NON_REFUNDABLE)
        Assertions.assertThat(refundRules.rules[2].penalty).isNull()
        Assertions.assertThat(refundRules.rules[2].startsAt).isEqualTo(checkin)
        Assertions.assertThat(refundRules.rules[2].endsAt).isNull()
    }

    @Test
    fun parseRefundRules_crashEqualDatesTest() {
        val firstCancellationPolicy = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS)
        val cancellationPolicies = listOf(
            getCancellationPolicy(firstCancellationPolicy.toString(), 0),
            getCancellationPolicy(firstCancellationPolicy.toString(), 5000)
        )
        val total = 5000
        try {
            callParseRefundRules(cancellationPolicies, total)
        } catch (e: RuntimeException) {
            Assertions.assertThat(e.localizedMessage)
                .endsWith("startAt $firstCancellationPolicy is equaling endsAt $firstCancellationPolicy")
        }
    }

    @Test
    @Ignore
    fun parseRefundRules_crashFirstDateTest() {
        val firstCancellationPolicy = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS)
        val cancellationPolicies = listOf(
            getCancellationPolicy(firstCancellationPolicy.toString(), 0)
        )
        val total = 5000
        try {
            callParseRefundRules(cancellationPolicies, total)
        } catch (e: RuntimeException) {
            Assertions.assertThat(e.localizedMessage).endsWith("lower than first cancelationPolicy")
        }
    }

    @Test
    fun parseRefundRules_crashLastNonRefundableTest() {
        val firstCancellationPolicy = Instant.now().minus(1, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS)
        val cancellationPolicies = listOf(
            getCancellationPolicy(firstCancellationPolicy.toString(), 0)
        )
        val total = 5000
        try {
            callParseRefundRules(cancellationPolicies, total)
        } catch (e: RuntimeException) {
            Assertions.assertThat(e.localizedMessage)
                .startsWith("ru.yandex.travel.hotels.common.partners.bronevik.BronevikRefundRulesException: Last cancellation policy must NON_REFUNDABLE")
        }
    }

    @Test
    fun generateMeals_Test() {

        val ans = BronevikUtils.generateMeals(MockUtils.mockNotIncludedFullBoardBreakfastDinner().meal, 0, Stack())
        Assertions.assertThat(ans.size).isEqualTo(8)

        val fullBoard =
            ans.filter { availableMeals -> availableMeals.meal.size == 1 && availableMeals.meal[0].id == FULL_BOARD.value }
        Assertions.assertThat(fullBoard.size).isEqualTo(1)

        val breakfast =
            ans.filter { availableMeals -> availableMeals.meal.size == 1 && availableMeals.meal[0].id == BUFFET_BREAKFAST.value }
        Assertions.assertThat(breakfast.size).isEqualTo(1)

        val dinner =
            ans.filter { availableMeals -> availableMeals.meal.size == 1 && availableMeals.meal[0].id == DINNER.value }
        Assertions.assertThat(dinner.size).isEqualTo(1)
    }

    @Test
    fun generateMealsForOffer_Test() {

        val ans = BronevikUtils.generateMealsForOffer(MockUtils.mockIncludedDinnerNotFullBoardBreakfast(), DEVELOPMENT)
        Assertions.assertThat(ans.size).isEqualTo(2)

        val dinner =
            ans.filter { availableMeals -> availableMeals.meal.size == 1 && availableMeals.meal[0].id == DINNER.value }
        Assertions.assertThat(dinner.size).isEqualTo(1)

        val breakfastAndDinner = ans.filter { availableMeals ->
            availableMeals.meal.size == 2
                && availableMeals.meal.map(AvailableMeal::getId).contains(BUFFET_BREAKFAST.value)
                && availableMeals.meal.map(AvailableMeal::getId).contains(DINNER.value)
        }
        Assertions.assertThat(breakfastAndDinner.size).isEqualTo(1)
    }

    private fun callParseRefundRules(
        cancellationPolicies: List<HotelOfferCancellationPolicy>,
        total: Int
    ): RefundRules {
        return try {
            BronevikUtils.parseRefundRules(cancellationPolicies, total)
        } catch (e: BronevikRefundRulesException) {
            throw RuntimeException(e)
        }
    }

    private fun getCancellationPolicy(penaltyDateTime: String?, penalty: Int): HotelOfferCancellationPolicy {
        val cancellationPolicy = HotelOfferCancellationPolicy()
        val clientPriceDetails = ClientPriceDetails()
        val priceDetails = CurrencyPriceDetails()
        val detailedPrice = DetailedPrice()
        detailedPrice.price = penalty.toFloat()
        detailedPrice.currency = "RUB"
        priceDetails.gross = detailedPrice
        clientPriceDetails.clientCurrency = priceDetails
        cancellationPolicy.penaltyPriceDetails = clientPriceDetails
        cancellationPolicy.penaltyDateTime = penaltyDateTime?.let { formatDate(it) }

        return cancellationPolicy
    }

    private fun formatDate(date: String): XMLGregorianCalendar {
        return try {
            DatatypeFactory.newInstance().newXMLGregorianCalendar(date)
        } catch (e: DatatypeConfigurationException) {
            throw RuntimeException(e)
        }
    }
}
