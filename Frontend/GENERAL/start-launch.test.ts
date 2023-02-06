import debugFactory, { IDebugger } from 'debug';
import { Response } from 'express';
import TestpalmApi from '@yandex-int/testpalm-api';

import LaunchApiService, {
    Dependencies as LaunchApiServiceDependencies,
    LaunchExistingError,
} from '../../../lib/api/launch';
import TestpalmApiService from '../../../lib/api/testpalm';
import { LaunchId } from '../../../models/Launch';
import * as errors from '../../errors';

import StartLaunchController, { ControllerRequest } from './start-launch';

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

function createTestpalmApiMock() {
    return {
        addTestRun: jest.fn(),
    } as unknown as TestpalmApi;
}

function createTestpalmApiServiceMock() {
    const debug = debugFactory('test');
    const testpalmApi = createTestpalmApiMock();

    return new TestpalmApiService({ debug, testpalmApi });
}

function getMockedResponse() {
    return {
        status: jest.fn().mockReturnThis(),
        json: jest.fn().mockReturnThis(),
        end: jest.fn(),
    } as unknown as Response;
}

describe('StartLaunchController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getValidateMiddleware', () => {
        it('should return function', () => {
            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });

            const expected = 'function';

            const actual = controller.getValidateMiddleware();

            expect(typeof actual).toBe(expected);
        });

        it('should call next function when payload are valid', () => {
            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });

            const body = {
                runnerConfig: {
                    runnerId: 'bulkcurl',
                    title: 'runner',
                },
            };

            const next = jest.fn();

            const middleware = controller.getValidateMiddleware();

            middleware({ body } as unknown as ControllerRequest, {} as Response, next);

            expect(next).toHaveBeenCalled();
            expect(next).toHaveBeenCalledWith();
        });

        it('should return InvalidPayloadError when payload are invalid', () => {
            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();
            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });

            const body = { runnerConfig: null };
            const response = getMockedResponse();
            const next = jest.fn();

            const expected = new errors.InvalidPayloadError(
                'failed to validate schema: data.runnerConfig should be object',
            );

            const middleware = controller.getValidateMiddleware();

            middleware({ body } as unknown as ControllerRequest, response, next);

            expect(next).toBeCalledWith(expected);
        });
    });

    describe('.getHandler', () => {
        let launchApi: LaunchApiService;
        let testpalmApi: TestpalmApiService;

        let getLaunchByIdMock: jest.Mock;
        let updateLaunchMock: jest.Mock;
        let createRunFromLaunchMock: jest.Mock;

        let request: ControllerRequest;

        beforeEach(() => {
            getLaunchByIdMock = jest.fn().mockImplementation((id: LaunchId) => Promise.resolve({ _id: id }));
            updateLaunchMock = jest.fn().mockImplementation(() => Promise.resolve());
            createRunFromLaunchMock = jest.fn().mockImplementation(() => Promise.resolve([{ id: '123' }]));

            launchApi = createLaunchApiServiceMock();
            testpalmApi = createTestpalmApiServiceMock();

            launchApi.getLaunchById = getLaunchByIdMock;
            launchApi.updateLaunch = updateLaunchMock;
            testpalmApi.createRunFromLaunch = createRunFromLaunchMock;

            request = {
                params: { id: '1' },
                body: {
                    runnerConfig: {
                        runnerId: 'bulkcurl',
                        title: 'runner',
                    },
                },
            } as ControllerRequest;
        });

        it('should return function', () => {
            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });

            const expected = 'function';

            const actual = controller.getHandler();

            expect(typeof actual).toBe(expected);
        });

        it('should return 202 with test-runs ids when launch successfully started', async() => {
            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const response = getMockedResponse();

            const expected = { testRunIds: ['123'] };

            await handler(request, response);

            expect(response.status).toHaveBeenCalledWith(202);
            expect(response.json).toHaveBeenCalledWith(expected);
        });

        it('should call all the APIs', async() => {
            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });
            const handler = controller.getHandler();

            const response = getMockedResponse();
            const expectedId = 'fake-id';

            await handler(request, response);

            expect(launchApi.getLaunchById).toHaveBeenCalledTimes(1);
            expect(testpalmApi.createRunFromLaunch).toHaveBeenCalledTimes(1);
            expect(launchApi.updateLaunch).toHaveBeenCalledTimes(1);

            expect(launchApi.getLaunchById).toHaveBeenCalledWith(expectedId);
            expect(testpalmApi.createRunFromLaunch).toHaveBeenCalledWith(
                { _id: expectedId },
                { runnerId: 'bulkcurl', title: 'runner' },
            );
            expect(launchApi.updateLaunch).toHaveBeenCalledWith(expectedId, {
                status: 'started',
                testRunIds: ['123'],
            });
        });

        it('should throw NotFoundError when launch does not exists', () => {
            getLaunchByIdMock.mockImplementation(() => {
                return Promise.reject(new LaunchExistingError('unknown' as unknown as LaunchId));
            });

            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });
            const response = getMockedResponse();
            const expected = new errors.NotFoundError();

            const handler = controller.getHandler();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });

        it('should throw InternalServerError when unknown error occurred', () => {
            getLaunchByIdMock.mockImplementation(() => Promise.reject(new Error('unknown')));

            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();

            launchApi.getLaunchById = getLaunchByIdMock;
            const controller = new StartLaunchController({ debug, launchApi, testpalmApi });

            const expected = new errors.InternalServerError('unknown');

            const response = getMockedResponse();

            const handler = controller.getHandler();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });
    });
});
