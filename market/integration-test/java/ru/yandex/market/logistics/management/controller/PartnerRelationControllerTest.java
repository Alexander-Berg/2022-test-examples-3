package ru.yandex.market.logistics.management.controller;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.ImmutableSet;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.domain.entity.Cutoff;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.PartnerRoute;
import ru.yandex.market.logistics.management.domain.entity.ProductRating;
import ru.yandex.market.logistics.management.domain.entity.Schedule;
import ru.yandex.market.logistics.management.domain.entity.ScheduleDay;
import ru.yandex.market.logistics.management.domain.entity.combinator.LogisticSegment;
import ru.yandex.market.logistics.management.domain.entity.combinator.QLogisticSegment;
import ru.yandex.market.logistics.management.domain.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.domain.entity.type.ShipmentType;
import ru.yandex.market.logistics.management.domain.entity.validation.EnabledPartnerRelationHasAcceptedToPartnerSubtype;
import ru.yandex.market.logistics.management.entity.logbroker.EventDto;
import ru.yandex.market.logistics.management.entity.type.EdgesFrozen;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.queue.producer.LogbrokerEventTaskProducer;
import ru.yandex.market.logistics.management.repository.CutoffRepository;
import ru.yandex.market.logistics.management.repository.PartnerRelationRepository;
import ru.yandex.market.logistics.management.repository.PartnerRouteRepository;
import ru.yandex.market.logistics.management.repository.ProductRatingRepository;
import ru.yandex.market.logistics.management.repository.ScheduleRepository;
import ru.yandex.market.logistics.management.repository.combinator.LogisticSegmentRepository;
import ru.yandex.market.logistics.management.service.client.PartnerRelationService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.DynamicValidationAspect;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.DynamicValidationService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.rule.PartnerRelationValidationRule;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.tskv.ValidationExceptionLogger;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup({
    "/data/service/combinator/db/before/service_codes.xml",
    "/data/service/combinator/db/before/logistic_segments_services_meta_keys.xml",
    "/data/controller/partnerRelation/prepare_data.xml",
    "/data/controller/partnerRelation/additional_create.xml",
})
@Import(PartnerRelationControllerTest.DynamicValidationConfiguration.class)
class PartnerRelationControllerTest extends AbstractContextualTest {

    private static final long ENTITY_ADDED_ID = 4L;
    private static final long ENTITY_UPDATED_ID = 1L;

    private static final LocalTime REGISTER_TIME = LocalTime.of(12, 0);
    private static final LocalTime INTAKE_TIME = LocalTime.of(13, 0);
    private static final LocalTime IMPORT_TIME = LocalTime.of(14, 0);

    @Autowired
    private PartnerRelationRepository partnerRelationRepository;

    @Autowired
    private ProductRatingRepository productRatingRepository;

    @Autowired
    private CutoffRepository cutoffRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private LogbrokerEventTaskProducer logbrokerEventTaskProducer;

    @Autowired
    private PartnerRouteRepository partnerRouteRepository;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private TestableClock clock;

    @Autowired
    private PartnerRelationService partnerRelationService;

    @Autowired
    private LogisticSegmentRepository logisticSegmentRepository;

    private Schedule registerSchedule;
    private Schedule intakeSchedule;
    private Schedule importSchedule;

    @BeforeEach
    void init() {
        registerSchedule = getSchedule(REGISTER_TIME);
        intakeSchedule = getSchedule(INTAKE_TIME);
        importSchedule = getSchedule(IMPORT_TIME);
        Mockito.doNothing().when(logbrokerEventTaskProducer).produceTask(Mockito.any());
        clock.setFixed(Instant.parse("2021-08-11T15:32:00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void teardown() {
        clock.clearFixed();
        Mockito.verifyNoMoreInteractions(logbrokerEventTaskProducer);
    }

    @Test
    @DisplayName("Связка изначально не существует. Создаем связку без зависимостей")
    @Transactional
    void testCreateNewPartnerRelationSuccessWithoutRelations() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_without_relations.json")
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/partnerRelation/new_partner_relation_without_relations_response.json")));

        verifyNewPartnerRelation();

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(productRatingRepository.count())
            .as("Number of product ratings were not changed properly")
            .isEqualTo(3);

        softly.assertThat(cutoffRepository.count())
            .as("Number of cutoffs were not changed properly")
            .isEqualTo(3);

        softly.assertThat(scheduleRepository.count())
            .as("Number of schedules were not changed properly")
            .isEqualTo(3);
    }

    @Test
    @DisplayName("Связка изначально не существует. Создаем связку с возвратным складом")
    void testCreateNewPartnerRelationWithReturnPartner() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_with_return_partner.json")
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/partnerRelation/new_partner_relation_with_return_partner_response.json")));

        assertPartnerRelationWithReturnPartnerCorrect(1L, 4L);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));
    }

    @Test
    @DisplayName("При создании связки кроссдок-партнера возвратный склад корректно устанавливается как фулфиллмент")
    @DatabaseSetup(value = "/data/controller/partnerRelation/additional_supplier.xml", type = DatabaseOperation.INSERT)
    void testCreateNewSupplierFulfillmentPartnerRelationWithReturnPartner() throws Exception {
        executePost("/externalApi/partner-relation", "new_supplier_fulfillment_partner_relation_request.json")
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/partnerRelation/new_supplier_fulfillment_partner_relation_response.json")));

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));
    }

    @Test
    @DisplayName(
        "Связка изначально не существует. При создании происходит нарушение ограничения основного ключа "
            + "(возможно при параллельном исполнении)."
    )
    void testCreateNewPartnerRelationViolatesDataIntegrity() throws Exception {
        DataIntegrityViolationException e = new DataIntegrityViolationException(
            "",
            new ConstraintViolationException("", new SQLException("", "23505", 0), "")
        );
        Mockito.doThrow(e).when(partnerRelationService).save(Mockito.any());
        executePost("/externalApi/partner-relation", "new_partner_relation_without_relations.json")
            .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Ошибка создания связки с возвратным партнером не дропшим или фулфиллмент")
    void testCreateNewPartnerRelationWithReturnPartnerError() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_with_return_partner_error.json")
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Return partner 2 doesn't exist."));
    }

    @Test
    @DisplayName("Связка изначально не существует. Создаем связку со всеми зависимостями")
    @Transactional
    void testCreateNewPartnerRelationFull() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_full.json")
            .andExpect(status().isOk())
            .andExpect(content()
                .json(pathToJson("data/controller/partnerRelation/new_partner_relation_full_response.json")));

        verifyNewPartnerRelation();

        verifyProductRatings(
            ENTITY_ADDED_ID,
            new ProductRating()
                .setLocationId(1)
                .setRating(1),
            new ProductRating()
                .setLocationId(2)
                .setRating(2)
        );
        verifyCutoffs(
            ENTITY_ADDED_ID,
            new Cutoff()
                .setLocationId(1)
                .setCutoffTime(LocalTime.of(10, 0))
                .setPackagingDuration(Duration.ofSeconds(0)),
            new Cutoff()
                .setLocationId(2)
                .setCutoffTime(LocalTime.of(20, 0))
                .setPackagingDuration(Duration.ofSeconds(100))
        );
        verifyImportSchedule(ENTITY_ADDED_ID);
        verifyIntakeSchedule(ENTITY_ADDED_ID);
        verifyRegisterSchedule(ENTITY_ADDED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.count())
            .as("Number of partner relations were not changed properly")
            .isEqualTo(4);

        softly.assertThat(cutoffRepository.count())
            .as("Number of cutoffs were not changed properly")
            .isEqualTo(5);

        softly.assertThat(scheduleRepository.count())
            .as("Number of schedules were not changed properly")
            .isEqualTo(8);
    }

    @Test
    @DisplayName(
        "Связка изначально не существует. Создаем связку с расписаниями. "
            + "Для одного типа расписаний добавляем несколько эвентов для одного дня. "
            + "Общее количество расписаний увеличилось на четыре (включая два по новому сегменту перемещения)"
    )
    @Transactional
    void testCreateNewPartnerRelationSuccessWithMultipleSchedulesForOneDay() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_with_multiple_schedules_for_one_day.json")
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/partnerRelation/new_partner_relation_with_multiple_schedules_for_one_day_response.json"
            )));

        verifyPartnerRelation(
            ENTITY_ADDED_ID,
            1L,
            true,
            ENTITY_ADDED_ID,
            1L,
            4L,
            1,
            true,
            Duration.ofHours(2),
            Duration.ofHours(3),
            ShipmentType.IMPORT
        );

        registerSchedule.addScheduledDay(getScheduleDay(REGISTER_TIME.plusHours(1), 3));
        registerSchedule.addScheduledDay(getScheduleDay(REGISTER_TIME.plusHours(2), 3));

        verifyImportSchedule(ENTITY_ADDED_ID);
        verifyIntakeSchedule(ENTITY_ADDED_ID);
        verifyRegisterSchedule(ENTITY_ADDED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were not changed properly")
            .hasSize(4);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were not changed properly")
            .hasSize(8);
    }

    @Test
    @DisplayName(
        "Связка изначально не существует. Создаем связку с расписаниями. Расписание отправки реестров не сохраняется"
    )
    void testCreateNewPartnerRelationWithRegisterScheduleDisabled() throws Exception {
        featureProperties.setRegisterScheduleDisabled(true);
        executePost("/externalApi/partner-relation", "new_partner_relation_with_multiple_schedules_for_one_day.json")
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/partnerRelation/"
                    + "new_partner_relation_with_multiple_schedules_for_one_day_response_without_register_schedule.json"
            )));
        featureProperties.setRegisterScheduleDisabled(false);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));
    }

    @Test
    @DisplayName(
        "Связка изначально не существует. Создаем связку с расписаниями."
            + "Для одного типа расписаний добавляем несколько эвентов для одного дня с одинаковым временем. "
            + "Добавляется только один эвент для которых было указано одинаковое время. "
            + "Общее количество расписаний увеличилось на четыре (включая два по новому сегменту перемещения)"
    )
    @Transactional
    void testCreateNewPartnerRelationSuccessWithEqualSchedulesForOneDay() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_with_equal_schedules_for_one_day.json")
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/partnerRelation/new_partner_relation_with_equal_schedules_for_one_day_response.json"
            )));

        verifyNewPartnerRelation();

        verifyImportSchedule(ENTITY_ADDED_ID);
        verifyIntakeSchedule(ENTITY_ADDED_ID);
        verifyRegisterSchedule(ENTITY_ADDED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were not changed properly")
            .hasSize(4);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were not changed properly")
            .hasSize(8);
    }

    @Test
    void testCreateNewPartnerRelationWithIncorrectLogisticsPoint() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_with_incorrect_logistics_point.json")
            .andExpect(status().isConflict());

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);
    }

    @Test
    void testCreateNewPartnerRelationWithInactiveLogisticsPoint() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_with_inactive_logistics_point.json")
            .andExpect(status().isNotFound())
            .andExpect(status().reason(
                "No LogisticsPoint found by id = 5 and type in [WAREHOUSE] for partner id = 4"
            ));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);
    }

    @Test
    @JpaQueriesCount(6)
    @DisplayName(
        "Связка изначально существует. Получаем ошибку при ее попытке создать"
            + "Общее количество расписаний не изменилось"
    )
    void testCreateNewPartnerRelationForExistingPartnerRelation() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_for_existing_partner_relation.json")
            .andExpect(status().isConflict());

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName("Связка с логистической точкой назначения - ПВЗ дропоффа")
    @DatabaseSetup(value = "/data/controller/partnerRelation/additional_dropoff.xml", type = DatabaseOperation.INSERT)
    void testCreateNewPartnerRelationForDropoff() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_dropoff_pickup_point_request.json")
            .andExpect(status().isOk());

        softly.assertThat(partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(5L, 2L))
            .hasValueSatisfying(relation ->
                softly.assertThat(relation.getToPartnerLogisticsPoint().getId()).isEqualTo(6L)
            );

        List<PartnerRoute> partnerRoutes = partnerRouteRepository.findAll();
        softly.assertThat(partnerRoutes.size())
            .as("Incorrect number of partner routes")
            .isEqualTo(1);

        PartnerRoute partnerRoute = partnerRoutes.get(0);
        softly.assertThat(partnerRoute.getPartner().getId())
            .as("Incorrect partner id")
            .isEqualTo(2);

        softly.assertThat(partnerRoute.getLocationFrom())
            .as("Incorrect location from")
            .isEqualTo(53);

        softly.assertThat(partnerRoute.getLocationTo())
            .as("Incorrect location to")
            .isEqualTo(225);

        checkLogbrokerEvent("data/controller/partnerRelation/logbroker/create_relation_from_dropship.json");
    }

    @Test
    @DisplayName("Связка с логистической точкой назначения - такси экспресс")
    @DatabaseSetup(value = "/data/controller/partnerRelation/additional_express.xml", type = DatabaseOperation.INSERT)
    void testCreateNewPartnerRelationForExpress() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_express_request.json")
            .andExpect(status().isOk());

        softly.assertThat(partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(5L, 6L))
            .hasValueSatisfying(relation ->
                softly.assertThat(relation.getToPartnerLogisticsPoint().getId()).isEqualTo(7L)
            );

        List<PartnerRoute> partnerRoutes = partnerRouteRepository.findAll();
        softly.assertThat(partnerRoutes.size())
            .as("Incorrect number of partner routes")
            .isEqualTo(1);

        PartnerRoute partnerRoute = partnerRoutes.get(0);
        softly.assertThat(partnerRoute.getPartner().getId())
            .as("Incorrect partner id")
            .isEqualTo(6);

        softly.assertThat(partnerRoute.getLocationFrom())
            .as("Incorrect location from")
            .isEqualTo(53);

        softly.assertThat(partnerRoute.getLocationTo())
            .as("Incorrect location to")
            .isEqualTo(225);

        checkLogbrokerEvent("data/controller/partnerRelation/logbroker/create_relation_from_dropship.json");
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/partnerRelation/import_to_delivery_before.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/import_to_delivery_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание связки и магистрали самопривоз в СД")
    void createPartnerRelationImportToDelivery() throws Exception {
        featureProperties.setAllowPartnerImportToDelivery(true);
        executePost("/externalApi/partner-relation", "new_partner_relation_import_to_delivery.json")
            .andExpect(status().isOk());
        featureProperties.setAllowPartnerImportToDelivery(false);
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/partnerRelation/import_to_delivery_before.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/controller/partnerRelation/import_to_delivery_after_withoute_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Создание только связки самопривоз в СД")
    void createOnlyPartnerRelationImportToDelivery() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_import_to_delivery.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Связка с логистической точкой назначения - ПВЗ дропоффа, отключенная точка назначения")
    @DatabaseSetup(value = "/data/controller/partnerRelation/additional_dropoff.xml", type = DatabaseOperation.INSERT)
    void testCreateNewPartnerRelationForDropoffInactivePoint() throws Exception {
        executePost("/externalApi/partner-relation", "new_partner_relation_dropoff_pickup_point_inactive_request.json")
            .andExpect(status().isNotFound())
            .andExpect(status().reason(
                "No LogisticsPoint found by id = 7 and type in [WAREHOUSE, PICKUP_POINT] for partner id = 2"
            ));
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/partnerRelation/additional_partner_relations.xml",
        type = DatabaseOperation.INSERT
    )
    void testCreateNewEnabledPartnerRelationWithIneligibleToPartnerSubtype() throws Exception {
        executePost(
            "/externalApi/partner-relation",
            "failed_create_new_enabled_partner_relation_with_ineligible_toPartner_subtype.json"
        )
            .andExpect(status().isConflict())
            .andExpect(status().reason(EnabledPartnerRelationHasAcceptedToPartnerSubtype.ERROR_MESSAGE));
    }

    @Test
    @DisplayName(
        "Изначально существует несколько связок. "
            + "Обновление одной из них с новыми партнерами, для которых уже существует другая связка. "
            + "Получаем ошибку \"Связка для этих партнеров уже существует\""
    )
    void testUpdatePartnersInPartnerRelationToExistingRelation() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_to_another_existing_partner_relation.json"
        )
            .andExpect(status().isConflict())
            .andExpect(status().reason("Partner relation already exists for partners 3 and 4"));
    }

    @Test
    @DisplayName(
        "У связки изначально есть все расписания с днями. После обновления расписания заполнены днями. "
            + "Общее количество расписаний не изменилось"
    )
    void testUpdateExistingPartnerRelationWithFullDescribedSchedules() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_full_described_schedules.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelation();

        verifyImportSchedule(ENTITY_UPDATED_ID);
        verifyIntakeSchedule(ENTITY_UPDATED_ID);
        verifyRegisterSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName(
        "У связки изначально установлен возвратный склад по умолчанию (FromPartner). "
            + "После обновления возвратный склад устанавливается. "
            + "После повторного обновления возвратный склад снова установлен по умолчанию"
    )
    void testUpdateExistingPartnerRelationWithReturnPartner() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_return_partner.json"
        )
            .andExpect(status().isOk());

        assertPartnerRelationWithReturnPartnerCorrect(1L, 2L);

        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_return_partner_set_null.json"
        )
            .andExpect(status().isOk());

        verify(buildWarehouseSegmentsProducer, times(2)).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(1L, 2L))
            .as("Partner relation exists")
            .hasValueSatisfying(
                partnerRelation -> softly.assertThat(partnerRelation.getReturnPartner().getId())
                    .as("Partner relation return partner id is correct")
                    .isEqualTo(partnerRelation.getFromPartner().getId()));
    }

    @Test
    @DisplayName("При обновлении связки кроссдок-партнера возвратный склад корректно устанавливается как фулфиллмент")
    @DatabaseSetup(value = "/data/controller/partnerRelation/additional_supplier.xml", type = DatabaseOperation.INSERT)
    void testUpdateExistingSupplierFulfillmentPartnerRelationWithReturnPartner() throws Exception {
        executePut(
            "/externalApi/partner-relation/4",
            "update_existing_crossdock_partner_relation_with_return_partner_set_null.json"
        )
            .andExpect(status().isOk());

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(5L, 1L))
            .as("Partner relation exists")
            .hasValueSatisfying(
                partnerRelation -> softly.assertThat(partnerRelation.getReturnPartner().getId())
                    .as("Crossdock artner relation return partner id is correct")
                    .isEqualTo(partnerRelation.getToPartner().getId()));

        checkLogbrokerEvent("data/controller/partnerRelation/logbroker/update_relation_from_supplier.json");
    }

    @Test
    @DisplayName(
        "Попытка обновить возвратный склад на партнера, который не дропшип или фулфиллмент, завершается неуспехом"
    )
    void testUpdateExistingPartnerRelationWithReturnPartnerError() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_return_partner_error.json"
        )
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Return partner 4 doesn't exist."));
    }

    @Test
    void testUpdateExistingPartnerRelationWithLogisticsPoint() throws Exception {
        executePut(
            "/externalApi/partner-relation/2",
            "update_partner_relation_with_logistics_point.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(pathToJson(
                "data/controller/partnerRelation/update_partner_relation_with_logistics_point_response.json"
            )));
    }

    @Test
    void testUpdateExistingPartnerRelationWithIncorrectTypeLogisticsPoint() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_partner_relation_with_incorrect_type_logistics_point.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(status().reason(
                "No LogisticsPoint found by id = 2 and type in [WAREHOUSE] for partner id = 2"
            ));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);
    }

    @Test
    void testUpdateExistingPartnerRelationWithInactiveLogisticsPoint() throws Exception {
        executePut(
            "/externalApi/partner-relation/2",
            "update_partner_relation_with_inactive_logistics_point.json"
        )
            .andExpect(status().isOk());

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName(
        "У связки изначально есть все расписания с днями. "
            + "После обновления одно из расписаний заполнено днями, а одно пустое. "
            + "Общее количество расписаний не изменилось"
    )
    void testUpdateExistingPartnerRelationWithPartiallyDescribedSchedules() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_partially_described_schedules.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelation();

        intakeSchedule.setScheduleDays(Collections.emptySet());
        verifyImportSchedule(ENTITY_UPDATED_ID);
        verifyIntakeSchedule(ENTITY_UPDATED_ID);
        verifyRegisterSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName(
        "У связки изначально есть все расписания с днями. "
            + "После обновления одно из расписаний заполнено днями, а одно пустое. "
            + "Общее количество расписаний не изменилось."
            + "Расписание отправки реестров не изменилось."
    )
    void testUpdateExistingPartnerRelationWithRegisterScheduleDisabled() throws Exception {
        featureProperties.setRegisterScheduleDisabled(true);
        Set<ScheduleDay> before = getRegisterScheduleDays();
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_partially_described_schedules.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelation();

        intakeSchedule.setScheduleDays(Collections.emptySet());

        verifyImportSchedule(ENTITY_UPDATED_ID);
        verifyIntakeSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);

        softly.assertThat(getRegisterScheduleDays())
            .as("Register schedule was changed")
            .hasSameElementsAs(before);
        featureProperties.setRegisterScheduleDisabled(false);
    }

    @Test
    @DisplayName(
        "У связки изначально есть все расписания с днями. После обновления все расписания пустые. "
            + "Общее количество расписаний не изменилось "
    )
    void testUpdateExistingPartnerRelationToEmptySchedules() throws Exception {
        executePut("/externalApi/partner-relation/1", "update_existing_partner_relation_to_empty_schedules.json")
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelation();

        importSchedule.setScheduleDays(Collections.emptySet());
        intakeSchedule.setScheduleDays(Collections.emptySet());
        registerSchedule.setScheduleDays(Collections.emptySet());

        verifyImportSchedule(ENTITY_UPDATED_ID);
        verifyIntakeSchedule(ENTITY_UPDATED_ID);
        verifyRegisterSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName(
        "У связки нет расписаний. После обновления созданы пустые расписания. "
            + "Общее количество расписаний увеличилось на два"
    )
    void testUpdateExistingPartnerRelationWithoutSchedulesToEmptySchedules() throws Exception {
        executePut(
            "/externalApi/partner-relation/2",
            "update_existing_partner_relation_without_schedules_to_empty_schedules.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelationWithoutSchedules();

        importSchedule.setScheduleDays(Collections.emptySet());
        intakeSchedule.setScheduleDays(Collections.emptySet());
        registerSchedule.setScheduleDays(Collections.emptySet());

        verifyImportSchedule(2L);
        verifyIntakeSchedule(2L);
        verifyRegisterSchedule(2L);

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were not changed properly")
            .hasSize(6);
    }

    @Test
    @DisplayName(
        "У связки нет расписаний. После обновления созданы расписания и заполнены днями. "
            + "Общее количество расписаний увеличилось на два"
    )
    void testUpdateExistingPartnerRelationWithoutSchedulesToFullDescribedSchedules() throws Exception {
        executePut(
            "/externalApi/partner-relation/2",
            "update_existing_partner_relation_without_schedules_to_full_described_schedules.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelationWithoutSchedules();

        verifyImportSchedule(2L);
        verifyIntakeSchedule(2L);
        verifyRegisterSchedule(2L);

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were not changed properly")
            .hasSize(6);
    }

    @Test
    @DisplayName(
        "Связка изначально не существует.  При попытке обновить получаем ошибку. "
            + "Общее количество расписаний расписаний не изменилось"
    )
    void testUpdateNotExistingPartnerRelation() throws Exception {
        Long partnerRelationId = 777L;
        executePut("/externalApi/partner-relation/" + partnerRelationId, "update_not_existing_partner_relation.json")
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Partner relation with id 777 not found"));

        PartnerRelation partnerRelation = partnerRelationRepository.findById(partnerRelationId).orElse(null);

        softly.assertThat(partnerRelation)
            .as("Partner relation should be not exist")
            .isNull();

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName(
        "У связки есть все расписания с днями. "
            + "После обновления одно из расписаний не изменилось, а у двух других добавились дни. "
            + "Общее количество расписаний не изменилось. "
    )
    void testUpdateExistingPartnerRelationWithPartiallyUpdatedSchedules() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_partially_updated_schedules.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelation();

        importSchedule.addScheduledDay(getScheduleDay(IMPORT_TIME, 4));
        registerSchedule.addScheduledDay(getScheduleDay(REGISTER_TIME, 4));

        verifyImportSchedule(ENTITY_UPDATED_ID);
        verifyIntakeSchedule(ENTITY_UPDATED_ID);
        verifyRegisterSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName(
        "У связки есть все расписания с днями. "
            + "Для одного типа расписаний добавляем несколько эвентов для одного дня. "
            + "Общее количество расписаний не изменилось"
    )
    void testUpdateExistingPartnerRelationSuccessWithMultipleSchedulesForOneDay() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_multiple_schedules_for_one_day.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelation();

        registerSchedule.addScheduledDay(getScheduleDay(REGISTER_TIME.plusHours(1), 3));
        registerSchedule.addScheduledDay(getScheduleDay(REGISTER_TIME.plusHours(2), 3));

        verifyImportSchedule(ENTITY_UPDATED_ID);
        verifyIntakeSchedule(ENTITY_UPDATED_ID);
        verifyRegisterSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    @DisplayName(
        "У связки изначально есть все расписания с днями. "
            + "Для одного типа расписаний добавляем несколько эвентов для одного дня с одинаковым временем. "
            + "Добавляется только один эвент для которых было указано одинаковое время. "
            + "Общее количество расписаний не изменилось"
    )
    void testUpdateExistingPartnerRelationSuccessWithEqualSchedulesForOneDay() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_equal_schedules_for_one_day.json"
        )
            .andExpect(status().isOk());

        verifyUpdatedPartnerRelation();

        verifyImportSchedule(ENTITY_UPDATED_ID);
        verifyIntakeSchedule(ENTITY_UPDATED_ID);
        verifyRegisterSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        softly.assertThat(partnerRelationRepository.findAll())
            .as("Number of partner relations were changed")
            .hasSize(3);

        softly.assertThat(scheduleRepository.findAll())
            .as("Number of schedules were changed")
            .hasSize(3);
    }

    @Test
    void testUpdateExistingPartnerRelationWithValidationViolation() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_validation_violation.json"
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status()
                .reason("Ошибки валидации для связки [id=1]: время обработки не может быть равно 999."));

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        PartnerRelation relation = partnerRelationRepository.findById(1L).get();
        assertThat(relation.getHandlingTime()).isEqualTo(10);
    }

    @Test
    void testUpdateExistingPartnerRelationWithMultipleValidationViolations() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_existing_partner_relation_with_multiple_validation_violations.json"
        )
            .andExpect(status().isUnprocessableEntity())
            .andExpect(status()
                .reason("Ошибки валидации для связки [id=1]: время обработки не может быть равно 999, "
                    + "забор недоступен."));

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));

        PartnerRelation relation = partnerRelationRepository.findById(1L).get();
        assertThat(relation.getHandlingTime()).isEqualTo(10);
    }

    @Test
    @DatabaseSetup(
        value = "/data/controller/partnerRelation/additional_partner_relations.xml",
        type = DatabaseOperation.INSERT
    )
    void testUpdateViaEnablingExistingDisabledPartnerRelationWithIneligibleToPartnerSubtype() throws Exception {
        executePut(
            "/externalApi/partner-relation/146",
            "failed_update_via_enabling_existing_disabled_partner_relation_with_ineligible_toPartner_subtype.json"
        )
            .andExpect(status().isConflict())
            .andExpect(status().reason(EnabledPartnerRelationHasAcceptedToPartnerSubtype.ERROR_MESSAGE));
    }

    @Test
    void updateWithExistingAndNewData() throws Exception {
        executePut(
            "/externalApi/partner-relation/1",
            "update_with_existing_and_new_data.json"
        )
            .andExpect(status().isOk())
            .andExpect(content().json(
                pathToJson("data/controller/partnerRelation/update_with_existing_and_new_data_response.json")));

        intakeSchedule = new Schedule().setScheduleDays(ImmutableSet.of(
            new ScheduleDay()
                .setDay(1)
                .setFrom(LocalTime.of(12, 0))
                .setTo(LocalTime.of(13, 0)),
            new ScheduleDay()
                .setDay(10)
                .setFrom(LocalTime.of(10, 0))
                .setTo(LocalTime.of(20, 0))
        ));
        registerSchedule = new Schedule().setScheduleDays(ImmutableSet.of(
            new ScheduleDay()
                .setDay(3)
                .setFrom(LocalTime.of(5, 0))
                .setTo(LocalTime.of(10, 0)),
            new ScheduleDay()
                .setDay(20)
                .setFrom(LocalTime.of(13, 0))
                .setTo(LocalTime.of(16, 0))
        ));

        verifyPartnerRelation(
            ENTITY_UPDATED_ID,
            null,
            false,
            ENTITY_UPDATED_ID,
            1L,
            2L,
            100,
            true,
            null,
            null,
            ShipmentType.IMPORT
        );
        verifyProductRatings(
            ENTITY_UPDATED_ID,
            new ProductRating()
                .setLocationId(225)
                .setRating(100),
            new ProductRating()
                .setLocationId(0)
                .setRating(200)
        );
        verifyCutoffs(
            ENTITY_UPDATED_ID,
            new Cutoff()
                .setLocationId(1)
                .setCutoffTime(LocalTime.of(14, 9))
                .setPackagingDuration(Duration.ofSeconds(100)),
            new Cutoff()
                .setLocationId(2)
                .setCutoffTime(LocalTime.of(20, 0))
                .setPackagingDuration(Duration.ofSeconds(200))
        );
        verifyIntakeSchedule(ENTITY_UPDATED_ID);
        verifyRegisterSchedule(ENTITY_UPDATED_ID);

        verify(buildWarehouseSegmentsProducer).produceTask(eq(1L));
    }

    @Nonnull
    private Set<ScheduleDay> getRegisterScheduleDays() {
        return scheduleRepository.findAllByIdIn(List.of(-2L))
            .stream()
            .flatMap(it -> it.getScheduleDays().stream())
            .collect(Collectors.toSet());
    }

    private void assertPartnerRelationWithReturnPartnerCorrect(long fromPartnerId, long toPartnerId) {
        softly.assertThat(partnerRelationRepository.findOneByFromPartnerIdAndToPartnerId(fromPartnerId, toPartnerId))
            .as("Partner relation exists")
            .hasValueSatisfying(
                partnerRelation -> softly.assertThat(
                        Optional.ofNullable(partnerRelation.getReturnPartner()).map(Partner::getId)
                    )
                    .as("Partner relation return partner id is correct")
                    .hasValueSatisfying(id -> softly.assertThat(id).isEqualTo(3L)));
    }

    private void verifyProductRatings(long partnerRelationId, ProductRating... expectedProductRatings) {
        List<ProductRating> productRatings = productRatingRepository.findAll().stream()
            .filter(pr -> partnerRelationId == pr.getPartnerRelation().getId())
            .collect(Collectors.toList());

        softly.assertThat(productRatings)
            .as("Product ratings should be saved")
            .containsExactlyInAnyOrder(expectedProductRatings);
    }

    private void verifyCutoffs(long partnerRelationId, Cutoff... expectedCutoffs) {
        List<Cutoff> cutoffs = cutoffRepository.findAll().stream()
            .filter(c -> partnerRelationId == c.getPartnerRelation().getId())
            .collect(Collectors.toList());

        softly.assertThat(cutoffs)
            .as("Cutoffs should be saved")
            .containsExactlyInAnyOrder(expectedCutoffs);
    }

    private void verifyImportSchedule(Long partnerRelationId) {
        verifySchedule(partnerRelationId, importSchedule, PartnerRelation::getImportSchedule);
    }

    private void verifyIntakeSchedule(Long partnerRelationId) {
        verifySchedule(partnerRelationId, intakeSchedule, PartnerRelation::getIntakeSchedule);
    }

    private void verifyRegisterSchedule(Long partnerRelationId) {
        verifySchedule(partnerRelationId, registerSchedule, PartnerRelation::getRegisterSchedule);
    }

    private void verifySchedule(
        Long partnerRelationId, Schedule expected,
        Function<PartnerRelation, Schedule> getSchedule
    ) {
        PartnerRelation actual = partnerRelationRepository.findWithScheduleDaysById(partnerRelationId).orElse(null);

        softly.assertThat(getSchedule.apply(actual))
            .as("Entity doesn't equal to expected")
            .isEqualToIgnoringNullFields(expected);
    }

    private void verifyNewPartnerRelation() {
        verifyPartnerRelation(
            ENTITY_ADDED_ID,
            4L,
            true,
            ENTITY_ADDED_ID,
            1L,
            4L,
            1,
            true,
            null,
            null,
            ShipmentType.WITHDRAW
        );
    }

    private void verifyUpdatedPartnerRelation() {
        verifyPartnerRelation(
            ENTITY_UPDATED_ID,
            null,
            false,
            ENTITY_UPDATED_ID,
            1L,
            2L,
            2,
            true,
            null,
            null,
            ShipmentType.WITHDRAW
        );
    }

    private void verifyUpdatedPartnerRelationWithoutSchedules() {
        verifyPartnerRelation(2L, null, false, 2L, 3L, 4L, 2, true, null, null, ShipmentType.WITHDRAW);
    }

    @SuppressWarnings("unchecked")
    private void verifyPartnerRelation(
        Long partnerRelationId,
        Long movementPartnerId,
        boolean isNew,
        Object... dataForCheck
    ) {
        PartnerRelation partnerRelation = partnerRelationRepository.findById(partnerRelationId).orElse(null);

        softly.assertThat(partnerRelation)
            .as("Partner relation should be exist")
            .isNotNull()
            .extracting(
                PartnerRelation::getId,
                pr -> pr.getFromPartner().getId(),
                pr -> pr.getToPartner().getId(),
                PartnerRelation::getHandlingTime,
                PartnerRelation::getEnabled,
                PartnerRelation::getTransferTime,
                PartnerRelation::getInboundTime,
                PartnerRelation::getShipmentType
            )
            .as("Proper fields values should be set")
            .containsExactly(dataForCheck);

        if (!isNew) {
            return;
        }

        LogisticSegment movementSegment = logisticSegmentRepository
            .findOne(QLogisticSegment.logisticSegment.partnerRelation.id.eq(partnerRelationId))
            .orElseThrow(() -> new IllegalStateException(
                "No logistic segment found for partner relation " + partnerRelationId
            ));

        softly.assertThat(movementSegment)
            .as("Asserting that a valid movement segment is created")
            .isNotNull()
            .extracting(
                LogisticSegment::getType,
                LogisticSegment::getName,
                LogisticSegment::getEdgesFrozen,
                logisticSegment -> logisticSegment.getPartner().getId(),
                logisticSegment -> logisticSegment.getPartnerRelation().getId()
            )
            .containsExactly(
                LogisticSegmentType.MOVEMENT,
                "Сегмент перемещения из Fulfillment service 1 в Delivery service 2",
                EdgesFrozen.AUTO,
                movementPartnerId,
                partnerRelationId
            );

        softly.assertThat(movementSegment.getServices())
            .as("Asserting that the created movement segment has valid services")
            .hasSize(3)
            .extracting(logisticSegmentService -> logisticSegmentService.getCode().getCode())
            .containsExactlyInAnyOrder(ServiceCodeName.INBOUND, ServiceCodeName.MOVEMENT, ServiceCodeName.SHIPMENT);
    }

    private ResultActions executePut(String urlTemplate, String path) throws Exception {
        return execute(urlTemplate, path, HttpMethod.PUT);
    }

    private ResultActions executePost(String urlTemplate, String path) throws Exception {
        return execute(urlTemplate, path, HttpMethod.POST);
    }

    private ResultActions execute(String urlTemplate, String path, HttpMethod httpMethod) throws Exception {
        return mockMvc.perform(
            MockMvcRequestBuilders
                .request(httpMethod, urlTemplate)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/partnerRelation/" + path))
        );
    }

    private Schedule getSchedule(LocalTime startTime) {
        Schedule schedule = new Schedule();
        for (int i = 1; i <= 3; i++) {
            schedule.addScheduledDay(getScheduleDay(startTime, i));
        }
        return schedule;
    }

    private static ScheduleDay getScheduleDay(LocalTime startTime, int dayNumber) {
        return new ScheduleDay()
            .setDay(dayNumber)
            .setFrom(startTime)
            .setTo(startTime.plusHours(1));
    }

    private void checkLogbrokerEvent(String jsonPath) throws IOException {
        ArgumentCaptor<EventDto> argumentCaptor = ArgumentCaptor.forClass(EventDto.class);
        verify(logbrokerEventTaskProducer).produceTask(argumentCaptor.capture());
        assertThatJson(argumentCaptor.getValue())
            .isEqualTo(objectMapper.readValue(pathToJson(jsonPath), EventDto.class));
    }

    @Configuration
    public static class DynamicValidationConfiguration {

        @Bean
        public ValidationRule firstValidationRule() {
            return new PartnerRelationValidationRule() {
                @Override
                public boolean isValid(@Nonnull PartnerRelationModel pr) {
                    return !pr.getHandlingTime().equals(999);
                }

                @Override
                public String cause(@Nonnull PartnerRelationModel pr) {
                    return "время обработки не может быть равно 999";
                }
            };
        }

        @Bean
        public ValidationRule secondValidationRule() {
            return new PartnerRelationValidationRule() {
                @Override
                public boolean isValid(@Nonnull PartnerRelationModel pr) {
                    return !pr.getHandlingTime().equals(999) || pr.getShipmentType() != ShipmentType.WITHDRAW;
                }

                @Override
                public String cause(@Nonnull PartnerRelationModel pr) {
                    return "забор недоступен";
                }
            };
        }

        @Bean
        public DynamicValidationAspect dynamicValidationAspect(PartnerRelationService partnerRelationService) {
            List<ValidationRule> rules = List.of(firstValidationRule(), secondValidationRule());
            DynamicValidationService validationService = new DynamicValidationService(rules);

            return new DynamicValidationAspect(
                Mockito.mock(EntityManager.class),
                partnerRelationService,
                validationService,
                Mockito.mock(ValidationExceptionLogger.class),
                true
            );
        }
    }
}
