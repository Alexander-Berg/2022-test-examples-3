package ru.yandex.market.logistics.calendaring.service.limit

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.util.stream.Stream

class QuotaDatesForOutboundProviderTest(
    @Autowired private val quotaDatesForOutboundProvider: QuotaDatesForOutboundProvider
) : AbstractContextualTest() {

    @ParameterizedTest
    @MethodSource("types")
    fun provide(partnerType: PartnerType, bookingType: BookingType, clazz: Class<QuotaDatesForOutbound>) {
        assertions()
            .assertThat(quotaDatesForOutboundProvider.provide(partnerType, bookingType))
            .isInstanceOf(clazz)
    }

    companion object {
        @JvmStatic
        fun types(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    PartnerType.FULFILLMENT,
                    BookingType.SUPPLY,
                    QuotaDatesForOutboundFulfillment::class.java
                ),
                Arguments.of(
                    PartnerType.FULFILLMENT,
                    BookingType.XDOCK_TRANSPORT_SUPPLY,
                    QuotaDatesForOutboundFulfillment::class.java
                ),
                Arguments.of(
                    PartnerType.FULFILLMENT,
                    BookingType.WITHDRAW,
                    QuotaDatesForOutboundFulfillment::class.java
                ),
                Arguments.of(
                    PartnerType.FULFILLMENT,
                    BookingType.MOVEMENT_WITHDRAW,
                    QuotaDatesForOutboundFulfillment::class.java
                ),
                Arguments.of(
                    PartnerType.FULFILLMENT,
                    BookingType.XDOCK_TRANSPORT_WITHDRAW,
                    QuotaDatesForOutboundDistributionCenter::class.java
                ),

                Arguments.of(
                    PartnerType.SORTING_CENTER,
                    BookingType.SUPPLY,
                    QuotaDatesForOutboundSortCenter::class.java
                ),
                Arguments.of(
                    PartnerType.SORTING_CENTER,
                    BookingType.MOVEMENT_SUPPLY,
                    QuotaDatesForOutboundSortCenter::class.java
                ),
                Arguments.of(
                    PartnerType.SORTING_CENTER,
                    BookingType.XDOCK_TRANSPORT_SUPPLY,
                    QuotaDatesForOutboundSortCenter::class.java
                ),
                Arguments.of(
                    PartnerType.SORTING_CENTER,
                    BookingType.WITHDRAW,
                    QuotaDatesForOutboundSortCenter::class.java
                ),
                Arguments.of(
                    PartnerType.SORTING_CENTER,
                    BookingType.MOVEMENT_WITHDRAW,
                    QuotaDatesForOutboundSortCenter::class.java
                ),
                Arguments.of(
                    PartnerType.SORTING_CENTER,
                    BookingType.XDOCK_TRANSPORT_WITHDRAW,
                    QuotaDatesForOutboundSortCenter::class.java
                ),

                Arguments.of(
                    PartnerType.DISTRIBUTION_CENTER,
                    BookingType.SUPPLY,
                    QuotaDatesForOutboundDistributionCenter::class.java
                ),
                Arguments.of(
                    PartnerType.DISTRIBUTION_CENTER,
                    BookingType.MOVEMENT_SUPPLY,
                    QuotaDatesForOutboundDistributionCenter::class.java
                ),
                Arguments.of(
                    PartnerType.DISTRIBUTION_CENTER,
                    BookingType.XDOCK_TRANSPORT_SUPPLY,
                    QuotaDatesForOutboundDistributionCenter::class.java
                ),
                Arguments.of(
                    PartnerType.DISTRIBUTION_CENTER,
                    BookingType.WITHDRAW,
                    QuotaDatesForOutboundDistributionCenter::class.java
                ),
                Arguments.of(
                    PartnerType.DISTRIBUTION_CENTER,
                    BookingType.MOVEMENT_WITHDRAW,
                    QuotaDatesForOutboundDistributionCenter::class.java
                ),
                Arguments.of(
                    PartnerType.DISTRIBUTION_CENTER,
                    BookingType.XDOCK_TRANSPORT_WITHDRAW,
                    QuotaDatesForOutboundDistributionCenter::class.java
                ),
            )
        }
    }
}
