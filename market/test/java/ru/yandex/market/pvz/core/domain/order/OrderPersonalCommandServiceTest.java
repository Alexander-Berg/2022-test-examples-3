package ru.yandex.market.pvz.core.domain.order;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.pvz.core.domain.order.model.params.OrderPersonalParams;
import ru.yandex.market.pvz.core.domain.order.model.personal.OrderPersonalRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.dbqueue.get_recipient_phone_tail_batch.GetRecipientPhoneTailBatchService.PHONE_TAIL_LENGTH;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_BUYER_YANDEX_UID;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderParams.DEFAULT_RECIPIENT_PHONE;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderPersonalParams.DEFAULT_RECIPIENT_EMAIL_ID;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderPersonalParams.DEFAULT_RECIPIENT_FULL_NAME_ID;
import static ru.yandex.market.pvz.core.test.factory.TestOrderFactory.OrderPersonalParams.DEFAULT_RECIPIENT_PHONE_ID;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderPersonalCommandServiceTest {

    private final TestOrderFactory orderFactory;

    private final OrderPersonalRepository orderPersonalRepository;

    private final OrderPersonalCommandService orderPersonalCommandService;

    @MockBean
    private PersonalExternalService personalExternalService;

    @Test
    void updatePersonal() {
        var order = orderFactory.createOrder();

        var createdPersonal = orderPersonalRepository.findActiveByOrderId(order.getId());
        assertThat(createdPersonal).isPresent();
        assertThat(createdPersonal.get().getRecipientFullNameId()).isEqualTo(DEFAULT_RECIPIENT_FULL_NAME_ID);
        assertThat(createdPersonal.get().getRecipientPhoneId()).isEqualTo(DEFAULT_RECIPIENT_PHONE_ID);
        assertThat(createdPersonal.get().getRecipientEmailId()).isEqualTo(DEFAULT_RECIPIENT_EMAIL_ID);
        assertThat(createdPersonal.get().getBuyerYandexUid()).isEqualTo(DEFAULT_BUYER_YANDEX_UID);
        assertThat(createdPersonal.get().isActive()).isTrue();
        assertThat(createdPersonal.get().getPhoneTail()).isNull();

        var personalForUpdate = OrderPersonalParams.builder()
                .recipientFullNameId("1")
                .recipientPhoneId("2")
                .recipientEmailId("3")
                .buyerYandexUid(99L)
                .build();

        var updatedPersonalParams = orderPersonalCommandService.update(order, personalForUpdate);

        var expectedPersonalParams = OrderPersonalParams.builder()
                .id(updatedPersonalParams.getId())
                .recipientFullNameId("1")
                .recipientPhoneId("2")
                .recipientEmailId("3")
                .buyerYandexUid(99L)
                .active(true)
                .build();

        assertThat(updatedPersonalParams).isEqualTo(expectedPersonalParams);

        var updatedPersonal = orderPersonalRepository.findActiveByOrderId(order.getId());

        assertThat(updatedPersonal).isPresent();
        assertThat(updatedPersonal.get().getRecipientFullNameId()).isEqualTo("1");
        assertThat(updatedPersonal.get().getRecipientPhoneId()).isEqualTo("2");
        assertThat(updatedPersonal.get().getRecipientEmailId()).isEqualTo("3");
        assertThat(updatedPersonal.get().getBuyerYandexUid()).isEqualTo(99L);
        assertThat(updatedPersonal.get().isActive()).isTrue();
        assertThat(updatedPersonal.get().getPhoneTail()).isNull();

        var allPersonals = orderPersonalRepository.findAllByOrderId(order.getId());
        assertThat(allPersonals).hasSize(2);

        var stalePersonal = orderPersonalRepository.findAllByOrderIdAndActive(order.getId(), false);
        assertThat(stalePersonal).hasSize(1);
    }

    @Test
    void updatePersonalWithoutInitialPersonal() {
        var order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .params(TestOrderFactory.OrderParams.builder()
                        .personal(TestOrderFactory.OrderPersonalParams.builder()
                                .createWithPersonal(false)
                                .build())
                        .build())
                .build());

        var createdPersonal = orderPersonalRepository.findActiveByOrderId(order.getId());
        assertThat(createdPersonal).isEmpty();

        var personalForUpdate = OrderPersonalParams.builder()
                .recipientFullNameId("1")
                .recipientPhoneId("2")
                .recipientEmailId("3")
                .buyerYandexUid(99L)
                .build();

        var updatedPersonalParams = orderPersonalCommandService.update(order, personalForUpdate);

        var expectedPersonalParams = OrderPersonalParams.builder()
                .id(updatedPersonalParams.getId())
                .recipientFullNameId("1")
                .recipientPhoneId("2")
                .recipientEmailId("3")
                .buyerYandexUid(99L)
                .active(true)
                .build();

        assertThat(updatedPersonalParams).isEqualTo(expectedPersonalParams);

        var updatedPersonal = orderPersonalRepository.findActiveByOrderId(order.getId());

        assertThat(updatedPersonal).isPresent();
        assertThat(updatedPersonal.get().getRecipientFullNameId()).isEqualTo("1");
        assertThat(updatedPersonal.get().getRecipientPhoneId()).isEqualTo("2");
        assertThat(updatedPersonal.get().getRecipientEmailId()).isEqualTo("3");
        assertThat(updatedPersonal.get().getBuyerYandexUid()).isEqualTo(99L);
        assertThat(updatedPersonal.get().isActive()).isTrue();
        assertThat(updatedPersonal.get().getPhoneTail()).isNull();

        var allPersonals = orderPersonalRepository.findAllByOrderId(order.getId());
        assertThat(allPersonals).hasSize(1);

        var stalePersonal = orderPersonalRepository.findAllByOrderIdAndActive(order.getId(), false);
        assertThat(stalePersonal).isEmpty();
    }

    @Test
    void updatePhoneTail() {
        var order = orderFactory.createOrder();

        var createdPersonal = orderPersonalRepository.findActiveByOrderId(order.getId());
        assertThat(createdPersonal).isPresent();
        assertThat(createdPersonal.get().getPhoneTail()).isNull();

        String phoneTail = DEFAULT_RECIPIENT_PHONE.substring(DEFAULT_RECIPIENT_PHONE.length() - PHONE_TAIL_LENGTH);
        orderPersonalCommandService.updatePhoneTail(
                createdPersonal.get().getId(), phoneTail);

        var updatedPersonal = orderPersonalRepository.findActiveByOrderId(order.getId());
        assertThat(updatedPersonal).isPresent();
        assertThat(updatedPersonal.get().getPhoneTail()).isEqualTo(phoneTail);
    }

}
