package ru.yandex.market.post;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.post.RusPostContractClient;
import ru.yandex.market.core.post.RusPostContractService;
import ru.yandex.market.core.post.model.ContractOfferEntity;
import ru.yandex.market.core.post.model.PostContractStatus;
import ru.yandex.market.core.post.model.dto.ContractOfferStatusDTO;
import ru.yandex.market.core.post.model.dto.ContractOfferStatusListDTO;
import ru.yandex.market.core.post.model.dto.HidListDto;
import ru.yandex.market.shop.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тесты для {@link RusPostSyncContractStatusExecutor}.
 */
@DbUnitDataSet(before = "RusPostSyncContractStatusExecutorTest.before.csv")
class RusPostSyncContractStatusExecutorTest extends FunctionalTest {

    private static final int SHOP_ID = 1000;
    private static final int USER_ID = 10;
    private static final String HID = "123abcde-1abc-1a2a-a123-12345abcdefgh";
    private static final HidListDto HID_LIST_DTO = new HidListDto(List.of(HID));

    @Autowired
    private RusPostSyncContractStatusExecutor executor;

    @Autowired
    private RusPostContractClient contractClient;

    @Autowired
    private RusPostContractService rusPostContractService;

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("getParams")
    void testStatusChangedFromNew(PostContractStatus oldStatus,
                                  ContractOfferStatusDTO.Status postStatus,
                                  PostContractStatus newStatus,
                                  boolean exceptionally) {
        checkStatusChangedFromNew(oldStatus, postStatus, newStatus, exceptionally);
    }

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("getApprovedParams")
    void testStatusChangedFromNewToApproved(
            PostContractStatus oldStatus,
            ContractOfferStatusDTO.Status postStatus,
            PostContractStatus newStatus,
            boolean exceptionally
    ) {
        checkStatusChangedFromNew(oldStatus, postStatus, newStatus, exceptionally);

        verifySentNotificationType(partnerNotificationClient, 1, 1596450038L);
    }

    /**
     * Проверка, что если для одного магазина не удастся проверить статус,
     * статус успешно проверенных при необходимости изменится, у этого останется старым.
     */
    @DisplayName("Проверка исключения при проверке статуса")
    @DbUnitDataSet(before = "RusPostSyncContractStatusExecutorTest.testException.before.csv")
    @Test
    void testException() {
        when(contractClient.getContractsStatuses(eq(HID_LIST_DTO), anyString()))
                .thenReturn(getStatusesDto(ContractOfferStatusDTO.Status.ACTIVATED));

        rusPostContractService.saveContractOffer(null, SHOP_ID, USER_ID, PostContractStatus.SENT);
        rusPostContractService.saveContractOffer(null, 1001, 11, PostContractStatus.SENT);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> executor.doJob(null));

        ContractOfferEntity offer = rusPostContractService.getContractOffer(SHOP_ID).orElseThrow();
        assertThat(offer.getStatus()).isEqualTo(PostContractStatus.APPROVED);
        offer = rusPostContractService.getContractOffer(1001).orElseThrow();
        assertThat(offer.getStatus()).isEqualTo(PostContractStatus.SENT);
    }

    /**
     * Проверка, что если идентификационный токен отсутствует, статус контракта не обновится,
     * но задача выполнится.
     */
    @DisplayName("Проверка отсутствующего идентификационного токена")
    @Test
    void testNullIdToken() {
        when(contractClient.getContractsStatuses(eq(HID_LIST_DTO), anyString()))
                .thenReturn(getStatusesDto(ContractOfferStatusDTO.Status.ACTIVATED));

        rusPostContractService.saveContractOffer(null, 1001, 11, PostContractStatus.SENT);
        rusPostContractService.saveContractOffer(null, SHOP_ID, USER_ID, PostContractStatus.SENT);

        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> executor.doJob(null));

        ContractOfferEntity offer = rusPostContractService.getContractOffer(SHOP_ID).orElseThrow();
        assertThat(offer.getStatus()).isEqualTo(PostContractStatus.APPROVED);
        offer = rusPostContractService.getContractOffer(1001).orElseThrow();
        assertThat(offer.getStatus()).isEqualTo(PostContractStatus.SENT);
    }

    private void checkStatusChangedFromNew(PostContractStatus oldStatus,
                                           ContractOfferStatusDTO.Status postStatus,
                                           PostContractStatus newStatus,
                                           boolean exceptionally) {
        when(contractClient.getContractsStatuses(eq(HID_LIST_DTO), anyString()))
                .thenReturn(getStatusesDto(postStatus));

        rusPostContractService.saveContractOffer(null, SHOP_ID, USER_ID, oldStatus);
        final Instant now = rusPostContractService.getContractOffer(SHOP_ID).orElseThrow().getUpdatedAt();

        if (exceptionally) {
            assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> executor.doJob(null));
        } else {
            executor.doJob(null);
        }

        final ContractOfferEntity offer = rusPostContractService.getContractOffer(SHOP_ID).orElseThrow();

        //проверяем, что статус сменился верно
        assertThat(offer.getStatus()).isEqualTo(newStatus);

        //проверяем, что время обновления поменялось только в случае смены статуса
        final Instant updatedAt = offer.getUpdatedAt();
        if (oldStatus.equals(newStatus)) {
            assertThat(updatedAt).isEqualTo(now);
        } else {
            assertThat(updatedAt.isAfter(now)).isTrue();
        }
    }

    private static Stream<Arguments> getApprovedParams() {
        return Stream.of(
                Arguments.of(PostContractStatus.SENT, ContractOfferStatusDTO.Status.ACTIVATED,
                        PostContractStatus.APPROVED, false),
                Arguments.of(PostContractStatus.ERROR, ContractOfferStatusDTO.Status.ACTIVATED,
                        PostContractStatus.APPROVED, false)
        );
    }

    private static Stream<Arguments> getParams() {
        return Stream.of(
                Arguments.of(PostContractStatus.SENT, ContractOfferStatusDTO.Status.CHECKING,
                        PostContractStatus.SENT, false),
                Arguments.of(PostContractStatus.SENT, ContractOfferStatusDTO.Status.MANUAL_CHECKING,
                        PostContractStatus.DECLINED, false),
                Arguments.of(PostContractStatus.SENT, ContractOfferStatusDTO.Status.STOP_LIST,
                        PostContractStatus.DECLINED, false),
                Arguments.of(PostContractStatus.ERROR, ContractOfferStatusDTO.Status.CHECKING,
                        PostContractStatus.SENT, false),
                Arguments.of(PostContractStatus.ERROR, ContractOfferStatusDTO.Status.MANUAL_CHECKING,
                        PostContractStatus.DECLINED, false),
                Arguments.of(PostContractStatus.ERROR, ContractOfferStatusDTO.Status.STOP_LIST,
                        PostContractStatus.DECLINED, false),
                Arguments.of(PostContractStatus.ERROR, null, PostContractStatus.NEW, false),
                Arguments.of(PostContractStatus.SENT, null, PostContractStatus.NEW, true)
        );
    }

    private static ContractOfferStatusListDTO getStatusesDto(ContractOfferStatusDTO.Status status) {
        return status == null
                ? null
                : new ContractOfferStatusListDTO(List.of(new ContractOfferStatusDTO(HID, status)));
    }
}
