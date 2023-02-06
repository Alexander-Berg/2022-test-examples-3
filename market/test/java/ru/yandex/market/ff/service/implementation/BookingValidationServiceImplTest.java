package ru.yandex.market.ff.service.implementation;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;

public class BookingValidationServiceImplTest extends IntegrationTest {

    @Autowired
    private final BookingValidationServiceImpl bookingValidationService;

    public BookingValidationServiceImplTest(
            BookingValidationServiceImpl bookingValidationService) {
        this.bookingValidationService = bookingValidationService;
    }


}
