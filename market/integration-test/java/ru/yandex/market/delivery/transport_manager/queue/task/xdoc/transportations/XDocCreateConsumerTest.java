package ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.caledaring_service.CalendaringServiceClientConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocRequestStatus;
import ru.yandex.market.delivery.transport_manager.facade.TransportationUpdateFacade;
import ru.yandex.market.delivery.transport_manager.facade.transportation.TransportationFacade;
import ru.yandex.market.delivery.transport_manager.factory.LmsFactory;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.dc.XDocCreateDcConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.dc.XDocCreateDcDto;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.ff.XDocCreateFfConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.ff.XDocCreateFfDto;
import ru.yandex.market.delivery.transport_manager.service.StatusHistoryService;
import ru.yandex.market.delivery.transport_manager.service.TransportationStatusService;
import ru.yandex.market.delivery.transport_manager.service.checker.TransportationChecker;
import ru.yandex.market.delivery.transport_manager.service.checker.dto.EnrichedTransportation;
import ru.yandex.market.delivery.transport_manager.service.external.marketd.MarketIdService;
import ru.yandex.market.delivery.transport_manager.util.BacklogCodes;
import ru.yandex.market.delivery.transport_manager.util.XDocTestingConstants;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// TODO: после рефакторинга этот тест проверяет непонятно что
// Надо часть методов унести в XDocInboundFacadeTest (удалить, если там есть такие же)
// а оставить только те, которые проверяют, что вызываются сначала получение данных из
// внешних систем, а потом метод XDocInboundFacade
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"}
)
@DatabaseSetup(
    value = "/repository/health/dbqueue/empty.xml",
    connection = "dbUnitDatabaseConnectionDbQueue"
)
@DatabaseSetup("/repository/transportation/empty.xml")
class XDocCreateConsumerTest extends AbstractContextualTest {

    @Autowired
    private XDocCreateFfConsumer consumer;

    @Autowired
    private XDocCreateDcConsumer dcConsumer;

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private MarketIdService marketIdService;

    @Autowired
    private CalendaringServiceClientApi csClient;

    @Autowired
    private TransportationChecker checker;

    @Autowired
    private TransportationFacade transportationFacade;

    @Autowired
    private TransportationUpdateFacade transportationUpdateFacade;

    @Autowired
    private TransportationStatusService transportationStatusService;

    @Autowired
    private StatusHistoryService statusHistoryService;

    private ShopRequestDetailsDTO shopRequestDetailsDTO3p;
    private ShopRequestDetailsDTO shopRequestDetailsDTO1p;
    private MarketAccount marketAccount;
    private MarketAccount xDocMarketAccount;
    private LogisticsPointResponse xDocLogisticsPoint;

    @BeforeEach
    void setUp() {
        clock.setFixed(
            LocalDateTime.of(2021, 5, 1, 20, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        shopRequestDetailsDTO1p = createShopRequestDto(
            XDocTestingConstants.EXTERNAL_REQUEST_ID,
            SupplierType.FIRST_PARTY, "ЯНДЕКС МАРКЕТ"
        );
        shopRequestDetailsDTO3p = createShopRequestDto(
            null,
            SupplierType.THIRD_PARTY,
            "Сторонний поставщик"
        );

        marketAccount = MarketAccount.newBuilder()
            .setMarketId(XDocTestingConstants.SUPPLIER_MARKET_ID)
            .setLegalInfo(
                LegalInfo.newBuilder()
                    .setInn("inn")
                    .setRegistrationNumber("ogrn")
                    .setLegalName("Name")
                    .setType("OOO")
                    .setLegalAddress("Address")
                    .build()
            )
            .build();

        xDocMarketAccount = MarketAccount.newBuilder()
            .setMarketId(XDocTestingConstants.X_DOC_MARKET_ID)
            .setLegalInfo(
                LegalInfo.newBuilder()
                    .setInn("xdoc_inn")
                    .setRegistrationNumber("xdoc_ogrn")
                    .setLegalName("xdoc_Name")
                    .setType("OOO")
                    .setLegalAddress("xdoc_Address")
                    .build()
            )
            .build();
        Address xDocAddress = Address.newBuilder()
            .settlement("s")
            .region("r")
            .build();
        Set<ScheduleDayResponse> xDocSchedule = Set.of(
            new ScheduleDayResponse(1L, 1, LocalTime.of(10, 0), LocalTime.of(20, 0))
        );
        xDocLogisticsPoint = LogisticsPointResponse.newBuilder()
            .id(XDocTestingConstants.X_DOC_WAREHOUSE_ID)
            .partnerId(XDocTestingConstants.X_DOC_PARTNER_ID)
            .address(xDocAddress)
            .schedule(xDocSchedule)
            .build();
    }

    private ShopRequestDetailsDTO createShopRequestDto(
        String externalRequestId,
        SupplierType supplierType,
        String supplierName
    ) {
        ShopRequestDetailsDTO shopRequestDetailsDTO = new ShopRequestDetailsDTO();
        shopRequestDetailsDTO.setType(RequestType.X_DOC_PARTNER_SUPPLY_TO_FF.getId());
        shopRequestDetailsDTO.setExternalRequestId(externalRequestId);
        shopRequestDetailsDTO.setServiceRequestId(XDocTestingConstants.SERVICE_REQUEST_ID);
        shopRequestDetailsDTO.setXDocServiceId(XDocTestingConstants.X_DOC_PARTNER_ID);
        shopRequestDetailsDTO.setXDocRequestedDate(XDocTestingConstants.X_DOC_REQUESTED_DATE);
        shopRequestDetailsDTO.setRequestedDate(XDocTestingConstants.REQUESTED_DATE);
        shopRequestDetailsDTO.setServiceId(XDocTestingConstants.TARGET_PARTNER_ID);
        shopRequestDetailsDTO.setShopId(XDocTestingConstants.SUPPLIER_ID);
        shopRequestDetailsDTO.setShopOrganizationName(supplierName);
        shopRequestDetailsDTO.setSupplierType(supplierType);
        return shopRequestDetailsDTO;
    }

    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_3p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_tag_3p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_dbqueue_single_transportation_3p.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testExecute3p() {
        createTransportation(
            () -> consumer.execute(
                Task.<XDocCreateFfDto>builder(new QueueShardId(""))
                    .withPayload(new XDocCreateFfDto(
                        XDocTestingConstants.REQUEST_ID,
                        XDocRequestStatus.VALIDATED
                    ))
                    .build()
            ),
            shopRequestDetailsDTO3p
        );
    }

    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_dbqueue_single_transportation_1p.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testExecute1p() {
        createTransportation(
            () -> consumer.execute(
                Task.<XDocCreateFfDto>builder(new QueueShardId(""))
                    .withPayload(new XDocCreateFfDto(
                        XDocTestingConstants.REQUEST_ID,
                        XDocRequestStatus.WAITING_FOR_CONFIRMATION
                    ))
                    .build()
            ),
            shopRequestDetailsDTO1p
        );
    }

    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_to_ff_transportation_1p.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_dbqueue_single_transportation_1p_with_submit_date_validated.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testExecute1pWIthSubmitInboundDate() {
        createTransportation(
            () -> consumer.execute(
                Task.<XDocCreateFfDto>builder(new QueueShardId(""))
                    .withPayload(new XDocCreateFfDto(
                        XDocTestingConstants.REQUEST_ID,
                        XDocRequestStatus.VALIDATED
                    ))
                    .build()
            ),
            shopRequestDetailsDTO1p
        );
    }

    @DatabaseSetup("/repository/transportation/transport_for_x_doc.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/xdoc_enriched.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void testExecuteWithEnrichment3p() {
        createTransportation(
            () -> dcConsumer.execute(
                Task.<XDocCreateDcDto>builder(new QueueShardId(""))
                    .withPayload(new XDocCreateDcDto(
                            XDocTestingConstants.REQUEST_ID,
                            XDocRequestStatus.ACCEPTED_BY_SERVICE,
                            null
                        )
                    )
                    .build()
            ),
            shopRequestDetailsDTO3p
        );

        when(lmsClient.getLogisticsPoint(XDocTestingConstants.X_DOC_WAREHOUSE_ID))
            .thenReturn(Optional.of(xDocLogisticsPoint));
        when(marketIdService.findAccountById(XDocTestingConstants.X_DOC_MARKET_ID))
            .thenReturn(Optional.of(xDocMarketAccount));

        transportationStatusService.setTransportationStatus(2L, TransportationStatus.CHECK_PREPARED);
        EnrichedTransportation enrichedTransportation = checker.check(transportationFacade.getById(2L));
        transportationUpdateFacade.updateExistingTransportationAndRecheck(
            enrichedTransportation,
            TransportationStatus.CHECK_PREPARED,
            BacklogCodes.TRANSPORTATION_CHECKER
        );
    }

    @NotNull
    private LogisticsPointResponse createTransportation(
        Supplier<TaskExecutionResult> supplier,
        ShopRequestDetailsDTO shopRequestDetailsDTO
    ) {
        when(ffwfClient.getRequest(eq(XDocTestingConstants.REQUEST_ID))).thenReturn(shopRequestDetailsDTO);
        when(lmsClient.getLogisticsPoints(eq(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(XDocTestingConstants.X_DOC_PARTNER_ID))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        ))).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(XDocTestingConstants.X_DOC_WAREHOUSE_ID)
                .handlingTime(XDocTestingConstants.X_DOC_HANDLING_TIME)
                .build()
        ));
        when(lmsClient.getLogisticsPoints(eq(
            LogisticsPointFilter.newBuilder()
                .partnerIds(Set.of(XDocTestingConstants.TARGET_PARTNER_ID))
                .type(PointType.WAREHOUSE)
                .active(true)
                .build()
        ))).thenReturn(List.of(
            LogisticsPointResponse.newBuilder().id(XDocTestingConstants.TARGET_WAREHOUSE_ID).build()
        ));

        when(marketIdService.findAccountByMbiSupplierId(XDocTestingConstants.SUPPLIER_ID))
            .thenReturn(Optional.of(marketAccount));
        when(marketIdService.findAccountById(XDocTestingConstants.SUPPLIER_MARKET_ID))
            .thenReturn(Optional.of(marketAccount));

        when(lmsClient.getPartner(XDocTestingConstants.X_DOC_PARTNER_ID)).thenReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(XDocTestingConstants.X_DOC_PARTNER_ID)
                .marketId(XDocTestingConstants.X_DOC_MARKET_ID)
                .build()
        ));

        when(lmsClient.searchPartnerApiSettingsMethods(Mockito.any()))
            .thenAnswer((Answer<List<SettingsMethodDto>>) invocation -> {
                SettingsMethodFilter filter = invocation.getArgument(0);
                List<SettingsMethodDto> result = new ArrayList<>();
                if (filter.getPartnerIds().contains(147L)) {
                    result.add(LmsFactory.settingsMethodDto(1L, 147L, "putInbound", true));
                }
                return result;
            });

        when(csClient.getSlotByExternalIdentifiersV2(
            eq(Set.of(Objects.toString(XDocTestingConstants.REQUEST_ID))),
            eq("FFWF"),
            eq(BookingStatus.ACTIVE)
        )).thenReturn(new BookingListResponseV2(List.of(
            new BookingResponseV2(
                1L,
                CalendaringServiceClientConfig.SOURCE,
                Objects.toString(XDocTestingConstants.REQUEST_ID),
                null,
                1L,
                XDocTestingConstants.X_DOC_REQUESTED_DATE.atZone(ZoneId.systemDefault()),
                XDocTestingConstants.X_DOC_REQUESTED_DATE.atZone(ZoneId.systemDefault()).plusMinutes(
                    XDocTestingConstants.SLOT_SIZE_MINUTES),
                BookingStatus.ACTIVE,
                clock.instant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                100L
            )
        )));

        supplier.get();
        return xDocLogisticsPoint;
    }
}
