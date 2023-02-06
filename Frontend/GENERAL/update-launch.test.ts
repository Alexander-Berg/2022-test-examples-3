import { Collection } from 'mongodb';
import debugFactory, { IDebugger } from 'debug';
import { Response } from 'express';

import LaunchApiService, { Dependencies as LaunchApiServiceDependencies } from '../../../lib/api/launch';

import UpdateLaunchController, { ControllerRequest } from './update-launch';

import * as errors from '../../errors';

import { getMockedDBClass, createCollectionStub } from '../../../../test/helpers/mocked-db';

function createDbMock(collection: Partial<Collection> = {}) {
    const collectionStub = createCollectionStub(jest.fn);
    const MockedDB = getMockedDBClass({ ...collectionStub, ...collection });

    return new MockedDB();
}

function createLaunchApiServiceMock(deps: Partial<LaunchApiServiceDependencies> = {}) {
    const debug = debugFactory('test');

    return new LaunchApiService({ debug, db: createDbMock(), ...deps });
}

function getMockedResponse() {
    return {
        status: jest.fn().mockReturnThis(),
        json: jest.fn().mockReturnThis(),
        end: jest.fn(),
    } as unknown as Response;
}

describe('UpdateLaunchController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getValidateMiddleware', () => {
        it('should return function', () => {
            const launchApi = createLaunchApiServiceMock();
            const controller = new UpdateLaunchController({ debug, launchApi });

            const expected = 'function';

            const actual = controller.getValidateMiddleware();

            expect(typeof actual).toBe(expected);
        });

        it('should call next function when payload are valid', () => {
            const launchApi = createLaunchApiServiceMock();
            const controller = new UpdateLaunchController({ debug, launchApi });

            const body = {};

            const next = jest.fn();

            const middleware = controller.getValidateMiddleware();

            middleware({ body } as unknown as ControllerRequest, {} as Response, next);

            expect(next).toHaveBeenCalled();
            expect(next).toHaveBeenCalledWith();
        });

        it('should return InvalidPayloadError when payload are invalid', () => {
            const launchApi = createLaunchApiServiceMock();
            const controller = new UpdateLaunchController({ debug, launchApi });

            const body = { title: 123 };
            const response = getMockedResponse();
            const next = jest.fn();

            const expected = new errors.InvalidPayloadError('failed to validate schema: data.title should be string');

            const middleware = controller.getValidateMiddleware();

            middleware({ body } as unknown as ControllerRequest, response, next);

            expect(next).toBeCalledWith(expected);
        });
    });

    describe('.getHandler', () => {
        it('should return function', () => {
            const launchApi = createLaunchApiServiceMock();
            const controller = new UpdateLaunchController({ debug, launchApi });

            const expected = 'function';

            const actual = controller.getHandler();

            expect(typeof actual).toBe(expected);
        });

        it('should return 202 when launch successfully updated', async() => {
            const updateOneMock = jest.fn().mockImplementation(() => Promise.resolve({ result: { n: 1 } }));
            const db = createDbMock({ updateOne: updateOneMock });
            const launchApi = createLaunchApiServiceMock({ db });

            const controller = new UpdateLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const request = { params: { id: 'test' }, body: {}, headers: { 'X-Request-ID': 'fake' } } as unknown as ControllerRequest;
            const response = getMockedResponse();

            await handler(request, response);

            expect(updateOneMock).toHaveBeenCalledTimes(1);
            expect(response.status).toHaveBeenCalledWith(202);
        });

        it('should call api with parsed payload', async() => {
            const updateOneMock = jest.fn().mockImplementation(() => Promise.resolve({ result: { n: 1 } }));
            const db = createDbMock({ updateOne: updateOneMock });
            const launchApi = createLaunchApiServiceMock({ db });
            jest.spyOn(launchApi, 'updateLaunch');
            const controller = new UpdateLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const payload = { title: 'test-title', tags: undefined, test: 1, additional: true };
            const request = { params: { id: 'test-id' }, body: payload } as unknown as ControllerRequest;
            const response = getMockedResponse();

            const expected = { title: 'test-title' };

            await handler(request, response);

            expect(launchApi.updateLaunch).toHaveBeenCalledTimes(1);
            expect(launchApi.updateLaunch).toHaveBeenCalledWith('fake-id', expected);
            expect(response.status).toHaveBeenCalledWith(202);
        });

        it('should throw NotFoundError when launch does not exists', async() => {
            const updateOneMock = jest.fn().mockImplementation(() => Promise.resolve({ result: { n: 0 } }));
            const db = createDbMock({ updateOne: updateOneMock });
            const launchApi = createLaunchApiServiceMock({ db });
            jest.spyOn(launchApi, 'updateLaunch');
            const controller = new UpdateLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const request = { params: { id: 'unknown' }, body: {} } as ControllerRequest;
            const response = getMockedResponse();

            const expected = new errors.NotFoundError();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });

        it('should throw InternalServerError when failed to create launch', async() => {
            const updateOneMock = jest.fn().mockImplementation(() => Promise.reject(new Error('unknown')));
            const db = createDbMock({ updateOne: updateOneMock });
            const launchApi = createLaunchApiServiceMock({ db });
            const controller = new UpdateLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const request = { params: { id: 'unknown' }, body: {} } as unknown as ControllerRequest;
            const response = getMockedResponse();

            const expected = new errors.InternalServerError('failed to update launch with id "fake-id": unknown');

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });
    });
});
