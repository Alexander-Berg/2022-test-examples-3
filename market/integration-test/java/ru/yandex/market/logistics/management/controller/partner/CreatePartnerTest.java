package ru.yandex.market.logistics.management.controller.partner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto;
import ru.yandex.market.logistics.management.entity.request.partner.LegalInfoDto;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.TaxationSystem;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.model.result.PartnerProcessingResult;
import ru.yandex.market.logistics.management.queue.producer.PartnerBillingRegistrationTaskProducer;
import ru.yandex.market.logistics.management.util.CleanDatabase;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@DatabaseSetup("/data/controller/partner/createPartner/setup.xml")
class CreatePartnerTest extends AbstractContextualTest {

    private static final String RESOURCE_PATH = "data/controller/create_partner/";
    private static final Map<PartnerType, Long> SUBTYPE_ID_BY_PARTNER_TYPE = Map.of(
        PartnerType.DELIVERY, 7L,
        PartnerType.FULFILLMENT, 8L,
        PartnerType.SORTING_CENTER, 9L,
        PartnerType.SUPPLIER, 10L,
        PartnerType.XDOC, 11L,
        PartnerType.DROPSHIP, 12L,
        PartnerType.OWN_DELIVERY, 13L
    );
    private static final Set<PartnerType> BILLING_REGISTERED_TYPES = Set.of(
        PartnerType.DELIVERY,
        PartnerType.FULFILLMENT,
        PartnerType.SORTING_CENTER
    );

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> partnerBillingClientCreationTaskProducer;

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("ALTER SEQUENCE partner_sequence RESTART WITH 47723");
        Mockito.reset(partnerBillingClientCreationTaskProducer);
        Mockito.doNothing().when(partnerBillingClientCreationTaskProducer)
            .produceTask(any(PartnerProcessingResult.class));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_delivery.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание DELIVERY партнера")
    void createDeliveryPartner() throws Exception {
        execRequestSuccess(partner(PartnerType.DELIVERY), "create_delivery_partner_response.json");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_fulfillment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание FULFILLMENT партнера")
    void createFulfillmentPartner() throws Exception {
        execRequestSuccess(
            partner(PartnerType.FULFILLMENT, partnerBuilder -> partnerBuilder.id(777L)),
            "create_fulfillment_partner_response.json"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание SC партнера")
    void createScPartner() throws Exception {
        execRequestSuccess(
            partner(PartnerType.SORTING_CENTER, partnerBuilder -> partnerBuilder.id(888L)),
            "create_sc_partner_response.json"
        );
    }

    @Test
    @DisplayName("Создание SC партнера с существующим id")
    @DatabaseSetup("/data/controller/partner/createPartner/before/sc.xml")
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_sc.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createScPartnerWithExistingId() throws Exception {
        execRequest(partner(PartnerType.SORTING_CENTER, partnerBuilder -> partnerBuilder.id(888L)))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Partner with id 888 already exists"));
    }

    @Test
    @DisplayName("Создание SC партнера с указанной по идентификатору существующей юридической информацией")
    @DatabaseSetup("/data/controller/partner/createPartner/before/legal_info.xml")
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_sc_existing_legal_info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createScPartnerWithExistingLegalInfo() throws Exception {
        execRequestSuccess(
            partnerBuilder(PartnerType.SORTING_CENTER, exitingLegalInfoBuilder().build()).id(888L).build(),
            "create_sc_partner_response.json"
        );
    }

    @Test
    @DisplayName("Создание SC партнера с указанной по идентификатору отсутствующей юридической информацией")
    void createScPartnerWithNotExistingLegalInfo() throws Exception {
        execRequest(partnerBuilder(PartnerType.SORTING_CENTER, exitingLegalInfoBuilder().build()).id(888L).build())
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Legal info with id 1000"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_own.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание OWN_DELIVERY партнера")
    void createOwnDelivery() throws Exception {
        execRequestSuccess(
            partner(
                PartnerType.OWN_DELIVERY,
                builder -> builder.legalInfo(null).passportUid(null).locationId(null).rating(null)
            ),
            "create_own_delivery_response.json"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_dropship_base_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание DROPSHIP партнера с базовыми полями")
    void createDropshipBaseFields() throws Exception {
        execRequestSuccess(
            basePartnerBuilder(PartnerType.DROPSHIP).build(),
            "create_dropship_partrer_base_fields_response.json"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_dropship_all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание DROPSHIP партнера со всеми полями")
    void createDropshipAllFields() throws Exception {
        execRequestSuccess(partner(PartnerType.DROPSHIP), "create_dropship_partner_all_fields_response.json");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_supplier_base_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание SUPPLIER партнера с базовыми полями")
    void createSupplierBaseFields() throws Exception {
        execRequestSuccess(
            basePartnerBuilder(PartnerType.SUPPLIER).build(),
            "create_supplier_partrer_base_fields_response.json"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_xdoc_all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание XDOC партнера со всеми полями")
    void createXdocAllFields() throws Exception {
        execRequestSuccess(partner(PartnerType.XDOC), "create_xdoc_partner_all_fields_response.json");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_xdoc_base_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание XDOC партнера с базовыми полями")
    void createXdocBaseFields() throws Exception {
        execRequestSuccess(
            basePartnerBuilder(PartnerType.XDOC).build(),
            "create_xdoc_partrer_base_fields_response.json"
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_supplier_all_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание SUPPLIER партнера со всеми полями")
    void createSupplierAllFields() throws Exception {
        execRequestSuccess(partner(PartnerType.SUPPLIER), "create_supplier_partner_all_fields_response.json");
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/partner/createPartner/after/create_dropship_by_seller_base_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание DSBS партнера с базовыми полями")
    void createDropshipBySellerBaseFields() throws Exception {
        execRequestSuccess(
            basePartnerBuilder(PartnerType.DROPSHIP_BY_SELLER)
                .build(),
            "create_dropship_by_seller_partner_base_fields_response.json"
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidSubtypeRequestSource")
    @DisplayName("Создание партнеров с некорректным подстатусом")
    void createPartnerWithInvalidSubstatus(
        @SuppressWarnings("unused") String displayName,
        CreatePartnerDto partner,
        int status,
        String errorMessage
    ) throws Exception {
        execRequest(partner)
            .andExpect(status().is(status))
            .andExpect(status().reason(errorMessage));
    }

    private static Stream<Arguments> invalidSubtypeRequestSource() {
        return Stream.of(
            Arguments.of(
                "Указан несуществующий подтип",
                partner(PartnerType.DELIVERY, partnerBuilder -> partnerBuilder.subtypeId(111L)),
                404,
                "Can't find Partner subtype with id=111"
            ),
            Arguments.of(
                "Указанный подтип не подходит для указанного типа",
                partner(PartnerType.DELIVERY, partnerBuilder -> partnerBuilder.subtypeId(12L)),
                400,
                "Can't set subtype with id=12 and type=DROPSHIP to partner with type=DELIVERY"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidRequestSource")
    @DisplayName("Создание партнеров с невалидным набором полей")
    void createPartnersWithInvalidData(
        @SuppressWarnings("unused") String displayName,
        CreatePartnerDto partner,
        String response
    ) throws Exception {
        execRequest(partner)
            .andExpect(status().isBadRequest())
            .andExpect(testJson(RESOURCE_PATH + response, Option.IGNORING_ARRAY_ORDER, Option.IGNORING_EXTRA_FIELDS));
    }

    private static Stream<Arguments> invalidRequestSource() {
        return Stream.of(
            Arguments.of(
                "Не указаны тип / имя партнера",
                partner(null, partnerBuilder -> partnerBuilder.name(null)),
                "create_without_type_and_name_response.json"
            ),
            Arguments.of(
                "Не указано LegalInfo для DELIVERY партнера",
                partner(PartnerType.DELIVERY, partnerBuilder -> partnerBuilder.legalInfo(null)),
                "create_without_legal_info_response.json"
            ),
            Arguments.of(
                "Не указано LegalInfo для SORTING_CENTER партнера",
                partner(PartnerType.SORTING_CENTER, partnerBuilder -> partnerBuilder.legalInfo(null)),
                "create_without_legal_info_response.json"
            ),
            Arguments.of(
                "Не указано LegalInfo для FULFILLMENT партнера",
                partner(PartnerType.FULFILLMENT, partnerBuilder -> partnerBuilder.legalInfo(null)),
                "create_without_legal_info_response.json"
            ),
            Arguments.of(
                "Пустой LegalInfo",
                partner(
                    PartnerType.DELIVERY,
                    partnerBuilder -> partnerBuilder.legalInfo(LegalInfoDto.newBuilder().build())
                ),
                "create_with_empty_legal_info_response.json"
            ),
            Arguments.of(
                "Некорректный ИНН в LegalInfo",
                partner(
                    PartnerType.DELIVERY,
                    partnerBuilder -> partnerBuilder.legalInfo(legalInfoBuilder().inn("123abc").build())
                ),
                "create_with_invalid_inn_response.json"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("billingRegistrationSchedulingTestSource")
    @DisplayName("Создание задачи регистрации партнера в Балансе")
    void billingRegistrationSchedulingTest(
        @SuppressWarnings("unused") String displayName,
        PartnerType partnerType,
        Boolean shouldRegister
    ) throws Exception {
        execRequest(partner(
            partnerType,
            builder -> builder.billingClientId(null)
        ))
            .andExpect(status().isOk());

        if (shouldRegister) {
            ArgumentCaptor<PartnerProcessingResult> argumentCaptor =
                ArgumentCaptor.forClass(PartnerProcessingResult.class);
            Mockito.verify(partnerBillingClientCreationTaskProducer, Mockito.times(1))
                .produceTask(argumentCaptor.capture());
            softly.assertThat(argumentCaptor.getValue())
                .extracting(PartnerProcessingResult::getPartnerId)
                .isEqualTo(47723L);
        } else {
            Mockito.verifyZeroInteractions(partnerBillingClientCreationTaskProducer);
        }
    }

    private static Stream<Arguments> billingRegistrationSchedulingTestSource() {
        return Arrays.stream(PartnerType.values())
            .map(type -> Arguments.of(
                type.name(),
                type,
                BILLING_REGISTERED_TYPES.contains(type)
            ));
    }

    private static CreatePartnerDto partner(PartnerType type) {
        return partnerBuilder(type, legalInfoBuilder().build()).build();
    }

    private static CreatePartnerDto partner(
        PartnerType type,
        Consumer<CreatePartnerDto.Builder> partnerModifier
    ) {
        CreatePartnerDto.Builder partnerBuilder = partnerBuilder(type, legalInfoBuilder().build());
        partnerModifier.accept(partnerBuilder);
        return partnerBuilder.build();
    }

    private static CreatePartnerDto.Builder partnerBuilder(PartnerType type, LegalInfoDto legalInfo) {
        return basePartnerBuilder(type)
            .marketId(829725L)
            .businessId(2222L)
            .passportUid(34234L)
            .billingClientId(5343L)
            .domain("http://partner.test")
            .logoUrl("http://partner.test/avatarka.jpg")
            .locationId(213)
            .rating(5)
            .legalInfo(legalInfo)
            .subtypeId(Optional.ofNullable(type).map(SUBTYPE_ID_BY_PARTNER_TYPE::get).orElse(null))
            .taxationSystem(TaxationSystem.COMMON)
            .defaultOutletName("default name");
    }

    private static CreatePartnerDto.Builder basePartnerBuilder(PartnerType type) {
        return CreatePartnerDto.newBuilder()
            .partnerType(type)
            .name("partner")
            .readableName("Partner Partner");
    }

    private static LegalInfoDto.LegalInfoDtoBuilder legalInfoBuilder() {
        return LegalInfoDto.newBuilder()
            .incorporation("Roga and Kopyta")
            .legalForm("OOO")
            .inn("010203")
            .ogrn(555777L)
            .url("roga.kopyta.org")
            .phone("+79139136328")
            .email("roga@kopyta.ru")
            .bik("112233")
            .kpp("332211")
            .account("account")
            .legalAddress(addressBuilder().settlement("Юридическое село").build())
            .postAddress(addressBuilder().settlement("Фактическое село").build());
    }

    private static LegalInfoDto.LegalInfoDtoBuilder exitingLegalInfoBuilder() {
        return LegalInfoDto.newBuilder()
            .id(1000L)
            .incorporation("E corp")
            .legalForm("Inc")
            .inn("606060606")
            .ogrn(666666666L)
            .url("e-corp.com")
            .phone("+199911111111")
            .email("price@e-corp.com")
            .bik("bik E")
            .kpp("kpp E")
            .account("account E");
    }

    private static Address.AddressBuilder addressBuilder() {
        return Address.newBuilder()
            .addressString("село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2")
            .shortAddressString("село Зудово, Солнечная улица, 9A, 2")
            .locationId(133543)
            .latitude(BigDecimal.valueOf(55.822463D))
            .longitude(BigDecimal.valueOf(84.258002D))
            .postCode("633372")
            .region("Новосибирская область")
            .subRegion("Болотнинский район")
            .street("Солнечная")
            .house("6")
            .housing("2")
            .building("А")
            .apartment("318");
    }

    @Nonnull
    private ResultActions execRequest(CreatePartnerDto partner) throws Exception {
        return mockMvc.perform(post("/externalApi/partners")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(partner)));
    }

    private void execRequestSuccess(CreatePartnerDto partner, String responsePath) throws Exception {
        mockMvc.perform(post("/externalApi/partners")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(partner)))
            .andExpect(status().is(200))
            .andExpect(content().json(pathToJson(responsePath)));
    }

    private static String pathToJson(String relativePath) {
        return TestUtil.pathToJson(RESOURCE_PATH + relativePath);
    }
}
