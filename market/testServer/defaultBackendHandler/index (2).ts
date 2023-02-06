import type {BackendHandler} from '~/configs/jest/mocks/mandrel/base/backendHandlers';

import blackboxResponse from './mockResponse/blackbox';

export const defaultBackendHandler: BackendHandler = name => {
    if (name === 'blackbox') {
        return blackboxResponse;
    }

    return undefined;
};
