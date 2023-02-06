package ru.yandex.market.delivery.transport_manager.repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.model.enums.ApiType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerMethodMapper;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;

@DatabaseSetup({
    "/repository/transportation/multiple_transportations_deps.xml",
    "/repository/transportation/multiple_transportations.xml",
    "/repository/transportation/partner_methods.xml"
})
class TransportationPartnerMethodMapperTest extends AbstractContextualTest {
    @Autowired
    private TransportationPartnerMethodMapper mapper;

    @Test
    void testGet() {
        var transportationPartnerMethods = mapper.get(1L);

        softly.assertThat(transportationPartnerMethods)
            .containsExactlyInAnyOrderElementsOf(List.of(
                method(1L, 1L, 1L, PartnerMethod.CREATE_INTAKE, null),
                method(2L, 1L, 1L, PartnerMethod.PUT_MOVEMENT, null),
                method(3L, 1L, 2L, PartnerMethod.CREATE_SELF_EXPORT, null)
            ));

        transportationPartnerMethods = mapper.get(2L);

        softly.assertThat(transportationPartnerMethods)
            .containsExactlyInAnyOrderElementsOf(List.of(
                method(4L, 2L, 123L, PartnerMethod.CREATE_INTAKE, null)
            ));

        transportationPartnerMethods = mapper.get(3L);

        softly.assertThat(transportationPartnerMethods)
            .containsExactlyInAnyOrderElementsOf(List.of(
                    method(5L, 3L, 123L, PartnerMethod.CREATE_INTAKE, ApiType.DELIVERY),
                    method(5L, 3L, 123L, PartnerMethod.CREATE_INTAKE, ApiType.FULFILLMENT)
            ));

        transportationPartnerMethods = mapper.get(4L);

        softly.assertThat(transportationPartnerMethods).isEmpty();
    }

    @Test
    void testSave() {
        Set<TransportationPartnerMethod> data = new HashSet<>();

        data.add(transportationPartnerMethod(2L, 4L, PartnerMethod.PUT_MOVEMENT));
        data.add(transportationPartnerMethod(2L, 123L, PartnerMethod.CREATE_INTAKE));

        mapper.insert(data);

        var result = mapper.get(2L);

        softly.assertThat(result)
            .containsExactlyInAnyOrderElementsOf(List.of(
                method(4L, 2L, 123L, PartnerMethod.CREATE_INTAKE, null),
                method(5L, 2L, 4L, PartnerMethod.PUT_MOVEMENT, null),
                method(6L, 2L, 123L, PartnerMethod.CREATE_INTAKE, null)
            ));

        result.forEach(System.out::println);
    }

    @Test
    void testDelete() {
        mapper.deleteForTransportation(1L);

        var result = mapper.get(1L);
        softly.assertThat(result).isEmpty();

        result = mapper.get(2L);
        softly.assertThat(result).isNotEmpty();
    }

    @Test
    void testGetSupportedMethodsNames() {
        Set<PartnerMethod> res = mapper.getSupportedMethods(1L, 1L);
        softly.assertThat(res).contains(PartnerMethod.CREATE_INTAKE, PartnerMethod.PUT_MOVEMENT);
    }

    @Test
    void testGetAllPartnersMethods() {
        var resForEmptyPartnersIds = mapper.getAllPartnersMethods(1L);
        softly.assertThat(resForEmptyPartnersIds.size()).isEqualTo(3);
    }

    private static TransportationPartnerMethod method(
        Long id,
        Long transportationId,
        Long partnerId,
        PartnerMethod method,
        ApiType apiType
    ) {
        return new TransportationPartnerMethod()
            .setId(id)
            .setTransportationId(transportationId)
            .setPartnerId(partnerId)
            .setMethod(method)
            .setApiType(apiType);
    }

    private static TransportationPartnerMethod transportationPartnerMethod(
        Long transportationId,
        Long partnerId,
        PartnerMethod method
    ) {
        return new TransportationPartnerMethod()
            .setTransportationId(transportationId)
            .setMethod(method)
            .setPartnerId(partnerId);
    }
}
