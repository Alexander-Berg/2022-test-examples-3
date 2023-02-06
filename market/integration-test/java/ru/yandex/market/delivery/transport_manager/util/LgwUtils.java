package ru.yandex.market.delivery.transport_manager.util;

import java.util.Arrays;
import java.util.List;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistic.gateway.common.model.common.CompositeId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialId;
import ru.yandex.market.logistic.gateway.common.model.common.PartialIdType;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCount;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCountType;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.common.request.restricted.PutInboundRestrictedData;
import ru.yandex.market.logistic.gateway.common.model.common.request.restricted.PutOutboundRestrictedData;

@UtilityClass
public class LgwUtils {

    public Partner partner(long id) {
        return new Partner(id);
    }

    public ResourceId id(String yandexId, String partnerId) {
        return ResourceId.builder()
            .setYandexId(yandexId)
            .setPartnerId(partnerId)
            .build();
    }

    public Person person(String name, String surname, String patronymic) {
        return Person.builder(name)
            .setSurname(surname)
            .setPatronymic(patronymic)
            .build();
    }

    public UnitInfo unitInfo(UnitCount count, PartialId... ids) {
        return unitInfo(count, null, ids);
    }

    public UnitInfo unitInfo(UnitCount count, String description, PartialId... ids) {
        return UnitInfo.builder()
            .setCounts(List.of(count))
            .setCompositeId(CompositeId.builder(Arrays.asList(ids)).build())
            .setRelations(List.of())
            .setDescription(description)
            .build();
    }

    public UnitCount count(UnitCountType type, int quantity) {
        return new UnitCount.UnitCountBuilder().setCountType(type).setQuantity(quantity).build();
    }

    public PartialId id(PartialIdType type, String value) {
        return new PartialId(type, value);
    }

    public PutInboundRestrictedData inboundRestrictedData(
        String transportationId,
        String axaptaMovementOrderId,
        Boolean confirmed,
        String supplierName
    ) {
        return PutInboundRestrictedData.builder()
            .setTransportationId(transportationId)
            .setAxaptaMovementRequestId(axaptaMovementOrderId)
            .setConfirmed(confirmed)
            .setSupplierName(supplierName)
            .build();
    }

    public PutOutboundRestrictedData outboundRestrictedData(String transportationId) {
        return PutOutboundRestrictedData.builder()
            .setTransportationId(transportationId)
            .build();
    }
}
