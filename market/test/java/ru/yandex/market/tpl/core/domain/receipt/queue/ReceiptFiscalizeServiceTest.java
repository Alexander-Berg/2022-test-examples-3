package ru.yandex.market.tpl.core.domain.receipt.queue;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.receipt.ReceiptDataType;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.FullName;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveRequestItem;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveRequest;
import ru.yandex.market.tpl.common.personal.client.model.PersonalMultiTypeRetrieveResponse;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptData;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptHelper;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptProcessorType;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptServiceClient;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptServiceClientRepository;
import ru.yandex.market.tpl.core.external.lifepay.LifePayClient;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_EMAIL_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_FIO_PERSONAL_ID;
import static ru.yandex.market.tpl.core.domain.order.OrderGenerateService.DEFAULT_PHONE_PERSONAL_ID;

/**
 * @author valter
 */
class ReceiptFiscalizeServiceTest extends TplAbstractTest {

    @Autowired
    ReceiptHelper receiptHelper;
    @Autowired
    ReceiptServiceClientRepository receiptServiceClientRepository;

    @Autowired
    ReceiptFiscalizeService receiptFiscalizeService;
    @Autowired
    LifePayClient lifePayClient;

    ReceiptData receiptData;

    @Autowired
    DefaultPersonalRetrieveApi personalRetrieveApi;

    @Autowired
    ConfigurationServiceAdapter configurationServiceAdapter;

    @BeforeEach
    void init() {
        var receiptServiceClient = receiptServiceClientRepository.findById("ReceiptHelper-test-client").orElseGet(
                () -> receiptServiceClientRepository.save(new ReceiptServiceClient(
                        "ReceiptHelper-test-client", "1234567890", ReceiptProcessorType.LIFE_PAY, "21312312"
                )));

        this.receiptData = receiptHelper.createReceiptData(ReceiptDataType.INCOME, receiptServiceClient);
        doReturn("123").when(lifePayClient).createReceipt(any());
        PersonalMultiTypeRetrieveRequest request = new PersonalMultiTypeRetrieveRequest().items(
                List.of(new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.EMAIL).id(DEFAULT_EMAIL_PERSONAL_ID),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.PHONE).id(DEFAULT_PHONE_PERSONAL_ID),
                        new MultiTypeRetrieveRequestItem().type(CommonTypeEnum.FULL_NAME).id(DEFAULT_FIO_PERSONAL_ID))
        );
        PersonalMultiTypeRetrieveResponse response = new PersonalMultiTypeRetrieveResponse().items(
                List.of(new MultiTypeRetrieveResponseItem().value(new CommonType().email("test@mail.ru"))
                                .type(CommonTypeEnum.EMAIL).id(DEFAULT_EMAIL_PERSONAL_ID),
                        new MultiTypeRetrieveResponseItem().value(new CommonType().fullName(
                                        new FullName().forename("Андрей").surname("Андреев"))).type(CommonTypeEnum.FULL_NAME)
                                .id(DEFAULT_FIO_PERSONAL_ID),
                        new MultiTypeRetrieveResponseItem().value(new CommonType().phone("092835"))
                                .type(CommonTypeEnum.PHONE).id(DEFAULT_PHONE_PERSONAL_ID))
        );
        doReturn(response).when(personalRetrieveApi).v1MultiTypesRetrievePost(request);
    }

    @Test
    void processPayload() {
        receiptFiscalizeService.processPayload(new ReceiptFiscalizePayload(receiptData.getId(), true));
        verify(lifePayClient).createReceipt(argThat(
                r -> r.getExtId().startsWith(String.valueOf(receiptData.getId()))
                        && !Objects.equals(r.getExtId(), String.valueOf(receiptData.getId()))
        ));
        verify(lifePayClient).createReceipt(argThat(
                r -> r.getCustomerName().equals("Иван Иваныч") && r.getCustomerPhone().equals("+7-555-ivaniva")
                        && r.getCustomerEmail().equals("ivan@ivan.iva")
        ));
    }

    @Test
    void processPayloadWithPersonal() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.LIFE_PAY_MAP_FROM_PERSONAL_ENABLED, true);
        receiptFiscalizeService.processPayload(new ReceiptFiscalizePayload(receiptData.getId(), true));
        verify(lifePayClient).createReceipt(argThat(
                r -> r.getExtId().startsWith(String.valueOf(receiptData.getId()))
                        && !Objects.equals(r.getExtId(), String.valueOf(receiptData.getId()))
        ));
        verify(lifePayClient).createReceipt(argThat(
                r -> {
                    System.out.println(r.getCustomerName());
                    return r.getCustomerName().equals("Андреев Андрей") && r.getCustomerPhone().equals("092835")
                            && r.getCustomerEmail().equals("test@mail.ru");
                }
        ));
    }

}
