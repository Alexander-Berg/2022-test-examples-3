import {getBackendHandler as mockBackendHandler} from 'configs/jest/testServer';

jest.mock('@yandex-market/mandrel/resolver', () => ({
    isRemote: () => true,
    createResolver: jest.fn().mockImplementation(fn => fn),
    createSyncResolver: jest.fn().mockImplementation(fn => fn),
    unsafeResource: ({sk}, name, params) => Promise.resolve(mockBackendHandler(sk)(name, params)),
}));
