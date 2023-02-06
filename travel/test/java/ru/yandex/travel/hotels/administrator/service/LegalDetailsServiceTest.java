package ru.yandex.travel.hotels.administrator.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.travel.hotels.administrator.EntityCreatingUtils;
import ru.yandex.travel.hotels.administrator.entity.LegalDetails;

import static org.assertj.core.api.Assertions.assertThat;


@ActiveProfiles("test")
@Slf4j
public class LegalDetailsServiceTest {
    @Test
    public void testHasNoMajorChanges() {
        LegalDetails legalDetails = EntityCreatingUtils.legalDetails(false);

        // equal
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .build()))
                .isTrue();

        // not major change
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .postAddress("4242").build()))
                .isTrue();
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .legalPostCode("4242").build()))
                .isTrue();
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .legalAddress("4242").build()))
                .isTrue();

        // major changes - must check each one of them
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .inn("4242").build()))
                .isFalse();
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .kpp("4242").build()))
                .isFalse();
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .bic("4242").build()))
                .isFalse();
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .paymentAccount("4242").build()))
                .isFalse();
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .legalName("4242").build()))
                .isFalse();
        assertThat(LegalDetailsService.hasNoMajorChanges(legalDetails, EntityCreatingUtils.hotelConnectionUpdate()
                .fullLegalName("4242").build()))
                .isFalse();
    }
}
