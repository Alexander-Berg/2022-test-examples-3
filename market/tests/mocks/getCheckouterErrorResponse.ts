import type {BackendResponseParams} from '~/configs/jest/mocks/mandrel/base/backendHandlers';
import type {ExpectedError} from '~/app/bcm/checkouter/Backend/types';

export default ({statusCode, ...error}: Partial<ExpectedError>): BackendResponseParams => ({
    body: error,
    statusCode,
});
