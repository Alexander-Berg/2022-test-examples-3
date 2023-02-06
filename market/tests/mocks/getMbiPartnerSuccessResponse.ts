import type {MBIResult} from '~/app/bcm/mbiPartner/Backend/types';

function getMbiPartnerSuccessResponse<R = unknown>(result: R): MBIResult<R> {
    return {result};
}

export default getMbiPartnerSuccessResponse;
