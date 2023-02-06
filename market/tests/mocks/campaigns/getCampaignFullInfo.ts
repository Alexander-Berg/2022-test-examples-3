import {PartnerProgramStatus, ProgramStatus, TestingState} from '~/app/entities/program/types';
import {PlacementType} from '~/app/entities/placement/types';
import {FullCampaignInfo, CampaignType} from '~/app/entities/campaign/types';

export default (info?: Partial<FullCampaignInfo>, programStatus?: Partial<PartnerProgramStatus>): FullCampaignInfo => {
    const partnerProgramStatus: PartnerProgramStatus = {
        program: 'marketplace',
        status: ProgramStatus.Restricted,
        isEnabled: true,
        subStatuses: [],
        needTestingState: TestingState.NotRequired,
        newbie: false,
        ...programStatus,
    };

    return {
        startDate: '2021-06-29T00:00:00+03:00',
        placementTypes: [PlacementType.Dropship],
        campaignId: 1001410791,
        partnerId: 11103659,
        clientId: 1352190526,
        tariffId: 1015,
        type: CampaignType.Supplier,
        canLimitBudget: true,
        budgetLimit: 0,
        homeRegion: 225,
        supplierStatus: partnerProgramStatus,
        partnerStatus: partnerProgramStatus,
        manager: {
            id: -2,
            name: 'Служба Яндекс.Маркет',
        },
        internalName: 'Экспрессович',
        ...info,
    };
};
