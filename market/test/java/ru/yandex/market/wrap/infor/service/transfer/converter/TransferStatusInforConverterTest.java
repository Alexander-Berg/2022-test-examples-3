package ru.yandex.market.wrap.infor.service.transfer.converter;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatus;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusEvent;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.TransferStatusType;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.model.TransferDTO;
import ru.yandex.market.wrap.infor.client.model.TransferStatusHistoryDTO;

class TransferStatusInforConverterTest extends SoftAssertionSupport {

    private static final String YANDEX_ID = "yandexId";
    private static final String PARTNER_ID = "partnerId";
    private static final String TRANSFER_STATUS = "0";
    private static final String TRANSFER_STATUS_HISTORY = "9";
    private static final String INPUT_TRANSFER_DATE_TIME = "2019-05-14T22:11:28";
    private static final String EXPECTED_TRANSFER_ADD_DATE_TIME = "2019-05-15T01:11:28+03:00";

    private TransferStatusInforConverter transferStatusInforConverter = new TransferStatusInforConverter();

    /**
     * Сценарий #1:
     * <p>Проверка конвертации из TransferStatusHistoryDTO в TransferStatusEvent
     */
    @Test
    void convertTransferStatusHistoryDtoToStatusEvent() {

        TransferStatusHistoryDTO historyDTO = getTransferStatusHistoryDTO();

        TransferStatusEvent transferStatusEvent = transferStatusInforConverter.convertToStatusEvent(historyDTO);

        softly
            .assertThat(transferStatusEvent.getStatusCode())
            .as("Asserting transfer status")
            .isEqualTo(TransferStatusType.COMPLETED);

        softly
            .assertThat(transferStatusEvent.getDate())
            .as("Asserting transfer add dateTime")
            .isEqualTo(new DateTime(EXPECTED_TRANSFER_ADD_DATE_TIME));
    }

    /**
     * Сценарий #2:
     * <p>Проверка конвертации ResourceId и TransferDTO в TransferStatus
     */
    @Test
    void convertToTransferStatus() {
        TransferDTO transferDTO = getTransferDTO();
        ResourceId transferId = getResourceId();

        TransferStatus transferStatus = transferStatusInforConverter.convertToStatus(transferId, transferDTO);

        softly
            .assertThat(transferStatus.getTransferId().getYandexId())
            .as("Asserting TransferStatus Yandex Id")
            .isEqualTo(YANDEX_ID);

        softly
            .assertThat(transferStatus.getTransferId().getPartnerId())
            .as("Asserting TransferStatus Partner Id")
            .isEqualTo(PARTNER_ID);

        softly
            .assertThat(transferStatus.getTransferStatusEvent())
            .as("Asserting TransferStatusEvent")
            .isNotNull();

        softly
            .assertThat(transferStatus.getTransferStatusEvent().getDate())
            .as("Asserting TransferStatusEvent date")
            .isEqualTo(new DateTime(EXPECTED_TRANSFER_ADD_DATE_TIME));

        softly
            .assertThat(transferStatus.getTransferStatusEvent().getStatusCode())
            .as("Asserting TransferStatusEvent status code")
            .isEqualTo(TransferStatusType.NEW);
    }

    private TransferStatusHistoryDTO getTransferStatusHistoryDTO() {
        TransferStatusHistoryDTO historyDTO = new TransferStatusHistoryDTO();
        historyDTO.setStatus(TRANSFER_STATUS_HISTORY);
        historyDTO.setAdddate(INPUT_TRANSFER_DATE_TIME);
        return historyDTO;
    }

    private TransferDTO getTransferDTO() {
        TransferDTO transferDTO = new TransferDTO();
        transferDTO.setStatus(TRANSFER_STATUS);
        transferDTO.setEditdate(INPUT_TRANSFER_DATE_TIME);
        return transferDTO;
    }

    private ResourceId getResourceId() {
        return new ResourceId(YANDEX_ID, PARTNER_ID);
    }
}
