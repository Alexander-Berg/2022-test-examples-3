import {PlacementType} from '~/app/entities/placement/types';
import {CampaignType, BriefCampaign} from '~/app/entities/campaign/types';

export default (brief?: Partial<BriefCampaign>): BriefCampaign => ({
    businessId: 1,
    campaignId: 1001410791,
    clientId: 1352190526,
    internalName: 'Экспрессович',
    partnerId: 11103659,
    placementTypes: [PlacementType.Dropship],
    type: CampaignType.Supplier,
    ...brief,
});
