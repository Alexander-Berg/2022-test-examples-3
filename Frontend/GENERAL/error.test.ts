import debugFactory, { IDebugger } from 'debug';
import { Request, Response } from 'express';

import ErrorController from './error';

import * as errors from './errors';

function getMockedResponse() {
    return {
        status: jest.fn().mockReturnThis(),
        json: jest.fn().mockReturnThis(),
        end: jest.fn(),
    } as unknown as Response;
}

describe('ErrorController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getHandler', () => {
        it('should return function', () => {
            const controller = new ErrorController({ debug });

            const expected = 'function';

            const actual = controller.getHandler();

            expect(typeof actual).toEqual(expected);
        });

        it('should response with status 500 when handled unexpected error', () => {
            const controller = new ErrorController({ debug });

            const fakeError = new Error('unknown error');
            const fakeRequest = { id: '123' } as unknown as Request;
            const fakeResponse = getMockedResponse();
            const fakeNext = jest.fn();

            const handler = controller.getHandler();

            handler(fakeError, fakeRequest, fakeResponse, fakeNext);

            expect(fakeResponse.status).toHaveBeenCalledWith(500);
            expect(fakeResponse.json).not.toHaveBeenCalled();
            expect(fakeResponse.end).toHaveBeenCalledWith();

            expect(fakeNext).not.toHaveBeenCalled();
        });

        it('should response with status 500 when handled error with typeof HttpResponseError', () => {
            const controller = new ErrorController({ debug });

            const fakeError = new errors.HttpResponseError();
            const fakeRequest = { id: '123' } as unknown as Request;
            const fakeResponse = getMockedResponse();
            const fakeNext = jest.fn();

            const handler = controller.getHandler();

            const expectedPayload = {
                comment: 'Internal Server Error',
                message: '',
            };

            handler(fakeError, fakeRequest, fakeResponse, fakeNext);

            expect(fakeResponse.status).toHaveBeenCalledWith(500);
            expect(fakeResponse.json).toHaveBeenCalledWith(expectedPayload);
            expect(fakeResponse.end).toHaveBeenCalledWith();

            expect(fakeNext).not.toHaveBeenCalled();
        });

        it('should response with status from HttpResponseError-based error', () => {
            const controller = new ErrorController({ debug });

            const fakeError = new errors.InvalidPayloadError('passed payload is invalid for some reason');
            const fakeRequest = { id: '123' } as unknown as Request;
            const fakeResponse = getMockedResponse();
            const fakeNext = jest.fn();

            const handler = controller.getHandler();

            const expectedPayload = {
                comment: 'Invalid Payload',
                message: 'passed payload is invalid for some reason',
            };

            handler(fakeError, fakeRequest, fakeResponse, fakeNext);

            expect(fakeResponse.status).toHaveBeenCalledWith(406);
            expect(fakeResponse.json).toHaveBeenCalledWith(expectedPayload);
            expect(fakeResponse.end).toHaveBeenCalledWith();

            expect(fakeNext).not.toHaveBeenCalled();
        });
    });
});
