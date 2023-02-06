package ru.yandex.market.tpl.carrier.lms.controller.audit;

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

}
