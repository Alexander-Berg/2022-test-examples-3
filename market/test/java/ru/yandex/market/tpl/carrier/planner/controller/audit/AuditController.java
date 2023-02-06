package ru.yandex.market.tpl.carrier.planner.controller.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;

@RequiredArgsConstructor
@Slf4j

@RestController
public class AuditController {

    @PostMapping("/internal/audit/login")
    public String auditLogin() {
        return "login: " + CarrierAuditTracer.getLogin();
    }

    @GetMapping("/internal/audit/source")
    public String auditSource() {
        return "source: " + CarrierAuditTracer.getSource();
    }

    @PostMapping("/manual/audit/login")
    public String auditLoginManual() {
        return "login: " + CarrierAuditTracer.getLogin();
    }

    @GetMapping("/manual/audit/source")
    public String auditSourceManual() {
        return "source: " + CarrierAuditTracer.getSource();
    }

    @PostMapping("/delivery/query-gateway/audit/login")
    public String dsApiLoginManual() {
        return "login: " + CarrierAuditTracer.getLogin();
    }

    @GetMapping("/delivery/query-gateway/audit/source")
    public String dsApiSourceManual() {
        return "source: " + CarrierAuditTracer.getSource();
    }

}
