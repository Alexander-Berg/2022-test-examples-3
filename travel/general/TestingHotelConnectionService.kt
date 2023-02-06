package ru.yandex.travel.hotels.extranet.cache

import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.yandex.travel.hotels.extranet.entities.HotelIdentifier

/**
 * In testing we want to send email to tester's email
 */
@Profile("testing")
@Primary
@Component
class TestingHotelConnectionService(private val delegate: IHotelConnectionService) : IHotelConnectionService {
    override fun getByHotelIdKey(hotelIdentifier: HotelIdentifier, requestedEmail: String?): HotelConnection {
        val result = delegate.getByHotelIdKey(hotelIdentifier) ?: HotelConnection(
            -1,
            hotelIdentifier.toProto(),
            "",
            "",
            ""
        )
        requestedEmail?.let {
            // as we use test sender, it'll send an email only to yandex-team account anyway, no need to filter here
            result.testEmail = requestedEmail
        }
        return result
    }
}
