import debugFactory, { IDebugger } from 'debug';
import { Response } from 'express';

import LaunchApiService, {
    Dependencies as LaunchApiServiceDependencies,
    LaunchExistingError,
} from '../../../lib/api/launch';
import { LaunchId } from '../../../models/Launch';

import GetLaunchController, { ControllerRequest } from './get-launch';

import * as errors from '../../errors';

import { getMockedDBClass, createCollectionStub } from '../../../../test/helpers/mocked-db';

function createDbMock() {
    const collectionStub = createCollectionStub(jest.fn);
    const MockedDB = getMockedDBClass(collectionStub);

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

describe('GetLaunchController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getHandler', () => {
        it('should return function', () => {
            const launchApi = createLaunchApiServiceMock();
            const controller = new GetLaunchController({ debug, launchApi });

            const expected = 'function';

            const actual = controller.getHandler();

            expect(typeof actual).toBe(expected);
        });

        it('should return 200 when launch exists', async() => {
            const launchApi = createLaunchApiServiceMock();
            jest.spyOn(launchApi, 'getLaunchById');
            const controller = new GetLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const request = { params: { id: '1' } } as ControllerRequest;
            const response = getMockedResponse();

            await handler(request, response);

            expect(launchApi.getLaunchById).toHaveBeenCalledTimes(1);
            expect(response.status).toHaveBeenCalledWith(200);
        });

        it('should return public launch', async() => {
            const launchApi = createLaunchApiServiceMock();
            jest.spyOn(launchApi, 'getLaunchById');
            jest.spyOn(launchApi, 'toPublic');
            const controller = new GetLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const request = { params: { id: '1' } } as ControllerRequest;
            const response = getMockedResponse();

            await handler(request, response);

            expect(launchApi.getLaunchById).toHaveBeenCalledTimes(1);
            expect(launchApi.toPublic).toHaveBeenCalledTimes(1);
        });

        it('should throw NotFoundError when launch does not exists', () => {
            const getLaunchByIdMock = jest.fn().mockImplementation(() => {
                return Promise.reject(new LaunchExistingError('unknown' as unknown as LaunchId));
            });

            const launchApi = createLaunchApiServiceMock();
            launchApi.getLaunchById = getLaunchByIdMock;
            const controller = new GetLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const request = { params: { id: 'unknown' } } as ControllerRequest;
            const response = getMockedResponse();

            const expected = new errors.NotFoundError();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });

        it('should throw InternalServerError when unknown error occurred', () => {
            const getLaunchByIdMock = jest.fn().mockImplementation(() => Promise.reject(new Error('unknown')));

            const launchApi = createLaunchApiServiceMock();
            launchApi.getLaunchById = getLaunchByIdMock;
            const controller = new GetLaunchController({ debug, launchApi });
            const handler = controller.getHandler();

            const request = { params: { id: '1' } } as ControllerRequest;
            const response = getMockedResponse();
            const expected = new errors.InternalServerError();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });
    });
});
