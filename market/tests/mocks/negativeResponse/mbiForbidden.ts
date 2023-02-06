import {ErrorCode} from '~/app/bcm/mbiPartner/Backend/errors';
import getMbiPartnerErrorResponse from '~/app/utils/tests/mocks/getMbiPartnerErrorResponse';

export default getMbiPartnerErrorResponse({
    code: ErrorCode.Unauthorized,
    statusCode: 403,
});
