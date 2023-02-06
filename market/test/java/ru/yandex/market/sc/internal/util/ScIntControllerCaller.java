package ru.yandex.market.sc.internal.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.sc.core.domain.cell.model.CellRequestDto;
import ru.yandex.market.sc.core.domain.cell.model.CellSubType;
import ru.yandex.market.sc.core.domain.inbound.model.GroupPutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.model.InboundAvailableAction;
import ru.yandex.market.sc.core.domain.inbound.model.InboundPartnerParamsDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundPartnerStatusDto;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.PutCarInfoRequest;
import ru.yandex.market.sc.core.domain.inbound.model.PutDocumentInfoRequest;
import ru.yandex.market.sc.core.domain.lot.model.PartnerLotRequestDto;
import ru.yandex.market.sc.core.domain.outbound.model.partner.AxaptaOutboundDocs;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.sortable.model.UnlinkSortablesRequest;
import ru.yandex.market.sc.core.domain.sortable.model.les_event.ResendSortableInfoToSqsRequest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.SneakyResultActions;
import ru.yandex.market.sc.internal.controller.dto.PartnerShipOutboundDto;
import ru.yandex.market.sc.internal.controller.dto.user.HermesUserRequestDto;
import ru.yandex.market.sc.internal.controller.manual.xdoc.CreateTopologyRequest;
import ru.yandex.market.sc.internal.controller.manual.xdoc.DeleteInboundRequest;
import ru.yandex.market.sc.internal.controller.manual.xdoc.DeleteSortablesRequest;
import ru.yandex.market.sc.internal.model.CreateDemoInboundWithRegistryDto;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Обеспечивает вызов контроллеров sc-int с помощью MockMvc
 */
@RequiredArgsConstructor
public class ScIntControllerCaller {

    public static final PutCarInfoRequest PUT_CAR_INFO_REQUEST = PutCarInfoRequest.builder()
            .fullName("name")
            .phoneNumber("+79998882211")
            .carNumber("XXX")
            .carBrand("volvo")
            .trailerNumber("YYY")
            .comment("no comments")
            .build();

    private static final String INTERNAL_PARTNER_OUTBOUNDS_PRESHIP_STATE_URL_GET = "/internal/partners/{0}/outbounds/{1}/preShipState";
    private static final String INTERNAL_PARTNER_OUTBOUNDS_SHIP_URL_PUT = "/internal/partners/{0}/outbounds/{1}/ship";
    private static final String INTERNAL_PARTNER_V2_OUTBOUNDS_SHIP_URL_PUT = "/internal/partners/{0}/v2/outbounds/{1}/ship";
    private static final String INTERNAL_PARTNER_OUTBOUNDS_PREPARE_URL_PUT =
            "/internal/partners/{0}/outbounds/{1}/prepareToShip";
    private static final String INTERNAL_PARTNER_LOTS_URL_POST = "/internal/partners/{0}/lots";
    private static final String INTERNAL_PARTNER_OUTBOUNDS_URL_GET = "/internal/partners/{0}/outbounds{1}";
    private static final String INTERNAL_PARTNER_OUTBOUNDS_SUMMARY_INFO_URL_GET =
            "/internal/partners/{0}/outbounds/summaryInfo{1}";
    private static final String INTERNAL_PARTNER_CELLS_URL_POST = "/internal/partners/{0}/cells";
    private static final String INTERNAL_PARTNER_CELLS_URL_PUT = "/internal/partners/{0}/cells/{1}";
    private static final String INTERNAL_PARTNER_CELL_TYPES_URL_GET = "/internal/partners/{0}/cellTypes";
    private static final String INTERNAL_PARTNER_CELL_CARGO_TYPES_URL_GET = "/internal/partners/{0}/cellCargoTypes";
    private static final String INTERNAL_PARTNER_CELLS_FOR_LOT_URL_GET = "/internal/partners/{0}/cells/forLots";
    private static final String INTERNAL_PARTNER_INBOUNDS_URL_GET = "/internal/partners/{0}/inbounds{1}";
    private static final String INTERNAL_PARTNER_INBOUNDS_STATUSES_URL_GET = "/internal/partners/{0}/inbounds/statuses";
    private static final String INTERNAL_PARTNER_INBOUNDS_STATUSES_V2_URL_GET =
            "/internal/partners/{0}/v2/inbounds/statuses";
    private static final String INTERNAL_PARTNER_INBOUNDS_TYPES_V2_URL_GET =
            "/internal/partners/{0}/v2/inbounds/types";
    private static final String INTERNAL_PARTNER_INBOUNDS_PERFORM_ACTION_URL_PUT =
            "/internal/partners/{0}/inbounds/{1}/performAction/{2}";
    private static final String INTERNAL_PARTNER_INBOUNDS_INBOUND_INFO_GET_URL =
            "/internal/partners/{0}/inbounds/{1}/inboundInfo";
    private static final String INTERNAL_PARTNER_INBOUNDS_CAR_ARRIVED_PUT_URL =
            "/internal/partners/{0}/inbounds/{1}/carArrived";
    private static final String INTERNAL_PARTNER_INBOUNDS_GROUP_CAR_ARRIVED_PUT_URL =
            "/internal/partners/{0}/inbounds/carArrived";
    private static final String INTERNAL_PARTNER_INBOUNDS_CAR_INFO_PUT_URL =
            "/internal/partners/{0}/inbounds/{1}/carInfo";
    private static final String INTERNAL_PARTNER_INBOUNDS_DOC_INFO_PUT_URL =
            "/internal/partners/{0}/inbounds/{1}/docInfo";

    static class SortableUriConst {
        private static final String INTERNAL_PARTNER_GET_SORTABLES_GET_URL =
                "/internal/partners/{0}/sortables{1}";
        private static final String INTERNAL_PARTNER_GET_SORTABLES_TYPES_GET_URL =
                "/internal/partners/{0}/sortables/types";
        private static final String INTERNAL_PARTNER_GET_SORTABLES_STATUSES_GET_URL =
                "/internal/partners/{0}/sortables/statuses";
        private static final String INTERNAL_PARTNER_GET_SORTABLES_STAGE_STATISTIC_GET_URL =
                "/internal/partners/{0}/sortables/stageStatistic";
    }

    private static final String TM_PARTNER_SAVE_DOCS_URL_POST =
            "/TM/sortingCenterPoint/{0}/outbounds/{1}/docs";

    private static final String TM_PARTNER_SAVE_AXAPTA_REQUEST_URL_PUT =
            "/TM/sortingCenterPoint/{0}/inbounds/{1}/axaptaRequestId";

    private static final String HERMES_USER_PUT = "/hermes/user";
    private static final String HERMES_USER_DELETE = "/hermes/user/{id}";

    private static final String AUTOTEST_DELETE_INBOUND_URL = "/manual/autotest/inbounds";
    private static final String AUTOTEST_DELETE_SORTABLE_URL = "/manual/autotest/sortables";

    private static final String MANUAL_CLEAR_CELL_URL_POST = "/manual/cells/clear";
    private static final String MANUAL_DELETE_CELL_URL_DELETE = "/manual/cells";
    private static final String MANUAL_GET_CELLS_FOR_ZONE_URL_GET = "/manual/cells/getCellsForZone";
    private static final String MANUAL_UNLINK_SORTABLES = "/manual/orders/unlinkSortables";
    private static final String MANUAL_GET_DIFFERENCE_STATE_URL_GET = "/manual/xdoc/stateDiff/{0}";
    private static final String MANUAL_CELLS_XDOC_URL_POST = "/manual/xdoc/cells";
    private static final String MANUAL_XDOC_PUSH_STATE = "/manual/xdoc/pushState/{0}";
    private static final String MANUAL_XDOC_SHIP_ANOMALY = "/manual/anomaly/ship";
    private static final String MANUAL_INBOUND_CREATE_DEMO = "/manual/inbounds/createDemo";

    private static final String MANUAL_SORTABLE_BETA_SORT = "/manual/sortable/beta/sort";
    private static final String MANUAL_SORTABLE_SEND_CARGO_EVENT = "/manual/sortable/sendCargoEvent/{0}";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final String scPartnerId;

    public ScIntControllerCaller(MockMvc mockMvc, String scPartnerId) {
        this.mockMvc = mockMvc;
        this.scPartnerId = scPartnerId;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * будет создан {@link ScIntControllerCaller} с использованием дефолтного сорт центра
     */
    public static ScIntControllerCaller createCaller(MockMvc mockMvc) {
        return createCaller(mockMvc, TestFactory.SC_PARTNER_ID);
    }

    /**
     * предоставляет возмжность выбрать другой сорт центр
     */
    public static ScIntControllerCaller createCaller(MockMvc mockMvc, String scPartnerId) {
        return new ScIntControllerCaller(mockMvc, scPartnerId);
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerOutboundController#prepareToShipOutbound}
     */
    @SneakyThrows
    public SneakyResultActions prepareToShipOutbound(String outboundExternalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_OUTBOUNDS_PREPARE_URL_PUT, scPartnerId, outboundExternalId))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link
     * ru.yandex.market.sc.internal.controller.tm.DocumentsController#saveDocs(String, String, AxaptaOutboundDocs)}
     */
    @SneakyThrows
    public SneakyResultActions saveOutboundDocs(String scPointId, String outboundExternalId, AxaptaOutboundDocs docs) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(MessageFormat.format(TM_PARTNER_SAVE_DOCS_URL_POST, scPointId,
                                        outboundExternalId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(docs))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualSortableController#sortSortable}
     */
    @SneakyThrows
    public SneakyResultActions manualSortableBetaSort(SortableSortRequestDto request, String scId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(MANUAL_SORTABLE_BETA_SORT)
                                .param("scId", scId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
        );
    }

    /**
     * Вызывает {@link
     * ru.yandex.market.sc.internal.controller.tm.AxaptaMovementController.setAxaptaMovementRequestId
     */
    @SneakyThrows
    public SneakyResultActions setAxaptaMovementRequestId(String scPointId, String inboundExternalId,
                                                          String axaptaRequestId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(TM_PARTNER_SAVE_AXAPTA_REQUEST_URL_PUT, scPointId,
                                        inboundExternalId))
                                .param("axaptaRequestId", axaptaRequestId)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerOutboundController#preShipState}
     */
    @SneakyThrows
    public SneakyResultActions preShipState(String outboundExternalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(INTERNAL_PARTNER_OUTBOUNDS_PRESHIP_STATE_URL_GET, scPartnerId,
                                        outboundExternalId)))
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerOutboundController#shipOutbound}
     */
    @SneakyThrows
    public SneakyResultActions shipOutbound(String outboundExternalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_OUTBOUNDS_SHIP_URL_PUT, scPartnerId, outboundExternalId))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerOutboundController#shipXDocOutbound}
     */
    @SneakyThrows
    public SneakyResultActions shipOutboundV2(String outboundExternalId, PartnerShipOutboundDto dto) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_V2_OUTBOUNDS_SHIP_URL_PUT, scPartnerId, outboundExternalId))
                                .content(objectMapper.writeValueAsString(dto))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerLotController#createLot}
     */
    @SneakyThrows
    public SneakyResultActions createLot(PartnerLotRequestDto partnerLotRequestDto) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(MessageFormat.format(INTERNAL_PARTNER_LOTS_URL_POST, scPartnerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(partnerLotRequestDto))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerOutboundController#getOutbounds}
     */
    @SneakyThrows
    public SneakyResultActions getOutbounds(
            @Nullable LocalDate dateFrom,
            @Nullable LocalDate dateTo,
            @Nullable String query,
            @Nullable Pageable pageable
    ) {
        return getOutbounds(INTERNAL_PARTNER_OUTBOUNDS_URL_GET, dateFrom, dateTo, query, pageable);

    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerCellController#createCell}
     */
    @SneakyThrows
    public SneakyResultActions createCell(CellRequestDto cellRequestDto) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(MessageFormat.format(INTERNAL_PARTNER_CELLS_URL_POST, scPartnerId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cellRequestDto))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerCellController#updateCell}
     */
    @SneakyThrows
    public SneakyResultActions updateCell(long cellId, CellRequestDto cellRequestDto) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(INTERNAL_PARTNER_CELLS_URL_PUT, scPartnerId, cellId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cellRequestDto))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerCellController#getCellTypes}
     */
    @SneakyThrows
    public SneakyResultActions getCellTypes() {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(INTERNAL_PARTNER_CELL_TYPES_URL_GET, scPartnerId))
                )
        );
    }

    @SneakyThrows
    public SneakyResultActions getCellCargoTypes() {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(INTERNAL_PARTNER_CELL_CARGO_TYPES_URL_GET, scPartnerId))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerCellController#getCellsForLots}
     */
    @SneakyThrows
    public SneakyResultActions getCellsForLots() {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(INTERNAL_PARTNER_CELLS_FOR_LOT_URL_GET, scPartnerId))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#getInbounds}
     * передает только параметры без Pageable
     */
    @SneakyThrows
    public SneakyResultActions getInbounds(InboundPartnerParamsDto params) {
        return getInbounds(params, null);
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#getInbounds}
     */
    @SneakyThrows
    public SneakyResultActions getInbounds(InboundPartnerParamsDto params, Pageable pageable) {
        String filter = getFilter(params, pageable);
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(INTERNAL_PARTNER_INBOUNDS_URL_GET, scPartnerId, filter))
                )
        );
    }

    private static String getFilter(InboundPartnerParamsDto params, Pageable pageable) {
        String filter = "?date=" + params.getDate();
        if (params.getDateTo() != null) {
            filter += "&dateTo=" + params.getDateTo();
        }
        if (CollectionUtils.isNonEmpty(params.getTypes())) {
            filter += "&types=" + StreamEx.of(params.getTypes()).map(InboundType::toString).joining(",");
        }
        if (pageable != null) {
            filter += "&page=" + pageable.getPageNumber() + "&size=" + pageable.getPageSize();
        }
        if (params.getWarehouseId() != null) {
            filter += "&warehouseId=" + params.getWarehouseId();
        }
        if (params.getNamePart() != null) {
            filter += "&namePart=" + encoded(params.getNamePart());
        }
        if (CollectionUtils.isNonEmpty(params.getStatuses())) {
            filter += "&statuses=" + StreamEx.of(params.getStatuses()).map(InboundPartnerStatusDto::toString).joining(",");
        }
        if (params.getSupplierNamePart() != null) {
            filter += "&supplierNamePart=" + encoded(params.getSupplierNamePart());
        }
        return filter;
    }

    @SneakyThrows
    private SneakyResultActions getOutbounds(
            String url,
            LocalDate dateFrom,
            LocalDate dateTo,
            @Nullable String query,
            @Nullable Pageable pageable
    ) {
        var filter = new StringBuilder("?");

        if (dateFrom != null && dateTo != null) {
            filter
                    .append("&dateFrom=").append(dateFrom)
                    .append("&dateTo=").append(dateTo);
        }

        if (query != null) {
            filter.append("&query=").append(query);
        }
        if (pageable != null) {
            filter.append("&page=").append(pageable.getPageNumber()).append("&size=").append(pageable.getPageSize());
        }

        var filterStr = filter.toString().replace("?&", "?");

        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(url, scPartnerId, filterStr))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualCellController#createCells}
     */
    @SneakyThrows
    public SneakyResultActions createCellsManual(
            long scId,
            CreateTopologyRequest request
    ) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(MANUAL_CELLS_XDOC_URL_POST)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                                .param("scId", Long.toString(scId))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualCellController#getCellForZone}
     */
    @SneakyThrows
    public SneakyResultActions getCellsForZone(
            long scId,
            long zoneId,
            Set<CellSubType> subtypes
    ) {
        String filter = "?scId=" + scId + "&zoneId=" + zoneId;
        if (subtypes != null) {
            filter = filter + "&subtypes=" + subtypes.stream().map(Enum::name).collect(Collectors.joining(","));
        }
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MANUAL_GET_CELLS_FOR_ZONE_URL_GET + filter)
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#performAction}
     */
    @SneakyThrows
    public SneakyResultActions performAction(String externalIdOrInfoListCode, InboundAvailableAction inboundAction) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_PERFORM_ACTION_URL_PUT,
                                        scPartnerId, encoded(externalIdOrInfoListCode), inboundAction.name()
                                ))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#inboundCarArrived}
     */
    @SneakyThrows
    public SneakyResultActions inboundCarArrived(String externalIdOrInfoListCode, PutCarInfoRequest request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_CAR_ARRIVED_PUT_URL,
                                        scPartnerId, externalIdOrInfoListCode
                                ))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#inboundsCarArrived}
     */
    @SneakyThrows
    public SneakyResultActions inboundsCarArrived(GroupPutCarInfoRequest request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_GROUP_CAR_ARRIVED_PUT_URL,
                                        scPartnerId
                                ))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#putCarInfo}
     */
    @SneakyThrows
    public SneakyResultActions putCarInfo(String externalIdOrInfoListCode, PutCarInfoRequest request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_CAR_INFO_PUT_URL,
                                        scPartnerId, externalIdOrInfoListCode
                                ))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#putDocInfo}
     */
    @SneakyThrows
    public SneakyResultActions putDocInfo(String externalId, PutDocumentInfoRequest request) {
        return putDocInfo(externalId, objectMapper.writeValueAsString(request));
    }

    @SneakyThrows
    public SneakyResultActions putDocInfo(String externalId, String request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_DOC_INFO_PUT_URL,
                                        scPartnerId, externalId
                                ))
                                .content(request)
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#getInboundInfo}
     */
    @SneakyThrows
    public SneakyResultActions getInboundInfo(String externalIdOrInfoListCode) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_INBOUND_INFO_GET_URL,
                                        scPartnerId, externalIdOrInfoListCode
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualCellController#clearCell}
     */
    @SneakyThrows
    public SneakyResultActions clearCell(long cellId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MANUAL_CLEAR_CELL_URL_POST)
                                .param("id", String.valueOf(cellId))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualCellController#deleteCell}
     */
    @SneakyThrows
    public SneakyResultActions deleteCell(long cellId, long scId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .delete(MANUAL_DELETE_CELL_URL_DELETE)
                                .param("cellId", String.valueOf(cellId))
                                .param("scId", String.valueOf(scId))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#getStatuses}
     */
    @SneakyThrows
    public SneakyResultActions getStatuses() {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_STATUSES_URL_GET, scPartnerId
                                ))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#getStatusesWrapper}
     */
    @SneakyThrows
    public SneakyResultActions getStatusesWrapper() {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_STATUSES_V2_URL_GET, scPartnerId
                                ))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.partner.PartnerInboundController#getAllowedTypesWrapper}
     */
    @SneakyThrows
    public SneakyResultActions getAllowedTypesWrapper() {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(
                                        INTERNAL_PARTNER_INBOUNDS_TYPES_V2_URL_GET, scPartnerId
                                ))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualSortableController#unlinkSortables}
     */
    @SneakyThrows
    public SneakyResultActions unlinkSortables(UnlinkSortablesRequest request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .put(MANUAL_UNLINK_SORTABLES)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualMiscController#getStateDiffBetweenTmAndSc}
     */
    @SneakyThrows
    public SneakyResultActions getDiffBetweenTmAndSc(long scId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(MessageFormat.format(MANUAL_GET_DIFFERENCE_STATE_URL_GET, scId))
                                .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.external.HermesUserController#createOrUpdateStockman}
     */
    @SneakyThrows
    public SneakyResultActions createOrUpdateStockman(HermesUserRequestDto hermesUserRequestDto) {
        return new SneakyResultActions(
                mockMvc.perform(put(HERMES_USER_PUT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JacksonUtil.toString(hermesUserRequestDto))
                ).andDo(print())
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.external.HermesUserController#deleteStockman}
     */
    @SneakyThrows
    public SneakyResultActions deleteStockman(long userId) {
        return new SneakyResultActions(
                mockMvc.perform(delete(HERMES_USER_DELETE, userId))
                        .andDo(print())
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualSortableController#deleteSortables}
     */
    @SneakyThrows
    public SneakyResultActions deleteSortables(DeleteSortablesRequest request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .delete(AUTOTEST_DELETE_SORTABLE_URL)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualInboundController#createDemo}
     */
    @SneakyThrows
    public SneakyResultActions createInboundDemo(CreateDemoInboundWithRegistryDto inboundWithRegistryDto) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.post(MANUAL_INBOUND_CREATE_DEMO)
                                .content(objectMapper.writeValueAsString(inboundWithRegistryDto))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    @SneakyThrows
    public SneakyResultActions shipAnomalies(long scId, String... barcodes) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(MANUAL_XDOC_SHIP_ANOMALY)
                                .param("scId", String.valueOf(scId))
                                .param("barcodes", barcodes)
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.internal.controller.manual.ManualInboundController#deleteInbound}
     */
    @SneakyThrows
    public SneakyResultActions deleteInbound(DeleteInboundRequest request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .delete(AUTOTEST_DELETE_INBOUND_URL)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.internal.controller.manual.ManualMiscController#pushXDocSortingCenterState}
     */
    @SneakyThrows
    public SneakyResultActions pushXDocSortingCenterState(long scId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(MessageFormat.format(MANUAL_XDOC_PUSH_STATE, scId))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.internal.controller.partner.PartnerSortableController#getSortableReport}
     */
    @SneakyThrows
    public SneakyResultActions getSortableReport(String partnerId, String queryParam) {
        queryParam = Optional.ofNullable(queryParam)
                .map(qp -> "?" + qp)
                .orElse("");
        String urlTemplate = MessageFormat.format(
                SortableUriConst.INTERNAL_PARTNER_GET_SORTABLES_GET_URL, partnerId, queryParam);
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(urlTemplate)
                                .param("sortingCenterId", String.valueOf(partnerId))
                )
        );
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.internal.controller.partner.PartnerSortableController#getSortableReport}
     */
    @SneakyThrows
    public SneakyResultActions getSortableReport(String partnerId, String queryParam, Pageable pageable) {
        queryParam = Optional.ofNullable(queryParam)
                .map(qp -> "?" + qp)
                .orElse("");

        String urlTemplate = MessageFormat.format(
                SortableUriConst.INTERNAL_PARTNER_GET_SORTABLES_GET_URL, partnerId, queryParam);
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(urlTemplate)
                                .param("sortingCenterId", String.valueOf(partnerId))
                                .param("page", String.valueOf(pageable.getPageNumber()))
                                .param("size", String.valueOf(pageable.getPageSize()))
                )
        );
    }

    private static String encoded(String source) {
        return URLEncoder.encode(source, StandardCharsets.UTF_8);
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.internal.controller.partner.PartnerSortableController#getSortableTypes}
     */
    @SneakyThrows
    public SneakyResultActions getSortableTypes(long scId) {
        String urlTemplate = MessageFormat.format(
                SortableUriConst.INTERNAL_PARTNER_GET_SORTABLES_TYPES_GET_URL, scId);
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(urlTemplate)
                                .param("sortingCenterId", String.valueOf(scId))
                )
        );
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.internal.controller.partner.PartnerSortableController#getSortableStatuses}
     */
    @SneakyThrows
    public SneakyResultActions getSortableStatuses(long scId) {
        String urlTemplate = MessageFormat.format(
                SortableUriConst.INTERNAL_PARTNER_GET_SORTABLES_STATUSES_GET_URL, scId);
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(urlTemplate)
                                .param("sortingCenterId", String.valueOf(scId))
                )
        );
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.internal.controller.partner.PartnerSortableController#getSortableStageStatistic}
     */
    @SneakyThrows
    public SneakyResultActions getSortableStageStatistic(String partnerId) {
        String urlTemplate = MessageFormat.format(
                SortableUriConst.INTERNAL_PARTNER_GET_SORTABLES_STAGE_STATISTIC_GET_URL, partnerId);
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(urlTemplate)
                                .param("sortingCenterId", String.valueOf(partnerId))
                )
        );
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.internal.controller.manual.ManualSortableController#sendCargoEvent}
     */
    @SneakyThrows
    public SneakyResultActions manualSendCargoEvent(ResendSortableInfoToSqsRequest request, long scId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders
                                .post(MessageFormat.format(MANUAL_SORTABLE_SEND_CARGO_EVENT, scId))
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

}
