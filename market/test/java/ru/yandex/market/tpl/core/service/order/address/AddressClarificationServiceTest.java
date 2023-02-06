package ru.yandex.market.tpl.core.service.order.address;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.GpsCoord;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalAddressKeys;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.core.domain.order.AddressClarification;
import ru.yandex.market.tpl.core.domain.order.AddressClarificationDto;
import ru.yandex.market.tpl.core.domain.order.OrderDeliveryRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
public class AddressClarificationServiceTest extends TplAbstractTest {

    private final AddressClarificationService addressClarificationService;
    private final TestDataFactory testDataFactory;
    private final AddressGenerator addressGenerator;
    @MockBean
    private final OrderDeliveryRepository orderDeliveryRepository;
    @MockBean
    private final PersonalExternalService personalExternalService;

    private long BUYER_UID;
    private String CITY;
    private String STREET;
    private String HOUSE;
    private String BUILDING;
    private String HOUSING;
    private Instant NOW = Instant.now();
    private Map<String, String> clarification1;
    private Map<String, String> clarification2;
    private Map<String, String> clarification3;
    private DeliveryAddress deliveryAddress;

    @BeforeEach
    void init() {
        BUYER_UID = 111L;
        CITY = "Москва";
        STREET = "Пушкина";
        HOUSE = "32";
        BUILDING = "2";
        HOUSING = "1";
        clarification1 = Map.of(
                PersonalAddressKeys.LOCALITY.getName(), CITY,
                PersonalAddressKeys.STREET.getName(), STREET,
                PersonalAddressKeys.HOUSE.getName(), HOUSE,
                PersonalAddressKeys.BUILDING.getName(), BUILDING,
                PersonalAddressKeys.HOUSING.getName(), HOUSING,
                PersonalAddressKeys.FLOOR.getName(), "7"
        );
        clarification2 = Map.of(
                PersonalAddressKeys.LOCALITY.getName(), CITY,
                PersonalAddressKeys.STREET.getName(), STREET,
                PersonalAddressKeys.HOUSE.getName(), HOUSE,
                PersonalAddressKeys.BUILDING.getName(), BUILDING,
                PersonalAddressKeys.HOUSING.getName(), HOUSING,
                PersonalAddressKeys.FLOOR.getName(), "8"
        );
        clarification3 = Map.of(
                PersonalAddressKeys.LOCALITY.getName(), CITY,
                PersonalAddressKeys.STREET.getName(), STREET,
                PersonalAddressKeys.HOUSE.getName(), HOUSE,
                PersonalAddressKeys.BUILDING.getName(), BUILDING,
                PersonalAddressKeys.HOUSING.getName(), HOUSING,
                PersonalAddressKeys.FLOOR.getName(), "6"
        );
        deliveryAddress = addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder()
                .city(CITY).street(STREET).house(HOUSE).building(BUILDING).housing(HOUSING)
                .build());
    }

    @Test
    @DisplayName("Тест получения уточнения адреса при использовании сервиса Personal")
    void getPreviousPersonalAddressClarification() {
        // given
        when(orderDeliveryRepository.findPersonalClarificationByBuyerUid(BUYER_UID)).thenReturn(
                List.of(
                        AddressClarificationDto.builder()
                                .personalAddressId("123")
                                .personalGpsId("456")
                                .updatedAt(NOW.minusSeconds(180))
                                .build(),
                        AddressClarificationDto.builder()
                                .personalAddressId("qwer")
                                .personalGpsId("asdf")
                                .updatedAt(NOW.minusSeconds(50))
                                .build(),
                        AddressClarificationDto.builder()
                                .personalAddressId("0987")
                                .personalGpsId("zxcv")
                                .updatedAt(NOW.minusSeconds(200))
                                .build()
                )
        );
        when(personalExternalService.getMultiTypePersonalByIds(anyList())).thenReturn(
                List.of(
                        new MultiTypeRetrieveResponseItem()
                                .id("123")
                                .type(CommonTypeEnum.ADDRESS)
                                .value(new CommonType().address(clarification1)),
                        new MultiTypeRetrieveResponseItem()
                                .id("0987")
                                .type(CommonTypeEnum.ADDRESS)
                                .value(new CommonType().address(clarification3)),
                        new MultiTypeRetrieveResponseItem()
                                .id("qwer")
                                .type(CommonTypeEnum.ADDRESS)
                                .value(new CommonType().address(clarification2))
                ),
                List.of(
                        new MultiTypeRetrieveResponseItem()
                                .id("456")
                                .type(CommonTypeEnum.GPS_COORD)
                                .value(new CommonType().gpsCoord(new GpsCoord())),
                        new MultiTypeRetrieveResponseItem()
                                .id("asdf")
                                .type(CommonTypeEnum.GPS_COORD)
                                .value(new CommonType().gpsCoord(new GpsCoord())),
                        new MultiTypeRetrieveResponseItem()
                                .id("zxcv")
                                .type(CommonTypeEnum.GPS_COORD)
                                .value(new CommonType().gpsCoord(new GpsCoord()))
                )
        );
        DeliveryAddress address = addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder()
                .city(CITY).street(STREET).house(HOUSE).building(BUILDING).housing(HOUSING)
                .build());

        // when
        Optional<AddressClarification> result =
                addressClarificationService.getPreviousPersonalAddressClarification(BUYER_UID, address);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPersonalAddressId()).isEqualTo("qwer");
        assertThat(result.get().getPersonalGpsId()).isEqualTo("asdf");
        assertThat(result.get().getFloor()).isEqualTo("8");
    }

    @Test
    @DisplayName("Тест получения уточнения адреса при использовании сервиса Personal (дубликаты personalId)")
    void getPreviousPersonalAddressClarification_whenDuplicatePersonalId() {
        // given
        when(orderDeliveryRepository.findPersonalClarificationByBuyerUid(BUYER_UID)).thenReturn(
                List.of(
                        AddressClarificationDto.builder()
                                .personalAddressId("qwer")
                                .personalGpsId("456")
                                .updatedAt(NOW.minusSeconds(180))
                                .build(),
                        AddressClarificationDto.builder()
                                .personalAddressId("qwer")
                                .personalGpsId("asdf")
                                .updatedAt(NOW.minusSeconds(50))
                                .build(),
                        AddressClarificationDto.builder()
                                .personalAddressId("0987")
                                .personalGpsId("asdf")
                                .updatedAt(NOW.minusSeconds(200))
                                .build()
                )
        );
        when(personalExternalService.getMultiTypePersonalByIds(anyList())).thenReturn(
                List.of(
                        new MultiTypeRetrieveResponseItem()
                                .id("0987")
                                .type(CommonTypeEnum.ADDRESS)
                                .value(new CommonType().address(clarification3)),
                        new MultiTypeRetrieveResponseItem()
                                .id("qwer")
                                .type(CommonTypeEnum.ADDRESS)
                                .value(new CommonType().address(clarification2))
                ),
                List.of(
                        new MultiTypeRetrieveResponseItem()
                                .id("456")
                                .type(CommonTypeEnum.GPS_COORD)
                                .value(new CommonType().gpsCoord(new GpsCoord())),
                        new MultiTypeRetrieveResponseItem()
                                .id("asdf")
                                .type(CommonTypeEnum.GPS_COORD)
                                .value(new CommonType().gpsCoord(new GpsCoord()))
                )
        );

        // when
        Optional<AddressClarification> result =
                addressClarificationService.getPreviousPersonalAddressClarification(BUYER_UID, deliveryAddress);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getPersonalAddressId()).isEqualTo("qwer");
        assertThat(result.get().getPersonalGpsId()).isEqualTo("asdf");
        assertThat(result.get().getFloor()).isEqualTo("8");
    }

    @Test
    @DisplayName("Тест получения уточнения адреса при использовании сервиса Personal (в бд нет дополнений)")
    void getPreviousPersonalAddressClarification_Empty() {
        // given
        var now = Instant.now();
        long BUYER_UID = 111L;
        DeliveryAddress address = addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder().build());
        when(orderDeliveryRepository.findPersonalClarificationByBuyerUid(BUYER_UID))
                .thenReturn(List.of());

        // when
        Optional<AddressClarification> result =
                addressClarificationService.getPreviousPersonalAddressClarification(BUYER_UID, address);

        // then
        assertThat(result).isEmpty();
    }

}
