package ru.yandex.market.partner.mvc;

import java.util.StringJoiner;

import ru.yandex.market.core.campaign.IdsResolver;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.servant.PartnerDefaultRequestHandler;

import static org.mockito.Mockito.mock;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class MockPartnerRequest extends PartnerDefaultRequestHandler.PartnerHttpServRequest {

    private long userId;
    private long effectiveUid;
    private long datasourceId;
    private long campaignId;
    private PartnerId partnerId;

    public MockPartnerRequest(long userId, long effectiveUid, long datasourceId, long campaignId) {
        super(userId, "redirect", "name", mock(IdsResolver.class));
        this.userId = userId;
        this.effectiveUid = effectiveUid;
        this.datasourceId = datasourceId;
        this.campaignId = campaignId;
        this.partnerId = PartnerId.datasourceId(datasourceId);
    }

    public MockPartnerRequest(long userId, long campaignId, PartnerId partnerId) {
        super(userId, "redirect", "name", mock(IdsResolver.class));
        this.userId = userId;
        this.effectiveUid = userId;
        this.datasourceId = partnerId != null ? partnerId.toLong() : -1;
        this.campaignId = campaignId;
        this.partnerId = partnerId;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public long getEffectiveUid() {
        return effectiveUid;
    }

    @Override
    public long getDatasourceId() {
        return datasourceId;
    }

    @Override
    public long getCampaignId() {
        return campaignId;
    }

    @Override
    public PartnerId getPartnerId() {
        return partnerId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MockPartnerRequest.class.getSimpleName() + "[", "]")
                .add("userId=" + userId)
                .add("partnerId=" + partnerId)
                .toString();
    }
}
