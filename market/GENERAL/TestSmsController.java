package ru.yandex.market.crm.operatorwindow.http.controller.api.admin.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.crm.operatorwindow.external.SentSms;
import ru.yandex.market.crm.operatorwindow.http.parser.infrastructure.UseParser;
import ru.yandex.market.crm.operatorwindow.http.parser.parsers.AuthParser;
import ru.yandex.market.crm.operatorwindow.http.security.roles.AdminRole;
import ru.yandex.market.crm.operatorwindow.informing.SmsService;
import ru.yandex.market.jmf.catalog.items.CatalogItemService;
import ru.yandex.market.jmf.security.Auth;
import ru.yandex.market.ocrm.module.sms.SmsSender;

@AdminRole
@RestController
@Transactional
public class TestSmsController {

    private static final Logger LOG = LoggerFactory.getLogger(TestSmsController.class);

    private final SmsService smsService;
    private final CatalogItemService catalogItemService;

    public TestSmsController(SmsService smsService, CatalogItemService catalogItemService) {
        this.smsService = smsService;
        this.catalogItemService = catalogItemService;
    }

    @RequestMapping(value = "/api/sms/send", method = RequestMethod.POST)
    public SentSmsView send(
            @RequestParam(name = "phone") String phone,
            @RequestParam(name = "text") String text,
            @RequestParam(name = "type") String type,
            @UseParser(AuthParser.class) Auth auth
    ) {
        LOG.info("User '{}' sending sms '{}' to '{}'", auth.getProfile().getUid(), text, phone);
        SmsSender sender = catalogItemService.get(SmsSender.FQN, type);
        return smsService.sendSms(phone, text, sender)
                .map(this::getView)
                .orElseThrow(() -> new RuntimeException("Ошибка при отправке смс"));
    }

    private SentSmsView getView(SentSms sentSms) {
        SentSmsView view = new SentSmsView();
        view.setId(sentSms.getId());
        view.setErrorCode(sentSms.getErrorCode());
        view.setErrorMessage(sentSms.getErrorMessage());
        return view;
    }
}
