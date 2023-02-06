package ru.yandex.market.logistics.iris.controller.testing;


import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import io.swagger.annotations.ApiParam;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.logistics.iris.client.http.IrisHttpMethod;
import ru.yandex.market.logistics.iris.client.model.response.MeasurementAuditResponse;
import ru.yandex.market.logistics.iris.converter.MeasurementAuditConverter;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.repository.audit.MeasurementAuditRepository;

@Profile({"testing"})
@RestController
@RequestMapping(path = IrisHttpMethod.MEASUREMENT_AUDIT_PREFIX)
public class MeasurementAuditController {

    private final MeasurementAuditRepository repository;

    public MeasurementAuditController(
            MeasurementAuditRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Transactional
    public List<MeasurementAuditResponse> getAuditForPartnerSku(
            @ApiParam(value = "Идентификатор магазина") @RequestParam("partner_id") String partnerId,
            @ApiParam(value = "Идентификатор товара в системе магазина") @RequestParam("partner_sku") String partnerSku
    ) {
        return repository.findByIdentifier(new EmbeddableItemIdentifier(partnerId, partnerSku)).stream()
                .map(MeasurementAuditConverter::convert).collect(Collectors.toList());
    }

}
