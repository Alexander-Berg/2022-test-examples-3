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

import CalculateLaunchLoadController, { ControllerRequest } from './calculate-launch-load';

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
    } as unknown as TestpalmApi;
}

function createTestpalmApiServiceMock() {
    const debug = debugFactory('test');
    const testpalmApi = createTestpalmApiMock();

    return new TestpalmApiService({ debug, testpalmApi });
}

function createBookingApiService() {
    const debug = debugFactory('test');
    const requests = jest.fn().mockResolvedValue({
        body: [{ name: 'YaBrowser', code: '1' }],
    }) as unknown as GotInstance<GotJSONFn>;
    const devicesDownloader = {
        downloadDevices: jest.fn().mockResolvedValue([
            { environment: 'yabro', type: 'desktop', united_environment: 'YaBrowser' },
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

describe('CalculateLaunchLoadController', () => {
    let debug: IDebugger;

    beforeEach(() => {
        debug = debugFactory('test');
    });

    describe('.getHandler', () => {
        let launchApi: LaunchApiService;
        let testpalmApi: TestpalmApiService;
        let bookingApi: BookingApiService;

        let getLaunchByIdMock: jest.Mock;
        let getTestcasesForLaunchMock: jest.Mock;

        let request: ControllerRequest;

        beforeEach(() => {
            getLaunchByIdMock = jest.fn().mockImplementation((id: LaunchId) => Promise.resolve({
                _id: id,
                platform: 'desktop',
                environments: ['Chrome', 'YaBro'],
            }));
            getTestcasesForLaunchMock = jest.fn().mockImplementation(() => {
                return Promise.resolve(Array(5).fill({ estimatedTime: 6596000 }));
            });

            launchApi = createLaunchApiServiceMock();
            testpalmApi = createTestpalmApiServiceMock();
            bookingApi = createBookingApiService();

            launchApi.getLaunchById = getLaunchByIdMock;
            testpalmApi.getTestcasesForLaunch = getTestcasesForLaunchMock;

            request = {
                params: { id: '1' },
            } as ControllerRequest;
        });

        it('should return function', () => {
            const controller = new CalculateLaunchLoadController({ debug, launchApi, testpalmApi, bookingApi });

            const expected = 'function';

            const actual = controller.getHandler();

            expect(typeof actual).toBe(expected);
        });

        it('should return 200 with calculated load and environments', async() => {
            const controller = new CalculateLaunchLoadController({ debug, launchApi, testpalmApi, bookingApi });
            const handler = controller.getHandler();

            const response = getMockedResponse();

            const expected = {
                load: 28,
                environments: {
                    Chrome: {
                        ratio: 0.5,
                        launchEnvironment: 'Chrome',
                        bookingEnvironmentName: null,
                        bookingEnvironmentCode: null,
                    },
                    YaBro: {
                        ratio: 0.5,
                        launchEnvironment: 'YaBro',
                        bookingEnvironmentName: 'YaBrowser',
                        bookingEnvironmentCode: '1',
                    },
                },
            };

            await handler(request, response);

            expect(response.status).toHaveBeenCalledWith(200);
            expect(response.json).toHaveBeenCalledWith(expected);
        });

        it('should throw NotFoundError when launch does not exists', () => {
            getLaunchByIdMock.mockImplementation(() => {
                return Promise.reject(new LaunchExistingError('unknown' as unknown as LaunchId));
            });

            const controller = new CalculateLaunchLoadController({ debug, launchApi, testpalmApi, bookingApi });
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
            const controller = new CalculateLaunchLoadController({ debug, launchApi, testpalmApi, bookingApi });

            const expected = new errors.InternalServerError('unknown');

            const response = getMockedResponse();

            const handler = controller.getHandler();

            const actual = handler(request, response);

            return expect(actual).rejects.toThrow(expected);
        });
    });
});
