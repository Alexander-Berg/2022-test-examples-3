package ru.yandex.autotests.direct.cmd.data.transfer;

import ru.yandex.autotests.direct.cmd.data.BasicDirectRequest;
import ru.yandex.autotests.httpclientlite.HttpClientLiteException;
import ru.yandex.autotests.httpclientlite.core.request.utils.keyvalue.annotations.SerializeKey;

import java.util.List;

public class TransferDoneRequest extends BasicDirectRequest {

    public TransferDoneRequest() {
    }

    public TransferDoneRequest(String clientFrom, String clientTo,
                               List<CampaignFromSum> campaignsFrom,
                               List<CampaignToSum> campaignsTo, TransferTypeEnum transferType) {
        withClientFrom(clientFrom)
                .withClientTo(clientTo);
        switch (transferType) {
            case FROM_ONE_TO_MANY:
                withTransferFrom(campaignsFrom.get(0).getObjectId())
                        .withTransferFromRadio(campaignsFrom.get(0).getObjectId())
                        .withCampaignToSums(campaignsTo);
                break;
            case FROM_MANY_TO_ONE:
                withTransferTo(campaignsTo.get(0).getObjectId())
                        .withTransferToRadio(campaignsTo.get(0).getObjectId())
                        .withCampaignFromSums(campaignsFrom);
                break;
            default:
                throw new HttpClientLiteException("Не указан тип переноса средств");
        }
    }

    @SerializeKey("client_from")
    private String clientFrom;

    @SerializeKey("client_to")
    private String clientTo;

    @SerializeKey("transfer-to-radio")
    private String transferToRadio;

    @SerializeKey("transfer_to")
    private String transferTo;

    @SerializeKey("transfer-from-radio")
    private String transferFromRadio;

    @SerializeKey("transfer_from")
    private String transferFrom;

    private List<CampaignFromSum> campaignFromSums;

    private List<CampaignToSum> campaignToSums;

    public String getClientFrom() {
        return clientFrom;
    }

    public TransferDoneRequest withClientFrom(String clientFrom) {
        this.clientFrom = clientFrom;
        return this;
    }

    public String getClientTo() {
        return clientTo;
    }

    public TransferDoneRequest withClientTo(String clientTo) {
        this.clientTo = clientTo;
        return this;
    }

    public String getTransferToRadio() {
        return transferToRadio;
    }

    public TransferDoneRequest withTransferToRadio(String transferToRadio) {
        this.transferToRadio = transferToRadio;
        return this;
    }

    public String getTransferTo() {
        return transferTo;
    }

    public TransferDoneRequest withTransferTo(String transferTo) {
        this.transferTo = transferTo;
        return this;
    }

    public String getTransferFromRadio() {
        return transferFromRadio;
    }

    public TransferDoneRequest withTransferFromRadio(String transferFromRadio) {
        this.transferFromRadio = transferFromRadio;
        return this;
    }

    public String getTransferFrom() {
        return transferFrom;
    }

    public TransferDoneRequest withTransferFrom(String transferFrom) {
        this.transferFrom = transferFrom;
        return this;
    }

    public List<CampaignFromSum> getCampaignFromSums() {
        return campaignFromSums;
    }

    public TransferDoneRequest withCampaignFromSums(List<CampaignFromSum> campaignFromSums) {
        this.campaignFromSums = campaignFromSums;
        return this;
    }

    public List<CampaignToSum> getCampaignToSums() {
        return campaignToSums;
    }

    public TransferDoneRequest withCampaignToSums(List<CampaignToSum> campaignToSums) {
        this.campaignToSums = campaignToSums;
        return this;
    }
}
