import type {CampaignId} from '~/app/entities/campaign/types';
import {ErrorCode} from '~/app/bcm/mbiPartner/Backend/errors';
import getMbiPartnerErrorResponse from '~/app/utils/tests/mocks/getMbiPartnerErrorResponse';

export default (campaignId: CampaignId) =>
    getMbiPartnerErrorResponse({
        code: ErrorCode.BadParam,
        statusCode: 400,
        details: {
            entity_name: 'partnerId',
            subcode: 'ENTITY_NOT_FOUND',
            entity_id: String(campaignId),
        },
    });
