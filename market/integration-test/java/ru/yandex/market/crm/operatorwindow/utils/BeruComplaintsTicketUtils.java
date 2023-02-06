package ru.yandex.market.crm.operatorwindow.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.exceptions.ValidationException;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.module.ticket.Brand;
import ru.yandex.market.jmf.module.ticket.DistributionAlgorithm;
import ru.yandex.market.jmf.module.ticket.Service;
import ru.yandex.market.jmf.module.ticket.Team;
import ru.yandex.market.jmf.timings.ServiceTime;
import ru.yandex.market.jmf.timings.ServiceTimeDayOfWeekPeriodEntity;
import ru.yandex.market.jmf.utils.Maps;
import ru.yandex.market.ocrm.module.complaints.BeruComplaintsTicket;
import ru.yandex.misc.lang.StringUtils;

@Component
public class BeruComplaintsTicketUtils extends ChatsTestUtils {

    public BeruComplaintsTicket createTicket(@Nullable Service service) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(BeruComplaintsTicket.TITLE, Randoms.string());
        if (service != null) {
            attributes.put(BeruComplaintsTicket.SERVICE, service);
            attributes.put(BeruComplaintsTicket.RESPONSIBLE_TEAM, service.getResponsibleTeam());
        }
        attributes.put(BeruComplaintsTicket.CLIENT_EMAIL, Randoms.email());

        return bcpService.create(BeruComplaintsTicket.FQN, attributes);
    }

    @Override
    public Service createService(String serviceCode, Team team, String brandCode) {
        try {
            return txService.doInNewTx(() -> {
                Service service = dbService.getByNaturalId(Service.FQN, Service.CODE, serviceCode);
                if (null != service) {
                    bcpService.edit(service, Map.of(Service.SERVICE_TIME, createServiceTime24x7()));
                    return service;
                }

                ServiceTime serviceTime = createServiceTime24x7();
                Entity brand = createBrand(brandCode);
                return createService(
                        team,
                        serviceTime,
                        brand,
                        Optional.ofNullable(serviceCode),
                        Collections.emptyMap()
                );
            });
        } catch (ValidationException e) {
            // service already exists, ignore
            throw new RuntimeException(e);
        }
    }

    public Service createService(
            Entity team,
            Entity serviceTime,
            Entity brand,
            Optional<String> serviceCode,
            Map<String, Object> additionalAttrs
    ) {
        return createService(team, serviceTime, brand, serviceCode, Service.FQN_OUTBOUND_TELEPHONY, additionalAttrs);
    }

    @Override
    public Service createService(
            Entity team,
            Entity serviceTime,
            Entity brand,
            Optional<String> serviceCode,
            Fqn serviceFqn,
            Map<String, Object> additionalAttrs
    ) {
        Map<String, Object> attributes = Maps.of(
                Service.TITLE, Randoms.string(),
                Service.CODE, serviceCode.orElse(Randoms.string()),
                Service.RESPONSIBLE_TEAM, team,
                Service.SERVICE_TIME, serviceTime,
                Service.SUPPORT_TIME, serviceTime,
                Service.RESOLUTION_TIME, "PT4H",
                Service.TAKING_TIME, "PT1H",
                Service.DEFAULT_PRIORITY, "50",
                Service.ALERT_TAKING_TIME, "PT1H",
                Service.BRAND, brand,
                Service.EMAIL_SENDER, Randoms.email(),
                Service.SEND_REGISTRATION_NOTIFICATION, true
        );
        attributes.putAll(additionalAttrs);
        return bcpService.create(serviceFqn, attributes);
    }

    public Brand createBrand(String brandCode) {
        Brand existsBrand = entityStorageService.getByNaturalId(Brand.FQN, brandCode);
        return existsBrand != null ? existsBrand : bcpService.create(Fqn.of("brand"), Map.of(
                "title", "Test",
                "code", brandCode,
                "emailSignature", "sign",
                "emailSender", "email@example.com",
                "icon", "url",
                "incomingPhone", "+7912" + StringUtils.leftPad(Randoms.stringNumber().substring(0, 7), 7, '1'),
                "outgoingPhone", "+79121112233"
        ));
    }

    private ServiceTime createServiceTime24x7() {
        ServiceTime st = createServiceTime();

        entityService.setAttribute(st, "periods", Set.of(
                createPeriod(st, "monday", "00:00", "23:59:59"),
                createPeriod(st, "tuesday", "00:00", "23:59:59"),
                createPeriod(st, "wednesday", "00:00", "23:59:59"),
                createPeriod(st, "thursday", "00:00", "23:59:59"),
                createPeriod(st, "friday", "00:00", "23:59:59"),
                createPeriod(st, "saturday", "00:00", "23:59:59"),
                createPeriod(st, "sunday", "00:00", "23:59:59")
        ));
        return st;
    }

    private ServiceTimeDayOfWeekPeriodEntity createPeriod(
            Entity st,
            String dayOfWeek,
            String startTime,
            String endTime
    ) {
        return bcpService.create(ServiceTimeDayOfWeekPeriodEntity.FQN, ImmutableMap.of(
                ServiceTimeDayOfWeekPeriodEntity.SERVICE_TIME, st,
                ServiceTimeDayOfWeekPeriodEntity.DAY_OF_WEEK, dayOfWeek,
                ServiceTimeDayOfWeekPeriodEntity.START_TIME, startTime,
                ServiceTimeDayOfWeekPeriodEntity.END_TIME, endTime
        ));
    }

    private ServiceTime createServiceTime() {
        return bcpService.create(ServiceTime.FQN, ImmutableMap.of(
                ServiceTime.CODE, Randoms.string(),
                ServiceTime.TITLE, Randoms.string()
        ));
    }

    @Override
    public Team createTeamIfNotExists(String teamCode) {
        return (Team) txService.doInNewTx(() ->
                Optional.ofNullable(getTeam(teamCode))
                        .orElseGet(() -> createTeam(teamCode)));
    }

    private Entity getTeam(String teamCode) {
        return dbService.getByNaturalId(Team.FQN, Team.CODE, teamCode);
    }

    @Override
    public Team createTeam(String code) {
        ImmutableMap<String, Object> attributes = ImmutableMap.of(
                Team.TITLE, Randoms.string(),
                Team.CODE, code,
                Team.DISTRIBUTION_ALGORITHM, DistributionAlgorithm.UNIFORM
        );
        return bcpService.create(Team.FQN_DEFAULT, attributes);
    }
}
