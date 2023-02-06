'use strict';

const mockMiddleware = () => 1;

jest.mock('../config/index.js', () => 0);
jest.mock('./create-auth.js', () => 1);
jest.mock('./create-core.js', () => 1);
jest.mock('./log-request.js', () => 1);
jest.mock('./tvm.js', () => 1);
jest.mock('@ps-int/mail-lib', () => ({
    middlewares: {
        dollarConfig: mockMiddleware,
        uatraits: 1
    }
}));
jest.mock('@yandex-int/duffman', () => ({
    middleware: {
        yandexuid: 1
    },
    express: {
        json: mockMiddleware
    }
}));

describe('middlewares', () => {
    it('returns middleware list', () => {
        const middlewares = require('./');

        expect(middlewares).toHaveLength(8);
        for (const middleware of middlewares) {
            expect(middleware).toBe(1);
        }
    });
});
