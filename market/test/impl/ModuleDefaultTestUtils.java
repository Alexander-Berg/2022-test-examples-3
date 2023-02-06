package ru.yandex.market.jmf.module.def.test.impl;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.def.Contact;

@Component
public class ModuleDefaultTestUtils {

    @Inject
    BcpService bcpService;

    public Contact createContact(Entity parent, List<String> emails) {
        return bcpService.create(Contact.FQN, Map.of(
                Contact.PARENT, parent,
                Contact.EMAILS, emails,
                Contact.TITLE, Randoms.string()
        ));
    }
}
