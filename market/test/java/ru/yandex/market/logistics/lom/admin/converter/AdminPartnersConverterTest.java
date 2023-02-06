package ru.yandex.market.logistics.lom.admin.converter;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;

import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsFfWaybillSegments;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createStorageUnit;

@DisplayName("AdminPartnersConverter")
class AdminPartnersConverterTest extends AbstractTest {
    private AdminPartnersConverter converter;
    private StorageUnit rootUnit;
    private StorageUnit place;

    @BeforeEach
    void setup() {
        converter = new AdminPartnersConverter(new AdminReferenceObjectConverter());
        place = createStorageUnit(StorageUnitType.PLACE, 2);
        rootUnit = createStorageUnit(StorageUnitType.ROOT, 1).setChildren(Set.of(place));
    }

    @Test
    @DisplayName("Конвертация вейбила в список партнеров")
    void convertPartners() {
        var waybill = createDsFfWaybillSegments(rootUnit).stream()
            .map(ws -> ws
                .setPartnerInfo(
                    new WaybillSegment.PartnerInfo()
                        .setReadableName("Имя " + ws.getPartnerId())
                ))
            .collect(Collectors.toList());
        var result = converter.computePartners(waybill);
        softly.assertThat(result)
            .contains(
                new ReferenceObject()
                    .setDisplayName("2 : Имя 2")
                    .setSlug("lms/partner")
                    .setId("2")
            )
            .contains(
                new ReferenceObject()
                    .setDisplayName("20 : Имя 20")
                    .setSlug("lms/partner")
                    .setId("20")
            );
    }

    @Test
    @DisplayName("Отсутствует readableName в partnerInfo")
    void convertPartnersNoReadableName() {
        var waybill = createDsFfWaybillSegments(rootUnit).stream()
            .map(ws -> ws
                .setPartnerInfo(
                    new WaybillSegment.PartnerInfo()
                        .setReadableName(null)
                ))
            .collect(Collectors.toList());
        var result = converter.computePartners(waybill);
        softly.assertThat(result)
            .contains(
                new ReferenceObject()
                    .setDisplayName("2")
                    .setSlug("lms/partner")
                    .setId("2")
            )
            .contains(
                new ReferenceObject()
                    .setDisplayName("20")
                    .setSlug("lms/partner")
                    .setId("20")
            );
    }

    @Test
    @DisplayName("Отсутствует partnerInfo")
    void convertPartnersNoPartnerInfo() {
        var waybill = createDsFfWaybillSegments(rootUnit).stream()
            .map(ws -> ws.setPartnerInfo(null))
            .collect(Collectors.toList());
        var result = converter.computePartners(waybill);
        softly.assertThat(result)
            .contains(
                new ReferenceObject()
                    .setDisplayName("2")
                    .setSlug("lms/partner")
                    .setId("2")
            )
            .contains(
                new ReferenceObject()
                    .setDisplayName("20")
                    .setSlug("lms/partner")
                    .setId("20")
            );
    }
}
