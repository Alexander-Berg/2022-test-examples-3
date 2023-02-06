import debugFactory, { IDebugger } from 'debug';
import { Response } from 'express';
import TestpalmApi from '@yandex-int/testpalm-api';
import { GotInstance, GotJSONFn } from '@yandex-int/si.ci.requests';

import LaunchApiService, {
    Dependencies as LaunchApiServiceDependencies,
    LaunchExistingError,
} from '../../../lib/api/launch';
import TestpalmApiService from '../../../lib/api/testpalm';
import BookingApiService, { DevicesDownloader } from '../../../lib/api/booking';
import { LaunchId } from '../../../models/Launch';
import * as errors from '../../errors';

import GetLaunchProjectInfoController, { ControllerRequest } from './get-launch-project-info';

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
        getTestSuite: jest.fn(),
        getTestCasesWithPost: jest.fn(),
        getProject: jest.fn(),
    } as unknown as TestpalmApi;
}

function createTestpalmApiServiceMock() {
    const debug = debugFactory('test');
    const testpalmApi = createTestpalmApiMock();

    return new TestpalmApiService({ debug, testpalmApi });
}

function createBookingApiService() {
    const debug = debugFactory('test');
    const requests = jest.fn().mockResolvedValue({ body: [] }) as unknown as GotInstance<GotJSONFn>;
    const devicesDownloader = {
        downloadDevices: jest.fn().mockResolvedValue([
            { environment: 'yabro', type: 'desktop', united_environment: 'YaBrowser' },
            { environment: 'ios_safari_7', type: 'touch-phone', united_environment: 'Safari' },
            { environment: 'ios_safari_9', type: 'touch-phone', united_environment: 'Safari' },
        ]),
    } as unknown as DevicesDownloader;

    return new BookingApiService({ debug, requests, devicesDownloader });
}

function getMockedResponse() {
    return {
        status: jest.fn().mockReturnThis(),
        json: jest.fn().mockReturnThis(),
        end: jest.fn(),
    } as unknown as Response;
}

describe('GetLaunchProjectInfoController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getHandler', () => {
        let launchApi: LaunchApiService;
        let testpalmApi: TestpalmApiService;
        let bookingApi: BookingApiService;

        let getLaunchByIdMock: jest.Mock;
        let getTestPalmProjectForLaunchMock: jest.Mock;

        let request: ControllerRequest;

        beforeEach(() => {
            getLaunchByIdMock = jest.fn().mockImplementation((id: LaunchId) => Promise.resolve({
                _id: id,
                project: 'serp-js',
            }));

            getTestPalmProjectForLaunchMock = jest.fn().mockResolvedValue({
                settings: {
                    environments: [
                        { title: 'yabro' },
                        { title: 'ios_safari_7' },
                        { title: 'ios_safari_11' },
                    ],
                    environmentGroups: [],
                    runnerConfigItems: [
                        { runnerId: 'bulkcurl', title: 'United' },
                        { runnerId: 'serpFormRunner', title: 'CrowdTest Runner' },
                    ],
                },
            });

            launchApi = createLaunchApiServiceMock();
            testpalmApi = createTestpalmApiServiceMock();
            bookingApi = createBookingApiService();

            launchApi.getLaunchById = getLaunchByIdMock;
            testpalmApi.getTestPalmProjectForLaunch = getTestPalmProjectForLaunchMock;

            request = {
                params: { id: '1' },
            } as ControllerRequest;
        });

        it('should return function', () => {
            const controller = new GetLaunchProjectInfoController({ debug, launchApi, testpalmApi, bookingApi });

            const expected = 'function';

            const actual = controller.getHandler();

            expect(typeof actual).toBe(expected);
        });

        it('should return 200 with platforms and runners', async() => {
            const controller = new GetLaunchProjectInfoController({ debug, launchApi, testpalmApi, bookingApi });
            const handler = controller.getHandler();

            const response = getMockedResponse();

            const expected = {
                platforms: [
                    {
                        platform: 'desktop',
                        environments: [
                            'yabro',
                        ],
                    },
                    {
                        platform: 'touch-phone',
                        environments: [
                            'ios_safari_7',
                        ],
                    },
                ],
                environmentsGroups: [],
                runners: [
                    {
                        runnerId: 'bulkcurl',
                        title: 'United',
                    },
                ],
            };

            await handler(request, response);

            expect(response.status).toHaveBeenCalledWith(200);
            expect(response.json).toHaveBeenCalledWith(expected);
        });

        it('should throw NotFoundError when launch does not exists', () => {
            getLaunchByIdMock.mockRejectedValue(new LaunchExistingError('unknown' as unknown as LaunchId));

            const controller = new GetLaunchProjectInfoController({ debug, launchApi, testpalmApi, bookingApi });
            const response = getMockedResponse();
            const expected = new errors.NotFoundError();

            const handler = controller.getHandler();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });

        it('should throw InternalServerError when unknown error occurred', () => {
            getLaunchByIdMock.mockRejectedValue(new Error('unknown'));

            const launchApi = createLaunchApiServiceMock();
            const testpalmApi = createTestpalmApiServiceMock();

            launchApi.getLaunchById = getLaunchByIdMock;
            const controller = new GetLaunchProjectInfoController({ debug, launchApi, testpalmApi, bookingApi });

            const expected = new errors.InternalServerError('unknown');

            const response = getMockedResponse();

            const handler = controller.getHandler();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });
    });
});
