package ru.yandex.market.pvz.internal.controller.pi.pickup_point;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.approve.crm_pre_pickup_point.CrmPrePickupPointParams;
import ru.yandex.market.pvz.core.domain.consumable.type.ConsumableTypeParams;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReason;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.DeactivationReasonRepository;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLog;
import ru.yandex.market.pvz.core.domain.pickup_point.deactivation.PickupPointDeactivationLogRepository;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestConsumableTypeFactory;
import ru.yandex.market.pvz.core.test.factory.TestCrmPrePickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointCourierMappingFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LegalPartnerPickupPointControllerTest extends BaseShallowTest {

    private final TestableClock clock;

    private final TestCrmPrePickupPointFactory crmPrePickupPointFactory;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestLegalPartnerFactory legalPartnerFactory;
    private final TestBrandRegionFactory brandRegionFactory;

    private final PickupPointRepository pickupPointRepository;
    private final DeactivationReasonRepository deactivationReasonRepository;
    private final PickupPointDeactivationLogRepository pickupPointDeactivationLogRepository;
    private final TestConsumableTypeFactory consumableTypeFactory;
    private final TestPickupPointCourierMappingFactory pickupPointCourierMappingFactory;

    @Test
    void updateSensitive() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());
        brandRegionFactory.createDefaults();
        mockMvc.perform(
                put("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                        "/pickup-points/" + crmPrePickupPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("pickup_point/request_update_sensitive.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("pickup_point/response_update_sensitive.json")));
    }

    @Test
    void updateSensitiveFullBrandWithNotAllWorkingDays() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());
        brandRegionFactory.createDefaults();
        mockMvc.perform(
                put("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                        "/pickup-points/" + crmPrePickupPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "pickup_point/request_update_sensitive_brand_with_not_all_working_days.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent(
                        "pickup_point/response_update_sensitive_brand_with_not_all_working_days.json")));
    }

    @Test
    void updateSensitiveNotBrand() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());

        mockMvc.perform(
                put("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                        "/pickup-points/" + crmPrePickupPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("pickup_point/request_update_sensitive_not_brand.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("pickup_point/response_update_sensitive_not_brand.json")));
    }

    @Test
    void tryToUpdateSensitiveWithNotApprovedPickupPoint() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());

        mockMvc.perform(
                put("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                        "/pickup-points/" + crmPrePickupPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("pickup_point/request_update_sensitive.json")))
                .andExpect(status().isBadRequest());

    }

    @Test
    void tryToUpdateSensitiveWithNotExistentBrandRegion() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());

        mockMvc.perform(
                put("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                        "/pickup-points/" + crmPrePickupPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "pickup_point/request_update_sensitive_with_not_existent_brand_region.json")))
                .andExpect(status().isBadRequest());

    }

    @Test
    void updateInsensitive() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());

        mockMvc.perform(
                patch("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                        "/pickup-points/" + crmPrePickupPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("pickup_point/request_update_insensitive.json")))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("pickup_point/response_update_insensitive.json"), false));
    }

    @Test
    @SneakyThrows
    void updateFailWithoutSquaresAndInstruction() {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());

        mockMvc.perform(
                put("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                        "/pickup-points/request/" + crmPrePickupPoint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("pickup_point/request_update_request.json")))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", is("Ошибка валидации: " +
                        "Поле [ 'Площадь клиентской части метр кв' ] должно быть задано. Получено значение: 'null'. " +
                        "Поле [ 'Информация о том, как добраться к пункту выдачи' ] не может быть пусто. " +
                        "Получено значение: 'null'. " +
                        "Поле [ 'Площадь складской части метр кв' ] должно быть задано. Получено значение: 'null'.")));
    }

    @Test
    void getPickupPoints() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());
        brandRegionFactory.createDefaults();
        pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .active(true)
                        .build());

        mockMvc.perform(
                get("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() + "/pickup-points/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_get_pickup_points.json"),
                        crmPrePickupPoint.getId(), pickupPoint.getPvzMarketId())));
    }

    @Test
    void getPickupPointsInfo() throws Exception {
        var legalPartner = legalPartnerFactory.createLegalPartner();
        var pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        mockMvc.perform(
                get("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() + "/pickup-points/info")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("pickup_point/response_get_pickup_points_info.json")));
    }

    @Test
    void getDeactivations() throws Exception {
        var date = OffsetDateTime.of(LocalDateTime.of(2021, 11, 30, 11, 15), ZoneOffset.UTC);
        clock.setFixed(date.toInstant(), ZoneOffset.UTC);
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .cashAllowed(false)
                        .returnAllowed(false)
                        .prepayAllowed(true)
                        .active(false)
                        .build());

        brandRegionFactory.createDefaults();

        var firstDeactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();

        var deactivationReason = DeactivationReason.builder()
                .reason("Причина")
                .details("Описание причины")
                .canBeCancelled(true)
                .fullDeactivation(true)
                .build();
        deactivationReason = deactivationReasonRepository.save(deactivationReason);

        var deactivationReasons = deactivationReasonRepository.findAll();

        var currentDeactivation = createDeactivation(pickupPoint.getPvzMarketId(),
                deactivationReason.getId(), date.toLocalDate().plusDays(1), null);
        var previousDeactivation = createDeactivation(pickupPoint.getPvzMarketId(),
                deactivationReason.getId(), date.toLocalDate().plusDays(1), date.toLocalDate().plusDays(1));

        mockMvc.perform(get("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                "/pickup-points/" + crmPrePickupPoint.getId() + "/change-active"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_deactivation.json"),
                        deactivationReasons.get(0).getId(),
                        deactivationReasons.get(1).getId(),
                        currentDeactivation.getId(),
                        previousDeactivation.getId(),
                        firstDeactivation.getId(),
                        firstDeactivation.getDeactivationDate(),
                        firstDeactivation.getDeactivationDate()), true
                ));
    }

    @Test
    void deactivate() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .cashAllowed(false)
                        .returnAllowed(false)
                        .prepayAllowed(true)
                        .active(false)
                        .build());

        brandRegionFactory.createDefaults();

        pickupPointDeactivationLogRepository.findAll().iterator().next();

        var deactivationReason = DeactivationReason.builder()
                .reason("Причина")
                .details("Описание причины")
                .canBeCancelled(true)
                .fullDeactivation(true)
                .build();
        deactivationReason = deactivationReasonRepository.save(deactivationReason);

        var deactivationReasons = deactivationReasonRepository.findAll();

        mockMvc.perform(post("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                "/pickup-points/" + crmPrePickupPoint.getId() + "/change-active")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        getFileContent("pickup_point/request_deactivation_create.json"),
                        deactivationReason.getId(), LocalDate.now(clock))))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_deactivation_create.json"),
                        deactivationReasons.get(1).getId(), deactivationReasons.get(0).getId(),
                        LocalDate.now(clock), LocalDate.now(clock))
                ));
    }

    @Test
    void cancelDeactivation() throws Exception {
        var date = OffsetDateTime.of(LocalDateTime.of(2021, 11, 30, 11, 15), ZoneOffset.UTC);
        clock.setFixed(date.toInstant(), ZoneOffset.UTC);
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .activateImmediately(false)
                        .build());
        pickupPoint = pickupPointFactory.updatePickupPoint(pickupPoint.getId(),
                TestPickupPointFactory.PickupPointTestParams.builder()
                        .cardAllowed(false)
                        .cashAllowed(false)
                        .returnAllowed(false)
                        .prepayAllowed(true)
                        .active(false)
                        .build());

        brandRegionFactory.createDefaults();
        var firstDeactivation = pickupPointDeactivationLogRepository.findAll().iterator().next();

        mockMvc.perform(patch("/v1/pi/partners/" + pickupPoint.getLegalPartner().getPartnerId() +
                "/pickup-points/" + crmPrePickupPoint.getId() + "/change-active?deactivationId="
                + firstDeactivation.getId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_deactivation_cancel.json"),
                        firstDeactivation.getDeactivationReason().getId(),
                        firstDeactivation.getId(),
                        firstDeactivation.getDeactivationDate(),
                        LocalDate.now(clock)), true
                ));
    }

    private PickupPointDeactivationLog createDeactivation(long pvzMarketId, long reasonId,
                                                          LocalDate deactivationDate, LocalDate activationDate) {
        var deactivation = PickupPointDeactivationLog.builder()
                .pickupPoint(pickupPointRepository.findByPvzMarketIdOrThrow(pvzMarketId))
                .deactivationReason(deactivationReasonRepository.findByIdOrThrow(reasonId))
                .deactivationDate(deactivationDate);

        if (activationDate != null) {
            deactivation = deactivation.activationDate(activationDate);
        }
        return pickupPointDeactivationLogRepository.save(deactivation.build());
    }

    @Test
    void getApprovedPickupPoint() throws Exception {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, legalPartner, null
        );

        mockMvc.perform(get("/v1/pi/partners/" + legalPartner.getPartnerId() +
                "/pickup-points/approved"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_approved.json"),
                        pickupPoint.getId(),
                        pickupPoint.getPvzMarketId(),
                        pickupPoint.getName(),
                        pickupPoint.getActive(),
                        pickupPoint.getBrandingType(),
                        pickupPoint.getCashAllowed(),
                        pickupPoint.getPrepayAllowed(),
                        pickupPoint.getCardAllowed(),
                        pickupPoint.getDropOffFeature()
                )));
    }

    @Test
    void getApprovedPickupPointEmpty() throws Exception {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();

        mockMvc.perform(get("/v1/pi/partners/" + legalPartner.getPartnerId() +
                "/pickup-points/approved"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("[]"));
    }

    @Test
    void getApprovedPickupPointByFilters() throws Exception {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, legalPartner, null
        );

        pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.NONE, true, legalPartner, null
        );

        String url = String.format(
                "/v1/pi/partners/%s/pickup-points/approved?brandType=FULL&dropOff=true", legalPartner.getPartnerId()
        );
        mockMvc.perform(get(url))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("pickup_point/response_approved.json"),
                        pickupPoint.getId(), pickupPoint.getPvzMarketId(), pickupPoint.getName(),
                        pickupPoint.getActive(), pickupPoint.getBrandingType(), pickupPoint.getCashAllowed(),
                        pickupPoint.getPrepayAllowed(), pickupPoint.getCardAllowed(), pickupPoint.getDropOffFeature()
                )));
    }

    @Test
    void requestSomeConsumables() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = createPickupPointWithCourierMapping(crmPrePickupPoint);

        ConsumableTypeParams consumable1 =
                consumableTypeFactory.create(TestConsumableTypeFactory
                        .ConsumableTypeTestParams.builder()
                        .name("Скотч")
                        .countPerPeriod(3)
                        .build());
        ConsumableTypeParams consumable2 = consumableTypeFactory.create(TestConsumableTypeFactory
                .ConsumableTypeTestParams.builder()
                .name("Сейф-пакет")
                .countPerPeriod(5)
                .build());

        String url = String.format(
                "/v1/pi/partners/%s/pickup-points/%s/consumables/request", pickupPoint.getLegalPartner().getPartnerId(),
                crmPrePickupPoint.getId()
        );
        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                String.format(getFileContent("pickup_point/request_request_consumables.json"),
                                        consumable1.getId(), consumable2.getId()))
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(getFileContent("pickup_point/response_request_consumables.json"),
                                consumable1.getId(), consumable2.getId())));
    }

    @Test
    void requestConsumablesMoreThanAvailable() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = createPickupPointWithCourierMapping(crmPrePickupPoint);

        ConsumableTypeParams consumable =
                consumableTypeFactory.create(TestConsumableTypeFactory.ConsumableTypeTestParams.builder().name(
                        "Скотч").countPerPeriod(1).build());

        String url = String.format("/v1/pi/partners/%s/pickup-points/%s/consumables/request",
                pickupPoint.getLegalPartner().getPartnerId(), crmPrePickupPoint.getId());
        mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        String.format(getFileContent("pickup_point/request_request_consumables_more_than_available.json"),
                        consumable.getId()))
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message", containsString("доступно не более")));
    }

    @Test
    void requestNotExistingConsumable() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = createPickupPointWithCourierMapping(crmPrePickupPoint);
        consumableTypeFactory.create();

        String url = String.format(
                "/v1/pi/partners/%s/pickup-points/%s/consumables/request", pickupPoint.getLegalPartner().getPartnerId(),
                crmPrePickupPoint.getId()
        );
        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent("pickup_point/request_request_consumables_invalid_order.json")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getConsumablesCapacity() throws Exception {
        CrmPrePickupPointParams crmPrePickupPoint = crmPrePickupPointFactory.create();
        PickupPoint pickupPoint = createPickupPointWithCourierMapping(crmPrePickupPoint);

        ConsumableTypeParams consumable1 =
                consumableTypeFactory.create(TestConsumableTypeFactory
                        .ConsumableTypeTestParams.builder()
                        .name("Скотч")
                        .countPerPeriod(3)
                        .build());
        ConsumableTypeParams consumable2 = consumableTypeFactory.create(TestConsumableTypeFactory
                .ConsumableTypeTestParams.builder()
                .name("Сейф-пакет")
                .countPerPeriod(5)
                .build());


        String getConsumablesCapacityUrl = String.format(
                "/v1/pi/partners/%s/pickup-points/%s/consumables/capacity", pickupPoint.getLegalPartner().getPartnerId(),
                crmPrePickupPoint.getId()
        );

        mockMvc.perform(get(getConsumablesCapacityUrl))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(getFileContent("pickup_point/response_consumables_capacity_before.json"),
                                consumable1.getId(), consumable2.getId())));

        String orderConsumablesUrl = String.format(
                "/v1/pi/partners/%s/pickup-points/%s/consumables/request", pickupPoint.getLegalPartner().getPartnerId(),
                crmPrePickupPoint.getId()
        );
        mockMvc.perform(post(orderConsumablesUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                String.format(getFileContent("pickup_point/request_request_consumables.json"),
                                        consumable1.getId(), consumable2.getId())))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get(getConsumablesCapacityUrl))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(String.format(getFileContent("pickup_point/response_consumables_capacity_after.json"),
                                consumable1.getId(), consumable2.getId())));
    }

    private PickupPoint createPickupPointWithCourierMapping(CrmPrePickupPointParams crmPrePickupPoint) {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .activateImmediately(true)
                        .crmPrePickupPoint(crmPrePickupPoint)
                        .build());
        addCourierMapping(pickupPoint);
        return pickupPoint;
    }
    private void addCourierMapping(PickupPoint activeMappedPickupPoint) {
        pickupPointCourierMappingFactory.create(
                TestPickupPointCourierMappingFactory.PickupPointCourierMappingTestParamsBuilder.builder()
                        .pickupPoint(activeMappedPickupPoint)
                        .build());
    }
}
