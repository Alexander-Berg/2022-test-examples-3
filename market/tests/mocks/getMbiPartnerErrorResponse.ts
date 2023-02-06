import type {BackendResponseParams} from '~/configs/jest/mocks/mandrel/base/backendHandlers';
import type {MBIError} from '~/app/bcm/mbiPartner/Backend/types';

export default (error: Partial<MBIError>): BackendResponseParams => ({
    body: {
        errors: [
            {
                code: error.code,
                details: error.details || {},
            },
        ],
    },
    statusCode: error.statusCode,
});
