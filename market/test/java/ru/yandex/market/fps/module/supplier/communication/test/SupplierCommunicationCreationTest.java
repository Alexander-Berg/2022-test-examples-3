package ru.yandex.market.fps.module.supplier.communication.test;

import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.fps.module.supplier.communication.SupplierCommunication;
import ru.yandex.market.fps.module.supplier.communication.SupplierCommunicationEmail;
import ru.yandex.market.fps.module.supplier1p.Supplier1p;
import ru.yandex.market.fps.module.supplier1p.test.impl.SupplierTestUtils;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.module.mail.ContentType;
import ru.yandex.market.jmf.module.mail.SendMailService;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@Transactional
@SpringJUnitConfig(InternalModuleSupplierCommunicationTestConfiguration.class)
public class SupplierCommunicationCreationTest {
    private final BcpService bcpService;
    private final SupplierTestUtils supplierTestUtils;
    private final SendMailService sendMailService;

    public SupplierCommunicationCreationTest(BcpService bcpService, SupplierTestUtils supplierTestUtils,
                                             SendMailService sendMailService) {
        this.bcpService = bcpService;
        this.supplierTestUtils = supplierTestUtils;
        this.sendMailService = sendMailService;
    }

    @Test
    public void testCreateSupplierCommunication() {
        Mockito.verifyNoInteractions(sendMailService);

        var supplier = supplierTestUtils.createSupplier(Map.of(
                Supplier1p.CLIENT_EMAIL, "foo@bar.ru",
                Supplier1p.FIRST_NAME, "Вася",
                Supplier1p.LAST_NAME, "Poupkine"
        ));

        bcpService.create(SupplierCommunicationEmail.FQN, Map.of(
                SupplierCommunicationEmail.SUBJECT, "foo",
                SupplierCommunicationEmail.HTML_BODY, "bar",
                SupplierCommunication.SUPPLIER, supplier
        ));

        Mockito.verify(sendMailService, Mockito.times(1)).send(
                anyString(),
                isNull(),
                eq(List.of("foo@bar.ru")),
                eq("Вася Poupkine"),
                anyList(),
                isNull(),
                eq("foo"),
                eq(ContentType.HTML),
                eq("bar"),
                anyCollection()
        );
    }
}
