package ru.yandex.market.sc.api.controller.inbound;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.api.util.ScApiControllerCaller;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.cell.model.CellType;
import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.model.ApiInboundDto;
import ru.yandex.market.sc.core.domain.inbound.model.ApiInboundInfoDto;
import ru.yandex.market.sc.core.domain.inbound.model.ApiInboundListDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.LinkToInboundRequestDto;
import ru.yandex.market.sc.core.domain.inbound.repository.Inbound;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundRepository;
import ru.yandex.market.sc.core.domain.inbound.repository.InboundStatus;
import ru.yandex.market.sc.core.domain.measurements_so.model.UnitMeasurementsDto;
import ru.yandex.market.sc.core.domain.measurements_so.repository.MeasurementSo;
import ru.yandex.market.sc.core.domain.measurements_so.repository.MeasurementsSoRepository;
import ru.yandex.market.sc.core.domain.measurements_so.repository.UnitMeasurements;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.sortable.SortableQueryService;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableStatus;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.sortable.repository.SortableMutableState;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.domain.warehouse.repository.WarehouseRepository;
import ru.yandex.market.sc.core.test.DefaultScUserWarehouseExtension;
import ru.yandex.market.sc.core.test.SortableTestFactory;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.flow.xdoc.XDocFlow;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScApiControllerTest
@ExtendWith(DefaultScUserWarehouseExtension.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InboundControllerTest {

    private static final String OLD_EXTERNAL_ID = "old-external-id";
    private static final String CREATED_EXTERNAL_ID = "accepted-external-id";
    private static final String CYRILLIC_TRIMMED_INFORMATION_LIST_CODE = "1342534";
    private static final String INFORMATION_LIST_CODE = "Зп-" + CYRILLIC_TRIMMED_INFORMATION_LIST_CODE;
    private static final String INFORMATION_LIST_CODE_URL_ENCODED = URLEncoder.encode(INFORMATION_LIST_CODE, StandardCharsets.UTF_8);
    private static final String SORTABLE_ID = "XDOC-123";
    private static final String FINISHED_EXTERNAL_ID = "finished-external-id";
    private static final String ANOTHER_CREATED_EXTERNAL_ID = "another-accepted-external-id";

    private final MockMvc mockMvc;
    private final ScApiControllerCaller caller;
    private final XDocFlow flow;
    private final TestFactory testFactory;
    private final SortableTestFactory sortableTestFactory;
    private final SortableQueryService sortableQueryService;
    private final InboundRepository inboundRepository;
    private final WarehouseRepository warehouseRepository;
    private final MeasurementsSoRepository measurementsSoRepository;

    SortingCenter sortingCenter;
    SortingCenter anotherSc;
    User anotherUser;
    ScApiControllerCaller tsdFromAnotherSc;

    @BeforeEach
    void init() {
        anotherSc = testFactory.storedSortingCenter(13L);
        anotherUser = testFactory.storedUser(anotherSc, 124);
        tsdFromAnotherSc = ScApiControllerCaller.createCaller(mockMvc, anotherUser.getUid());
        sortingCenter = testFactory.storedSortingCenter(12L);
    }

    @Test
    void getInboundForAcceptHappyPath() {
        var inbound = flow.createInboundAndGet(CREATED_EXTERNAL_ID);

        var apiInboundDto = caller.acceptInbound(CREATED_EXTERNAL_ID, null)
                .andExpect(status().isOk())
                .getResponseAsClass(ApiInboundDto.class);

        assertThat(apiInboundDto)
                .extracting(
                        ApiInboundDto::getId,
                        ApiInboundDto::getExternalId,
                        ApiInboundDto::getStatus,
                        ApiInboundDto::getType
                ).containsExactly(
                        inbound.getId(),
                        CREATED_EXTERNAL_ID,
                        InboundStatus.CONFIRMED,
                        InboundType.XDOC_TRANSIT
                );
    }

    @Test
    void getUnknownInbound() {
        caller.acceptInbound("anyExternalId-0", null)
                .andExpect(status().isNotFound());
    }

    @Test
    void getFinishedInboundForAccept() {
        flow.createInbound(FINISHED_EXTERNAL_ID)
                .linkPallets(1)
                .fixInbound();

        caller.acceptInbound(FINISHED_EXTERNAL_ID, InboundType.XDOC_TRANSIT)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"INBOUND_FIXED\"}"));
    }

    @Test
    void getInboundForAcceptWithWrongType() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .linkPallets(1)
                .fixInbound();

        caller.acceptInbound(CREATED_EXTERNAL_ID, InboundType.DEFAULT)
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"INCORRECT_INBOUND_TYPE\"}"));
    }

    @DisplayName("Приемка поставки успешна, если поставка в статусе READY_TO_RECEIVE")
    @Test
    void acceptInboundIfItsReadyToReceive() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .carArrived()
                .readyToReceive()
                .apiAcceptInbound(res -> res.andExpect(status().isOk()));
    }

    @DisplayName("Приемка поставки падает с ошибкой, если поставка в статусе CREATE")
    @Test
    void acceptInboundFailsIfStatusCreate() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiAcceptInbound(res -> res.andExpect(status().isBadRequest())
                        .andExpect(content().json("{\"error\": \"INBOUND_NOT_READY_TO_RECEIVE\"}")));
    }

    @Test
    void linkPalletToInboundHappyPath() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiLinkPallet(SORTABLE_ID, res -> res.andExpect(status().isOk()));

        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        assertThat(sortableQueryService.find(sc, SORTABLE_ID)
                .map(Sortable::getMutableState)
                .map(SortableMutableState::getInbound)
                .map(Inbound::getExternalId)
                .orElseThrow(() -> new TplIllegalStateException("Инбаунд не проставился"))
        ).isEqualTo(CREATED_EXTERNAL_ID);
        assertThat(sortableQueryService.find(sc, SORTABLE_ID)
                .map(Sortable::getRequiredBarcodeOrThrow)
                .orElseThrow(() -> new TplIllegalStateException("Сортабл не создался"))
        ).isEqualTo(SORTABLE_ID);
    }

    @Test
    void linkPalletToInboundHappyPathCyrillicInformationListCode() {
        flow.inboundBuilder(CREATED_EXTERNAL_ID)
                .informationListBarcode(INFORMATION_LIST_CODE)
                .confirm(true)
                .build()
                .getInbound();

        caller.acceptInbound(INFORMATION_LIST_CODE_URL_ENCODED, InboundType.XDOC_TRANSIT)
                .andExpect(status().isOk());

        var response = caller.acceptInboundsV2(INFORMATION_LIST_CODE_URL_ENCODED, InboundType.XDOC_TRANSIT)
                .andExpect(status().isOk())
                .getResponseAsClass(ApiInboundListDto.class);

        assertThat(response.getInbounds()).hasSize(1);

        caller.linkToInbound(
                        INFORMATION_LIST_CODE_URL_ENCODED,
                        new LinkToInboundRequestDto(SORTABLE_ID, SortableType.XDOC_PALLET)
                )
                .andExpect(status().isOk());
    }

    @Test
    void getInboundByInformationListBarcode() {
        var inbound = flow.inboundBuilder(CREATED_EXTERNAL_ID)
                .informationListBarcode(INFORMATION_LIST_CODE)
                .createAndGet();

        var apiInboundDto = caller.acceptInbound(CREATED_EXTERNAL_ID, null)
                .andExpect(status().isOk())
                .getResponseAsClass(ApiInboundDto.class);

        assertThat(apiInboundDto)
                .extracting(
                        ApiInboundDto::getId,
                        ApiInboundDto::getExternalId,
                        ApiInboundDto::getStatus,
                        ApiInboundDto::getType
                ).containsExactly(
                        inbound.getId(),
                        CREATED_EXTERNAL_ID,
                        InboundStatus.CONFIRMED,
                        InboundType.XDOC_TRANSIT
                );
    }

    @Test
    void linkPalletToInboundByInformationListBarcode() {
        flow.inboundBuilder(CREATED_EXTERNAL_ID)
                .informationListBarcode(INFORMATION_LIST_CODE)
                .build();

        caller.linkToInbound(
                        CYRILLIC_TRIMMED_INFORMATION_LIST_CODE,
                        new LinkToInboundRequestDto(SORTABLE_ID, SortableType.XDOC_PALLET)
                )
                .andExpect(status().isOk());

        Sortable sortable = sortableQueryService.find(testFactory.getSortingCenterById(TestFactory.SC_ID), SORTABLE_ID)
                .orElseThrow();
        assertThat(sortable.getInbound().getExternalId()).isEqualTo(CREATED_EXTERNAL_ID);
    }

    @Test
    void linkPalletToInboundByInformationListBarcodeButThereAreDuplicatesByInfoListCode() {
        flow.inboundBuilder(OLD_EXTERNAL_ID)
                .informationListBarcode(INFORMATION_LIST_CODE)
                .build();
        flow.inboundBuilder(CREATED_EXTERNAL_ID)
                .informationListBarcode(INFORMATION_LIST_CODE)
                .build();

        caller.linkToInbound(
                        CYRILLIC_TRIMMED_INFORMATION_LIST_CODE,
                        new LinkToInboundRequestDto(SORTABLE_ID, SortableType.XDOC_PALLET)
                )
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(
                        "$.message",
                        equalTo("Несколько активных поставок с номером 1342534! Обратитесь в поддержку!")
                ));
    }

    @Test
    void linkBoxToInboundHappyPath() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiLinkBox(SORTABLE_ID, res -> res.andExpect(status().isOk()));

        var sc = testFactory.getSortingCenterById(TestFactory.SC_ID);
        assertThat(sortableQueryService.find(sc, SORTABLE_ID)
                .map(Sortable::getMutableState)
                .map(SortableMutableState::getInbound)
                .map(Inbound::getExternalId)
                .orElseThrow(() -> new TplIllegalStateException("Инбаунд не проставился"))
        ).isEqualTo(CREATED_EXTERNAL_ID);
    }

    @Test
    void linkPalletAgainToTheSameInbound() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiLinkPallet(SORTABLE_ID, res -> res.andExpect(status().isOk()))
                .apiLinkPallet(SORTABLE_ID, res -> res.andExpect(status().isOk()));

        Inbound inbound = inboundRepository.findByExternalId(CREATED_EXTERNAL_ID).orElseThrow();
        assertThat(inbound.getExternalId()).isEqualTo(CREATED_EXTERNAL_ID);
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);

        Sortable sortable = sortableQueryService.find(sortingCenter, SORTABLE_ID).orElseThrow();
        assertThat(sortable.getRequiredBarcodeOrThrow()).isEqualTo(SORTABLE_ID);
        assertThat(sortable.getStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
    }

    @Test
    void linkTwoPalletsToOneInbound() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiLinkPallet(SORTABLE_ID, res -> res.andExpect(status().isOk()))
                .apiLinkPallet("XDOC-125", res -> res.andExpect(status().isOk()));

        Inbound inbound = inboundRepository.findByExternalId(CREATED_EXTERNAL_ID).orElseThrow();
        assertThat(inbound.getExternalId()).isEqualTo(CREATED_EXTERNAL_ID);
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);

        Sortable sortable = sortableQueryService.find(sortingCenter, SORTABLE_ID).orElseThrow();
        assertThat(sortable.getRequiredBarcodeOrThrow()).isEqualTo(SORTABLE_ID);
        assertThat(sortable.getStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);

        Sortable sortable2 = sortableQueryService.find(sortingCenter, "XDOC-125").orElseThrow();
        assertThat(sortable2.getRequiredBarcodeOrThrow()).isEqualTo("XDOC-125");
        assertThat(sortable2.getStatus()).isEqualTo(SortableStatus.ARRIVED_DIRECT);
    }

    @Test
    void linkXdocPalletToNonXdocInbound() {
        flow.inboundBuilder(CREATED_EXTERNAL_ID)
                .type(InboundType.DS_SC)
                .build()
                .apiLinkPallet(SORTABLE_ID, res -> res.andExpect(status().isBadRequest()));
    }

    @Test
    void linkInboundFromDifferentSortingCenter() {
        flow.createInbound(CREATED_EXTERNAL_ID);

        tsdFromAnotherSc.linkToInbound(
                        CREATED_EXTERNAL_ID,
                        new LinkToInboundRequestDto("XDOC-124", SortableType.XDOC_PALLET)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void linkNonXdocPalletToXdocInbound() {
        flow.createInbound(CREATED_EXTERNAL_ID);

        caller.linkToInbound(CREATED_EXTERNAL_ID, new LinkToInboundRequestDto("XDOC-124", SortableType.PALLET))
                .andExpect(status().isBadRequest());
    }

    @Test
    void linkXdocPalletWithWrongBarcodePrefix() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiLinkPallet("123", res -> res.andExpect(status().isBadRequest()));
    }

    @Test
    void linkPalletToFinishedInbound() {
        flow.createInbound(FINISHED_EXTERNAL_ID)
                .apiLinkPallet("XDOC-666", res -> res.andExpect(status().isOk()))
                .fixInbound()
                .toArrival(FINISHED_EXTERNAL_ID)
                .apiLinkPallet(SORTABLE_ID, res -> res.andExpect(status().isBadRequest()));
    }

    @Test
    void linkAcceptedXdocPalletToAnotherInbound() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .linkPallets(SORTABLE_ID)
                .and()
                .createInbound(ANOTHER_CREATED_EXTERNAL_ID)
                .apiLinkPallet(SORTABLE_ID, res -> res.andExpect(status().isBadRequest())
                        .andExpect(content().json("{\"error\": \"LINKED_TO_ANOTHER_INBOUND\"}")));
    }

    @Test
    void unlinkPalletFromInbound() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .linkPallets(SORTABLE_ID);

        var maybeSortable = sortableQueryService.find(flow.getSortingCenter(), SORTABLE_ID);
        assertThat(maybeSortable).isNotEmpty();

        caller.unlinkFromInbound(CREATED_EXTERNAL_ID, SORTABLE_ID)
                .andExpect(status().isOk());

        maybeSortable = sortableQueryService.find(flow.getSortingCenter(), SORTABLE_ID);
        assertThat(maybeSortable).isEmpty();
    }

    @Test
    void unlinkPalletFromAcceptedInbound() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .linkPallets(SORTABLE_ID)
                .finishAcceptance();

        caller.unlinkFromInbound(CREATED_EXTERNAL_ID, SORTABLE_ID)
                .andExpect(status().is4xxClientError());

        var maybeSortable = sortableQueryService.find(flow.getSortingCenter(), SORTABLE_ID);
        assertThat(maybeSortable).isNotEmpty();
    }

    @Test
    void unlinkPalletFromFixedInbound() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .linkPallets(SORTABLE_ID)
                .fixInbound();

        caller.unlinkFromInbound(CREATED_EXTERNAL_ID, SORTABLE_ID)
                .andExpect(status().is4xxClientError());

        var maybeSortable = sortableQueryService.find(flow.getSortingCenter(), SORTABLE_ID);
        assertThat(maybeSortable).isNotEmpty();
    }

    @Test
    void unlinkNotExistentSortable() {
        flow.createInbound(CREATED_EXTERNAL_ID)
                .linkPallets(SORTABLE_ID);

        caller.unlinkFromInbound(CREATED_EXTERNAL_ID, "XDOC-666123123123")
                .andExpect(status().is4xxClientError());
    }

    @DisplayName("Приемка палеты успешна, если поставка в статусе READY_TO_RECEIVE")
    @Test
    void linkPalletToInboundIfItsReadyToReceive() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .carArrived()
                .readyToReceive()
                .apiLinkPallet("XDOC-1", res -> res.andExpect(status().isOk()));
    }

    @DisplayName("Приемка коробки успешна, если поставка в статусе READY_TO_RECEIVE")
    @Test
    void linkBoxToInboundIfItsReadyToReceive() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .carArrived()
                .readyToReceive()
                .apiLinkBox("XDOC-1", res -> res.andExpect(status().isOk()));
    }

    @DisplayName("Приемка палеты падает с ошибкой, если поставка в статусе CREATE")
    @Test
    void linkPalletToInboundFailsIfItsNotReadyToReceive() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiLinkPallet("XDOC-1", res -> res.andExpect(status().isBadRequest())
                        .andExpect(content().json("{\"error\": \"INBOUND_NOT_READY_TO_RECEIVE\"}")));
    }

    @DisplayName("Приемка коробки падает с ошибкой, если поставка в статусе CREATE")
    @Test
    void linkBoxToInboundFailsIfItsNotReadyToReceive() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .apiLinkBox("XDOC-1", res -> res.andExpect(status().isBadRequest())
                        .andExpect(content().json("{\"error\": \"INBOUND_NOT_READY_TO_RECEIVE\"}")));
    }

    @DisplayName("После приемки первой коробки, остальные коробки тоже могут быть приняты")
    @Test
    void subsequentBoxesAllowedToReceiveAfterFirstSortableWasLinkedToInbound() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .carArrived()
                .readyToReceive()
                .apiLinkBox("XDOC-1", res -> res.andExpect(status().isOk()))
                .apiLinkBox("XDOC-2", res -> res.andExpect(status().isOk()))
                .apiLinkBox("XDOC-3", res -> res.andExpect(status().isOk()));
    }

    @DisplayName("После приемки первой палеты, остальные палеты тоже могут быть приняты")
    @Test
    void subsequentPalletsAllowedToReceiveAfterFirstSortableWasLinkedToInbound() {
        enableReadyToReceiveStatus();

        flow.createInbound(CREATED_EXTERNAL_ID)
                .carArrived()
                .readyToReceive()
                .apiLinkPallet("XDOC-1", res -> res.andExpect(status().isOk()))
                .apiLinkPallet("XDOC-2", res -> res.andExpect(status().isOk()))
                .apiLinkPallet("XDOC-3", res -> res.andExpect(status().isOk()));
    }

    @DisplayName("Ошибка при попытке привязать сортаблы разных типов к поставке")
    @Test
    void linkBoxAndPalletToInbound() {
        flow.createInbound("in-1")
                .linkBoxes("XDOC-b-1")
                .apiLinkPallet("XDOC-p-1", res -> res.andExpect(status().is4xxClientError()));
    }

    @DisplayName("Приемка поставки возвращает тип сортабла привязанного к поставке")
    @Test
    void returnSortableType() {
        flow.createInbound("in1");
        caller.acceptInbound("in1", InboundType.XDOC_TRANSIT)
                .andExpect(status().isOk())
                .andExpect(content()
                        .string(
                                not(content().json("{\"sortableType\":\"XDOC_PALLET\"}"))
                        )
                );

        flow.toArrival("in1")
                .linkPallets("XDOC-1");
        caller.acceptInbound("in1", InboundType.XDOC_TRANSIT)
                .andExpect(status().isOk())
                .andExpect(content().json("{\"sortableType\": \"XDOC_PALLET\"}"));
    }

    @Nested
    @DisplayName("Фиксация поставки через приложение")
    class FixInbound {

        SortingCenter sortingCenter;
        Warehouse nextLogisticPoint;
        Warehouse nextLogisticPoint2;
        Cell keep;

        @BeforeEach
        void init() {
            sortingCenter = testFactory.storedSortingCenter();
            nextLogisticPoint = testFactory.storedWarehouse("101010987");
            nextLogisticPoint2 = testFactory.storedWarehouse("101010988");
            keep = testFactory.storedCell(
                    sortingCenter,
                    "samara-keep",
                    CellType.BUFFER,
                    CellSubType.BUFFER_XDOC,
                    nextLogisticPoint.getYandexId()
            );

        }

        @DisplayName("Получение информации о поставке по externalId, обрезанному ШК с инфо листа и по ШК " +
                "корбки/палеты для заверения поставки")
        @Test
        void getInboundInfoForAcceptance() {
            var inbound = flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .carArrived()
                    .readyToReceive()
                    .linkPallets("XDOC-1")
                    .getInbound();

            var apiInboundDto = caller.getInboundForFinishAcceptance(CREATED_EXTERNAL_ID)
                    .andExpect(status().isOk())
                    .getResponseAsClass(ApiInboundDto.class);

            assertThat(apiInboundDto)
                    .extracting(
                            ApiInboundDto::getId,
                            ApiInboundDto::getExternalId,
                            ApiInboundDto::getInformationListCode,
                            ApiInboundDto::getStatus,
                            ApiInboundDto::getType,
                            ApiInboundDto::getInfo
                    ).containsExactly(
                            inbound.getId(),
                            CREATED_EXTERNAL_ID,
                            INFORMATION_LIST_CODE,
                            InboundStatus.ARRIVED,
                            InboundType.XDOC_TRANSIT,
                            ApiInboundInfoDto.builder()
                                    .pallets(List.of("XDOC-1"))
                                    .unsortedPallets(List.of("XDOC-1"))
                                    .build()
                    );
        }

        @DisplayName("Получение информации о поставке по externalId, обрезанному ШК с инфо листа и по ШК корбки/палеты")
        @ParameterizedTest
        @ValueSource(strings = {CREATED_EXTERNAL_ID, CYRILLIC_TRIMMED_INFORMATION_LIST_CODE, "XDOC-1"})
        void getInboundInfo(String identifier) {
            var inbound = flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkPallets("XDOC-1")
                    .getInbound();

            var apiInboundDto = caller.getInboundForFix(identifier)
                    .andExpect(status().isOk())
                    .getResponseAsClass(ApiInboundDto.class);

            assertThat(apiInboundDto)
                    .extracting(
                            ApiInboundDto::getId,
                            ApiInboundDto::getExternalId,
                            ApiInboundDto::getInformationListCode,
                            ApiInboundDto::getStatus,
                            ApiInboundDto::getType,
                            ApiInboundDto::getInfo
                    ).containsExactly(
                            inbound.getId(),
                            CREATED_EXTERNAL_ID,
                            INFORMATION_LIST_CODE,
                            InboundStatus.ARRIVED,
                            InboundType.XDOC_TRANSIT,
                            ApiInboundInfoDto.builder()
                                    .pallets(List.of("XDOC-1"))
                                    .unsortedPallets(List.of("XDOC-1"))
                                    .build()
                    );
        }

        @DisplayName("Получение информации о поставке по externalId, обрезанному ШК с инфо листа и по ШК корбки/палеты")
        @ParameterizedTest
        @ValueSource(strings = {CREATED_EXTERNAL_ID, CYRILLIC_TRIMMED_INFORMATION_LIST_CODE, "XDOC-1"})
        void getInboundInfoBoxes(String identifier) {
            Cell cell = flow.createBufferCellBoxAndGet("cell-1", nextLogisticPoint.getYandexId());
            var inbound = flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkBoxes("XDOC-1")
                    .getInbound();

            var apiInboundDto = caller.getInboundForFix(identifier)
                    .andExpect(status().isOk())
                    .getResponseAsClass(ApiInboundDto.class);

            assertThat(apiInboundDto)
                    .extracting(
                            ApiInboundDto::getId,
                            ApiInboundDto::getExternalId,
                            ApiInboundDto::getInformationListCode,
                            ApiInboundDto::getStatus,
                            ApiInboundDto::getType,
                            ApiInboundDto::getInfo
                    ).containsExactly(
                            inbound.getId(),
                            CREATED_EXTERNAL_ID,
                            INFORMATION_LIST_CODE,
                            InboundStatus.ARRIVED,
                            InboundType.XDOC_TRANSIT,
                            ApiInboundInfoDto.builder()
                                    .boxes(List.of("XDOC-1"))
                                    .unpackedBoxes(List.of("XDOC-1"))
                                    .build()
                    );
            flow.sortToAvailableCell("XDOC-1");
            apiInboundDto = caller.getInboundForFix(identifier)
                    .andExpect(status().isOk())
                    .getResponseAsClass(ApiInboundDto.class);

            assertThat(sortableQueryService.find(sortingCenter, "XDOC-1").orElseThrow())
                    .extracting(Sortable::getStatus).isEqualTo(SortableStatus.KEEPED_DIRECT);

            assertThat(apiInboundDto)
                    .extracting(
                            ApiInboundDto::getId,
                            ApiInboundDto::getExternalId,
                            ApiInboundDto::getInformationListCode,
                            ApiInboundDto::getStatus,
                            ApiInboundDto::getType,
                            ApiInboundDto::getInfo
                    ).containsExactly(
                            inbound.getId(),
                            CREATED_EXTERNAL_ID,
                            INFORMATION_LIST_CODE,
                            InboundStatus.ARRIVED,
                            InboundType.XDOC_TRANSIT,
                            ApiInboundInfoDto.builder()
                                    .boxes(List.of("XDOC-1"))
                                    .unpackedBoxes(List.of())
                                    .build()
                    );
        }

        @DisplayName("Получение информации о поставке по externalId, обрезанному ШК с инфо листа и по ШК корбки/палеты")
        @Test
        void getInboundInfoForFixed() {
            flow.createBufferCellAndGet("cell-1", nextLogisticPoint.getYandexId());
            flow.createShipCellAndGet("cell-2");
            flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkPallets("XDOC-1")
                    .fixInbound()
                    .sortToAvailableCell("XDOC-1")
                    .createOutbound("out-1")
                    .buildRegistry("XDOC-1")
                    .sortToAvailableCell("XDOC-1")
                    .prepareToShip("XDOC-1")
                    .shipAndGet("out-1");

            caller.getInboundForFix(CYRILLIC_TRIMMED_INFORMATION_LIST_CODE)
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().json("""
                            {
                                "message": "Поставка отгружена, обратитесь в поддержку!"
                            }
                            """));
        }

        @DisplayName("Получение информации о поставке по обрезанному ШК с инфо листа, существуют две такие поставки, " +
                "fallback на ШК палеты/коробки")
        @Test
        void getInboundForFixThanTwoInboundsWithSameInfoListCodeFallbackToSortableBarcode() {
            flow.createBufferCellAndGet("cell-1", nextLogisticPoint.getYandexId());
            flow.createShipCellAndGet("cell-2");

            flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkPallets("XDOC-1");

            flow.inboundBuilder(ANOTHER_CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint2.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build();

            caller.getInboundForFix(CYRILLIC_TRIMMED_INFORMATION_LIST_CODE)
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().json("{\"message\": \"По коду инфо листа " +
                            CYRILLIC_TRIMMED_INFORMATION_LIST_CODE + " найдено 2 поставки. Попробуйте отсканировать номер палеты/коробки\" }"));


            var inboundDto = caller.getInboundForFix("XDOC-1")  // сканим ШК палеты
                    .andExpect(status().isOk())
                    .getResponseAsClass(ApiInboundDto.class);

            assertThat(inboundDto.getExternalId()).isEqualTo(CREATED_EXTERNAL_ID);

            caller.fixInbound(inboundDto.getExternalId())
                    .andExpect(status().isOk());
        }

        @ParameterizedTest(name = "получение информации о поставках различных по составу (с сортировкой и без)")
        @MethodSource("inboundsInfo")
        void emptyInfoIfInboundIsEmpty(String[] boxes, String[] boxesToSort,
                                       String[] pallets, String[] palletsToSort,
                                       ApiInboundInfoDto expectedInfo) {
            flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkBoxes(boxes)
                    .linkPallets(pallets);

            // сортировка в коробок в лот
            var lotDto = sortableTestFactory.createEmptyLot(sortingCenter, keep);
            User user = testFactory.findUserByUid(TestFactory.USER_UID_LONG);
            Arrays.stream(boxesToSort).forEach(
                    barcode -> sortableTestFactory.lotSort(barcode, lotDto.getExternalId(), user)
            );
            // сортировка в палет в ячейку
            Arrays.stream(palletsToSort).forEach(
                    barcode -> sortableTestFactory.sortByBarcode(barcode, keep.getId())
            );

            var apiInboundDto = caller.getInboundForFix(CREATED_EXTERNAL_ID)
                    .andExpect(status().isOk())
                    .getResponseAsClass(ApiInboundDto.class);

            assertThat(apiInboundDto.getInfo())
                    .isEqualTo(expectedInfo);
        }

        private static List<Arguments> inboundsInfo() {
            return List.of(
                    Arguments.of(new String[]{}, new String[]{}, new String[]{}, new String[]{},
                            ApiInboundInfoDto.builder()
                                    .boxes(Collections.emptyList())
                                    .unpackedBoxes(Collections.emptyList())
                                    .pallets(Collections.emptyList())
                                    .unsortedPallets(Collections.emptyList())
                                    .build()
                    ),
                    Arguments.of(new String[]{"XDOC-1"}, new String[]{}, new String[]{}, new String[]{},
                            ApiInboundInfoDto.builder()
                                    .boxes(List.of("XDOC-1"))
                                    .unpackedBoxes(List.of("XDOC-1"))
                                    .pallets(Collections.emptyList())
                                    .unsortedPallets(Collections.emptyList())
                                    .build()
                    ),
                    Arguments.of(new String[]{"XDOC-1"}, new String[]{"XDOC-1"}, new String[]{}, new String[]{},
                            ApiInboundInfoDto.builder()
                                    .boxes(List.of("XDOC-1"))
                                    .unpackedBoxes(Collections.emptyList())
                                    .pallets(Collections.emptyList())
                                    .unsortedPallets(Collections.emptyList())
                                    .build()
                    ),
                    Arguments.of(new String[]{"XDOC-1", "XDOC-2"}, new String[]{"XDOC-1"}, new String[]{},
                            new String[]{},
                            ApiInboundInfoDto.builder()
                                    .boxes(List.of("XDOC-1", "XDOC-2"))
                                    .unpackedBoxes(List.of("XDOC-2"))
                                    .pallets(Collections.emptyList())
                                    .unsortedPallets(Collections.emptyList())
                                    .build()
                    ),
                    Arguments.of(new String[]{}, new String[]{}, new String[]{"XDOC-1"}, new String[]{},
                            ApiInboundInfoDto.builder()
                                    .boxes(Collections.emptyList())
                                    .unpackedBoxes(Collections.emptyList())
                                    .pallets(List.of("XDOC-1"))
                                    .unsortedPallets(List.of("XDOC-1"))
                                    .build()
                    ),
                    Arguments.of(new String[]{}, new String[]{}, new String[]{"XDOC-1"}, new String[]{"XDOC-1"},
                            ApiInboundInfoDto.builder()
                                    .boxes(Collections.emptyList())
                                    .unpackedBoxes(Collections.emptyList())
                                    .pallets(List.of("XDOC-1"))
                                    .unsortedPallets(Collections.emptyList())
                                    .build()
                    ),
                    Arguments.of(new String[]{}, new String[]{}, new String[]{"XDOC-1", "XDOC-2"}, new String[]{"XDOC" +
                                    "-1"},
                            ApiInboundInfoDto.builder()
                                    .boxes(Collections.emptyList())
                                    .unpackedBoxes(Collections.emptyList())
                                    .pallets(List.of("XDOC-1", "XDOC-2"))
                                    .unsortedPallets(List.of("XDOC-2"))
                                    .build()
                    )
            );
        }


        @DisplayName("Успешное завершение поставки по externalId")
        @Test
        void successfulFixInbound() {
            flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkPallets("XDOC-1");
            sortableTestFactory.sortByBarcode("XDOC-1", keep.getId());

            caller.fixInbound(CREATED_EXTERNAL_ID)
                    .andExpect(status().isOk());

            var inbound = inboundRepository.findByExternalId(CREATED_EXTERNAL_ID).orElseThrow();
            assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.FIXED);
        }

        @DisplayName("Попытка завершить пустую поставку по externalId")
        @Test
        void attemptToFixEmptyInbound() {
            flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build();

            caller.fixInbound(CREATED_EXTERNAL_ID)
                    .andExpect(status().is(400))
                    .andExpect(jsonPath("$.error").value("CANT_FIX_EMPTY_INBOUND"));

            var inbound = inboundRepository.findByExternalId(CREATED_EXTERNAL_ID).orElseThrow();
            assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.CONFIRMED);
        }

        @DisplayName("Попытка завершить не существующую поставку")
        @Test
        void attemptToFixInboundThatDoesNotExist() {
            var nonExistentId = "99999111";
            caller.fixInbound(nonExistentId)
                    .andExpect(status().is(404))
                    .andExpect(jsonPath("$.error").value("ENTITY_NOT_FOUND"));
        }

        @DisplayName("Попытка завершить поставку, которая принадлежит другому Сорт Центру")
        @Test
        void attemptToFixInboundWhichDoesNotBelongToSortingCenter() {
            flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkPallets("XDOC-1");
            sortableTestFactory.sortByBarcode("XDOC-1", keep.getId());

            tsdFromAnotherSc.fixInbound(CREATED_EXTERNAL_ID)
                    .andExpect(status().is(404))
                    .andExpect(jsonPath("$.error").value("ENTITY_NOT_FOUND"));

            var inbound = inboundRepository.findByExternalId(CREATED_EXTERNAL_ID).orElseThrow();
            assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.ARRIVED);
        }

        @DisplayName("Вызов fixInbound с ШК с инфо листа")
        @Test
        void attemptToFixByInformationListCode() {
            flow.inboundBuilder(CREATED_EXTERNAL_ID)
                    .nextLogisticPoint(nextLogisticPoint.getYandexId())
                    .informationListBarcode(INFORMATION_LIST_CODE)
                    .build()
                    .linkPallets("XDOC-1");
            sortableTestFactory.sortByBarcode("XDOC-1", keep.getId());

            caller.fixInbound(INFORMATION_LIST_CODE_URL_ENCODED)
                    .andExpect(status().isOk());

            var inbound = inboundRepository.findByExternalId(CREATED_EXTERNAL_ID).orElseThrow();
            assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.FIXED);
        }

    }

    private void enableReadyToReceiveStatus() {
        testFactory.setSortingCenterProperty(
                TestFactory.SC_ID,
                SortingCenterPropertiesKey.ENABLE_XDOC_INBOUND_READY_TO_RECEIVE_STATUS,
                true);
    }

    @Test
    public void acceptNotConfirmedInbound() {
        flow.inboundBuilder("in-1")
                .confirm(false)
                .build();
        caller.acceptInbound("in-1", null)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void acceptConfirmedInbound() {
        flow.inboundBuilder("in-1")
                .confirm(true)
                .build();
        caller.acceptInbound("in-1", null)
                .andExpect(status().isOk());
    }

    @Test
    public void acceptTwoInboundsV2() {
        Warehouse warehouse = testFactory.storedWarehouse("second-warehouse");
        Inbound inbound = flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-111")
                .confirm(true)
                .nextLogisticPoint(TestFactory.WAREHOUSE_YANDEX_ID)
                .build()
                .getInbound();
        Inbound inbound1 = flow.inboundBuilder("in-2")
                .informationListBarcode("Зп-111")
                .confirm(true)
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .getInbound();

        caller.acceptInboundsV2("111", null)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponseForInboundsV2(List.of(inbound, inbound1))));
    }

    @Test
    public void acceptTwoInboundsOneNotNeededV2() {
        Warehouse warehouse = testFactory.storedWarehouse("second-warehouse");
        Inbound inbound = flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-111")
                .confirm(true)
                .nextLogisticPoint(TestFactory.WAREHOUSE_YANDEX_ID)
                .build()
                .getInbound();
        Inbound inbound1 = flow.inboundBuilder("in-2")
                .informationListBarcode("Зп-111")
                .confirm(true)
                .nextLogisticPoint(warehouse.getYandexId())
                .build()
                .getInbound();

        caller.acceptInboundsV2("111", null)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponseForInboundsV2(List.of(inbound, inbound1))));
    }

    @Test
    public void acceptInboundV2NotXdoc() {
        Inbound inbound = flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-111")
                .confirm(true)
                .build()
                .getInbound();

        Inbound inbound2 = flow.inboundBuilder("in-2")
                        .type(InboundType.CROSSDOCK)
                        .informationListBarcode("Зп-111")
                        .nextLogisticPoint("1")
                        .createAndGet();

        caller.acceptInboundsV2("111", InboundType.XDOC_TRANSIT)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponseForInboundsV2(List.of(inbound))));
    }

    @Test
    public void acceptInboundV2() {
        Inbound inbound = flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-111")
                .confirm(true)
                .build()
                .getInbound();

        caller.acceptInboundsV2("111", InboundType.XDOC_TRANSIT)
                .andExpect(status().isOk())
                .andExpect(content().json(getExpectedResponseForInboundsV2(List.of(inbound))));
    }

    @Test
    public void acceptInboundV2Fail() {
        Inbound inbound = flow.inboundBuilder("in-1")
                .informationListBarcode("Зп-111")
                .confirm(true)
                .build()
                .getInbound();

        caller.acceptInboundsV2("slkdjf", InboundType.XDOC_TRANSIT)
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void saveVghPallet() {
        flow.createInbound("in-1")
                .linkPallets("XDOC-1");

        caller.saveVgh("XDOC-1", SaveVGHRequestDto.builder()
                        .vgh(new UnitMeasurementsDto(
                                        new BigDecimal(120),
                                        new BigDecimal(150),
                                        new BigDecimal(80),
                                        new BigDecimal(200)
                                )
                        )
                        .sortableType(SortableType.XDOC_PALLET)
                        .build()
                )
                .andExpect(status().isOk());
        Sortable sortable = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        Optional<MeasurementSo> sortableVGH = measurementsSoRepository.findBySortable(sortable);
        assertThat(sortableVGH).isNotEmpty();
    }

    @Test
    public void saveVghBasket() {
        Cell cell = flow.createBufferCellAndGet("cell-1", TestFactory.WAREHOUSE_YANDEX_ID);
        var lot = flow.createBasket(cell);
        flow.createInbound("in-1")
                .linkBoxes("XDOC-1");
        Sortable box = sortableQueryService.find(flow.getSortingCenter(), "XDOC-1").orElseThrow();
        flow.sortBoxToLot(box, lot);

        caller.saveVgh(lot.getBarcode(), SaveVGHRequestDto.builder()
                        .vgh(new UnitMeasurementsDto(
                                        new BigDecimal(120),
                                        new BigDecimal(150),
                                        new BigDecimal(80),
                                        new BigDecimal(200)
                                )
                        )
                        .sortableType(SortableType.XDOC_BASKET)
                        .build()
                )
                .andExpect(status().isOk());

        Sortable sortable = sortableQueryService.find(flow.getSortingCenter(), lot.getBarcode()).orElseThrow();
        Optional<MeasurementSo> sortableVGH = measurementsSoRepository.findBySortable(sortable);
        assertThat(sortableVGH).isNotEmpty();
        assertThat(sortableVGH.get().getUnitMeasurements()).isEqualTo(UnitMeasurements.builder()
                .width(new BigDecimal(120))
                .height(new BigDecimal(150))
                .length(new BigDecimal(80))
                .weight(new BigDecimal(200))
                .build()
        );
        caller.saveVgh(lot.getBarcode(), SaveVGHRequestDto.builder()
                        .vgh(new UnitMeasurementsDto(
                                        new BigDecimal(140),
                                        new BigDecimal(160),
                                        new BigDecimal(90),
                                        new BigDecimal(300)
                                )
                        )
                        .sortableType(SortableType.XDOC_BASKET)
                        .build()
                )
                .andExpect(status().isOk());
        sortableVGH = measurementsSoRepository.findBySortable(sortable);
        assertThat(sortableVGH.get().getUnitMeasurements()).isEqualTo(UnitMeasurements.builder()
                .width(new BigDecimal(140))
                .height(new BigDecimal(160))
                .length(new BigDecimal(90))
                .weight(new BigDecimal(300))
                .build()
        );
    }

    @Test
    void finishAcceptance() {
        flow.createInbound("in-1")
                .linkPallets("XDOC-1")
                .finishAcceptance();

        Inbound inbound = inboundRepository.findByExternalId("in-1").orElseThrow();
        assertThat(inbound.getInboundStatus()).isEqualTo(InboundStatus.INITIAL_ACCEPTANCE_COMPLETED);
    }

    private String getExpectedResponseForInboundsV2(List<Inbound> inbounds) {
        String expectedResponse = inbounds.stream()
                .map(this::getExpectedResponseNonStrict)
                .collect(Collectors.joining(","));
        return """
                {
                    "inbounds": [
                        %s
                    ]
                }
                """.formatted(expectedResponse);
    }

    private String getExpectedResponseNonStrict(Inbound inbound) {
        Warehouse warehouse = warehouseRepository.findByYandexIdOrThrow(inbound.getNextLogisticPointId());
        return """
                {
                    "externalId": "%s",
                    "informationListCode": "%s",
                    "destination": "%s"
                }
                """.formatted(inbound.getExternalId(), inbound.getInformationListCode(), warehouse.getIncorporation());
    }

}
