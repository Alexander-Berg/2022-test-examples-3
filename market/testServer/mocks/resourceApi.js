import MockResponse from 'Response';

import {getBackendHandler as mockBackendHandler} from 'configs/jest/testServer/mocks/base/backendHandlers';

jest.mock('@yandex-market/b2b-core/app/stout/decorators/ResourceApi', () => ({
    __esModule: true,
    default() {
        this.resource = (name, params) => {
            const result = new MockResponse();

            mockBackendHandler()(name, params).then(
                resp => {
                    result.resolve(resp);
                },
                err => {
                    result.reject(err);
                },
            );

            return result;
        };
        this.resourceDebug = {
            calls: [],
            __proto__: {
                push(v) {
                    this.calls.push(v);
                },
            },
        };
        this.resourceConfig = () => ({});
    },
}));

jest.mock('@yandex-market/mandrel/modules/Resource', () => {
    const MockResourceModule = function() {
        this.resource = ({sk}, name, params) => Promise.resolve(mockBackendHandler(sk)(name, params));
        this.name = 'Resource';
    };

    MockResourceModule.prototype.getCacheKeyParams = () => ({});
    MockResourceModule.make = () => {
        const result = new MockResponse();
        result.resolve(new MockResourceModule());

        return result;
    };

    return MockResourceModule;
});
