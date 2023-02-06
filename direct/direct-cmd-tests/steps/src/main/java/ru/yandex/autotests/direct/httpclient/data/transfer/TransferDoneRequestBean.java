package ru.yandex.autotests.direct.httpclient.data.transfer;

import ru.yandex.autotests.direct.httpclient.core.BasicDirectRequestParameters;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 17.06.15
 */
public class TransferDoneRequestBean extends BasicDirectRequestParameters {

    @JsonPath(requestPath = "client_from")
    private String clientFrom;

    @JsonPath(requestPath = "client_to")
    private String clientTo;

    @JsonPath(requestPath = "transfer-to-radio")
    private String transferToRadio;

    @JsonPath(requestPath = "transfer_to")
    private String transferTo;

    @JsonPath(requestPath = "transfer-from-radio")
    private String transferFromRadio;

    @JsonPath(requestPath = "transfer_from")
    private String transferFrom;

    private List<CampaignFromSumRequestBean> campaignFromSums;

    private List<CampaignToSumRequestBean> campaignToSums;

    public String getClientFrom() {
        return clientFrom;
    }

    public void setClientFrom(String clientFrom) {
        this.clientFrom = clientFrom;
    }

    public String getClientTo() {
        return clientTo;
    }

    public void setClientTo(String clientTo) {
        this.clientTo = clientTo;
    }

    public String getTransferToRadio() {
        return transferToRadio;
    }

    public void setTransferToRadio(String transferToRadio) {
        this.transferToRadio = transferToRadio;
    }

    public String getTransferTo() {
        return transferTo;
    }

    public void setTransferTo(String transferTo) {
        this.transferTo = transferTo;
    }

    public String getTransferFromRadio() {
        return transferFromRadio;
    }

    public void setTransferFromRadio(String transferFromRadio) {
        this.transferFromRadio = transferFromRadio;
    }

    public String getTransferFrom() {
        return transferFrom;
    }

    public void setTransferFrom(String transferFrom) {
        this.transferFrom = transferFrom;
    }

    public List<CampaignFromSumRequestBean> getCampaignFromSums() {
        if(campaignFromSums == null) {
            campaignFromSums = new ArrayList<CampaignFromSumRequestBean>();
        }
        return campaignFromSums;
    }

    public List<CampaignToSumRequestBean> getCampaignToSums() {
        if(campaignToSums == null) {
            campaignToSums = new ArrayList<CampaignToSumRequestBean>();
        }
        return campaignToSums;
    }

    public void setCampaignFromSums(List<CampaignFromSumRequestBean> campaignFromSums) {
        this.campaignFromSums = campaignFromSums;
    }

    public void addCampaignFromSum(CampaignFromSumRequestBean campaignFromSumRequestBean) {
        getCampaignFromSums().add(campaignFromSumRequestBean);
    }

    public void addCampaignToSum(CampaignToSumRequestBean campaignToSumRequestBean) {
        getCampaignToSums().add(campaignToSumRequestBean);
    }

    public void setCampaignToSums(List<CampaignToSumRequestBean> campaignToSums) {
        this.campaignToSums = campaignToSums;
    }
}
