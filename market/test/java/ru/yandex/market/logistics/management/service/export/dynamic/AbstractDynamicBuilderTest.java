package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.error.AssertionErrorCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.domain.entity.DynamicFault;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.service.calendar.CalendarService;
import ru.yandex.market.logistics.management.service.client.LogisticsPointService;
import ru.yandex.market.logistics.management.service.client.PartnerService;
import ru.yandex.market.logistics.management.service.client.PlatformClientService;
import ru.yandex.market.logistics.management.service.client.ScheduleDayService;
import ru.yandex.market.logistics.management.service.export.FileContent;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.DeliveryDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.WarehouseDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.repository.JdbcDeliveryDistributorParamsRepository;
import ru.yandex.market.logistics.management.service.export.dynamic.source.repository.JdbcDeliveryRepository;
import ru.yandex.market.logistics.management.service.export.dynamic.source.repository.JdbcPartnerPlatformClientRepository;
import ru.yandex.market.logistics.management.service.export.dynamic.source.repository.JdbcPartnerRelationRepository;
import ru.yandex.market.logistics.management.service.export.dynamic.source.repository.JdbcWarehouseRepository;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.DynamicLogService;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.PartnerRelationDynamicValidationService;
import ru.yandex.market.logistics.management.util.DynamicBuilderJUnitSoftAssertions;
import ru.yandex.market.logistics.management.util.UnitTestUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractDynamicBuilderTest {

    static final String PATH_PREFIX = "data/mds/";
    static final String DAYS_SET_POSTFIX = "__days_set.json";

    @RegisterExtension
    final DynamicBuilderJUnitSoftAssertions softly = new DynamicBuilderJUnitSoftAssertions(new AssertionErrorCreator());

    @Mock
    protected PartnerRelationDynamicValidationService validationService;

    @Mock
    protected DynamicLogService dynamicLogService;

    protected RegionService regionService = UnitTestUtil.getRegionTree();

    @Mock
    protected PartnerService partnerService;

    @Mock
    protected JdbcDeliveryDistributorParamsRepository deliveryDistributorParamsRepository;

    @Mock
    protected JdbcDeliveryRepository jdbcDeliveryRepository;

    @Mock
    protected JdbcWarehouseRepository jdbcWarehouseRepository;

    @Mock
    protected JdbcPartnerPlatformClientRepository partnerPlatformClientRepository;

    @Mock
    protected JdbcPartnerRelationRepository partnerRelationRepository;

    @Mock
    protected PlatformClientService platformClientService;

    @Mock
    protected LogisticsPointService logisticsPointService;

    @Mock
    protected ScheduleDayService scheduleDayService;

    @Mock
    protected CalendarService calendarService;

    protected CapacityPrepareService capacityPrepareService;

    protected CapacityMergeService capacityMergeService = new CapacityMergeService();

    protected DeliveryCapacityBuilderFactory factory;

    ReportDynamicBuilder builder;

    @Mock
    protected TransactionTemplate transactionTemplate;

    static final Clock CLOCK_MOCK = Clock.fixed(
        ZonedDateTime.of(2018, 10, 4, 12, 0, 0, 0, ZoneId.systemDefault()).toInstant(),
        ZoneId.systemDefault()
    );

    @BeforeEach
    void setUp() {
        capacityPrepareService =
            new CapacityPrepareService(new CapacityTreeProcessorService(new RegionHelper(regionService)));
        factory = new DeliveryCapacityBuilderFactory(regionService, capacityPrepareService, capacityMergeService);
        initBuilder();
        mockServices(createDeliveries(), createFulfillments(), createPlatformClients());
    }

    void initBuilder() {
        builder = new ReportDynamicBuilder(
            validationService,
            partnerService,
            platformClientService,
            deliveryDistributorParamsRepository,
            jdbcDeliveryRepository,
            jdbcWarehouseRepository,
            partnerPlatformClientRepository,
            partnerRelationRepository,
            dynamicLogService,
            CLOCK_MOCK,
            calendarService,
            factory,
            logisticsPointService,
            transactionTemplate
        );
        builder.setDateOffset(0);
    }

    void mockServices(List<DeliveryDto> deliveries, List<WarehouseDto> fulfillments, Set<PlatformClient> platforms) {
        Mockito.doAnswer((Answer<Pair<List<PartnerRelationDto>, List<DynamicFault>>>) invocation ->
            Pair.of(invocation.getArgument(0), Collections.emptyList())).when(validationService)
            .validate(Mockito.anyList());

        Mockito.doReturn(createPartnerRelations(deliveries, fulfillments)).when(partnerRelationRepository)
            .findAllForDynamic(any(), anySet(), anySet(), any());
        Mockito.doReturn(deliveries).when(jdbcDeliveryRepository)
            .findAll(Mockito.any(), Mockito.any());
        Mockito.doReturn(fulfillments).when(jdbcWarehouseRepository)
            .findAll(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doReturn(platforms).when(platformClientService).findAllForDynamic();
        platforms.forEach(p -> Mockito.doReturn(createPlatformClientPartners(deliveries, fulfillments))
            .when(partnerPlatformClientRepository).getStatusByPartnerIdMap(p));

        Mockito.when(transactionTemplate.execute(any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            return ((TransactionCallback) args[0]).doInTransaction(null);
        });
    }

    Map<Long, PartnerStatus> createPlatformClientPartners(List<DeliveryDto> deliveries,
                                                          List<WarehouseDto> fulfillments) {
        return Stream.of(deliveries, fulfillments)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(PartnerDto::getId, PartnerDto::getStatus));
    }

    static List<DeliveryDto> createDeliveries() {
        return Arrays.asList(
            (DeliveryDto) new DeliveryDto()
                .setName("Delivery1")
                .setTrackingType("tt1")
                .setRating(1)
                .setId(100L)
                .setPartnerType(PartnerType.DELIVERY)
                .setLocationId(101)
                .setStatus(PartnerStatus.ACTIVE),
            (DeliveryDto) new DeliveryDto()
                .setName("Delivery2")
                .setTrackingType("tt2")
                .setRating(2)
                .setId(101L)
                .setPartnerType(PartnerType.DELIVERY)
                .setLocationId(101)
                .setStatus(PartnerStatus.INACTIVE)
        );
    }

    static List<WarehouseDto> createFulfillments() {
        return Arrays.asList(
            (WarehouseDto) new WarehouseDto()
                .setId(200L)
                .setPartnerType(PartnerType.FULFILLMENT)
                .setLocationId(1)
                .setStatus(PartnerStatus.ACTIVE)
                .addActiveWarehouse(new LogisticsPointDto()
                    .setType(PointType.WAREHOUSE)
                    .setLocationId(20479)),
            (WarehouseDto) new WarehouseDto()
                .setId(201L)
                .setPartnerType(PartnerType.DROPSHIP)
                .setLocationId(1)
                .setStatus(PartnerStatus.ACTIVE)
                .addActiveWarehouse(new LogisticsPointDto()
                    .setType(PointType.WAREHOUSE)
                    .setLocationId(20479))
        );
    }

    static List<PartnerRelationDto> createPartnerRelations() {
        return createPartnerRelations(createDeliveries(), createFulfillments());
    }

    static List<PartnerRelationDto> createPartnerRelations(List<DeliveryDto> deliveries,
                                                           List<WarehouseDto> fulfillments) {
        return IntStream.range(0, deliveries.size())
            .mapToObj(i -> {
                int id = i + 1;
                return new PartnerRelationDto()
                    .setId((long) id)
                    .setFromPartner(fulfillments.get(i))
                    .setToPartner(deliveries.get(i))
                    .setHandlingTime(id * 10)
                    .setEnabled(true);
            })
            .collect(Collectors.toList());
    }

    @Nonnull
    static List<PartnerRelationDto> createPartnerRelations(List<WarehouseDto> fulfillments) {
        return IntStream.range(0, fulfillments.size())
            .mapToObj(i -> {
                int id = i + 1;
                return new PartnerRelationDto()
                    .setId((long) id)
                    .setFromPartner(fulfillments.get(i))
                    .setToPartner(fulfillments.get(fulfillments.size() - id))
                    .setHandlingTime(id * 10)
                    .setEnabled(true);
            })
            .collect(Collectors.toList());
    }

    static Set<PlatformClient> createPlatformClients() {
        PlatformClient platformClient1 = new PlatformClient();
        platformClient1.setId(1L);
        platformClient1.setName("Беру");

        PlatformClient platformClient2 = new PlatformClient();
        platformClient2.setId(2L);
        platformClient2.setName("Брингли");

        return new HashSet<>(Arrays.asList(platformClient1, platformClient2));
    }

    Logistics.MetaInfo buildReport() {
        return buildReport(new PlatformClient().setId(1L));
    }

    Logistics.MetaInfo buildReport(PlatformClient client) {
        return builder.buildFilesContent().stream()
            .filter(f -> f.getFileName().endsWith(platformReportPostfix(client)))
            .findFirst()
            .map(FileContent::getContent)
            .orElseThrow(() -> new RuntimeException("Not file content found"));
    }

    private String platformReportPostfix(PlatformClient client) {
        return "_" + client.getId();
    }
}
