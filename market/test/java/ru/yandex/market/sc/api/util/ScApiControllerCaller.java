package ru.yandex.market.sc.api.util;

import java.text.MessageFormat;

import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.cell.repository.Cell;
import ru.yandex.market.sc.core.domain.inbound.model.InboundType;
import ru.yandex.market.sc.core.domain.inbound.model.LinkToInboundRequestDto;
import ru.yandex.market.sc.core.domain.lot.model.TransferFromLotToLotRequestDto;
import ru.yandex.market.sc.core.domain.lot.repository.LotSize;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifier;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundIdentifierType;
import ru.yandex.market.sc.core.domain.outbound.model.OutboundInternalStatusDto;
import ru.yandex.market.sc.core.domain.scan.model.AcceptLotRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SaveVGHRequestDto;
import ru.yandex.market.sc.core.domain.scan.model.SortableSortRequestDto;
import ru.yandex.market.sc.core.domain.scan_log.model.ScanLogContext;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableAPIAction;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sortable.repository.Sortable;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.core.util.SneakyResultActions;
import ru.yandex.market.tpl.common.util.JacksonUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Обеспечивает вызов контроллеров sc-api с помощью MockMvc
 */
@RequiredArgsConstructor
public class ScApiControllerCaller {

    private static final String API_INBOUNDS_ACCEPT_URL_GET = "/api/inbounds/{0}/accept{1}";
    private static final String API_INBOUNDS_ACCEPT_URL_GET_V2 = "/api/v2/inbounds/{0}/accept{1}";
    private static final String API_INBOUNDS_LINK_URL_POST = "/api/inbounds/{0}/link";
    private static final String API_INBOUNDS_UNLINK_URL_DELETE = "/api/inbounds/{0}/unlink/{1}";
    private static final String API_INBOUNDS_URL_GET = "/api/inbounds/{0}";
    private static final String API_INBOUNDS_FOR_ACCEPTANCE_URL_GET = "/api/inbounds/{0}/finishAcceptance";
    private static final String API_INBOUNDS_FIX_URL_PUT = "/api/inbounds/{0}/fixInbound";
    private static final String API_ORDERS_URL_GET = "/api/orders";
    private static final String API_ORDERS_SORT_URL_PUT = "/api/orders/{0}";
    private static final String API_LOTS_PRESHIP_URL_PUT = "/api/lots/{0}/preship";
    private static final String API_LOT_INVENTORY_GET_URL = "/api/lots/inventory";
    private static final String API_LOT_SWITCH_SIZE_PUT_URL = "/api/lots/{0}/size";
    private static final String API_LOT_AVAILABLE_SIZES_GET_URL = "/api/lots/availableSizes";
    private static final String API_LOT_TRANSFER_POST_URL = "/api/lots/transfer";
    private static final String API_XDOC_OUTBOUNDS_URL_GET = "/api/xdoc/outbounds";
    private static final String API_XDOC_OUTBOUND_EXTERNAL_ID_URL_GET = "/api/xdoc/outbounds/{0}";
    private static final String API_SORTABLE_BETA_SORT_PUT_URL = "/api/sortable/beta/sort";
    private static final String API_ORDERS_ACCEPT = "/api/orders/accept";
    private static final String API_SAVE_VGH_PUT_URL = "/api/inbounds/{0}/saveVGH";
    private static final String API_FINISH_ACCEPTANCE = "/api/inbounds/{0}/finishAcceptance";
    private static final String API_OUTBOUND_BIND_LOT = "/api/outbounds/bindLot?lotExternalId={0}&routeId={1}";
    private static final String API_LOT_FOR_OUTBOUND = "/api/lots/{0}/forOutbound";
    private static final String API_ROUTE_V2_ROUTES_ID = "/api/v2/routes/{0}";
    private static final String API_CELL_WITH_LOT_INFO = "/api/cells/magistral/{0}?routeId={1}";
    private static final String API_MARK_FILLED_STATUS_PUT = "/api/cells/{0}/markFilledStatus";
    private static final String API_LOT_FOR_MOVING_URL = "/api/lots/forMoving";
    private static final String API_LOT_MOVE_URL = "/api/lots/move";
    private static final String API_ACCEPT_LOT_WITH_PLACES = "/api/lots/accept";

    private final MockMvc mockMvc;
    private final String userUID;
    private final HttpHeaders authHeaderUID;
    private final ObjectMapper objectMapper;

    public ScApiControllerCaller(MockMvc mockMvc, String userUID, HttpHeaders headers) {
        this.mockMvc = mockMvc;
        this.userUID = userUID;
        this.authHeaderUID = headers;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public static HttpHeaders auth(long uid) {
        var headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "OAuth uid-" + uid);
        return headers;
    }

    public static HttpHeaders authWith(long uid, String locale) {
        var headers = auth(uid);
        headers.add("SC-Lang", locale);
        return headers;
    }

    public static HttpHeaders authWith(long uid, ScanLogContext scanLogContext) {
        var headers = auth(uid);
        headers.add("SC-Application-Context", scanLogContext.name());
        return headers;
    }

    /**
     * будет создан {@link ScApiControllerCaller} с использованием дефолтного пользователя
     */
    public static ScApiControllerCaller createCaller(MockMvc mockMvc) {
        return new ScApiControllerCaller(mockMvc, TestFactory.USER_UID, auth(TestFactory.USER_UID_LONG));
    }

    public static ScApiControllerCaller createCaller(MockMvc mockMvc, HttpHeaders headers) {
        return new ScApiControllerCaller(mockMvc, TestFactory.USER_UID, headers);
    }

    /**
     * предоставляет возможность переопределить пользователя
     */
    public static ScApiControllerCaller createCaller(MockMvc mockMvc, long userUID) {
        return new ScApiControllerCaller(mockMvc, Long.toString(userUID), auth(userUID));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#getInboundForAccept}
     */
    @SneakyThrows
    public SneakyResultActions acceptInbound(String externalId, @Nullable InboundType type) {
        var filter = type == null ? "" : "?type=" + type;
        return new SneakyResultActions(
                mockMvc.perform(
                        get(MessageFormat.format(API_INBOUNDS_ACCEPT_URL_GET, externalId, filter))
                                .headers(authHeaderUID)
                ));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OutboundController#bindLotToOutboundAndShipLot}
     */
    @SneakyThrows
    public SneakyResultActions bindLotToOutbound(String outboundExternalId, String lotExternalId, long routeId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        put(MessageFormat.format(API_OUTBOUND_BIND_LOT, lotExternalId, Long.toString(routeId)))
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(
                                        new OutboundIdentifier(OutboundIdentifierType.EXTERNAL_ID, outboundExternalId)))
                ));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.LotController#getByExternalIdForOutbound}
     */
    @SneakyThrows
    public SneakyResultActions getLotForOutbound(String lotExternalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        get(MessageFormat.format(API_LOT_FOR_OUTBOUND, lotExternalId))
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                ));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OutboundController#bindLotToOutboundAndShipLot}
     */
    @SneakyThrows
    public SneakyResultActions bindLotToOutbound(String lotExternalId, long routeId,
                                                 String carNumber) {
        return bindLotToOutbound(lotExternalId, routeId, new OutboundIdentifier(OutboundIdentifierType.VEHICLE_NUM,
                carNumber));
    }

    @SneakyThrows
    public SneakyResultActions bindLotToOutbound(String lotExternalId, long routeId,
                                                 OutboundIdentifier outboundIdentifier) {
        return new SneakyResultActions(
                mockMvc.perform(
                        put(MessageFormat.format(API_OUTBOUND_BIND_LOT, lotExternalId, Long.toString(routeId)))
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JacksonUtil.toString(outboundIdentifier))
                ));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.RouteController#getOutgoingRouteBaseDto}
     */
    @SneakyThrows
    public SneakyResultActions getApiV2RoutesByID(long routeId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        get(MessageFormat.format(API_ROUTE_V2_ROUTES_ID, Long.toString(routeId)))
                                .headers(authHeaderUID)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.CellController#getCellWithLotInfo}
     */
    @SneakyThrows
    public SneakyResultActions getCellWithLotInfo(long cellId, long routeId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        get(MessageFormat.format(API_CELL_WITH_LOT_INFO, Long.toString(cellId), Long.toString(routeId)))
                                .headers(authHeaderUID)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#getInboundForAcceptV2}
     */
    @SneakyThrows
    public SneakyResultActions acceptInboundsV2(String externalId, @Nullable InboundType type) {
        var filter = type == null ? "" : "?type=" + type;
        return new SneakyResultActions(
                mockMvc.perform(
                        get(MessageFormat.format(API_INBOUNDS_ACCEPT_URL_GET_V2, externalId, filter))
                                .headers(authHeaderUID)
                ));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#linkToInbound}
     */
    @SneakyThrows
    public SneakyResultActions linkToInbound(String externalId, LinkToInboundRequestDto linkRequest) {
        return new SneakyResultActions(
                mockMvc.perform(
                        post(MessageFormat.format(API_INBOUNDS_LINK_URL_POST, externalId))
                                .headers(authWith(Long.parseLong(userUID), ScanLogContext.XDOC_FIX_INBOUND))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(linkRequest))
                ));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#unlinkFromInbound}
     */
    @SneakyThrows
    public SneakyResultActions unlinkFromInbound(String inboundExternalId, String barcode) {
        return new SneakyResultActions(
                mockMvc.perform(
                        delete(MessageFormat.format(API_INBOUNDS_UNLINK_URL_DELETE, inboundExternalId, barcode))
                                .headers(authWith(Long.parseLong(userUID), ScanLogContext.XDOC_ACCEPTANCE))
                ));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#getInboundForFix}
     */
    @SneakyThrows
    public SneakyResultActions getInboundForFix(String externalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        get(MessageFormat.format(API_INBOUNDS_URL_GET, externalId))
                                .headers(authWith(Long.parseLong(userUID), ScanLogContext.XDOC_FIX_INBOUND))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#getInboundForFinishAcceptance}
     */
    @SneakyThrows
    public SneakyResultActions getInboundForFinishAcceptance(String externalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        get(MessageFormat.format(API_INBOUNDS_FOR_ACCEPTANCE_URL_GET, externalId))
                                .headers(authWith(Long.parseLong(userUID), ScanLogContext.XDOC_ACCEPTANCE))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#fixInbound}
     */
    @SneakyThrows
    public SneakyResultActions fixInbound(String externalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        put(MessageFormat.format(API_INBOUNDS_FIX_URL_PUT, externalId))
                                .headers(authHeaderUID)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OrderController#getOrder}
     */
    @SneakyThrows
    public SneakyResultActions getOrder(String barcode) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.get(API_ORDERS_URL_GET)
                                .headers(authHeaderUID)
                                .param("externalId", barcode)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.LotController#prepareToShipLot}
     */
    @SneakyThrows
    public SneakyResultActions preship(long sortableId, SortableType type, SortableAPIAction action) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(MessageFormat.format(API_LOTS_PRESHIP_URL_PUT, sortableId))
                                .headers(authHeaderUID)
                                .param("type", type.name())
                                .param("action", action.name())
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.LotController#getLotInfo}
     *
     * @param barcode идентификатор лота
     */
    @SneakyThrows
    public SneakyResultActions getLotInfoXdoc(String barcode) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.get(API_LOT_INVENTORY_GET_URL)
                                .headers(authHeaderUID)
                                .param("barcode", barcode)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.LotController#switchLotSize}
     *
     * @param lotId идентификатор лота
     * @param size  габаритность лота
     */
    @SneakyThrows
    public SneakyResultActions switchLotSize(Long lotId, LotSize size) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(MessageFormat.format(API_LOT_SWITCH_SIZE_PUT_URL, lotId))
                                .param("size", size.name())
                                .headers(authHeaderUID)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.LotController#availableSizes}
     */
    @SneakyThrows
    public SneakyResultActions availableSizes() {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.get(API_LOT_AVAILABLE_SIZES_GET_URL)
                                .headers(authHeaderUID)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.LotController#transfer}
     *
     * @param transferFromLotToLotRequestDto дто с указанием из какого и в какой лот переносить плейсы
     */
    @SneakyThrows
    public SneakyResultActions transferFromLotToLot(TransferFromLotToLotRequestDto transferFromLotToLotRequestDto) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.post(API_LOT_TRANSFER_POST_URL)
                                .headers(authWith(Long.parseLong(userUID), ScanLogContext.TRANSFER_FROM_LOT_TO_LOT))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(transferFromLotToLotRequestDto))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OutboundController#getXDocOutbounds}
     */
    @SneakyThrows
    public SneakyResultActions getXDocOutbounds(@Nullable OutboundInternalStatusDto status) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.get(API_XDOC_OUTBOUNDS_URL_GET)
                                .param("status", status == null ? null : status.name())
                                .headers(authHeaderUID)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.OutboundController#getXDocOutbound}
     */
    @SneakyThrows
    public SneakyResultActions getXDocOutbound(String externalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.get(MessageFormat.format(API_XDOC_OUTBOUND_EXTERNAL_ID_URL_GET,
                                        externalId))
                                .headers(authHeaderUID)
                )
        );
    }

    @SneakyThrows
    public SneakyResultActions accept(String orderExternalId, String placeExternalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(API_ORDERS_ACCEPT)
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(acceptRequest(orderExternalId, placeExternalId))
                )
        );
    }

    public SneakyResultActions sort(String barcode, Cell destination) {
        return sort(sortRequest(barcode, destination.getId().toString()));
    }

    public SneakyResultActions sort(String barcode, Sortable destination) {
        return sort(sortRequest(barcode, destination.getRequiredBarcodeOrThrow()));
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.SortableController#sort}
     */
    @SneakyThrows
    public SneakyResultActions sort(String request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(API_SORTABLE_BETA_SORT_PUT_URL)
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#saveVGH}
     */
    @SneakyThrows
    public SneakyResultActions saveVgh(String barcode, SaveVGHRequestDto request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(MessageFormat.format(API_SAVE_VGH_PUT_URL, barcode))
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
        );
    }

    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.InboundController#saveVGH}
     */
    @SneakyThrows
    public SneakyResultActions finishAcceptance(String externalId) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(MessageFormat.format(API_FINISH_ACCEPTANCE, externalId))
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
        );
    }

    private static String sortRequest(String barcode, String destination) {
        return String.format("""
                {
                "sortableExternalId": "%s",
                "destinationExternalId": "%s"
                }
                """, barcode, destination);
    }

    private static String acceptRequest(String orderExternalId, @Nullable String placeExternalId) {
        if (placeExternalId == null) {
            return String.format("""
                    {
                    "externalId": "%s"
                    }
                    """, orderExternalId);
        } else {
            return String.format("""
                    {
                    "externalId": "%s",
                    "placeExternalId": "%s"
                    }
                    """, orderExternalId, placeExternalId);
        }
    }

    @SneakyThrows
    public SneakyResultActions markFilledStatus(Long cellId, boolean isFull) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(MessageFormat.format(API_MARK_FILLED_STATUS_PUT, cellId))
                                .headers(authHeaderUID)
                                .param("isFull", Boolean.valueOf(isFull).toString())
                ).andDo(print())
        );
    }


    /**
     * Вызывает {@link ru.yandex.market.sc.api.controller.LotController#getForMoving(String, ScanLogContext, User)}
     *
     * @param barcode идентификатор лота
     */
    @SneakyThrows
    public SneakyResultActions getLotForMoving(String barcode) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.get(API_LOT_FOR_MOVING_URL)
                                .headers(authHeaderUID)
                                .param("externalId", barcode)
                )
        );
    }

    public SneakyResultActions moveLot(String barcode, String cellId) {
        return moveLot(sortRequest(barcode, cellId));
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.api.controller.LotController#moveLot(SortableSortRequestDto, ScanLogContext, User)}
     *
     * @param request запрос
     */
    @SneakyThrows
    public SneakyResultActions moveLot(String request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(API_LOT_MOVE_URL)
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
        );
    }

    /**
     * Вызывает
     * {@link ru.yandex.market.sc.api.controller.OrderController#acceptLot(ScanLogContext, AcceptLotRequestDto, User)}
     *
     * @param request запрос
     */
    @SneakyThrows
    public SneakyResultActions acceptLot(String request) {
        return new SneakyResultActions(
                mockMvc.perform(
                        MockMvcRequestBuilders.put(API_ACCEPT_LOT_WITH_PLACES)
                                .headers(authHeaderUID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
        );
    }
}
