'use strict';

const mockModelsToResponse = jest.fn();
jest.mock('@ps-int/mail-lib', () => ({
    helpers: {
        'models-to-response': mockModelsToResponse
    }
}));

const requestMethods = require('./requestMethods.js');

describe('requestMethods', () => {
    it('fills res.locals and calls `next`', () => {
        const req = {
            body: { methods: 1 },
            core: { request: { safe: jest.fn().mockResolvedValue(2) } }
        };
        const res = { locals: {} };
        mockModelsToResponse.mockReturnValue(3);

        return new Promise((resolve) => {
            requestMethods(req, res, resolve);
        }).then(() => {
            expect(req.core.request.safe).toHaveBeenCalledWith(1);
            expect(mockModelsToResponse).toHaveBeenCalledWith(1, 2);
            expect(res.locals.methods).toBe(3);
        });
    });

    it('logs errors and calls `next`', () => {
        const request = { safe: jest.fn().mockRejectedValue(2) };
        const console = { error: jest.fn() };
        const req = { body: { methods: 1 }, core: { request, console } };

        return new Promise((resolve) => {
            requestMethods(req, null, resolve);
        }).then(() => {
            expect(req.core.console.error).toHaveBeenCalledWith('COMMON_REQUEST_ERROR', 2);
        });
    });
});
