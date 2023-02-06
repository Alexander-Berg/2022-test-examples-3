import {getBackendHandler as mockBackendHandler} from './base/backendHandlers';

jest.mock('@yandex-market/mandrel/modules/Resource', () => ({
    default: {resource: ({sk}, name, params) => Promise.resolve(mockBackendHandler(sk)(name, params))},
}));
