'use strict';

const mockTvmMiddleware = jest.fn();
jest.mock('@ps-int/mail-lib', () => ({ middlewares: { tvm2: mockTvmMiddleware } }));

describe('tvm', () => {
    const OLD_ENV = process.env;

    beforeEach(() => {
        jest.resetModules();
        mockTvmMiddleware.mockReset();
        process.env = {
            ...OLD_ENV,
            QLOUD_TVM_CONFIG: '',
            QLOUD_TVM_TOKEN: 't',
            QLOUD_TVM_INTERFACE_ORIGIN: 'i'
        };
    });

    afterEach(() => {
        process.env = OLD_ENV;
    });

    it('uses QLOUD_TVM_CONFIG', () => {
        process.env.QLOUD_TVM_CLIENT_ID = 'c1';
        process.env.QLOUD_TVM_CONFIG = JSON.stringify({
            clients: { c1: { dsts: { k1: {}, k2: {} } } }
        });
        require('./tvm.js');
        expect(mockTvmMiddleware).toHaveBeenCalledWith({
            self: 'c1',
            destinations: [ 'k1', 'k2' ]
        });
    });

    it('falls back to empty object', () => {
        require('./tvm.js');
        expect(mockTvmMiddleware).toHaveBeenCalledWith({
            self: 'litemail',
            destinations: []
        });
    });
});
