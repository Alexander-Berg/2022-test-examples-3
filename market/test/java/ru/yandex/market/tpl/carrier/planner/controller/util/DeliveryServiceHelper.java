package ru.yandex.market.tpl.carrier.planner.controller.util;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company.CompanyRepository;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.partner.DsRepository;
import ru.yandex.market.tpl.carrier.core.domain.partnerka.PartnerkaCommandService;

@AllArgsConstructor
@Service
public class DeliveryServiceHelper {

    private final PartnerkaCommandService partnerkaCommandService;
    private final CompanyRepository companyRepository;
    private final DsRepository dsRepository;

    @Transactional
    public Long createDeliveryService(DsCreateCommand command) {
        return partnerkaCommandService.execute(command, cmd -> {
            DeliveryService ds = new DeliveryService();
            ds.setId(cmd.getDsId());
            ds.setToken(cmd.getToken());
            ds.setName(cmd.getName());
            ds.setUrl(cmd.getUrl());
            dsRepository.save(ds);

            Company company = companyRepository.findByIdOrThrow(cmd.getCompanyId());
            DeliveryService deliveryService = dsRepository.findByIdOrThrow(cmd.getDsId());
            deliveryService.getCompanies().add(company);
            return company.getId();
        });
    }

}
