package ru.yandex.market.logistics.management.service.point.sync;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Location;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.PickupPoint;
import ru.yandex.market.logistic.gateway.common.model.delivery.PickupPointType;
import ru.yandex.market.logistic.gateway.common.model.delivery.Service;
import ru.yandex.market.logistic.gateway.common.model.delivery.ServiceType;
import ru.yandex.market.logistic.gateway.common.model.delivery.TimeInterval;
import ru.yandex.market.logistic.gateway.common.model.delivery.WorkTime;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.type.MethodType;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.facade.PartnerCountryFacade;
import ru.yandex.market.logistics.management.queue.producer.PickupPointSyncProducer;
import ru.yandex.market.logistics.management.repository.PartnerRepository;
import ru.yandex.market.logistics.management.repository.SettingsMethodSyncRepository;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;
import ru.yandex.market.logistics.management.service.client.PartnerExternalParamService;
import ru.yandex.market.logistics.management.util.TestableClock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_REGION_ID;

@DatabaseSetup(
    value = "/data/executor/before/sync_partner_pickup_points_photo.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
@SuppressWarnings({"checkstyle:MagicNumber"})
class PickupPointsSyncServicePhotoTest extends AbstractContextualTest {

    private static final long PARTNER_ID = 1L;
    private static final String FIRST_PHOTO_CONTENT = "first_photo_content";
    private static final String SECOND_PHOTO_CONTENT = "second_photo_content";
    private static final double LATITUDE = 120;
    private static final double LONGITUDE = 220;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private DeliveryClient deliveryClient;

    @Autowired
    private ImportPartnerPickupPointsService importPartnerPickupPointsService;

    @Autowired
    private LogisticsPointPhotoService logisticsPointPhotoService;

    @Autowired
    private MdsS3BucketClient lmsMdsS3BucketClient;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private TestableClock clock;

    @Autowired
    private PartnerExternalParamService partnerExternalParamService;

    @Autowired
    private SettingsMethodSyncRepository settingsMethodSyncRepository;

    @Autowired
    private PickupPointSyncProducer pickupPointSyncProducer;

    @Autowired
    private PartnerCountryFacade partnerCountryFacade;

    private PickupPointsSyncService pickupPointsSyncService;

    @BeforeEach
    void init() {
        pickupPointsSyncService = new PickupPointsSyncService(
            deliveryClient,
            importPartnerPickupPointsService,
            logisticsPointPhotoService,
            partnerExternalParamService,
            settingsMethodSyncRepository,
            pickupPointSyncProducer,
            clock,
            partnerCountryFacade
        );
        when(httpGeobase.getRegionId(anyDouble(), anyDouble()))
            .thenReturn(MOSCOW_REGION_ID);
    }

    /**
     * Сценарий - успешний импорт логистической точки с двумя фотографиями:
     * <ul>
     * <li>Загрузка логистической точки с двумя различными фотографиями</li>
     * <li>Валидация корректности записей в таблицах s3_file, file_collection, logistics_point</li>
     * </ul>
     */
    @Test
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_points_photo_1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED

    )
    void twoPhotoLogisticPointImportSuccessfullyTest() throws GatewayApiException {
        // По данному партнеру одна точка будет обновлена, одна будет удалена (не пришла от deliveryClient)
        List<String> photos = Arrays.asList(FIRST_PHOTO_CONTENT, SECOND_PHOTO_CONTENT);
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(lgwPartner(PARTNER_ID))))
            .thenReturn(Collections.singletonList(createPickupPointWithPhoto(photos)));
        syncPickupPoints();
    }

    /**
     * Сценарий - обновление фотографии у точки:
     * <ul>
     * <li>Загрузка логистической точки с фотографией FIRST_PHOTO_CONTENT</li>
     * <li>Обновление фотографии в PickupPoint на SECOND_PHOTO_CONTENT</li>
     * <li>Повторная загрузка логистической точки</li>
     * <li>Проверяется, что: у логистической точки не изменился идентификатор коллекции, <br/>
     * старая сущность s3File была удалена, информация о текущей фотографии - актуальная</li>
     * </ul>
     */
    @Test
    @ExpectedDatabase(
        value = "/data/executor/after/sync_partner_pickup_points_photo_2.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void photoDataUpdateTest() throws GatewayApiException {
        List<String> photos = Collections.singletonList(FIRST_PHOTO_CONTENT);
        PickupPoint pickupPointWithPhoto = createPickupPointWithPhoto(photos);
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(lgwPartner(PARTNER_ID))))
            .thenReturn(Collections.singletonList(pickupPointWithPhoto));

        syncPickupPoints();
        // замена фотографии на вторую
        photos = Collections.singletonList(SECOND_PHOTO_CONTENT);
        pickupPointWithPhoto = createPickupPointWithPhoto(photos);
        when(deliveryClient.getReferencePickupPoints(any(), any(), any(), eq(lgwPartner(PARTNER_ID))))
            .thenReturn(Collections.singletonList(pickupPointWithPhoto));

        syncPickupPoints();
        // проверяем, что было вызывано удаление mds-s3
        verify(lmsMdsS3BucketClient).delete("logisticPointPhotos/1/1");
        verify(httpGeobase).getRegionId(LATITUDE, LONGITUDE);
    }

    private void syncPickupPoints() {
        partnerRepository.findPartnersSupportingMethod(
                Arrays.asList(PartnerStatus.ACTIVE, PartnerStatus.TESTING),
                MethodType.GET_REFERENCE_PICKUP_POINTS
            )
            .forEach(
                partnerId -> pickupPointsSyncService.syncPickupPoints(partnerId)
            );
    }

    private static Partner lgwPartner(long id) {
        return new Partner(id);
    }

    private static PickupPoint createPickupPointWithPhoto(List<String> photos) {
        return PickupPoint.builder()
            .setCode("CODE1")
            .setName("POINT 1")
            .setAddress(location1())
            .setPhones(Collections.singletonList(phone1()))
            .setActive(true)
            .setCardAllowed(false)
            .setCashAllowed(false)
            .setPrepayAllowed(false)
            .setReturnAllowed(false)
            .setMaxWeight(new BigDecimal("500"))
            .setMaxLength(BigDecimal.valueOf(330))
            .setMaxWidth(BigDecimal.valueOf(40))
            .setMaxHeight(BigDecimal.valueOf(60))
            .setMaxSidesSum(BigDecimal.valueOf(666))
            .setStoragePeriod(12)
            .setInstruction("instruction 1")
            .setType(PickupPointType.PICKUP_POINT)
            .setSchedule(Collections.singletonList(workTime1()))
            .setServices(Collections.singletonList(service(ServiceType.CHECK)))
            .setPhotos(photos)
            .build();
    }

    private static Location location1() {
        return new Location.LocationBuilder("Россия", "Москва", "Москва и Московская область")
            .setStreet("Ленина")
            .setHouse("12")
            .setHousing("10")
            .setBuilding("34")
            .setRoom("12a")
            .setZipCode("649220")
            .setLat(new BigDecimal(LATITUDE))
            .setLng(new BigDecimal(LONGITUDE))
            .setLocationId(54321)
            .build();
    }

    private static Phone phone1() {
        return new Phone.PhoneBuilder("555777")
            .setAdditional("333555")
            .build();
    }

    private static WorkTime workTime1() {
        return new WorkTime(1, Arrays.asList(
            TimeInterval.of(LocalTime.of(9, 0), LocalTime.of(12, 0)),
            TimeInterval.of(LocalTime.of(13, 0), LocalTime.of(19, 0))
        ));
    }

    private static Service service(ServiceType type) {
        return new Service(type, false, null, null, null, null);
    }
}
