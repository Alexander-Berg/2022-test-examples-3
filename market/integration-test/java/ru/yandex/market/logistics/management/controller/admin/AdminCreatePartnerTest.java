package ru.yandex.market.logistics.management.controller.admin;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.partner.PartnerNewDto;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.model.result.PartnerProcessingResult;
import ru.yandex.market.logistics.management.queue.producer.PartnerBillingRegistrationTaskProducer;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/admin/partnerCreation/prepare_data.xml")
class AdminCreatePartnerTest extends AbstractContextualTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private PartnerRepository partnerRepository;
    @Autowired
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> partnerBillingClientCreationTaskProducer;

    @BeforeEach
    void setup() {
        Mockito.reset(partnerBillingClientCreationTaskProducer);
        Mockito.doNothing().when(partnerBillingClientCreationTaskProducer)
            .produceTask(any(PartnerProcessingResult.class));
    }

    @Test
    @DisplayName("???????????????? ?????????? ???????????????? ?????????? ????????????????")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_PARTNER_EDIT)
    void getNewPartnerView() throws Exception {
        mockMvc.perform(get("/admin/lms/partner/new"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/partnerCreation/response/new_partner_view.json"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("createPartnerData")
    @DisplayName("???????????????? ?????????????????? ???????????? ??????????")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    void createPartner(String caseName, PartnerType partnerType, boolean shouldBeSuccessful) throws Exception {
        ResultActions resultActions = mockMvc.perform(post("/admin/lms/partner")
            .contentType(MediaType.APPLICATION_JSON)
            .content(String.format(
                pathToJson("data/controller/admin/partnerCreation/request/create.json"),
                partnerType.name()
            )));
        if (shouldBeSuccessful) {
            resultActions
                .andExpect(status().isCreated())
                .andExpect(header().string("location", "http://localhost/admin/lms/partner/120"));
            List<Partner> allPartners = partnerRepository.findAll();
            softly.assertThat(allPartners).hasSize(1);
            Partner partner = allPartners.get(0);
            softly.assertThat(partner.getId()).isEqualTo(120L);
            softly.assertThat(partner.getMarketId()).isEqualTo(12432L);
            softly.assertThat(partner.getName()).isEqualTo("LOGISTICS PARTNER");
            softly.assertThat(partner.getReadableName()).isEqualTo("The best logistics partner");
            softly.assertThat(partner.getPartnerType()).isEqualTo(partnerType);
            softly.assertThat(partner.getLegalInfo().getId()).isEqualTo(1L);
            softly.assertThat(partner.getLocationId()).isEqualTo(225);
            softly.assertThat(partner.getRating()).isEqualTo(10050);
            softly.assertThat(partner.getBillingClientId()).isEqualTo(453421L);
            softly.assertThat(partner.getPassportUid()).isEqualTo(12477343L);
            softly.assertThat(partner.getDomain()).isEqualTo("https://partner.site");
            softly.assertThat(partner.getLogoUrl()).isEqualTo("https://partner.site/images/logo.png");
        } else {
            resultActions.andExpect(status().isBadRequest());
            softly.assertThat(partnerRepository.findAll()).isEmpty();
        }
    }

    private static Stream<Arguments> createPartnerData() {
        return Stream.of(
            Arguments.of("???????????? ????????????????", PartnerType.DELIVERY, true),
            Arguments.of("?????????????????????????????????? ?????????? X-DOC", PartnerType.DISTRIBUTION_CENTER, true),
            Arguments.of("1P-??????????????????", PartnerType.FIRST_PARTY_SUPPLIER, true),
            Arguments.of("????????????????????", PartnerType.SCRAP_DISPOSER, true),
            Arguments.of("xDoc ??????????", PartnerType.XDOC, true),
            Arguments.of("??????????????-??????????", PartnerType.DROPSHIP, false),
            Arguments.of("????????????????-??????????", PartnerType.SUPPLIER, false),
            Arguments.of("?????????????? - ?????????????????????? ???? ???????????????? ??????", PartnerType.OWN_DELIVERY, false),
            Arguments.of("??????????????, ?????? ???????????????????? ???? ????????????????", PartnerType.DROPSHIP_BY_SELLER, false)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("createInvalidPartnerData")
    @DisplayName("?????????????? ?????????????? ?????????????????????? ????????????????")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER_EDIT})
    void createInvalidPartner(String caseName, String requestContent) throws Exception {
        mockMvc.perform(post("/admin/lms/partner")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestContent))
            .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> createInvalidPartnerData() throws IOException {
        String jsonTemplate = pathToJson(
            "data/controller/admin/partnerCreation/response/invalid_response_template.json");
        return Stream.of(
            Arguments.of(
                "?????????????? ?????? ??????????",
                generateRequest(dto -> dto.setName(null))
            ),
            Arguments.of(
                "?????????????? ?????? ?????????????????? ??????????",
                generateRequest(dto -> dto.setReadableName(null))
            ),
            Arguments.of(
                "?????????????? ?????? ????????",
                generateRequest(dto -> dto.setPartnerType(null))
            ),
            Arguments.of(
                "?????????????? ?????? ?????????????????????? ????????????????????",
                generateRequest(dto -> dto.setLegalInfoId(null))
            ),
            Arguments.of(
                "?????????????? ?? ?????????????????????????? ??????????????????",
                generateRequest(dto -> dto.setRating(-10))
            )
        );
    }

    @Nonnull
    private static String generateRequest(Consumer<PartnerNewDto> consumer) throws IOException {
        PartnerNewDto dto = MAPPER.readValue(
            String.format(pathToJson("data/controller/admin/partnerCreation/request/create.json"), "DELIVERY"),
            PartnerNewDto.class
        );
        consumer.accept(dto);
        return MAPPER.writeValueAsString(dto);
    }
}
