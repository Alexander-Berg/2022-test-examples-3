package ru.yandex.autotests.direct.cmd.data.stat.filter;
import com.google.gson.annotations.SerializedName;

public class Data {

    @SerializedName("campaign_type")
    private CampaignType campaignType;

    @SerializedName("targettype")
    private TargetType targetType;

    @SerializedName("contexttype")
    private ContextType contextType;

    @SerializedName("agoalincome")
    private Agoalincome agoalincome;

    @SerializedName("position")
    private Position position;
    
    public CampaignType getCampaignType() {
        return campaignType;
    }
    
    public void setCampaignType(CampaignType campaignType) {
        this.campaignType = campaignType;
    }
    
    public TargetType getTargetType() {
        return targetType;
    }
    
    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }
    
    public ContextType getContextType() {
        return contextType;
    }
    
    public void setContextType(ContextType contextType) {
        this.contextType = contextType;
    }
    
    public Agoalincome getAgoalincome() {
        return agoalincome;
    }
    
    public void setAgoalincome(Agoalincome agoalincome) {
        this.agoalincome = agoalincome;
    }
}
