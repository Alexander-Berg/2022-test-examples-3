import type {BackendResponseParams} from '~/configs/jest/mocks/mandrel/base/backendHandlers';
import type {BackendErrorPayload} from '~/app/bcm/ff4Shops/Backend/types';

export default ({statusCode, subCode, message}: BackendErrorPayload): BackendResponseParams => ({
    body: {
        errors: [
            {
                subCode,
                message,
            },
        ],
    },
    statusCode,
});
