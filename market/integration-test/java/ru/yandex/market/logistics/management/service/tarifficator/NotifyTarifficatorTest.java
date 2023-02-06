package ru.yandex.market.logistics.management.service.tarifficator;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.ResourceAccessException;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.model.enums.PartnerSubtypeConstants;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.repository.PartnerSubtypeRepository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Тесты нотификации тарификатора о новых партнерских службах ПВЗ")
class NotifyTarifficatorTest extends AbstractContextualTest {

    @Autowired
    private PartnerRepository partnerRepository;
    @Autowired
    private PartnerSubtypeRepository partnerSubtypeRepository;

    @Autowired
    private NotifyTarifficatorService notifyTarifficatorService;

    @Rule
    public WireMockRule tarifficatorMock = new WireMockRule(28080);

    @AfterEach
    public void cleanup() {
        tarifficatorMock.shutdown();
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DatabaseSetup({
        "/data/service/tarifficator/before/partner_subtype.xml",
        "/data/service/tarifficator/before/partner_1.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/tarifficator/after/single_partner_triggered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addPartnersWithEligibleSubtype(
        @SuppressWarnings("unused") String displayName,
        Long partnerSubtypeId
    ) {
        setPartnerSubtype(partnerSubtypeId);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource(value = "addPartnersWithEligibleSubtype")
    @DatabaseSetup({
        "/data/service/tarifficator/before/partner_subtype.xml",
        "/data/service/tarifficator/before/partner_null_subtype.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/tarifficator/after/single_partner_triggered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addPartnersWithNullSubtype(
        @SuppressWarnings("unused") String displayName,
        Long partnerSubtypeId
    ) {
        setPartnerSubtype(partnerSubtypeId);
    }

    @Nonnull
    private static Stream<Arguments> addPartnersWithEligibleSubtype() {
        return Stream.of(
            Arguments.of(
                "Маркет свои ПВЗ",
                3L
            ),
            Arguments.of(
                "Партнерские ПВЗ (ИПэшники)",
                4L
            ),
            Arguments.of(
                "Маркет Локеры",
                5L
            ),
            Arguments.of(
                "Go Платформа",
                103L
            ),
            Arguments.of(
                "Постаматы партнеров GO",
                PartnerSubtypeConstants.GO_PARTNER_LOCKER_ID
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DatabaseSetup({
        "/data/service/tarifficator/before/partner_subtype.xml",
        "/data/service/tarifficator/before/partner_1.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/tarifficator/after/empty_partner_to_notify_tarifficator.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void addPartnersWithIneligibleSubtype(
        @SuppressWarnings("unused") String displayName,
        Long partnerSubtypeId
    ) {
        setPartnerSubtype(partnerSubtypeId);
    }

    @Nonnull
    private static Stream<Arguments> addPartnersWithIneligibleSubtype() {
        return Stream.of(
            Arguments.of(
                "Партнерская доставка (контрактная)",
                1L
            ),
            Arguments.of(
                "Маркет Курьер",
                2L
            ),
            Arguments.of(
                "СЦ для МК",
                6L
            ),
            Arguments.of(
                "Партнерский СЦ",
                7L
            ),
            Arguments.of(
                "Такси-Лавка",
                8L
            ),
            Arguments.of(
                "Такси-Экспресс",
                34L
            )
        );
    }

    @Test
    @DatabaseSetup({
        "/data/service/tarifficator/before/partner_subtype.xml",
        "/data/service/tarifficator/before/partner_3.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/tarifficator/after/empty_partner_to_notify_tarifficator.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void jobShouldCallTarifficator() {
        tarifficatorMock.stubFor(
            post(urlEqualTo("/tarifficator/tariffs/destinationPartners/1"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                )
        );
        tarifficatorMock.start();

        notifyTarifficatorService.execute();
    }

    @Test
    @DatabaseSetup({
        "/data/service/tarifficator/before/partner_subtype.xml",
        "/data/service/tarifficator/before/partner_3.xml"
    })
    @ExpectedDatabase(
        value = "/data/service/tarifficator/after/single_partner_triggered.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void jobShouldNotDeleteNotifyIfTarifficatorDidntRespond200() {
        assertThrows(ResourceAccessException.class, () -> notifyTarifficatorService.execute());
    }

    private void setPartnerSubtype(Long partnerSubtypeId) {
        var partner = partnerRepository.getPartner(1);
        partner.setPartnerSubtype(partnerSubtypeRepository.findByIdOrThrow(partnerSubtypeId));
        partnerRepository.save(partner);
    }
}
