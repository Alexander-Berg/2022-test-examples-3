import {PlacementType} from '~/app/entities/placement/types';
import {PartnerProgramStatus, ProgramStatus, TestingState} from '~/app/entities/program/types';
import {CampaignType, BriefCampaignWithStatus} from '~/app/entities/campaign/types';

export default (
    brief?: Partial<BriefCampaignWithStatus>,
    programStatus?: Partial<PartnerProgramStatus>,
): BriefCampaignWithStatus => ({
    businessId: 1,
    campaignId: 1001410791,
    clientId: 1352190526,
    internalName: 'Экспрессович',
    partnerId: 11103659,
    placementTypes: [PlacementType.Dropship],
    type: CampaignType.Supplier,
    partnerStatus: {
        program: 'marketplace',
        status: ProgramStatus.Restricted,
        isEnabled: true,
        subStatuses: [],
        needTestingState: TestingState.NotRequired,
        newbie: false,
        ...programStatus,
    },
    ...brief,
});
