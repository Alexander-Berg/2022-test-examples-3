package ru.yandex.market.pvz.core.domain.dbqueue.get_recipient_phone_tail_batch;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.pvz.core.domain.order.OrderPersonalQueryService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.personal.client.model.CommonType;
import ru.yandex.market.tpl.common.personal.client.model.CommonTypeEnum;
import ru.yandex.market.tpl.common.personal.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.dbqueue.get_recipient_phone_tail_batch.GetRecipientPhoneTailBatchService.PHONE_TAIL_LENGTH;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderPersonalParams.DEFAULT_RECIPIENT_PHONE_ID;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class GetRecipientPhoneTailBatchServiceTest {

    private final TestOrderFactory orderFactory;

    private final OrderPersonalQueryService orderPersonalQueryService;

    private final GetRecipientPhoneTailBatchService getRecipientPhoneTailBatchService;

    @MockBean
    private PersonalExternalService personalExternalService;


    @Test
    void getRecipientPhoneTail() {
        MultiTypeRetrieveResponseItem personalPhone = new MultiTypeRetrieveResponseItem();
        CommonType commonType = new CommonType();
        commonType.phone(DEFAULT_RECIPIENT_PHONE);
        personalPhone.value(commonType);
        when(personalExternalService.getPersonalById(DEFAULT_RECIPIENT_PHONE_ID, CommonTypeEnum.PHONE))
                .thenReturn(Optional.of(personalPhone));

        var order = orderFactory.createOrder();

        getRecipientPhoneTailBatchService.processEntity(order.getId());

        var updatedPersonal = orderPersonalQueryService.getActive(order.getId());
        assertThat(updatedPersonal).isPresent();
        assertThat(updatedPersonal.get().getPhoneTail()).isEqualTo(
                DEFAULT_RECIPIENT_PHONE.substring(DEFAULT_RECIPIENT_PHONE.length() - PHONE_TAIL_LENGTH));
    }

    @Test
    void doNothingWhenOrderWithNoPersonal() {
        orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .personal(TestOrderFactory.OrderPersonalParams.builder()
                                .createWithPersonal(false)
                                .build())
                        .build())
                .build());

        verify(personalExternalService, never()).getPersonalById(DEFAULT_RECIPIENT_PHONE_ID, CommonTypeEnum.PHONE);
    }

    @Test
    void throwExceptionWhenPersonalHasNoSuchPhone() {
        when(personalExternalService.getPersonalById(DEFAULT_RECIPIENT_PHONE_ID, CommonTypeEnum.PHONE))
                .thenReturn(Optional.empty());

        var order = orderFactory.createOrder();

        assertThatThrownBy(() -> getRecipientPhoneTailBatchService.processEntity(order.getId()))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }

    @Test
    void throwExceptionWhenPersonalHasNullPhone() {
        MultiTypeRetrieveResponseItem personalPhone = new MultiTypeRetrieveResponseItem();
        CommonType commonType = new CommonType();
        personalPhone.value(commonType);
        when(personalExternalService.getPersonalById(DEFAULT_RECIPIENT_PHONE_ID, CommonTypeEnum.PHONE))
                .thenReturn(Optional.empty());

        var order = orderFactory.createOrder();

        assertThatThrownBy(() -> getRecipientPhoneTailBatchService.processEntity(order.getId()))
                .isExactlyInstanceOf(TplEntityNotFoundException.class);
    }
}
