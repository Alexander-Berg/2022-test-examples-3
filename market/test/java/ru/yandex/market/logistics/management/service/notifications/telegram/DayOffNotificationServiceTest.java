package ru.yandex.market.logistics.management.service.notifications.telegram;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.util.region.Region;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTree;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerCapacity;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.queue.producer.PechkinNotificationTaskProducer;
import ru.yandex.market.logistics.management.repository.PartnerCapacityRepository;
import ru.yandex.market.logistics.management.service.client.DayOffNotificationService;
import ru.yandex.market.logistics.management.service.notification.email.YandexSenderClient;
import ru.yandex.market.logistics.management.service.notification.telegram.TelegramMessageBuilder;
import ru.yandex.market.logistics.management.service.notification.telegram.TelegramStatisticsMessageBuilder;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DayOffNotificationServiceTest {

    private static final String DELIVERY_DAYOFF_CHANNEL = "Delivery_capacity";
    private static final String WAREHOUSE_DAYOFF_CHANNEL = "Warehouse_capacity";
    private static final String DROPSHIP_DAYOFF_CHANNEL = "Dropship_capacity";
    private static final String SORTING_CENTER_DAYOFF_CHANNEL = "Sorting_Center_capacity";
    private static final String XDOC_DAYOFF_CHANNEL = "Xdoc_capacity";
    private static final String SUPPLIER_DAYOFF_CHANNEL = "Supplier_capacity";

    private DayOffNotificationService dayOffNotificationService;

    @Mock
    private PechkinHttpClient pechkinHttpClient;
    @Mock
    private YandexSenderClient yandexSenderClient;
    @Mock
    private PartnerCapacityRepository partnerCapacityRepository;
    @Mock
    private RegionService regionService;
    @Mock
    private PechkinNotificationTaskProducer pechkinNotificationTaskProducer;

    @Mock
    private TransactionTemplate transactionTemplate;

    private Clock clock = Clock.fixed(ZonedDateTime.of(2000, 11, 11, 0, 0, 0, 0,
        ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

    private TelegramMessageBuilder telegramMessageBuilder = new TelegramMessageBuilder();
    private TelegramStatisticsMessageBuilder telegramStatisticsMessageBuilder = new TelegramStatisticsMessageBuilder();


    @BeforeEach
    void before() {
        dayOffNotificationService = new DayOffNotificationService(pechkinHttpClient,
            yandexSenderClient,
            partnerCapacityRepository,
            regionService,
            clock,
            telegramMessageBuilder,
            telegramStatisticsMessageBuilder,
            transactionTemplate,
            pechkinNotificationTaskProducer);

        Mockito.when(regionService.getRegionTree())
            .thenReturn(new RegionTree<>(new Region(1, "name", RegionType.CITY, null)));

        Mockito.when(transactionTemplate.execute(any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            return ((TransactionCallback) args[0]).doInTransaction(null);
        });
    }

    @Test
    void sendDeliveryServiceMessageTest() {
        Mockito.when(partnerCapacityRepository.findById(Mockito.anyLong()))
            .thenReturn(partnerCapacity(PartnerType.DELIVERY));
        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2011, 1, 1), true);
        ArgumentCaptor<MessageDto> argument = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinHttpClient).sendMessage(argument.capture());
        Assertions.assertThat(argument.getValue().getMessage()).contains("DeliveryService");
        Assertions.assertThat(argument.getValue().getChannel()).isEqualTo(DELIVERY_DAYOFF_CHANNEL);
    }

    @Test
    void sendFulfillmentMessageTest() {
        Mockito.when(partnerCapacityRepository.findById(Mockito.anyLong()))
            .thenReturn(partnerCapacity(PartnerType.FULFILLMENT));
        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2011, 1, 1), true);
        ArgumentCaptor<MessageDto> argument = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinHttpClient).sendMessage(argument.capture());
        Assertions.assertThat(argument.getValue().getMessage()).contains("Fulfillment");
        Assertions.assertThat(argument.getValue().getChannel()).isEqualTo(WAREHOUSE_DAYOFF_CHANNEL);
    }

    @Test
    void sendDropShipMessageTest() {
        Mockito.when(partnerCapacityRepository.findById(Mockito.anyLong()))
            .thenReturn(partnerCapacity(PartnerType.DROPSHIP));
        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2011, 1, 1), true);
        ArgumentCaptor<MessageDto> argument = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinHttpClient).sendMessage(argument.capture());
        Assertions.assertThat(argument.getValue().getMessage()).contains("Dropship");
        Assertions.assertThat(argument.getValue().getChannel()).isEqualTo(DROPSHIP_DAYOFF_CHANNEL);
    }

    @Test
    void sendDropXDocTest() {
        Mockito.when(partnerCapacityRepository.findById(Mockito.anyLong()))
            .thenReturn(partnerCapacity(PartnerType.XDOC));
        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2011, 1, 1), true);
        ArgumentCaptor<MessageDto> argument = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinHttpClient).sendMessage(argument.capture());
        Assertions.assertThat(argument.getValue().getMessage()).contains("XDoc");
        Assertions.assertThat(argument.getValue().getChannel()).isEqualTo(XDOC_DAYOFF_CHANNEL);
    }

    @Test
    void sendSortingCenterTest() {
        Mockito.when(partnerCapacityRepository.findById(Mockito.anyLong()))
            .thenReturn(partnerCapacity(PartnerType.SORTING_CENTER));
        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2011, 1, 1), true);
        ArgumentCaptor<MessageDto> argument = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinHttpClient).sendMessage(argument.capture());
        Assertions.assertThat(argument.getValue().getMessage()).contains("SortingCenter");
        Assertions.assertThat(argument.getValue().getChannel()).isEqualTo(SORTING_CENTER_DAYOFF_CHANNEL);
    }

    @Test
    void sendSupplierTest() {
        Mockito.when(partnerCapacityRepository.findById(Mockito.anyLong()))
                .thenReturn(partnerCapacity(PartnerType.SUPPLIER));
        dayOffNotificationService.notifyDayOff(1L, LocalDate.of(2011, 1, 1), true);
        ArgumentCaptor<MessageDto> argument = ArgumentCaptor.forClass(MessageDto.class);
        Mockito.verify(pechkinHttpClient).sendMessage(argument.capture());
        Assertions.assertThat(argument.getValue().getMessage()).contains("Supplier");
        Assertions.assertThat(argument.getValue().getChannel()).isEqualTo(SUPPLIER_DAYOFF_CHANNEL);
    }

    private Optional<PartnerCapacity> partnerCapacity(PartnerType partnerType) {
        Partner partner = new Partner();
        partner.setPartnerType(partnerType);
        partner.setId(1L);
        PartnerCapacity partnerCapacity = new PartnerCapacity();
        partnerCapacity.setPartner(partner);
        partnerCapacity.setLocationFrom(1);
        partnerCapacity.setLocationTo(1);
        partnerCapacity.setId(1L);
        partnerCapacity.setValue(500L);
        partnerCapacity.setPlatformClient(
            new PlatformClient().setId(1L).setName("Beru")
        );
        return Optional.of(partnerCapacity);
    }
}
